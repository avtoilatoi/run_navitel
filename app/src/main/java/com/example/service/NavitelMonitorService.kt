package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.NavitelApp
import com.example.R
import com.example.data.AppLogger
import com.example.network.NetworkMonitor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class NavitelMonitorService : Service() {

    companion object {
        const val ACTION_START_SERVICE = "com.example.action.START_MONITOR"
        const val ACTION_STOP_SERVICE = "com.example.action.STOP_MONITOR"
        private const val NOTIFICATION_ID = 2026
        private const val CHANNEL_ID = "navitel_pip_monitor"
    }

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var networkMonitor: NetworkMonitor? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP_SERVICE) {
            AppLogger.i("SERVICE", "Nhận được action dừng dịch vụ từ thông báo.")
            stopForegroundService()
            return START_NOT_STICKY
        }

        try {
            val notification = buildNotification()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startForeground(
                        NOTIFICATION_ID,
                        notification,
                        ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                    )
                } else {
                    startForeground(NOTIFICATION_ID, notification)
                }
            } else {
                startForeground(NOTIFICATION_ID, notification)
            }

            NavitelApp.setServiceRunning(true)
            startNetworkMonitoring()
            AppLogger.i("SERVICE", "Dịch vụ giám sát kết nối Navitel đã khởi chạy thành công.")
        } catch (e: Exception) {
            AppLogger.e("SERVICE", "Lỗi khi chạy foreground service: ${e.message}", e)
            stopSelf()
        }

        return START_STICKY
    }

    private fun startNetworkMonitoring() {
        if (networkMonitor == null) {
            val app = application as NavitelApp
            networkMonitor = NetworkMonitor(
                context = this,
                settingsRepository = app.settingsRepository,
                scope = serviceScope,
                onInternetReadyTransition = {
                    AppLogger.i("SERVICE", "Kích hoạt restart Navitel từ dịch vụ giám sát mạng...")
                    app.coordinator.restartNavitelCold(
                        explicitDisplayId = null,
                        reason = "Internet phục hồi sẵn sàng"
                    )
                }
            )
        }
        networkMonitor?.startMonitoring()
    }

    private fun stopForegroundService() {
        networkMonitor?.stopMonitoring()
        networkMonitor = null
        NavitelApp.setServiceRunning(false)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        networkMonitor?.stopMonitoring()
        networkMonitor = null
        serviceScope.cancel()
        NavitelApp.setServiceRunning(false)
        AppLogger.i("SERVICE", "Dịch vụ giám sát Navitel đã kết thúc.")
        super.onDestroy()
    }

    private fun buildNotification(): Notification {
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, NavitelMonitorService::class.java).apply {
            action = ACTION_STOP_SERVICE
        }
        val stopPendingIntent = PendingIntent.getService(
            this,
            1,
            stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.notification_title))
            .setContentText(getString(R.string.notification_monitoring))
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                getString(R.string.notification_action_stop),
                stopPendingIntent
            )
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = getString(R.string.notification_channel_name)
            val descriptionText = getString(R.string.notification_channel_desc)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
                setShowBadge(false)
            }
            val notificationManager =
                getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
}
