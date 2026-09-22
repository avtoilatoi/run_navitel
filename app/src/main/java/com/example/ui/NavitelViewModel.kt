package com.example.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.NavitelApp
import com.example.data.AppLogger
import com.example.model.AdbConnectionState
import com.example.model.ExecutorMode
import com.example.model.LogEntry
import com.example.model.RestartAttempt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DashboardUiState(
    val isInternetReady: Boolean = false,
    val stabilizationCountdown: Int? = null,
    val isServiceRunning: Boolean = false,
    val detectedDisplayId: Int? = null,
    val lastRestartAttempt: RestartAttempt? = null,
    val isRestarting: Boolean = false,
    val executorMode: ExecutorMode = ExecutorMode.SIMULATION_FAKE,
    val simulatedDisplayId: Int = 21,
    val autoStartOnBoot: Boolean = false,
    val runOncePerBoot: Boolean = true,
    val checkOnAppStart: Boolean = false,
    val skipColdRestartWarning: Boolean = false,
    val adbState: AdbConnectionState = AdbConnectionState(),
    val showConfirmRestartDialog: Boolean = false,
    val showAdbDialog: Boolean = false,
    val infoMessage: String? = null
)

class NavitelViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as NavitelApp
    private val settings = app.settingsRepository
    private val coordinator = app.coordinator

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    val logs: StateFlow<List<LogEntry>> = AppLogger.logs

    init {
        // Observe Service Running state
        viewModelScope.launch {
            NavitelApp.isServiceRunning.collectLatest { running ->
                _uiState.value = _uiState.value.copy(isServiceRunning = running)
            }
        }

        // Observe Coordinator Display & Attempts
        viewModelScope.launch {
            coordinator.lastDetectedDisplay.collectLatest { displayId ->
                _uiState.value = _uiState.value.copy(detectedDisplayId = displayId)
            }
        }

        viewModelScope.launch {
            coordinator.lastAttempt.collectLatest { attempt ->
                _uiState.value = _uiState.value.copy(lastRestartAttempt = attempt)
            }
        }

        viewModelScope.launch {
            coordinator.isRestarting.collectLatest { restarting ->
                _uiState.value = _uiState.value.copy(isRestarting = restarting)
            }
        }

        // Observe Settings DataStore
        viewModelScope.launch {
            settings.autoStartOnBoot.collectLatest {
                _uiState.value = _uiState.value.copy(autoStartOnBoot = it)
            }
        }

        viewModelScope.launch {
            settings.runOncePerBoot.collectLatest {
                _uiState.value = _uiState.value.copy(runOncePerBoot = it)
            }
        }

        viewModelScope.launch {
            settings.checkOnAppStart.collectLatest {
                _uiState.value = _uiState.value.copy(checkOnAppStart = it)
            }
        }

        viewModelScope.launch {
            settings.skipColdRestartWarning.collectLatest {
                _uiState.value = _uiState.value.copy(skipColdRestartWarning = it)
            }
        }

        viewModelScope.launch {
            app.activeExecutorMode.collectLatest { mode ->
                _uiState.value = _uiState.value.copy(executorMode = mode)
            }
        }

        viewModelScope.launch {
            settings.simulatedDisplayId.collectLatest { displayId ->
                _uiState.value = _uiState.value.copy(simulatedDisplayId = displayId)
            }
        }

        // Initialize ADB state from saved host and port
        viewModelScope.launch {
            val host = settings.adbHost.first()
            val port = settings.adbPort.first()
            val hasKeys = app.adbExecutor.hasStoredKeys()
            _uiState.value = _uiState.value.copy(
                adbState = _uiState.value.adbState.copy(
                    host = host,
                    port = port,
                    hasKeys = hasKeys
                )
            )
        }
    }

    fun detectNavitelDisplay() {
        viewModelScope.launch {
            showMessage("Đang dò tìm display của Navitel qua dumpsys...")
            val displayId = coordinator.detectDisplayWithRetry(maxRetries = 3, retryDelayMs = 2000L)
            if (displayId != null) {
                showMessage("Đã tìm thấy Navitel tại Display #$displayId")
            } else {
                showMessage("Không tìm thấy task Navitel trên display nào!")
            }
        }
    }

    fun requestColdRestart() {
        if (_uiState.value.skipColdRestartWarning) {
            executeColdRestartInternal()
        } else {
            _uiState.value = _uiState.value.copy(showConfirmRestartDialog = true)
        }
    }

    fun dismissConfirmDialog() {
        _uiState.value = _uiState.value.copy(showConfirmRestartDialog = false)
    }

    fun confirmColdRestart(dontAskAgain: Boolean) {
        _uiState.value = _uiState.value.copy(showConfirmRestartDialog = false)
        if (dontAskAgain) {
            viewModelScope.launch {
                settings.setSkipColdRestartWarning(true)
            }
        }
        executeColdRestartInternal()
    }

    private fun executeColdRestartInternal() {
        viewModelScope.launch {
            showMessage("Bắt đầu quy trình khởi động lạnh Navitel...")
            val result = coordinator.restartNavitelCold(
                explicitDisplayId = null,
                reason = "Người dùng bấm khởi động lạnh"
            )
            if (result.statusOk || result.isColdLaunch) {
                showMessage("Khởi động Navitel thành công trên Display #${result.displayId}")
            } else {
                showMessage("Khởi động thất bại: ${result.message}")
            }
        }
    }

    fun startService() {
        NavitelApp.startMonitorService(getApplication())
        showMessage("Đã kích hoạt dịch vụ giám sát chạy nền.")
    }

    fun stopService() {
        NavitelApp.stopMonitorService(getApplication())
        showMessage("Đã dừng dịch vụ giám sát.")
    }

    fun toggleAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            settings.setAutoStartOnBoot(enabled)
            AppLogger.i("SETTINGS", "Tự động chạy sau khi khởi động: $enabled")
        }
    }

    fun toggleRunOncePerBoot(enabled: Boolean) {
        viewModelScope.launch {
            settings.setRunOncePerBoot(enabled)
            AppLogger.i("SETTINGS", "Chỉ chạy một lần mỗi lần khởi động: $enabled")
        }
    }

    fun toggleCheckOnAppStart(enabled: Boolean) {
        viewModelScope.launch {
            settings.setCheckOnAppStart(enabled)
            AppLogger.i("SETTINGS", "Kiểm tra Navitel khi mở app: $enabled")
        }
    }

    fun setExecutorMode(mode: ExecutorMode) {
        viewModelScope.launch {
            settings.setExecutorMode(mode)
            showMessage("Chuyển sang ${mode.displayName}")
        }
    }

    fun setSimulatedDisplayId(displayId: Int) {
        viewModelScope.launch {
            settings.setSimulatedDisplayId(displayId)
            app.fakeExecutor.setSimulatedDisplay(displayId)
            showMessage("Đã cấu hình mô phỏng Navitel ở Display #$displayId")
        }
    }

    fun openAdbDialog() {
        _uiState.value = _uiState.value.copy(showAdbDialog = true)
    }

    fun closeAdbDialog() {
        _uiState.value = _uiState.value.copy(showAdbDialog = false)
    }

    fun connectAdb(host: String, port: Int) {
        viewModelScope.launch {
            settings.setAdbConfig(host, port)
            showMessage("Đang kết nối ADB tới $host:$port...")
            val result = app.adbExecutor.connect(host, port)
            result.fold(
                onSuccess = { msg ->
                    _uiState.value = _uiState.value.copy(
                        adbState = _uiState.value.adbState.copy(
                            isConnected = true,
                            host = host,
                            port = port,
                            shellUid = "uid=2000(shell)",
                            lastTestResult = msg,
                            hasKeys = app.adbExecutor.hasStoredKeys()
                        )
                    )
                    showMessage(msg)
                    AppLogger.success("ADB", msg)
                },
                onFailure = { err ->
                    _uiState.value = _uiState.value.copy(
                        adbState = _uiState.value.adbState.copy(
                            isConnected = false,
                            lastTestResult = err.message
                        )
                    )
                    showMessage("Lỗi kết nối ADB: ${err.message}")
                    AppLogger.e("ADB", "Lỗi kết nối ADB: ${err.message}", err)
                }
            )
        }
    }

    fun disconnectAdb() {
        app.adbExecutor.disconnect()
        _uiState.value = _uiState.value.copy(
            adbState = _uiState.value.adbState.copy(
                isConnected = false,
                shellUid = null,
                lastTestResult = "Đã ngắt kết nối"
            )
        )
        showMessage("Đã ngắt kết nối ADB.")
    }

    fun testAdbId() {
        viewModelScope.launch {
            val executor = app.getActiveExecutor()
            val result = executor.execute("id")
            AppLogger.cmd("id", result)
            _uiState.value = _uiState.value.copy(
                adbState = _uiState.value.adbState.copy(
                    lastTestResult = if (result.isSuccess) result.stdout.trim() else result.stderr.trim()
                )
            )
            showMessage("Kiểm tra ID: ${result.stdout.trim().ifBlank { result.stderr }}")
        }
    }

    fun clearAdbKeys() {
        val success = app.adbExecutor.clearStoredKeys()
        _uiState.value = _uiState.value.copy(
            adbState = _uiState.value.adbState.copy(
                hasKeys = false,
                isConnected = false,
                shellUid = null
            )
        )
        if (success) {
            showMessage("Đã xóa toàn bộ khóa ADB lưu trên thiết bị.")
            AppLogger.i("ADB", "Đã xóa toàn bộ khóa ADB lưu trong bộ nhớ riêng.")
        }
    }

    fun clearLogs() {
        AppLogger.clear()
        showMessage("Đã xóa nhật ký.")
    }

    fun showMessage(msg: String) {
        _uiState.value = _uiState.value.copy(infoMessage = msg)
    }

    fun clearMessage() {
        _uiState.value = _uiState.value.copy(infoMessage = null)
    }
}
