package com.example.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.filled.WifiOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppLogger
import com.example.model.ExecutorMode
import com.example.model.LogEntry
import com.example.model.LogLevel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NavitelDashboardScreen(
    viewModel: NavitelViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val logs by viewModel.logs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.infoMessage) {
        uiState.infoMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearMessage()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(if (uiState.isServiceRunning) Color(0xFF10B981) else Color(0xFF9CA3AF))
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Navitel PiP Restarter",
                            fontSize = 19.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "LECO Auto Assistant",
                            fontSize = 12.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                },
                actions = {
                    // Quick ADB status chip
                    OutlinedButton(
                        onClick = { viewModel.openAdbDialog() },
                        modifier = Modifier
                            .testTag("btn_adb_settings")
                            .height(38.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = when (uiState.executorMode) {
                                ExecutorMode.ADB_PRIVILEGED -> if (uiState.adbState.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                                ExecutorMode.SIMULATION_FAKE -> Color(0xFF06B6D4)
                                ExecutorMode.UNPRIVILEGED_TEST -> Color(0xFFF59E0B)
                            }
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = "ADB Status",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = when (uiState.executorMode) {
                                ExecutorMode.ADB_PRIVILEGED -> if (uiState.adbState.isConnected) "ADB: Shell OK" else "ADB: Chưa kết nối"
                                ExecutorMode.SIMULATION_FAKE -> "Mô phỏng (AI Studio)"
                                ExecutorMode.UNPRIVILEGED_TEST -> "Kiểm tra an toàn"
                            },
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF0B132B)
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            val isLandscape = maxWidth > maxHeight && maxWidth > 700.dp

            if (isLandscape) {
                // Automotive Landscape Two-Pane Layout
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Left Pane: Controls & Status
                    Column(
                        modifier = Modifier
                            .weight(1.15f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatusOverviewSection(uiState = uiState, onOpenAdb = { viewModel.openAdbDialog() })
                        PrimaryActionSection(
                            uiState = uiState,
                            onDetectDisplay = { viewModel.detectNavitelDisplay() },
                            onRequestColdRestart = { viewModel.requestColdRestart() },
                            onStartService = { viewModel.startService() },
                            onStopService = { viewModel.stopService() }
                        )
                        SettingsSwitchesSection(
                            uiState = uiState,
                            onToggleAutoStart = { viewModel.toggleAutoStartOnBoot(it) },
                            onToggleRunOnce = { viewModel.toggleRunOncePerBoot(it) },
                            onToggleCheckOnStart = { viewModel.toggleCheckOnAppStart(it) }
                        )
                    }

                    // Right Pane: Console & Simulator Controls
                    Column(
                        modifier = Modifier
                            .weight(0.85f)
                            .fillMaxHeight(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        SimulatorModeCard(
                            uiState = uiState,
                            onModeChanged = { viewModel.setExecutorMode(it) },
                            onSimulatedDisplayChanged = { viewModel.setSimulatedDisplayId(it) },
                            onTestId = { viewModel.testAdbId() }
                        )
                        LogConsoleCard(
                            logs = logs,
                            onCopyLogs = {
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                val clip = ClipData.newPlainText("Navitel Restarter Logs", AppLogger.getExportText())
                                clipboard.setPrimaryClip(clip)
                                viewModel.showMessage("Đã sao chép toàn bộ nhật ký vào khay nhớ tạm!")
                            },
                            onClearLogs = { viewModel.clearLogs() },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            } else {
                // Portrait Layout
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatusOverviewSection(uiState = uiState, onOpenAdb = { viewModel.openAdbDialog() })
                    PrimaryActionSection(
                        uiState = uiState,
                        onDetectDisplay = { viewModel.detectNavitelDisplay() },
                        onRequestColdRestart = { viewModel.requestColdRestart() },
                        onStartService = { viewModel.startService() },
                        onStopService = { viewModel.stopService() }
                    )
                    SettingsSwitchesSection(
                        uiState = uiState,
                        onToggleAutoStart = { viewModel.toggleAutoStartOnBoot(it) },
                        onToggleRunOnce = { viewModel.toggleRunOncePerBoot(it) },
                        onToggleCheckOnStart = { viewModel.toggleCheckOnAppStart(it) }
                    )
                    SimulatorModeCard(
                        uiState = uiState,
                        onModeChanged = { viewModel.setExecutorMode(it) },
                        onSimulatedDisplayChanged = { viewModel.setSimulatedDisplayId(it) },
                        onTestId = { viewModel.testAdbId() }
                    )
                    LogConsoleCard(
                        logs = logs,
                        onCopyLogs = {
                            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                            val clip = ClipData.newPlainText("Navitel Restarter Logs", AppLogger.getExportText())
                            clipboard.setPrimaryClip(clip)
                            viewModel.showMessage("Đã sao chép toàn bộ nhật ký vào khay nhớ tạm!")
                        },
                        onClearLogs = { viewModel.clearLogs() },
                        modifier = Modifier.height(280.dp)
                    )
                }
            }
        }
    }

    // Confirmation dialog for cold restart
    if (uiState.showConfirmRestartDialog) {
        ColdRestartConfirmDialog(
            onConfirm = { dontAskAgain ->
                viewModel.confirmColdRestart(dontAskAgain)
            },
            onDismiss = { viewModel.dismissConfirmDialog() }
        )
    }

    // ADB configuration dialog
    if (uiState.showAdbDialog) {
        AdbSetupDialog(
            uiState = uiState,
            onDismiss = { viewModel.closeAdbDialog() },
            onConnect = { host, port -> viewModel.connectAdb(host, port) },
            onDisconnect = { viewModel.disconnectAdb() },
            onTestId = { viewModel.testAdbId() },
            onClearKeys = { viewModel.clearAdbKeys() }
        )
    }
}

@Composable
fun StatusOverviewSection(
    uiState: DashboardUiState,
    onOpenAdb: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "TRẠNG THÁI HỆ THỐNG",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                // Internet Status
                StatusPill(
                    icon = if (uiState.isInternetReady) Icons.Default.Wifi else Icons.Default.WifiOff,
                    title = "Internet",
                    status = if (uiState.stabilizationCountdown != null) {
                        "Đang ổn định (${uiState.stabilizationCountdown}s)"
                    } else if (uiState.isInternetReady) {
                        "Đã xác thực"
                    } else {
                        "Mất kết nối"
                    },
                    accentColor = if (uiState.stabilizationCountdown != null) Color(0xFFF59E0B)
                    else if (uiState.isInternetReady) Color(0xFF10B981)
                    else Color(0xFFEF4444),
                    modifier = Modifier.weight(1f)
                )

                // Display Navitel
                StatusPill(
                    icon = Icons.Default.Tv,
                    title = "Display Navitel",
                    status = uiState.detectedDisplayId?.let { "Display #$it" } ?: "Chưa phát hiện",
                    accentColor = if (uiState.detectedDisplayId != null) Color(0xFF06B6D4) else Color(0xFF9CA3AF),
                    modifier = Modifier.weight(1f)
                )

                // Privileged Shell Status
                StatusPill(
                    icon = Icons.Default.Terminal,
                    title = "ADB Quyền Shell",
                    status = when (uiState.executorMode) {
                        ExecutorMode.ADB_PRIVILEGED -> if (uiState.adbState.isConnected) "Sẵn sàng" else "Chưa có quyền"
                        ExecutorMode.SIMULATION_FAKE -> "Giả lập UID 2000"
                        ExecutorMode.UNPRIVILEGED_TEST -> "Không đặc quyền"
                    },
                    accentColor = when (uiState.executorMode) {
                        ExecutorMode.ADB_PRIVILEGED -> if (uiState.adbState.isConnected) Color(0xFF10B981) else Color(0xFFEF4444)
                        ExecutorMode.SIMULATION_FAKE -> Color(0xFF06B6D4)
                        ExecutorMode.UNPRIVILEGED_TEST -> Color(0xFFF59E0B)
                    },
                    onClick = onOpenAdb,
                    modifier = Modifier.weight(1.2f)
                )
            }

            // Last Restart Summary Card
            uiState.lastRestartAttempt?.let { attempt ->
                Surface(
                    color = if (attempt.statusOk || attempt.isColdLaunch) Color(0xFF064E3B).copy(alpha = 0.5f) else Color(0xFF7F1D1D).copy(alpha = 0.5f),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (attempt.statusOk || attempt.isColdLaunch) Icons.Default.CheckCircle else Icons.Default.Warning,
                            contentDescription = null,
                            tint = if (attempt.statusOk || attempt.isColdLaunch) Color(0xFF10B981) else Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Lần restart gần nhất: ${attempt.message}",
                                fontSize = 12.sp,
                                color = Color.White,
                                fontWeight = FontWeight.Medium
                            )
                            if (attempt.pidBefore != null || attempt.pidAfter != null) {
                                Text(
                                    text = "PID: ${attempt.pidBefore ?: "none"} -> ${attempt.pidAfter ?: "none"} | Exit: ${attempt.exitCode}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFCBD5E1)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(
    icon: ImageVector,
    title: String,
    status: String,
    accentColor: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Surface(
        onClick = { onClick?.invoke() },
        enabled = onClick != null,
        color = Color(0xFF0F172A),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor.copy(alpha = 0.4f)),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = title,
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = status,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun PrimaryActionSection(
    uiState: DashboardUiState,
    onDetectDisplay: () -> Unit,
    onRequestColdRestart: () -> Unit,
    onStartService: () -> Unit,
    onStopService: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                text = "ĐIỀU KHIỂN NAVITEL & DỊCH VỤ",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Button 1: Detect display
                Button(
                    onClick = onDetectDisplay,
                    enabled = !uiState.isRestarting,
                    modifier = Modifier
                        .testTag("btn_detect_display")
                        .weight(1f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dò display Navitel", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }

                // Button 2: Cold restart Navitel
                Button(
                    onClick = onRequestColdRestart,
                    enabled = !uiState.isRestarting,
                    modifier = Modifier
                        .testTag("btn_cold_restart")
                        .weight(1.3f)
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.detectedDisplayId != null) Color(0xFFDC2626) else Color(0xFFB91C1C)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    if (uiState.isRestarting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (uiState.isRestarting) "Đang xử lý..." else "Khởi động lạnh Navitel",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Row 2: Service start/stop
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onStartService,
                    enabled = !uiState.isServiceRunning,
                    modifier = Modifier
                        .testTag("btn_start_service")
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Bắt đầu theo dõi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onStopService,
                    enabled = uiState.isServiceRunning,
                    modifier = Modifier
                        .testTag("btn_stop_service")
                        .weight(1f)
                        .height(46.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Icon(imageVector = Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Dừng theo dõi", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
fun SettingsSwitchesSection(
    uiState: DashboardUiState,
    onToggleAutoStart: (Boolean) -> Unit,
    onToggleRunOnce: (Boolean) -> Unit,
    onToggleCheckOnStart: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = "TÙY CHỌN TỰ ĐỘNG HÓA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF94A3B8),
                letterSpacing = 1.sp
            )

            SettingSwitchRow(
                title = "Tự chạy sau khi khởi động",
                subtitle = "Tự động kích hoạt Foreground Service sau khi Android box nhận BOOT_COMPLETED",
                checked = uiState.autoStartOnBoot,
                onCheckedChange = onToggleAutoStart
            )

            SettingSwitchRow(
                title = "Chỉ chạy một lần mỗi lần khởi động",
                subtitle = "Ngăn chặn restart lặp lại khi Wi-Fi ô tô bị chập chờn ngắt kết nối nhiều lần",
                checked = uiState.runOncePerBoot,
                onCheckedChange = onToggleRunOnce
            )

            SettingSwitchRow(
                title = "Kiểm tra Navitel khi mở ứng dụng",
                subtitle = "Nếu thiết bị đã có mạng từ trước, tự kiểm tra và kích hoạt phục hồi ngay",
                checked = uiState.checkOnAppStart,
                onCheckedChange = onToggleCheckOnStart
            )
        }
    }
}

@Composable
fun SettingSwitchRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(text = subtitle, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 14.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF2563EB),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF334155)
            )
        )
    }
}

@Composable
fun SimulatorModeCard(
    uiState: DashboardUiState,
    onModeChanged: (ExecutorMode) -> Unit,
    onSimulatedDisplayChanged: (Int) -> Unit,
    onTestId: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CHẾ ĐỘ THỰC THI PRIVILEGED EXECUTOR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF94A3B8),
                    letterSpacing = 0.5.sp
                )

                OutlinedButton(
                    onClick = onTestId,
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF38BDF8))
                ) {
                    Text("Test 'id'", fontSize = 11.sp)
                }
            }

            // Radio/Button selector for Modes
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                ModeButton(
                    title = "Giả lập AI Studio",
                    isSelected = uiState.executorMode == ExecutorMode.SIMULATION_FAKE,
                    onClick = { onModeChanged(ExecutorMode.SIMULATION_FAKE) },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    title = "Kiểm tra an toàn",
                    isSelected = uiState.executorMode == ExecutorMode.UNPRIVILEGED_TEST,
                    onClick = { onModeChanged(ExecutorMode.UNPRIVILEGED_TEST) },
                    modifier = Modifier.weight(1f)
                )
                ModeButton(
                    title = "ADB Thiết bị thật",
                    isSelected = uiState.executorMode == ExecutorMode.ADB_PRIVILEGED,
                    onClick = { onModeChanged(ExecutorMode.ADB_PRIVILEGED) },
                    modifier = Modifier.weight(1f)
                )
            }

            // If in Simulation mode: allow switching simulated display 21 vs 23
            AnimatedVisibility(visible = uiState.executorMode == ExecutorMode.SIMULATION_FAKE) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Mô phỏng Navitel ở PiP Display:",
                        fontSize = 11.sp,
                        color = Color(0xFFCBD5E1)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = { onSimulatedDisplayChanged(21) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.simulatedDisplayId == 21) Color(0xFF0284C7) else Color(0xFF334155)
                            ),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Display #21", fontSize = 11.sp)
                        }
                        Button(
                            onClick = { onSimulatedDisplayChanged(23) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.simulatedDisplayId == 23) Color(0xFF0284C7) else Color(0xFF334155)
                            ),
                            modifier = Modifier.height(32.dp),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text("Display #23", fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ModeButton(
    title: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(36.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) Color(0xFF2563EB) else Color(0xFF334155),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(6.dp)
    ) {
        Text(text = title, fontSize = 11.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
    }
}

@Composable
fun LogConsoleCard(
    logs: List<LogEntry>,
    onCopyLogs: () -> Unit,
    onClearLogs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF090D16)),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Terminal,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NHẬT KÝ THỰC THI (${logs.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF94A3B8),
                        letterSpacing = 0.5.sp
                    )
                }

                Row {
                    IconButton(onClick = onCopyLogs, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Sao chép",
                            tint = Color(0xFFCBD5E1),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(onClick = onClearLogs, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Xóa nhật ký",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Log entries
            if (logs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Chưa có bản ghi nào. Hãy bấm 'Dò display Navitel' để bắt đầu.",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        val textColor = when (log.level) {
                            LogLevel.ERROR -> Color(0xFFF87171)
                            LogLevel.WARN -> Color(0xFFFBBF24)
                            LogLevel.SUCCESS -> Color(0xFF34D399)
                            LogLevel.COMMAND -> Color(0xFF38BDF8)
                            LogLevel.INFO -> Color(0xFFE2E8F0)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 1.dp)
                        ) {
                            Text(
                                text = timeFormat.format(Date(log.timestamp)),
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF64748B),
                                modifier = Modifier.width(55.dp)
                            )
                            Text(
                                text = "[${log.tag}]",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = textColor.copy(alpha = 0.8f),
                                modifier = Modifier.width(90.dp)
                            )
                            Text(
                                text = log.message,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = textColor,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ColdRestartConfirmDialog(
    onConfirm: (Boolean) -> Unit,
    onDismiss: () -> Unit
) {
    var dontAskAgain by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color(0xFFEF4444)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Xác nhận khởi động lại Navitel", color = Color.White, fontSize = 16.sp)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Navitel sẽ bị đóng và mở lại. Tuyến đường hoặc thao tác chưa lưu có thể bị gián đoạn.",
                    color = Color(0xFFE2E8F0),
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Quy trình thực thi:\n1. dumpsys activity activities\n2. am force-stop com.navitel\n3. Chờ 2 giây\n4. am start --display DISPLAY_ID -W -f 0x10000000 -n com.navitel/.app.MainActivity",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(8.dp)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Checkbox(
                        checked = dontAskAgain,
                        onCheckedChange = { dontAskAgain = it },
                        colors = CheckboxDefaults.colors(checkedColor = Color(0xFF2563EB))
                    )
                    Text(
                        text = "Không hỏi lại lần sau",
                        fontSize = 12.sp,
                        color = Color(0xFFCBD5E1)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(dontAskAgain) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("Thực hiện ngay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Hủy bỏ", color = Color(0xFF94A3B8))
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}

@Composable
fun AdbSetupDialog(
    uiState: DashboardUiState,
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit,
    onDisconnect: () -> Unit,
    onTestId: () -> Unit,
    onClearKeys: () -> Unit
) {
    var host by remember { mutableStateOf(uiState.adbState.host) }
    var portText by remember { mutableStateOf(uiState.adbState.port.toString()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Thiết lập ADB Shell Cục Bộ (127.0.0.1)", color = Color.White, fontSize = 16.sp)
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Quy định đặc quyền: App thông thường KHÔNG THỂ chạy 'am force-stop' hoặc điều khiển secondary display nếu không có ADB shell hoặc privileged bridge.",
                    fontSize = 12.sp,
                    color = Color(0xFFFBBF24),
                    lineHeight = 16.sp
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = host,
                        onValueChange = { host = it },
                        label = { Text("Host (Mặc định 127.0.0.1)") },
                        modifier = Modifier.weight(1.5f),
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = portText,
                        onValueChange = { portText = it },
                        label = { Text("Port (Mặc định 5555)") },
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Button(
                        onClick = {
                            val port = portText.toIntOrNull() ?: 5555
                            onConnect(host, port)
                        },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB))
                    ) {
                        Text("Kết nối ADB", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onDisconnect,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF475569))
                    ) {
                        Text("Ngắt kết nối", fontSize = 12.sp)
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    OutlinedButton(
                        onClick = onTestId,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Chạy lệnh 'id'", fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = onClearKeys,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFEF4444))
                    ) {
                        Text("Xóa khóa ADB", fontSize = 12.sp)
                    }
                }

                uiState.adbState.lastTestResult?.let { result ->
                    Surface(
                        color = Color(0xFF0F172A),
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Kết quả kiểm tra gần nhất:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = result,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (result.contains("uid=2000") || result.contains("shell")) Color(0xFF34D399) else Color(0xFFEF4444)
                            )
                        }
                    }
                }

                Surface(
                    color = Color(0xFF0F172A),
                    shape = RoundedCornerShape(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "HƯỚNG DẪN THIẾT LẬP TRÊN ANDROID BOX Ô TÔ:",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Text(
                            text = "1. Vào Cài đặt Android -> Giới thiệu thiết bị -> Nhấp 7 lần vào 'Số phiên bản' (Build number) để bật Tùy chọn nhà phát triển.\n" +
                                    "2. Trong Tùy chọn nhà phát triển, bật 'Gỡ lỗi USB' hoặc 'Gỡ lỗi không dây' (Wireless Debugging).\n" +
                                    "3. Nhập Port gỡ lỗi cục bộ (thường là 5555 hoặc port hiển thị trong Gỡ lỗi không dây) và bấm Kết nối.\n" +
                                    "4. Khóa xác thực RSA được lưu an toàn trong thư mục riêng của app, không bao giờ gửi ra ngoài mạng.",
                            fontSize = 11.sp,
                            color = Color(0xFFCBD5E1),
                            lineHeight = 15.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Đóng")
            }
        },
        containerColor = Color(0xFF1E293B)
    )
}
