package com.example.executor

import com.example.model.CommandResult
import com.example.model.ExecutorMode

/**
 * Safe unprivileged test executor.
 * Strictly adheres to the rule that standard app UID cannot execute `am force-stop`
 * or launch tasks on secondary displays. Does NOT fake privileged execution.
 */
class UnprivilegedTestExecutor : PrivilegedCommandExecutor {

    override val mode: ExecutorMode = ExecutorMode.UNPRIVILEGED_TEST

    override suspend fun isPrivilegedAvailable(): Boolean = false

    override suspend fun getStatusDescription(): String =
        "Chưa có quyền ADB shell (Chế độ kiểm tra - Không thực thi lệnh hệ thống)"

    override suspend fun execute(command: String): CommandResult {
        val trimmed = command.trim()
        val startTime = System.currentTimeMillis()

        return when {
            trimmed.startsWith("id") -> {
                // Show current standard untrusted app UID
                val currentProcessUid = android.os.Process.myUid()
                CommandResult(
                    exitCode = 0,
                    stdout = "uid=$currentProcessUid(u0_a${currentProcessUid % 100000}) gid=$currentProcessUid [CẢNH BÁO: UID này là quyền ứng dụng thường, KHÔNG PHẢI uid=2000(shell)]",
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("dumpsys") -> {
                CommandResult(
                    exitCode = 1,
                    stdout = "",
                    stderr = "Permission Denial: cannot dump activity from unprivileged UID without DUMP permission.",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("am force-stop") -> {
                CommandResult(
                    exitCode = -1,
                    stdout = "[CHẾ ĐỘ KIỂM TRA] Lệnh dự kiến: $command\nKhông thực thi vì chưa có kết nối ADB shell đặc quyền.",
                    stderr = "Error: Permission Denial: am force-stop requires android.permission.FORCE_STOP_PACKAGES or shell UID.",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("am start") -> {
                CommandResult(
                    exitCode = -1,
                    stdout = "[CHẾ ĐỘ KIỂM TRA] Lệnh dự kiến: $command\nKhông thực thi vì chưa có quyền điều khiển secondary display.",
                    stderr = "Error: Permission Denial: starting activity on secondary display requires privileged permissions.",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            trimmed.startsWith("pidof") -> {
                CommandResult(
                    exitCode = 0,
                    stdout = "pidof yêu cầu quyền đặc quyền để đọc proc của ứng dụng khác.",
                    stderr = "",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
            else -> {
                CommandResult(
                    exitCode = -1,
                    stdout = "[CHẾ ĐỘ KIỂM TRA] Bỏ qua thực thi: $command",
                    stderr = "Chưa kết nối ADB shell.",
                    executedCommand = command,
                    durationMs = System.currentTimeMillis() - startTime
                )
            }
        }
    }
}
