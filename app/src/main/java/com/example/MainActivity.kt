package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.PhoneAndroid
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SdCard
import androidx.compose.material.icons.rounded.Sensors
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SettingsSuggest
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Thermostat
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material.icons.rounded.BatteryAlert
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.diagnostic.DashboardUiState
import com.example.diagnostic.DiagnosticStatus
import com.example.diagnostic.DiagnosticTestItem
import com.example.diagnostic.DiagnosticsViewModel
import com.example.diagnostic.HardwareSpecCard
import com.example.diagnostic.ThermalProfile
import com.example.diagnostic.TuningState
import com.example.diagnostic.SystemUpdatesTab
import com.example.ui.theme.MyApplicationTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    DashboardScreen(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    viewModel: DiagnosticsViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("HARDWARE STATS", "AUTOMATED RUNS", "THERMAL TUNING", "UPDATES & BUGS")

    Box(
        modifier = modifier
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header bar
            HeaderView(
                onRefresh = { viewModel.refreshSpecs() }
            )

            // Dynamic Score panel (glowing dial showing performance scores)
            IntegrityGaugePanel(
                score = uiState.overallScore,
                statusText = uiState.systemStatusText,
                deviceName = uiState.deviceSummary.deviceName,
                onRunDiagnostics = {
                    selectedTab = 1
                    viewModel.runAutomatedCheckups()
                },
                onOptimize = { viewModel.optimizeSystemDrivers() }
            )

            // Category Sliders
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color.Transparent,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = MaterialTheme.colorScheme.primary,
                        height = 3.dp
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (selectedTab == index) MaterialTheme.colorScheme.primary else Color(0xFF44474E),
                                letterSpacing = 0.5.sp
                            )
                        },
                        modifier = Modifier.testTag("tab_button_$index")
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                when (selectedTab) {
                    0 -> HardwareSpecificationTab(
                        specsList = uiState.hardwareSpecs,
                        onCardSelect = { viewModel.selectHardwareSpecCard(it) }
                    )
                    1 -> AutomatedDiagnosticsTab(
                        diagnosticsList = uiState.diagnosticsList,
                        isRunning = uiState.isRunningDiagnostics,
                        onTriggerDiagnostic = { viewModel.runAutomatedCheckups() }
                    )
                    2 -> ThermalTuningTab(
                        thermalProfile = uiState.thermalProfile,
                        onTriggerOptimize = { viewModel.optimizeSystemDrivers() }
                    )
                    3 -> SystemUpdatesTab()
                }
            }
        }

        // Expanded Card Details Dialog
        uiState.currentSelectedCard?.let { spec ->
            HardwareDetailPopup(
                spec = spec,
                onDismiss = { viewModel.selectHardwareSpecCard(null) }
            )
        }

        // Optimization Console Overlay Modal
        when (val tuning = uiState.tuningState) {
            is TuningState.TuningInProgress -> {
                TuningTerminalPopup(
                    progress = tuning.progress,
                    message = tuning.logMessage,
                    onDismiss = { /* Non-dismissible while in progress */ }
                )
            }
            is TuningState.Completed -> {
                TuningTerminalPopup(
                    progress = 100,
                    message = tuning.message,
                    isCompleted = true,
                    onDismiss = { viewModel.dismissTuning() }
                )
            }
            TuningState.Idle -> { /* do nothing */ }
        }
    }
}

