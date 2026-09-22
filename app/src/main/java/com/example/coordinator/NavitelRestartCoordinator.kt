package com.example.coordinator

import com.example.data.AppLogger
import com.example.detector.NavitelDisplayDetector
import com.example.executor.PrivilegedCommandExecutor
import com.example.model.CommandResult
import com.example.model.RestartAttempt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Coordinates the safe detection, cold restart, and verification of Navitel Navigation
 * on its dynamic secondary PiP display.
 *
 * Strict safety rules:
 * - NEVER force-stops if display ID is null.
 * - NEVER force-stops any package other than com.navitel.
 * - Does NOT hardcode display ID.
 * - Retries display detection up to 3 times (5 seconds apart) if null.
 * - Checks "Status: ok" and prioritizes "LaunchState: COLD".
 */
class NavitelRestartCoordinator(
    private val getExecutor: () -> PrivilegedCommandExecutor
) {
    private val mutex = Mutex()

    private val _lastDetectedDisplay = MutableStateFlow<Int?>(null)
    val lastDetectedDisplay: StateFlow<Int?> = _lastDetectedDisplay.asStateFlow()

    private val _lastAttempt = MutableStateFlow<RestartAttempt?>(null)
    val lastAttempt: StateFlow<RestartAttempt?> = _lastAttempt.asStateFlow()

    private val _isRestarting = MutableStateFlow(false)
    val isRestarting: StateFlow<Boolean> = _isRestarting.asStateFlow()

    /**
     * Executes `dumpsys activity activities` to detect which display ID Navitel currently occupies.
     * Retries up to [maxRetries] times with [retryDelayMs] between attempts.
     */
    suspend fun detectDisplayWithRetry(
        maxRetries: Int = 3,
        retryDelayMs: Long = 5000L
    ): Int? = withContext(Dispatchers.IO) {
        val executor = getExecutor()
        AppLogger.i("COORDINATOR", "Bắt đầu dò tìm display của Navitel (Chế độ: ${executor.mode.name})...")

        for (attempt in 1..maxRetries) {
            val dumpsysCmd = "dumpsys activity activities"
            val result = try {
                executor.execute(dumpsysCmd)
            } catch (e: Exception) {
                AppLogger.e("COORDINATOR", "Lỗi khi chạy lệnh dò display (lần $attempt): ${e.message}", e)
                CommandResult(
                    exitCode = -1,
                    stdout = "",
                    stderr = e.message ?: "Exception",
                    executedCommand = dumpsysCmd
                )
            }

            AppLogger.cmd(dumpsysCmd, result)

            if (result.isSuccess && result.stdout.isNotBlank()) {
                val displayId = NavitelDisplayDetector.findNavitelDisplay(result.stdout)
                if (displayId != null) {
                    _lastDetectedDisplay.value = displayId
                    AppLogger.success(
                        "COORDINATOR",
                        "Đã xác định Navitel đang chạy trên Display #$displayId (thành công ở lần thử $attempt/$maxRetries)"
                    )
                    return@withContext displayId
                } else {
                    AppLogger.w(
                        "COORDINATOR",
                        "Không tìm thấy task com.navitel trên bất kỳ display nào (lần thử $attempt/$maxRetries)."
                    )
                }
            } else {
                AppLogger.w(
                    "COORDINATOR",
                    "dumpsys không trả về dữ liệu hợp lệ (lần $attempt/$maxRetries). Lỗi: ${result.stderr}"
                )
            }

            if (attempt < maxRetries) {
                AppLogger.i("COORDINATOR", "Chờ ${retryDelayMs / 1000}s trước khi thử lại lần ${attempt + 1}...")
                delay(retryDelayMs)
            }
        }

        AppLogger.e("COORDINATOR", "Không thể tìm thấy display ID cho Navitel sau $maxRetries lần thử. HỦY lệnh dừng để bảo vệ hệ thống!")
        null
    }

    /**
     * Executes the strict restart sequence:
     * 1. Detect display (with retry)
     * 2. ABORT if display is null (NO force-stop allowed without display ID)
     * 3. Record PID before
     * 4. am force-stop com.navitel
     * 5. Wait 2000 ms
     * 6. am start --display DISPLAY_ID -W -f 0x10000000 -n com.navitel/.app.MainActivity
     * 7. Verify result for "Status: ok" and "LaunchState: COLD"
     * 8. Record PID after and log summary
     */
    suspend fun restartNavitelCold(
        explicitDisplayId: Int? = null,
        reason: String = "Yêu cầu thủ công"
    ): RestartAttempt = mutex.withLock {
        withContext(Dispatchers.IO) {
            _isRestarting.value = true
            val executor = getExecutor()
            AppLogger.i("COORDINATOR", "=== BẮT ĐẦU QUY TRÌNH KHỞI ĐỘNG LẠNH NAVITEL ($reason) ===")

            try {
                // Step 1: Detect display ID
                val displayId = explicitDisplayId ?: detectDisplayWithRetry(maxRetries = 3, retryDelayMs = 5000L)

                // Step 2: Strict safety barrier - do NOT stop Navitel if display ID could not be detected!
                if (displayId == null) {
                    val errorAttempt = RestartAttempt(
                        displayId = null,
                        pidBefore = null,
                        pidAfter = null,
                        statusOk = false,
                        isColdLaunch = false,
                        exitCode = -1,
                        message = "Không tìm thấy task Navitel hoặc display ID. ĐÃ HỦY lệnh force-stop theo quy tắc an toàn."
                    )
                    _lastAttempt.value = errorAttempt
                    AppLogger.e("COORDINATOR", errorAttempt.message)
                    return@withContext errorAttempt
                }

                // Step 3: Check PID before
                val pidBeforeResult = executor.execute("pidof com.navitel")
                val pidBefore = pidBeforeResult.stdout.trim().ifBlank { null }
                AppLogger.i("COORDINATOR", "PID trước khi dừng: ${pidBefore ?: "Chưa chạy hoặc không lấy được"}")

                // Step 4: am force-stop com.navitel
                val forceStopCmd = "am force-stop com.navitel"
                AppLogger.i("COORDINATOR", "Thực thi dừng: $forceStopCmd")
                val stopResult = executor.execute(forceStopCmd)
                AppLogger.cmd(forceStopCmd, stopResult)

                if (!stopResult.isSuccess && stopResult.stderr.contains("Permission Denial", ignoreCase = true)) {
                    val permErrorAttempt = RestartAttempt(
                        displayId = displayId,
                        pidBefore = pidBefore,
                        pidAfter = null,
                        statusOk = false,
                        isColdLaunch = false,
                        exitCode = stopResult.exitCode,
                        message = "Thực thi thất bại: Quyền bị từ chối (${stopResult.stderr.trim()}). Cần có quyền ADB Shell!",
                        stderr = stopResult.stderr
                    )
                    _lastAttempt.value = permErrorAttempt
                    return@withContext permErrorAttempt
                }

                // Step 5: Wait mandatory 2000ms
                AppLogger.i("COORDINATOR", "Chờ 2000ms để hệ thống dọn dẹp task Navitel...")
                delay(2000L)

                // Step 6: am start on secondary display
                val startCmd = "am start --display $displayId -W -f 0x10000000 -n com.navitel/.app.MainActivity"
                AppLogger.i("COORDINATOR", "Thực thi khởi động: $startCmd")
                val startResult = executor.execute(startCmd)
                AppLogger.cmd(startCmd, startResult)

                // Step 7: Verify result
                val output = startResult.stdout
                val hasStatusOk = output.contains("Status: ok", ignoreCase = true)
                val isColdLaunch = output.contains("LaunchState: COLD", ignoreCase = true)

                // Step 8: Check PID after
                delay(300L)
                val pidAfterResult = executor.execute("pidof com.navitel")
                val pidAfter = pidAfterResult.stdout.trim().ifBlank { null }
                AppLogger.i("COORDINATOR", "PID sau khi khởi động: ${pidAfter ?: "Không lấy được"}")

                val isSuccess = startResult.isSuccess && (hasStatusOk || isColdLaunch)
                val statusMessage = buildString {
                    if (isSuccess) {
                        append("Khởi động Navitel thành công trên Display #$displayId. ")
                        if (isColdLaunch) append("[COLD LAUNCH] ")
                        if (hasStatusOk) append("[Status: OK] ")
                        if (pidBefore != null && pidAfter != null && pidBefore != pidAfter) {
                            append("(PID: $pidBefore -> $pidAfter)")
                        }
                    } else {
                        append("Khởi động Navitel thất bại hoặc không xác nhận được trạng thái. Exit: ${startResult.exitCode}")
                    }
                }

                val attempt = RestartAttempt(
                    displayId = displayId,
                    pidBefore = pidBefore,
                    pidAfter = pidAfter,
                    statusOk = hasStatusOk,
                    isColdLaunch = isColdLaunch,
                    exitCode = startResult.exitCode,
                    message = statusMessage,
                    stdout = startResult.stdout,
                    stderr = startResult.stderr
                )

                _lastAttempt.value = attempt
                if (isSuccess) {
                    AppLogger.success("COORDINATOR", statusMessage)
                } else {
                    AppLogger.e("COORDINATOR", statusMessage)
                }

                attempt
            } catch (e: Exception) {
                val errorMsg = "Ngoại lệ trong quá trình khởi động lại: ${e.message}"
                AppLogger.e("COORDINATOR", errorMsg, e)
                val errorAttempt = RestartAttempt(
                    displayId = _lastDetectedDisplay.value,
                    pidBefore = null,
                    pidAfter = null,
                    statusOk = false,
                    isColdLaunch = false,
                    exitCode = -1,
                    message = errorMsg,
                    stderr = e.stackTraceToString()
                )
                _lastAttempt.value = errorAttempt
                errorAttempt
            } finally {
                _isRestarting.value = false
                AppLogger.i("COORDINATOR", "=== KẾT THÚC QUY TRÌNH ===")
            }
        }
    }
}
