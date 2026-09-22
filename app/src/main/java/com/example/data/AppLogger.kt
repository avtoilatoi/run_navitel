package com.example.data

import com.example.model.CommandResult
import com.example.model.LogEntry
import com.example.model.LogLevel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppLogger {

    private const val MAX_LOG_SIZE = 300
    private val timeFormat = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault())

    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    @Synchronized
    fun log(level: LogLevel, tag: String, message: String) {
        val entry = LogEntry(
            level = level,
            tag = tag,
            message = message
        )
        val current = _logs.value.toMutableList()
        current.add(0, entry) // Newest first
        if (current.size > MAX_LOG_SIZE) {
            current.removeAt(current.size - 1)
        }
        _logs.value = current
    }

    fun i(tag: String, message: String) = log(LogLevel.INFO, tag, message)

    fun w(tag: String, message: String) = log(LogLevel.WARN, tag, message)

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        val fullMsg = if (throwable != null) "$message\n${throwable.stackTraceToString()}" else message
        log(LogLevel.ERROR, tag, fullMsg)
    }

    fun success(tag: String, message: String) = log(LogLevel.SUCCESS, tag, message)

    fun cmd(command: String, result: CommandResult) {
        val statusStr = if (result.isSuccess) "EXIT 0" else "EXIT ${result.exitCode}"
        val builder = StringBuilder("[$statusStr in ${result.durationMs}ms] $command")
        if (result.stdout.isNotBlank()) {
            builder.append("\nSTDOUT: ").append(result.stdout.trim())
        }
        if (result.stderr.isNotBlank()) {
            builder.append("\nSTDERR: ").append(result.stderr.trim())
        }
        log(if (result.isSuccess) LogLevel.COMMAND else LogLevel.ERROR, "SHELL", builder.toString())
    }

    fun clear() {
        _logs.value = emptyList()
    }

    fun getExportText(): String {
        return _logs.value.reversed().joinToString("\n") { entry ->
            val time = timeFormat.format(Date(entry.timestamp))
            "[$time] [${entry.level}] [${entry.tag}] ${entry.message}"
        }
    }
}
