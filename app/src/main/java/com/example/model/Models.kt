package com.example.model

enum class ExecutorMode(val displayName: String, val description: String) {
    SIMULATION_FAKE(
        "Chế độ giả lập (AI Studio)",
        "Mô phỏng dumpsys, PiP display 21/23 và phản hồi lệnh am an toàn trong môi trường ảo"
    ),
    UNPRIVILEGED_TEST(
        "Chế độ kiểm tra (Không đặc quyền)",
        "Chỉ hiển thị các lệnh shell dự kiến mà không thực thi, không thay đổi hệ thống"
    ),
    ADB_PRIVILEGED(
        "ADB Shell đặc quyền (Thiết bị thật)",
        "Kết nối ADB qua localhost (127.0.0.1) bằng UID shell để thực thi am force-stop và am start"
    )
}

data class CommandResult(
    val exitCode: Int,
    val stdout: String,
    val stderr: String,
    val executedCommand: String,
    val durationMs: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
) {
    val isSuccess: Boolean get() = exitCode == 0
}

enum class LogLevel {
    INFO,
    WARN,
    ERROR,
    COMMAND,
    SUCCESS
}

data class LogEntry(
    val id: Long = System.nanoTime(),
    val timestamp: Long = System.currentTimeMillis(),
    val level: LogLevel,
    val tag: String,
    val message: String
)

data class RestartAttempt(
    val timestamp: Long = System.currentTimeMillis(),
    val displayId: Int?,
    val pidBefore: String?,
    val pidAfter: String?,
    val statusOk: Boolean,
    val isColdLaunch: Boolean,
    val exitCode: Int,
    val message: String,
    val stdout: String = "",
    val stderr: String = ""
)

data class AdbConnectionState(
    val isConnected: Boolean = false,
    val host: String = "127.0.0.1",
    val port: Int = 5555,
    val pairingPort: Int = 5555,
    val shellUid: String? = null,
    val lastTestResult: String? = null,
    val hasKeys: Boolean = false
)
