package com.example

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import com.example.coordinator.NavitelRestartCoordinator
import com.example.data.AppLogger
import com.example.data.AppSettingsRepository
import com.example.executor.AdbPrivilegedCommandExecutor
import com.example.executor.FakePrivilegedCommandExecutor
import com.example.executor.PrivilegedCommandExecutor
import com.example.executor.UnprivilegedTestExecutor
import com.example.model.ExecutorMode
import com.example.service.NavitelMonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class NavitelApp : Application() {

    companion object {
        lateinit var instance: NavitelApp
            private set

        private val _isServiceRunning = MutableStateFlow(false)
        val isServiceRunning: StateFlow<Boolean> = _isServiceRunning.asStateFlow()

        fun setServiceRunning(running: Boolean) {
            _isServiceRunning.value = running
        }

        fun startMonitorService(context: Context) {
            try {
                val intent = Intent(context, NavitelMonitorService::class.java).apply {
                    action = NavitelMonitorService.ACTION_START_SERVICE
                }
                ContextCompat.startForegroundService(context, intent)
                AppLogger.i("APP", "Đã gửi lệnh khởi động dịch vụ giám sát chạy nền.")
            } catch (e: Exception) {
                AppLogger.e("APP", "Không thể khởi động dịch vụ giám sát: ${e.message}", e)
            }
        }

        fun stopMonitorService(context: Context) {
            try {
                val intent = Intent(context, NavitelMonitorService::class.java).apply {
                    action = NavitelMonitorService.ACTION_STOP_SERVICE
                }
                context.startService(intent)
                AppLogger.i("APP", "Đã gửi lệnh dừng dịch vụ giám sát.")
            } catch (e: Exception) {
                AppLogger.e("APP", "Không thể gửi lệnh dừng dịch vụ: ${e.message}", e)
            }
        }
    }

    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    lateinit var settingsRepository: AppSettingsRepository
        private set

    lateinit var fakeExecutor: FakePrivilegedCommandExecutor
        private set

    lateinit var unprivilegedExecutor: UnprivilegedTestExecutor
        private set

    lateinit var adbExecutor: AdbPrivilegedCommandExecutor
        private set

    lateinit var coordinator: NavitelRestartCoordinator
        private set

    private val _activeExecutorMode = MutableStateFlow(ExecutorMode.SIMULATION_FAKE)
    val activeExecutorMode: StateFlow<ExecutorMode> = _activeExecutorMode.asStateFlow()

    override fun onCreate() {
        super.onCreate()
        instance = this

        settingsRepository = AppSettingsRepository(this)
        fakeExecutor = FakePrivilegedCommandExecutor(simulatedDisplayId = 21)
        unprivilegedExecutor = UnprivilegedTestExecutor()
        adbExecutor = AdbPrivilegedCommandExecutor(this)

        coordinator = NavitelRestartCoordinator {
            getActiveExecutor()
        }

        // Sync executor mode and simulated display from settings
        applicationScope.launch {
            settingsRepository.executorMode.collectLatest { mode ->
                _activeExecutorMode.value = mode
                AppLogger.i("APP", "Đã chuyển chế độ thực thi lệnh sang: ${mode.displayName}")
            }
        }

        applicationScope.launch {
            settingsRepository.simulatedDisplayId.collectLatest { displayId ->
                fakeExecutor.setSimulatedDisplay(displayId)
            }
        }

        AppLogger.i("APP", "Ứng dụng Navitel PiP Restarter đã khởi tạo.")
    }

    fun getActiveExecutor(): PrivilegedCommandExecutor {
        return when (_activeExecutorMode.value) {
            ExecutorMode.SIMULATION_FAKE -> fakeExecutor
            ExecutorMode.UNPRIVILEGED_TEST -> unprivilegedExecutor
            ExecutorMode.ADB_PRIVILEGED -> adbExecutor
        }
    }
}
