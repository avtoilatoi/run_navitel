package com.example.executor

import com.example.model.CommandResult
import com.example.model.ExecutorMode

interface PrivilegedCommandExecutor {
    val mode: ExecutorMode
    suspend fun execute(command: String): CommandResult
    suspend fun isPrivilegedAvailable(): Boolean
    suspend fun getStatusDescription(): String
}
