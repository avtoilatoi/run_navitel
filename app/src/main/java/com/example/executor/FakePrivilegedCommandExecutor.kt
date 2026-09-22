package com.example.executor

import com.example.model.CommandResult
import com.example.model.ExecutorMode
import kotlinx.coroutines.delay

/**
 * Simulator executor specifically designed for the Google AI Studio cloud emulator environment.
 * Allows full verification of UI, restart coordination workflow, dumpsys detection across
 * dynamic display IDs (21, 23), and error handling without requiring a physical car box.
 */
class FakePrivilegedCommandExecutor(
    var simulatedDisplayId: Int = 21,
    var simulateMissingNavitel: Boolean = false
) : PrivilegedCommandExecutor {

    override val mode: ExecutorMode = ExecutorMode.SIMULATION_FAKE

    private var simulatedPid: Int? = 14205
    private var forceStopCount = 0
    private var startCount = 0

    override suspend fun isPrivilegedAvailable(): Boolean = true

    override suspend fun getStatusDescription(): String =
        "Đang giả lập ADB Shell (uid=2000/Display #$simulatedDisplayId) - Sẵn sàng thử nghiệm an toàn"

    fun setSimulatedDisplay(displayId: Int) {
        simulatedDisplayId = displayId
    }

    override suspend fun execute(command: String): CommandResult {
        val trimmed = command.trim()
        val startTime = System.currentTimeMillis()
        delay(120) // Simulate brief execution latency

        return when {
            trimmed == "id" -> {
                CommandResult(
                    exitCode = 0,
                    stdout = "uid=2000(shell) gid=2000(shell) groups=2000(shell),1004(input),1007(log),1011(adb),1015(sdcard_rw),1028(sdcard_r),3003(inet)",
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("dumpsys activity activities") -> {
                val dumpsysContent = if (simulateMissingNavitel) {
                    """
                    ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)
                    Display #0 (type=INTERNAL):
                      Stack #1: Task id #1 cmp=com.leco.launcher/.MainActivity
                    Display #22 (zone=pip1):
                      Stack #2: Task id #2 cmp=com.github.slashmax.aabrowser/.MainActivity
                    """.trimIndent()
                } else {
                    """
                    ACTIVITY MANAGER ACTIVITIES (dumpsys activity activities)
                    Display #0 (type=INTERNAL):
                      Stack #1:
                        Task id #1
                          Task{a1b2c3 #1 type=home cmp=com.leco.launcher/.MainActivity}
                    Display #22 (zone=pip1):
                      Stack #2:
                        Task id #20
                          realActivity=com.github.slashmax.aabrowser/.MainActivity
                          baseIntent={act=android.intent.action.MAIN cmp=com.github.slashmax.aabrowser/.MainActivity}
                    Display #$simulatedDisplayId (zone=pip2_navitel):
                      Stack #3:
                        Task id #25
                          realActivity=com.navitel/.app.MainActivity
                          baseIntent={act=android.intent.action.MAIN flg=0x10000000 cmp=com.navitel/.app.MainActivity}
                          topActivity=ComponentInfo{com.navitel/com.navitel.app.MainActivity}
                    """.trimIndent()
                }
                CommandResult(
                    exitCode = 0,
                    stdout = dumpsysContent,
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("am force-stop com.navitel") -> {
                forceStopCount++
                simulatedPid = null
                CommandResult(
                    exitCode = 0,
                    stdout = "",
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("am start") -> {
                startCount++
                simulatedPid = (18000..19999).random()
                val targetDisplay = Regex("""--display\s+(\d+)""").find(trimmed)?.groupValues?.get(1) ?: "$simulatedDisplayId"
                val output = """
                    Starting: Intent { act=android.intent.action.MAIN flg=0x10000000 cmp=com.navitel/.app.MainActivity }
                    Status: ok
                    LaunchState: COLD
                    Activity: com.navitel/.app.MainActivity
                    TotalTime: 395
                    WaitTime: 402
                    TargetDisplay: $targetDisplay
                    Complete
                """.trimIndent()
                CommandResult(
                    exitCode = 0,
                    stdout = output,
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("pidof com.navitel") -> {
                val pidOutput = simulatedPid?.toString() ?: ""
                CommandResult(
                    exitCode = if (simulatedPid != null) 0 else 1,
                    stdout = pidOutput,
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            else -> {
                CommandResult(
                    exitCode = 0,
                    stdout = "Lệnh mô phỏng hoàn tất: $command",
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }
}
