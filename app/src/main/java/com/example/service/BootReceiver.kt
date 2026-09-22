package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.NavitelApp
import com.example.data.AppLogger
import com.example.data.AppSettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == "android.intent.action.QUICKBOOT_POWERON") {
            AppLogger.i("BOOT", "Nhận được sự kiện thiết bị vừa khởi động xong ($action).")

            // Reset boot execution flag for this fresh boot session
            AppSettingsRepository.hasExecutedThisBoot = false

            val pendingResult = goAsync()
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val settings = AppSettingsRepository(context)
                    val autoStart = settings.autoStartOnBoot.first()
                    if (autoStart) {
                        AppLogger.i("BOOT", "Tùy chọn 'Tự chạy sau khi khởi động' đang bật. Đang khởi động dịch vụ giám sát...")
                        NavitelApp.startMonitorService(context)
                    } else {
                        AppLogger.i("BOOT", "Tùy chọn 'Tự chạy sau khi khởi động' đang tắt. Không khởi chạy dịch vụ.")
                    }
                } catch (e: Exception) {
                    AppLogger.e("BOOT", "Lỗi xử lý khởi động sau BOOT_COMPLETED: ${e.message}", e)
                } finally {
                    pendingResult.finish()
                }
            }
        }
    }
}