@Composable
fun HeaderView(onRefresh: () -> Unit) {
    val currentTime = remember {
        val sdf = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        sdf.format(Date())
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .statusBarsPadding(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "System Insight",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1B1B1F),
                    letterSpacing = (-0.5).sp
                )
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .background(Color(0xFFE1E2E6))
                        .padding(horizontal = 10.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "Redmi 13 5G",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF44474E),
                        letterSpacing = 0.5.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2ECC71))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "HyperOS 3.0.1 • Android 16",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF44474E)
                )
            }
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Live UTC / Timestamp label
            Text(
                text = "UTC $currentTime",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFF44474E),
                modifier = Modifier.padding(end = 12.dp),
                fontFamily = FontFamily.Monospace
            )

            // Re-scan button
            IconButton(
                onClick = onRefresh,
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFE1E2E6))
                    .testTag("reload_specs_button")
            ) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = "Refresh sensors",
                    tint = Color(0xFF1B1B1F),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun IntegrityGaugePanel(
    score: Int,
    statusText: String,
    deviceName: String,
    onRunDiagnostics: () -> Unit,
    onOptimize: () -> Unit
) {
    val scoreAnimated = animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(1200, easing = FastOutSlowInEasing),
        label = "score_pulse"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFD3E4FF)),
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Radial dial on the left side
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(105.dp)
                    .padding(4.dp)
            ) {
                // Background track
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        color = Color(0x33004A77),
                        startAngle = -220f,
                        sweepAngle = 260f,
                        useCenter = false,
                        style = Stroke(width = 8.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                // Cyber progress arc
                val animatedAngleValue = (scoreAnimated.value / 100f) * 260f
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawArc(
                        brush = Brush.linearGradient(
                            colors = listOf(Color(0xFF004A77), Color(0xFF0061A4))
                        ),
                        startAngle = -220f,
                        sweepAngle = animatedAngleValue,
                        useCenter = false,
                        style = Stroke(width = 9.dp.toPx(), cap = StrokeCap.Round)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "${scoreAnimated.value.toInt()}",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF001D36)
                    )
                    Text(
                        text = "CORE INDEX",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF004A77),
                        letterSpacing = 0.5.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Subsystem controls and description on the right side
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = "OS Integrity Check",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF001D36)
                )

                Text(
                    text = "HyperOS Driver Framework v6.4",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF004A77),
                    modifier = Modifier.padding(top = 1.dp)
                )

                Text(
                    text = statusText,
                    fontSize = 12.sp,
                    color = Color(0xFF004A77),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Button(
                        onClick = onRunDiagnostics,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF0061A4),
                            contentColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(1.1f)
                            .height(36.dp)
                            .testTag("run_diagnostics_cta_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Speed,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "DIAGNOSE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    OutlinedButton(
                        onClick = onOptimize,
                        border = BorderStroke(1.dp, Color(0xFF0061A4)),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF0061A4),
                            containerColor = Color.White
                        ),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                        modifier = Modifier
                            .weight(0.9f)
                            .height(36.dp)
                            .testTag("speed_boost_cta_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Bolt,
                            contentDescription = null,
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "OPTIMIZE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0061A4),
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HardwareSpecificationTab(
    specsList: List<HardwareSpecCard>,
    onCardSelect: (HardwareSpecCard) -> Unit
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("hardware_specs_list")
    ) {
        item {
            Text(
                text = "SUB-SYSTEM HARDWARE READOUT",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF44474E),
                letterSpacing = 1.sp,
                modifier = Modifier.padding(bottom = 6.dp, top = 4.dp)
            )
        }

        // Layout specs list
        items(specsList) { spec ->
            HardwareCardItem(
                spec = spec,
                onClick = { onCardSelect(spec) }
            )
        }
    }
}

@Composable
fun HardwareCardItem(
    spec: HardwareSpecCard,
    onClick: () -> Unit
) {
    val statusColor = when (spec.status) {
        "Warning" -> Color(0xFFFF9F0A)
        "Critical" -> Color(0xFFE74C3C)
        else -> Color(0xFF1A8038)
    }

    val iconVector = when (spec.iconName) {
        "android" -> Icons.Rounded.Android
        "cpu" -> Icons.Rounded.DeveloperBoard
        "screen" -> Icons.Rounded.PhoneAndroid
        "ram" -> Icons.Rounded.Memory
        "storage" -> Icons.Rounded.SdCard
        "wifi" -> Icons.Rounded.Wifi
        "bluetooth" -> Icons.Rounded.Bluetooth
        "battery" -> Icons.Rounded.Thermostat
        else -> Icons.Rounded.Sensors
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("hardware_card_${spec.title.lowercase().replace(" ", "_")}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFF0F4FA)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = spec.title,
                    tint = if (spec.status == "Optimal") Color(0xFF0061A4) else statusColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = spec.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = spec.subtitle,
                    fontSize = 11.sp,
                    color = Color(0xFF44474E)
                )
            }

            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = spec.value,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color(0xFF1B1B1F),
                    textAlign = TextAlign.End,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontFamily = FontFamily.Monospace
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = spec.status,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }
            }

            Icon(
                imageVector = Icons.Rounded.ChevronRight,
                contentDescription = "Expand Spec Details",
                tint = Color(0xFF44474E),
                modifier = Modifier.padding(start = 10.dp).size(20.dp)
            )
        }
    }
}

