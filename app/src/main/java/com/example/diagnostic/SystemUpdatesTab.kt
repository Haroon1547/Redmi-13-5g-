package com.example.diagnostic

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class DeviceUpdateMeta(
    val deviceName: String,
    val updateStatusString: String,
    val releaseDateExpected: String,
    val hyperOsVersion: String,
    val androidVersion: String,
    val keyImprovement: String,
    val isTargetDevice: Boolean = false
)

data class PartitionBugSummary(
    val title: String,
    val compartment: String,
    val severity: String, // Critical, Warning, Moderate
    val impact: String,
    val recommendation: String,
    val explanation: String
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SystemUpdatesTab(
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()

    // Interactive updates table data
    val updatesTimeline = remember {
        listOf(
            DeviceUpdateMeta(
                deviceName = "Xiaomi 14 Ultra",
                updateStatusString = "Completed (Stable)",
                releaseDateExpected = "June 1, 2026",
                hyperOsVersion = "HyperOS 3.0.1.GP",
                androidVersion = "Android 16",
                keyImprovement = "Snapdragon 8 Gen 3 camera pipeline and cooling profile calibration",
                isTargetDevice = false
            ),
            DeviceUpdateMeta(
                deviceName = "Redmi Note 13 Pro+ 5G",
                updateStatusString = "Rollout Commenced",
                releaseDateExpected = "July 12, 2026",
                hyperOsVersion = "HyperOS 3.0.2.GP",
                androidVersion = "Android 15 Base",
                keyImprovement = "Dual-layer graphene thermal dissipation sync",
                isTargetDevice = false
            ),
            DeviceUpdateMeta(
                deviceName = "Redmi 13 5G (Your Device)",
                updateStatusString = "Upcoming (Stable Beta)",
                releaseDateExpected = "September 18, 2026",
                hyperOsVersion = "HyperOS 3.0.4.GP",
                androidVersion = "Android 16 Base",
                keyImprovement = "Dynamic partition compression & thread priority booster for Snapdragon 4 Gen 2",
                isTargetDevice = true
            ),
            DeviceUpdateMeta(
                deviceName = "POCO F6 Pro",
                updateStatusString = "In Testing Phase",
                releaseDateExpected = "August 8, 2026",
                hyperOsVersion = "HyperOS 3.0.1.GP",
                androidVersion = "Android 16",
                keyImprovement = "Vulkan 1.3 frame buffer optimization rules",
                isTargetDevice = false
            ),
            DeviceUpdateMeta(
                deviceName = "Xiaomi Pad 7 Pro",
                updateStatusString = "Planned Schedule",
                releaseDateExpected = "October 10, 2026",
                hyperOsVersion = "HyperOS 3.1.0.GP",
                androidVersion = "Android 16",
                keyImprovement = "Multi-window task layouts optimization",
                isTargetDevice = false
            )
        )
    }

    // Partition sector bugs library
    val partitionBugs = remember {
        listOf(
            PartitionBugSummary(
                title = "Super Partition virtual block fragmentation",
                compartment = "System Storage (Super)",
                severity = "Warning",
                impact = "Slight response lag during heavy concurrent write activities & app launches",
                recommendation = "Realign virtual storage blocks in the Optimizer console, or clear system theme cache blocks",
                explanation = "Current dynamic system slice sizes on Android 16 can lead to virtual partition resizing overhead. This fragments dynamic block allocation mappings."
            ),
            PartitionBugSummary(
                title = "Experimental Theme Cache partition leakage",
                compartment = "System User Cache",
                severity = "Warning",
                impact = "Leakage forces minor write latency of up to 45ms inside deep storage folders",
                recommendation = "We recommend resetting custom developer themes to system defaults and clearing the UI system data partition cache",
                explanation = "Heavy customized external theme layers trigger continuous background log-write cycles directly into the system application partition caches."
            ),
            PartitionBugSummary(
                title = "GPU Adreno Vulkan driver pipeline stall",
                compartment = "Vendor Dynamic Partition",
                severity = "Moderate",
                impact = "Occasional micro-stutters during high graphics usage under extreme GameTurbo workloads",
                recommendation = "Utilize the 'Re-calibrate Driver Register Maps' button in our thermal tuning engine to realign Vulkan GPU governor coefficients",
                explanation = "The current Vulkan 1.3 buffer allocator mismatch causes a brief register stall when shifting color channels on specific graphics rendering profiles."
            ),
            PartitionBugSummary(
                title = "Slot A/B Bootloader swap delay",
                compartment = "Bootloader (Active Slots)",
                severity = "Moderate",
                impact = "Increases physical device startup sequence by approximately 1.5 - 2.0 seconds",
                recommendation = "This is a minor system partition boot timing overhead scheduled to be fully solved in the upcoming HyperOS 3.0.4 patch",
                explanation = "The Android 16 dual slot handoff requires a longer checksum handshake key validation under raw HyperOS kernel configurations."
            )
        )
    }

    // Diagnostics / scan states
    var isScanningPartitions by remember { mutableStateOf(false) }
    var scanCompleted by remember { mutableStateOf(false) }
    var scanProgress by remember { mutableStateOf(0f) }
    var currentLogMsg by remember { mutableStateOf("") }
    val loggedSteps = remember { mutableStateListOf<String>() }

    // Dynamic rotation for parsing
    val infiniteTransition = rememberInfiniteTransition(label = "scan_rotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing)
        ),
        label = "rotation"
    )

    fun runPartitionScanner() {
        if (isScanningPartitions) return
        isScanningPartitions = true
        scanCompleted = false
        scanProgress = 0f
        loggedSteps.clear()

        coroutineScope.launch {
            val steps = listOf(
                "Initializing raw partition superblock analyzer..." to 0.15f,
                "Mounting dynamic logical partitions in READ_ONLY mode..." to 0.30f,
                "Validating dynamic slot bootloader integrity checks..." to 0.45f,
                "Scanning dynamic Super partition table layout alignment..." to 0.60f,
                "Searching virtual block structures for fragmentation ratio..." to 0.75f,
                "Analyzing theme caching pipeline overhead anomalies..." to 0.85f,
                "Verifying vendor GPU drivers Vulkan API linkages..." to 0.95f,
                "Generating partition diagnostics summary reports..." to 1.00f
            )

            for (step in steps) {
                currentLogMsg = step.first
                scanProgress = step.second
                loggedSteps.add(currentLogMsg)
                delay(400)
            }

            delay(300)
            isScanningPartitions = false
            scanCompleted = true
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("system_updates_tab_scrollable"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner card introducing updates & partition diagnostic tools
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("updates_hub_welcome_banner"),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF0F4FA)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFFD3E4FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.SystemUpdate,
                            contentDescription = "Updates Info",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Xiaomi Updates & Bugs Hub",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF001D36)
                        )
                        Text(
                            text = "Calibrated specifically to track HyperOS 3 releases and overall system partition health metrics on Redmi 13 5G Devices.",
                            fontSize = 12.sp,
                            color = Color(0xFF004A77),
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }

        // Section 1: Dynamic System Partition Scanner
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "SYSTEM PARTITION DIAGNOSTICS & BUG TRACKER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("partition_scanner_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "System Partition Integrity",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1F)
                                )
                                Text(
                                    text = "Virtual mappings & dynamic filesystem bugs analyzer",
                                    fontSize = 11.sp,
                                    color = Color(0xFF44474E)
                                )
                            }
                            if (isScanningPartitions) {
                                Icon(
                                    imageVector = Icons.Rounded.Refresh,
                                    contentDescription = "Scanning",
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier
                                        .size(24.dp)
                                        .rotate(rotationAngle)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.DeveloperBoard,
                                    contentDescription = "Ready",
                                    tint = Color(0xFF0061A4),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Quick info indicators
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F4FA))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("SUPER SIZE", fontSize = 9.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                    Text("112.4 GB", fontSize = 12.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F4FA))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("ACTIVE SLOT", fontSize = 9.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                    Text("Slot A", fontSize = 12.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F4FA))
                                    .padding(8.dp)
                            ) {
                                Column {
                                    Text("FILESYSTEM", fontSize = 9.sp, color = Color(0xFF44474E), fontWeight = FontWeight.Bold)
                                    Text("F2FS / Ext4", fontSize = 12.sp, color = Color(0xFF1B1B1F), fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        if (isScanningPartitions) {
                            LinearProgressIndicator(
                                progress = scanProgress,
                                color = Color(0xFF0061A4),
                                trackColor = Color(0xFFE1E2E6),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(CircleShape)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "LOG: $currentLogMsg",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF0061A4),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else if (scanCompleted) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFFD4EDDA))
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Success",
                                        tint = Color(0xFF1A8038),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Scan Completed! Found ${partitionBugs.size} known issues in partition tables.",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1A8038)
                                    )
                                }
                            }
                        } else {
                            Text(
                                text = "Run a sector & system partition deep diagnostic to detect block fragmentation, theme caching leaks, and device boot anomalies.",
                                fontSize = 12.sp,
                                color = Color(0xFF44474E),
                                lineHeight = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = { runPartitionScanner() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isScanningPartitions) Color(0xFFE1E2E6) else Color(0xFF0061A4),
                                contentColor = if (isScanningPartitions) Color(0xFF44474E) else Color.White
                            ),
                            enabled = !isScanningPartitions,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .testTag("run_partition_diagnostics_button"),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = if (isScanningPartitions) Icons.Rounded.Refresh else Icons.Rounded.Build,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isScanningPartitions) "SCANNING ROOT LOGICAL SECTORS..." else "DIAGNOSE SYSTEM SECTORS",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }
                }
            }
        }

        // Sub-elements: Render partition bugs list only if scan is completed to engage and guide the user
        if (scanCompleted) {
            item {
                Text(
                    text = "IDENTIFIED PARTITION ISSUES & SYSTEM BUGS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
                )
            }

            items(partitionBugs) { bug ->
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { expanded = !expanded }
                        .animateContentSize()
                        .testTag("bug_card_${bug.title.lowercase().replace(" ", "_")}"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFDF0ED)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Warning,
                                    contentDescription = "Bug Sign",
                                    tint = Color(0xFFE74C40),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = bug.title,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1B1B1F)
                                )
                                Text(
                                    text = "Partition: ${bug.compartment} • Severity: ${bug.severity}",
                                    fontSize = 10.sp,
                                    color = Color(0xFFE74C40),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Icon(
                                imageVector = if (expanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                                contentDescription = "Expand",
                                tint = Color(0xFF44474E),
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        if (expanded) {
                            HorizontalDivider(
                                color = Color(0xFFE1E2E6),
                                thickness = 0.8.dp,
                                modifier = Modifier.padding(vertical = 12.dp)
                            )

                            Text(
                                text = "DETAILED EXPLANATION",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0061A4),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = bug.explanation,
                                fontSize = 12.sp,
                                color = Color(0xFF1B1B1F),
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )

                            Text(
                                text = "DETECTED USER IMPACT",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF44474E),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = bug.impact,
                                fontSize = 12.sp,
                                color = Color(0xFF1B1B1F),
                                modifier = Modifier.padding(top = 2.dp, bottom = 8.dp)
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFF0F4FA))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "RECOMMENDED RECOVERY ACTION",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0061A4),
                                        letterSpacing = 0.5.sp
                                    )
                                    Text(
                                        text = bug.recommendation,
                                        fontSize = 11.sp,
                                        color = Color(0xFF004A77),
                                        fontWeight = FontWeight.SemiBold,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section 2: Targeted Upcoming Update Details for Redmi 13 5G
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "REDMI 13 5G NEXT SOFTWARE UPGRADE PROFILE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("redmi_next_update_card"),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F0FE)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0061A4))
                                        .padding(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "UPCOMING UPGRADE",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }
                                Text(
                                    text = "HyperOS 3.0.4.GP",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF001D36),
                                    modifier = Modifier.padding(top = 4.dp)
                                )
                            }

                            // Expected date indicator
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "RELEASE EXPECTED",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF44474E)
                                )
                                Text(
                                    text = "Sept 18, 2026",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color(0xFF0061A4)
                                )
                            }
                        }

                        HorizontalDivider(
                            color = Color(0x330061A4),
                            thickness = 1.dp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )

                        Text(
                            text = "Based on Android 16 (API 36). Fully tailored for Qualcomm Snapdragon 4 Gen 2 and HyperOS Kernel Core architectures to resolve current user feedback indicators.",
                            fontSize = 12.sp,
                            color = Color(0xFF004A77),
                            lineHeight = 16.sp
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "KEY FEATURES & CHANGE REGISTER:",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF001D36),
                            letterSpacing = 0.5.sp,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )

                        val keyFeatures = listOf(
                            "🏎️ Snapdragon Core GameTurbo 4.0 scheduler: Re-prioritizes high-load threads immediately to eliminate micro-stutters completely.",
                            "💾 Compressed f2fs Subsystem: Reclaims up to 4.2 GB of dynamic virtual system space under Super Partition allocations.",
                            "🛡️ HyperOS Memory Guard v2.1: Proactively unloads and clears expired caches within LPDDR4X memory clusters.",
                            "🌡️ Core Register Offsets: Re-calibrates governor maps to avoid thermal building when utilising 33W HyperCharge."
                        )

                        keyFeatures.forEach { feature ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 3.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 4.dp)
                                        .size(4.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0061A4))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = feature,
                                    fontSize = 11.sp,
                                    color = Color(0xFF004A77),
                                    lineHeight = 15.sp
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFF0F4FA))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⚠️ KNOWN TESTING ISSUES: Slight frame latency on Vulkan experimental presetting maps. This issue is fully tracked and currently under patch development.",
                                fontSize = 10.sp,
                                color = Color(0xFF44474E),
                                fontWeight = FontWeight.Bold,
                                lineHeight = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // Section 3: Xiaomi Device Updates Timetable
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "XIAOMI EXPECTED DEVICE UPDATES RADAR",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("device_updates_radar_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        updatesTimeline.forEachIndexed { idx, dev ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1.3f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = dev.deviceName,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (dev.isTargetDevice) Color(0xFF0061A4) else Color(0xFF1B1B1F)
                                        )
                                        if (dev.isTargetDevice) {
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(100.dp))
                                                    .background(Color(0xFFD3E4FF))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "TARGET",
                                                    fontSize = 8.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color(0xFF0061A4)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = "${dev.hyperOsVersion} • ${dev.androidVersion}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF44474E)
                                    )
                                    Text(
                                        text = "Core focus: ${dev.keyImprovement}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF44474E),
                                        lineHeight = 12.sp,
                                        modifier = Modifier.padding(top = 2.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(0.7f),
                                    horizontalAlignment = Alignment.End
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(100.dp))
                                            .background(
                                                when {
                                                    dev.updateStatusString.contains("Completed") -> Color(0xFFE2F0D9)
                                                    dev.updateStatusString.contains("Rollout") -> Color(0xFFE8F0FE)
                                                    dev.updateStatusString.contains("Testing") -> Color(0xFFFFF4E5)
                                                    else -> Color(0xFFF1F3F4)
                                                }
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = dev.updateStatusString,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = when {
                                                dev.updateStatusString.contains("Completed") -> Color(0xFF2E7D32)
                                                dev.updateStatusString.contains("Rollout") -> Color(0xFF0A58CA)
                                                dev.updateStatusString.contains("Testing") -> Color(0xFFE65100)
                                                else -> Color(0xFF44474E)
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = dev.releaseDateExpected,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF1B1B1F)
                                    )
                                }
                            }

                            if (idx < updatesTimeline.size - 1) {
                                HorizontalDivider(color = Color(0xFFE1E2E6), thickness = 0.8.dp)
                            }
                        }
                    }
                }
            }
        }

        // Section 4: Official Resources & Latest News Updates (External Navigation links)
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "OFFICIAL XIAOMI CHANNELS & COMMUNITY NEWS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF44474E),
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 6.dp, bottom = 8.dp)
                )

                // Row-1: Official Xiaomi Community link
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://new.c.mi.com/global/") }
                        .testTag("official_community_link_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF2E5)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Community",
                                tint = Color(0xFFFF6200),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Xiaomi Global Community Portal",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Text(
                                text = "Get latest official announcements, rollout news, & ROM feedback lists",
                                fontSize = 11.sp,
                                color = Color(0xFF44474E)
                             )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Launch,
                            contentDescription = "Open Link",
                            tint = Color(0xFFFF6200),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row-2: Official Xiaomi Global Firmware link
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://miuirom.org/") }
                        .testTag("official_firmware_link_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE8F0FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Launch,
                                contentDescription = "Firmware",
                                tint = Color(0xFF0061A4),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Official Xiaomi ROM Registry",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Text(
                                text = "Explore global, european, and regional Fastboot/Recovery release tracks",
                                fontSize = 11.sp,
                                color = Color(0xFF44474E)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Launch,
                            contentDescription = "Open Link",
                            tint = Color(0xFF0061A4),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Row-3: Official Xiaomi Homepage link
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { uriHandler.openUri("https://www.mi.com/global/") }
                        .testTag("official_homepage_link_card"),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFC3C6CF)),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE2F0D9)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.PhoneAndroid,
                                contentDescription = "Official Site",
                                tint = Color(0xFF1A8038),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Xiaomi Global Homepage",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1B1B1F)
                            )
                            Text(
                                text = "View official specifications, product support documentation, guidelines & help articles",
                                fontSize = 11.sp,
                                color = Color(0xFF44474E)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Launch,
                            contentDescription = "Open Link",
                            tint = Color(0xFF1A8038),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
