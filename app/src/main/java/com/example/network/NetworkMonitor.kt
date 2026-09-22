package com.example.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import com.example.data.AppLogger
import com.example.data.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Monitors device Internet availability using ConnectivityManager.NetworkCallback.
 *
 * Requirements:
 * - Requires both NET_CAPABILITY_INTERNET and NET_CAPABILITY_VALIDATED.
 * - Remembers previous connection state.
 * - Only triggers on transition from disconnected -> connected.
 * - Waits 8 seconds for network stabilization and YouTube reload before recovery.
 * - Respects "Run only once per boot" to prevent repeated restarts on flaky Wi-Fi.
 */
class NetworkMonitor(
    private val context: Context,
    private val settingsRepository: AppSettingsRepository,
    private val scope: CoroutineScope,
    private val onInternetReadyTransition: suspend () -> Unit
) {
    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val _isInternetReady = MutableStateFlow(false)
    val isInternetReady: StateFlow<Boolean> = _isInternetReady.asStateFlow()

    private val _stabilizationCountdown = MutableStateFlow<Int?>(null)
    val stabilizationCountdown: StateFlow<Int?> = _stabilizationCountdown.asStateFlow()

    @Volatile
    private var previousHadInternet: Boolean = false

    private var callback: ConnectivityManager.NetworkCallback? = null
    private var stabilizationJob: Job? = null

    fun startMonitoring() {
        if (callback != null) return

        // Check initial network state
        val initialCapabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        val initialValidated = initialCapabilities != null &&
                initialCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                initialCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

        _isInternetReady.value = initialValidated
        previousHadInternet = initialValidated

        AppLogger.i(
            "NETWORK",
            "Bắt đầu theo dõi mạng. Trạng thái ban đầu: ${if (initialValidated) "ĐÃ KẾT NỐI & XÁC THỰC" else "CHƯA CÓ MẠNG"}"
        )

        // If user configured "Kiểm tra Navitel khi khởi động" and device already has internet
        scope.launch {
            val checkOnStart = settingsRepository.checkOnAppStart.first()
            if (checkOnStart && initialValidated && !AppSettingsRepository.hasExecutedThisBoot) {
                AppLogger.i("NETWORK", "Tùy chọn 'Kiểm tra Navitel khi khởi động' đang bật. Kích hoạt quy trình kiểm tra ban đầu...")
                handleInternetRestored(initialLaunch = true)
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()

        val newCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val hasInternet = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                val isValidated = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
                val isValidNow = hasInternet && isValidated

                val wasValid = _isInternetReady.value
                _isInternetReady.value = isValidNow

                if (!wasValid && isValidNow) {
                    AppLogger.success("NETWORK", "Internet đã xác thực sẵn sàng (NET_CAPABILITY_VALIDATED: true)")
                    if (!previousHadInternet) {
                        previousHadInternet = true
                        handleInternetRestored(initialLaunch = false)
                    }
                }
            }

            override fun onLost(network: Network) {
                _isInternetReady.value = false
                previousHadInternet = false
                stabilizationJob?.cancel()
                _stabilizationCountdown.value = null
                AppLogger.w("NETWORK", "Mất kết nối Internet. Sẵn sàng theo dõi lần kết nối phục hồi tiếp theo...")
            }

            override fun onUnavailable() {
                _isInternetReady.value = false
                previousHadInternet = false
                _stabilizationCountdown.value = null
            }
        }

        callback = newCallback
        try {
            connectivityManager.registerNetworkCallback(request, newCallback)
        } catch (e: Exception) {
            AppLogger.e("NETWORK", "Không thể đăng ký NetworkCallback: ${e.message}", e)
        }
    }

    fun stopMonitoring() {
        val currentCallback = callback ?: return
        try {
            connectivityManager.unregisterNetworkCallback(currentCallback)
        } catch (e: Exception) {
            AppLogger.w("NETWORK", "Lỗi khi hủy NetworkCallback: ${e.message}")
        }
        callback = null
        stabilizationJob?.cancel()
        _stabilizationCountdown.value = null
        AppLogger.i("NETWORK", "Đã dừng theo dõi mạng.")
    }

    private fun handleInternetRestored(initialLaunch: Boolean) {
        stabilizationJob?.cancel()
        stabilizationJob = scope.launch(Dispatchers.IO) {
            val runOnce = settingsRepository.runOncePerBoot.first()
            if (runOnce && AppSettingsRepository.hasExecutedThisBoot && !initialLaunch) {
                AppLogger.i(
                    "NETWORK",
                    "Đã thực hiện phục hồi trong phiên khởi động này. Bỏ qua lần kết nối lại này để tránh lặp lại khi Wi-Fi chập chờn."
                )
                return@launch
            }

            AppLogger.i(
                "NETWORK",
                "Chờ 8 giây để mạng ổn định và YouTube Car (PiP 1) hoàn tất reload trước khi khôi phục Navitel..."
            )

            for (sec in 8 downTo 1) {
                _stabilizationCountdown.value = sec
                delay(1000L)
            }
            _stabilizationCountdown.value = null

            AppSettingsRepository.hasExecutedThisBoot = true
            AppLogger.i("NETWORK", "Mạng đã ổn định 8 giây. Kích hoạt khôi phục Navitel PiP...")
            onInternetReadyTransition()
        }
    }
}