@Composable
fun HardwareDetailPopup(
    spec: HardwareSpecCard,
    onDismiss: () -> Unit
) {
    val iconVector = when (spec.iconName) {
        "android" -> Icons.Rounded.Android
        "cpu" -> Icons.Rounded.DeveloperBoard
        "screen" -> Icons.Rounded.PhoneAndroid
        "ram" -> Icons.Rounded.Memory
        "storage" -> Icons.Rounded.SdCard
        "wifi" -> Icons.Rounded.Wifi
        "bluetooth" -> Icons.Rounded.Bluetooth
        "battery" -> Icons.Rounded.Thermostat
        else -> Icons.Rounded.Sensors
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
                .testTag("hardware_detail_modal"),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFC3C6CF))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F4FA)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = iconVector,
                                contentDescription = null,
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = spec.title,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Text(
                                text = spec.subtitle,
                                fontSize = 11.sp,
                                color = Color(0xFF44474E)
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE1E2E6))
                            .testTag("close_detail_modal")
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close specifications sheet",
                            tint = Color(0xFF1B1B1F),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                HorizontalDivider(
                    color = Color(0xFFE1E2E6),
                    thickness = 1.dp,
                    modifier = Modifier.padding(vertical = 16.dp)
                )

                // Render detail fields
                Text(
                    text = "SPECIFICATION & CALIBRATION METRICS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0061A4),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                spec.metadata.forEach { (key, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = key,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF44474E)
                        )
                        Text(
                            text = value,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF1B1B1F),
                            textAlign = TextAlign.End,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    HorizontalDivider(color = Color(0xFFE1E2E6), thickness = 0.8.dp)
                }

                Spacer(modifier = Modifier.height(18.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFD3E4FF))
                        .border(BorderStroke(1.dp, Color(0xFFBCCEEA)), RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.SettingsSuggest,
                            contentDescription = null,
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Calibrated for Snapdragon 4 Gen 2 and HyperOS 3.0 kernel drivers.",
                            fontSize = 11.sp,
                            color = Color(0xFF004A77),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AutomatedDiagnosticsTab(
    diagnosticsList: List<DiagnosticTestItem>,
    isRunning: Boolean,
    onTriggerDiagnostic: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "AUTOMATED DIAGNOSTICS SUITE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E),
                    letterSpacing = 1.sp
                )
                Text(
                    text = if (isRunning) "Actively benchmarking core physical modules..." else "Continuous physical performance tests",
                    fontSize = 11.sp,
                    color = if (isRunning) Color(0xFF0061A4) else Color(0xFF44474E)
                )
            }

            Button(
                onClick = onTriggerDiagnostic,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isRunning) Color(0xFFE1E2E6) else Color(0xFF0061A4),
                    contentColor = if (isRunning) Color(0xFF44474E) else Color.White
                ),
                enabled = !isRunning,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp),
                modifier = Modifier
                    .height(34.dp)
                    .testTag("run_diagnostics_suite_button"),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        color = Color(0xFF0061A4),
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("TESTING...", fontSize = 11.sp, fontWeight = FontWeight.Black)
                } else {
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("START SUITE", fontSize = 11.sp, fontWeight = FontWeight.Black)
                }
            }
        }

        if (diagnosticsList.isEmpty()) {
            // First Launch / Empty State
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Build,
                    contentDescription = null,
                    tint = Color(0xFFC3C6CF),
                    modifier = Modifier.size(72.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No diagnostic tests run yet.",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1B1B1F)
                )
                Text(
                    text = "Trigger a full hardware loop diagnostic to analyze your cores, memory caches, speed delays and drivers.",
                    fontSize = 12.sp,
                    color = Color(0xFF44474E),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp, start = 12.dp, end = 12.dp)
                )
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("diagnostic_runs_list")
            ) {
                items(diagnosticsList) { test ->
                    DiagnosticTestCard(test = test)
                }
            }
        }
    }
}

@Composable
fun DiagnosticTestCard(test: DiagnosticTestItem) {
    var expanded by remember { mutableStateOf(false) }

    val statusColor = when (test.status) {
        DiagnosticStatus.PASSED -> Color(0xFF1A8038)
        DiagnosticStatus.WARNING -> Color(0xFFFF9F0A)
        DiagnosticStatus.FAILED -> Color(0xFFE74C3C)
        DiagnosticStatus.RUNNING -> Color(0xFF0061A4)
        else -> Color(0xFF44474E)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (test.status == DiagnosticStatus.PASSED || test.status == DiagnosticStatus.WARNING) expanded = !expanded }
            .animateContentSize()
            .testTag("diagnostic_item_${test.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (test.status == DiagnosticStatus.RUNNING) Color(0xFF0061A4) else Color(0xFFC3C6CF)),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Status icon indicator
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .clip(CircleShape)
                        .background(
                            if (test.status == DiagnosticStatus.RUNNING) Color.White else statusColor.copy(alpha = 0.15f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (test.status == DiagnosticStatus.RUNNING) {
                        CircularProgressIndicator(
                            color = Color(0xFF0061A4),
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(16.dp)
                        )
                    } else {
                        Icon(
                            imageVector = when (test.status) {
                                DiagnosticStatus.PASSED -> Icons.Rounded.CheckCircle
                                DiagnosticStatus.WARNING -> Icons.Rounded.Warning
                                DiagnosticStatus.FAILED -> Icons.Rounded.BatteryAlert
                                else -> Icons.Rounded.Speed
                            },
                            contentDescription = null,
                            tint = statusColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = test.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1B1B1F)
                    )
                    Text(
                        text = test.description,
                        fontSize = 11.sp,
                        color = Color(0xFF44474E)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = test.resultText,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = statusColor,
                        fontFamily = FontFamily.Monospace
                    )
                    if (test.status == DiagnosticStatus.RUNNING) {
                        LinearProgressIndicator(
                            progress = test.progress,
                            color = Color(0xFF0061A4),
                            trackColor = Color(0xFFE1E2E6),
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .width(64.dp)
                                .height(3.dp)
                                .clip(CircleShape)
                        )
                    }
                }
            }

            // Expanded technical metrics
            if (expanded && test.metrics.isNotEmpty()) {
                HorizontalDivider(
                    color = Color(0xFFE1E2E6),
                    thickness = 0.8.dp,
                    modifier = Modifier.padding(vertical = 12.dp)
                )

                Text(
                    text = "DIAGNOSTIC TELEMETRY OUTPUT",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0061A4),
                    modifier = Modifier.padding(bottom = 6.dp)
                )

                test.metrics.forEach { (metric, value) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = metric,
                            fontSize = 12.sp,
                            color = Color(0xFF44474E)
                        )
                        Text(
                            text = value,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ThermalTuningTab(
    thermalProfile: ThermalProfile,
    onTriggerOptimize: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp)
    ) {
        Text(
            text = "REDMI THERMAL PROFILE & CORE CALIBRATION",
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF44474E),
            letterSpacing = 1.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Thermostat,
                            contentDescription = null,
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Thermal Headroom State",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1B1B1F)
                        )
                    }

                    val stateColor = when (thermalProfile.state) {
                        "COOL" -> Color(0xFF1A8038)
                        "BALANCED" -> Color(0xFF0061A4)
                        "WARM" -> Color(0xFFFF9F0A)
                        else -> Color(0xFFE74C3C)
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(stateColor.copy(alpha = 0.15f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = thermalProfile.state,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            color = stateColor,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Render dynamic indicator meter
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "0.0x (Idle)",
                        fontSize = 11.sp,
                        color = Color(0xFF44474E),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "Load Factor: " + String.format("%.2f x", thermalProfile.thermalHeadroom),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF1B1B1F),
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        text = "1.5x+ (Throttling)",
                        fontSize = 11.sp,
                        color = Color(0xFF44474E),
                        fontFamily = FontFamily.Monospace
                    )
                }

                LinearProgressIndicator(
                    progress = (thermalProfile.thermalHeadroom / 1.5f).coerceIn(0f, 1f),
                    color = when {
                        thermalProfile.thermalHeadroom > 0.8f -> Color(0xFFE74C3C)
                        thermalProfile.thermalHeadroom > 0.5f -> Color(0xFFFF9F0A)
                        else -> Color(0xFF0061A4)
                    },
                    trackColor = Color(0xFFE1E2E6),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .height(6.dp)
                        .clip(CircleShape)
                )

                Text(
                    text = "A thermal headroom of 1.0 indicates the device is reaching its maximum sustainable thermal limit. Throttling is applied under HyperOS GameTurbo rules above 0.9x.",
                    fontSize = 11.sp,
                    color = Color(0xFF44474E),
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
        }

        // Action parameters
        Card(
            modifier = Modifier.fillMaxWidth().wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "HYPEROS DRIVER CALIBRATIONS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF0061A4),
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("GPU Driver Allocation", fontSize = 13.sp, color = Color(0xFF44474E))
                    Text("Vulkan 1.3 Active", fontSize = 13.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Color(0xFFE1E2E6))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Copper Plate Link State", fontSize = 13.sp, color = Color(0xFF44474E))
                    Text("4-Layer Graphite Engaged", fontSize = 13.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(color = Color(0xFFE1E2E6))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Active Governor Scheduler", fontSize = 13.sp, color = Color(0xFF44474E))
                    Text(thermalProfile.activeOptimizerMode, fontSize = 13.sp, color = Color(0xFF0061A4), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider(color = Color(0xFFE1E2E6))

                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Liquid Cooling Fan Engine", fontSize = 13.sp, color = Color(0xFF44474E))
                    Text(thermalProfile.coolingDriverStatus, fontSize = 13.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                HorizontalDivider(color = Color(0xFFE1E2E6))

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onTriggerOptimize,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0x1A0061A4),
                        contentColor = Color(0xFF0061A4)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .border(1.dp, Color(0xFF0061A4), RoundedCornerShape(12.dp))
                        .testTag("driver_recalibrate_button"),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "RE-CALIBRATE DRIVER REGISTER MAPS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun TuningTerminalPopup(
    progress: Int,
    message: String,
    isCompleted: Boolean = false,
    onDismiss: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse_trans")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (!isCompleted) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation_indicator"
    )

    Dialog(
        onDismissRequest = { if (isCompleted) onDismiss() },
        properties = DialogProperties(
            dismissOnBackPress = isCompleted,
            dismissOnClickOutside = isCompleted
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .testTag("tuning_console_modal"),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0A0A0E)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, if (isCompleted) Color(0xFF1A8038) else Color(0xFF0061A4))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isCompleted) Color(0x221A8038) else Color(0x220061A4)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isCompleted) {
                            Icon(
                                imageVector = Icons.Rounded.CheckCircle,
                                contentDescription = null,
                                tint = Color(0xFF1A8038),
                                modifier = Modifier.size(20.dp)
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Bolt,
                                contentDescription = null,
                                tint = Color(0xFF0061A4),
                                modifier = Modifier
                                    .size(20.dp)
                                    .rotate(rotation)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    Column {
                        Text(
                            text = if (isCompleted) "DRIVERS OPTIMZED" else "TUNING KERNEL STACK",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Text(
                            text = if (isCompleted) "SpeedBoost applied successfully" else "Applying Xiaomi hardware speed offsets",
                            fontSize = 11.sp,
                            color = Color(0xFF8888A0)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Progress dial
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF1E1E28))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress / 100f)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(Color(0xFF004A77), Color(0xFF0061A4))
                                )
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Speed Calibration Matrix",
                        fontSize = 11.sp,
                        color = Color(0xFF8888A0)
                    )
                    Text(
                        text = "$progress%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Technical console listing values
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF040406))
                        .border(BorderStroke(1.dp, Color(0xFF1C1C26)), RoundedCornerShape(10.dp))
                        .padding(14.dp)
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isCompleted) Color(0xFF1A8038) else Color(0xFF0061A4))
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "LOG CONSOLE TERMINAL",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF8888A0)
                            )
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Text(
                            text = message,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF0061A4),
                            lineHeight = 16.sp
                        )
                    }
                }

                if (isCompleted) {
                    Spacer(modifier = Modifier.height(20.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF1A8038),
                            contentColor = Color.White
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(44.dp)
                            .testTag("dismiss_tuning_button"),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "RETURN TO CONSOLE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }
        }
    }
}
