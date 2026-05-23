package com.example.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.AttackEvent
import com.example.data.model.IncidentEntity
import com.example.ui.theme.*
import com.example.ui.viewmodel.ThreatViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SentryDashboard(
    viewModel: ThreatViewModel,
    modifier: Modifier = Modifier
) {
    var activeTab by remember { mutableStateOf(0) } // 0 = Operations Command, 1 = Incident Intel database

    val activeAttacks by viewModel.activeAttacks.collectAsStateWithLifecycle()
    val loggedIncidents by viewModel.loggedIncidents.collectAsStateWithLifecycle()
    val totalAttacks by viewModel.totalAttacksToday.collectAsStateWithLifecycle()
    val threatStats by viewModel.threatLevelStats.collectAsStateWithLifecycle()
    val targetedPort by viewModel.mostTargetedPort.collectAsStateWithLifecycle()
    val selectedIncident by viewModel.selectedIncident.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(CyberGreen)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "CYBERSENTRY COMMAND",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = CyberCyan
                        )
                    }
                },
                actions = {
                    Text(
                        text = "NODE [ACTIVE]",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberGreen,
                        modifier = Modifier
                            .border(1.dp, CyberGreen, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = CyberDarkBg,
                    titleContentColor = TextPrimary
                )
            )
        },
        bottomBar = {
            NavigationBar(
                containerColor = CyberDarkSurface,
                tonalElevation = 8.dp,
                modifier = Modifier.windowInsetsPadding(WindowInsets.navigationBars)
            ) {
                NavigationBarItem(
                    selected = activeTab == 0,
                    onClick = { activeTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Command Map") },
                    label = { Text("LIVE MAP", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("nav_live_map")
                )
                NavigationBarItem(
                    selected = activeTab == 1,
                    onClick = { activeTab = 1 },
                    icon = { 
                        BadgedBox(badge = {
                            if (loggedIncidents.isNotEmpty()) {
                                Badge(containerColor = CyberRed) {
                                    Text(
                                        text = "${loggedIncidents.size}",
                                        color = Color.White,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }) {
                            Icon(Icons.Default.Lock, contentDescription = "Incidents")
                        }
                    },
                    label = { Text("SEC OPERATIONS", fontFamily = FontFamily.Monospace, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color.Black,
                        selectedTextColor = CyberCyan,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary,
                        indicatorColor = CyberCyan
                    ),
                    modifier = Modifier.testTag("nav_sec_ops")
                )
            }
        },
        containerColor = CyberDarkBg
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(CyberDarkBg, CyberDarkBg)
                    )
                )
        ) {
            Crossfade(targetState = activeTab, label = "TabSwitch") { tab ->
                when (tab) {
                    0 -> LiveThreatIntelScreen(
                        activeAttacks = activeAttacks,
                        totalAttacks = totalAttacks,
                        threatStats = threatStats,
                        targetedPort = targetedPort,
                        onFileIncident = { attack -> viewModel.fileIncidentReport(attack) }
                    )
                    1 -> SecOperationsScreen(
                        loggedIncidents = loggedIncidents,
                        selectedIncident = selectedIncident,
                        onSelectIncident = { inc -> viewModel.selectIncident(inc) },
                        onUpdateNotesAndStatus = { id, notes, status -> 
                            viewModel.updateIncidentNotesAndStatus(id, notes, status) 
                        },
                        onTriggerAI = { inc -> viewModel.triggerAiAnalystBriefing(inc) },
                        onPurge = { id -> viewModel.purgeIncident(id) },
                        aiState = viewModel.aiState.collectAsStateWithLifecycle().value,
                        aiReply = viewModel.aiReply.collectAsStateWithLifecycle().value
                    )
                }
            }
        }
    }
}

// ==========================================
// SCREEN 0: LIVE THREAT INTEL MAP & TICKER (Bento Grid Edition)
// ==========================================
@Composable
fun LiveThreatIntelScreen(
    activeAttacks: List<AttackEvent>,
    totalAttacks: Int,
    threatStats: Map<String, Int>,
    targetedPort: Int,
    onFileIncident: (AttackEvent) -> Unit
) {
    var radialIntensity by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Stats/Status Row - Traditional upper stats styled as flat premium bento tiles
        item {
            StatsHeaderSection(totalAttacks, targetedPort, threatStats)
        }

        // BENTO CARD 1: WORLD MAP CARD (Visual Centerpiece - Spans 2 cols, contains Radar rendering)
        item {
            OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, BorderColor),
                shape = RoundedCornerShape(24.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Actual Holographic Map Radar Vector underneath
                    SentryRadarMap(
                        activeAttacks = activeAttacks,
                        modifier = Modifier.fillMaxSize()
                    )

                    // overlay top banner content
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Box(
                                modifier = Modifier
                                    .background(
                                        CyberRed.copy(alpha = 0.2f),
                                        RoundedCornerShape(percent = 100)
                                    )
                                    .border(
                                        1.dp,
                                        CyberRed.copy(alpha = 0.4f),
                                        RoundedCornerShape(percent = 100)
                                    )
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(CyberRed)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "LIVE ACTIVITY",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = CyberRed
                                    )
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = String.format("%,d", totalAttacks),
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "Attacks traced this session",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                        }

                        // Top Right locator metadata
                        val locatorNode = activeAttacks.firstOrNull()
                        val locatorText = if (locatorNode != null) {
                            "TRACING: ${locatorNode.attackerCountryCode} [PORT ${locatorNode.port}]"
                        } else {
                            "LAT: 34.05 / LON: -118.24"
                        }
                        Text(
                            text = locatorText,
                            fontSize = 9.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = CyberCyan,
                            modifier = Modifier
                                .background(Color.Black.copy(alpha = 0.4f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        )
                    }

                    // overlay bottom banner details
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Quick Country targets row
                        Row(
                            horizontalArrangement = Arrangement.spacedBy((-6).dp)
                        ) {
                            val sampleAvatars = listOf("US", "CN", "DE", "RU")
                            sampleAvatars.forEach { country ->
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(CyberDarkCard)
                                        .border(2.dp, CyberDarkSurface, RoundedCornerShape(14.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = country,
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        fontFamily = FontFamily.Monospace,
                                        color = TextPrimary
                                    )
                                }
                            }
                        }

                        // Quick action button
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .background(CyberCyan)
                                .clickable { radialIntensity = !radialIntensity }
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = if (radialIntensity) "GRID DENSE" else "HOLO DEPLOYED",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }
        }

        // BENTO COLUMN 2 & 3: Double horizontal cell split
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Cell: Top Vector Tracker
                OutlinedCard(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkCard),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(145.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Security Status",
                                tint = CyberCyan,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "CORE FLOW",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = TextSecondary
                            )
                        }

                        Column {
                            Text(
                                text = "Top Vector",
                                fontSize = 11.sp,
                                color = TextSecondary
                            )
                            Text(
                                text = "DDoS Flood",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberCyan,
                                fontFamily = FontFamily.Monospace
                            )
                        }

                        // Static sleek status tracks matching Tailwind spec
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(percent = 100))
                                    .background(CyberDarkBg)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(0.74f)
                                        .clip(RoundedCornerShape(percent = 100))
                                        .background(CyberCyan)
                                )
                            }
                            Text(
                                text = "74% of active threat egress",
                                fontSize = 9.sp,
                                color = TextSecondary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }

                // Right Cell: Recent Event Log brief
                OutlinedCard(
                    colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                    border = BorderStroke(1.dp, BorderColor),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(145.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(14.dp),
                        verticalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "LATEST INCIDENTS",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = TextSecondary
                        )

                        // 3 micro indicators
                        Column(
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val recentLogs = activeAttacks.take(3)
                            if (recentLogs.isEmpty()) {
                                Text(
                                    "No telemetry log",
                                    fontSize = 10.sp,
                                    color = TextSecondary,
                                    fontFamily = FontFamily.Monospace
                                )
                            } else {
                                recentLogs.forEach { log ->
                                    val logDotColor = when (log.threatLevel) {
                                        "CRITICAL" -> CyberRed
                                        "HIGH" -> CyberOrange
                                        "MEDIUM" -> CyberCyan
                                        else -> CyberGreen
                                    }
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(logDotColor)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "${log.attackType.take(13)} - [P:${log.port}]",
                                            fontSize = 9.sp,
                                            fontFamily = FontFamily.Monospace,
                                            color = TextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }

                        HorizontalDivider(color = BorderColor)
                        
                        Text(
                            text = if (activeAttacks.isNotEmpty()) "Ingress active · Now" else "Antenna offline",
                            fontSize = 8.sp,
                            color = CyberCyan,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }

        // BENTO CARD 4: Level Alert status bar (Spans full width, matches Tailwind amber warning styling)
        item {
            OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = AlertBannerBg),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(0.dp, Color.Transparent),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Shield Watch Status Alert",
                            tint = AlertBannerText,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "SYSTEM ALERT: LEVEL ORANGE INTRUSION",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = AlertBannerText
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Observe warning",
                        tint = AlertBannerText,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Section header for Ingress buffer log feeds
        item {
            Column(modifier = Modifier.padding(top = 8.dp)) {
                Text(
                    text = "REAL-TIME INTERCEPTION FEEDS",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                )
                Text(
                    text = "Select any anomalous socket connection event to log and evaluate in your Command Center database.",
                    fontSize = 10.sp,
                    color = TextSecondary,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )
            }
        }

        // List of all active attacks
        if (activeAttacks.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(140.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = CyberCyan)
                }
            }
        } else {
            items(activeAttacks, key = { it.id }) { attack ->
                AttackTickerItem(attack = attack, onFileIncident = onFileIncident)
            }
        }
    }
}


@Composable
fun StatsHeaderSection(
    totalAttacks: Int,
    targetedPort: Int,
    threatStats: Map<String, Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Box 1: Core Tally
        Column(
            modifier = Modifier
                .weight(1.1f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Text("THREAT COUNTER", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            Text(
                text = String.format("%,d", totalAttacks),
                fontSize = 20.sp,
                color = CyberCyan,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )
        }

        // Box 2: Primary Port Vector
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Text("INGRESS TARGET", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            Text(
                text = "PORT $targetedPort",
                fontSize = 18.sp,
                color = CyberOrange,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }

        // Box 3: Network Status
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .background(CyberDarkSurface)
                .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
                .padding(10.dp)
        ) {
            Text("SYS CONDUIT", fontSize = 10.sp, color = TextSecondary, fontFamily = FontFamily.Monospace)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(CyberGreen)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "STABLE",
                    fontSize = 15.sp,
                    color = CyberGreen,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun SentryRadarMap(activeAttacks: List<AttackEvent>, modifier: Modifier = Modifier) {
    // Transition animations for sweep and dots
    val infiniteTransition = rememberInfiniteTransition(label = "RadarSweep")
    val sweepAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Sweep"
    )

    val signalFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "TargetPulse"
    )

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height

        // 1. Draw grid circles representing visual dashboard radar overlay
        val center = Offset(w / 2f, h / 2f)
        val maxRadius = minOf(w, h) / 1.7f
        
        // Solid black backing
        drawRect(color = CyberDarkSurface)

        // Grid Lines
        for (i in 1..4) {
            drawCircle(
                color = BorderColor.copy(alpha = 0.6f),
                radius = maxRadius * (i / 4f),
                center = center,
                style = Stroke(width = 1f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f))
            )
        }

        // Core Horizontal & Vertical cross beams
        drawLine(
            color = BorderColor.copy(alpha = 0.6f),
            start = Offset(0f, h / 2f),
            end = Offset(w, h / 2f),
            strokeWidth = 1f
        )
        drawLine(
            color = BorderColor.copy(alpha = 0.6f),
            start = Offset(w / 2f, 0f),
            end = Offset(w / 2f, h),
            strokeWidth = 1f
        )

        // Radar Dynamic Sweep line
        val sweepEndX = center.x + maxRadius * cos(sweepAngle)
        val sweepEndY = center.y + maxRadius * sin(sweepAngle)
        drawLine(
            brush = Brush.linearGradient(
                colors = listOf(CyberCyan, Color.Transparent),
                start = center,
                end = Offset(sweepEndX, sweepEndY)
            ),
            start = center,
            end = Offset(sweepEndX, sweepEndY),
            strokeWidth = 4f
        )

        // 2. Plot simplified outline vectors representing continent zones (to establish visual geography)
        val continentPaths = listOf(
            // North America
            listOf(Offset(0.12f, 0.32f), Offset(0.28f, 0.30f), Offset(0.32f, 0.45f), Offset(0.25f, 0.52f), Offset(0.18f, 0.44f)),
            // South America
            listOf(Offset(0.28f, 0.58f), Offset(0.34f, 0.62f), Offset(0.38f, 0.78f), Offset(0.33f, 0.84f), Offset(0.29f, 0.68f)),
            // Eurasia/Europe/Asia
            listOf(Offset(0.42f, 0.32f), Offset(0.55f, 0.28f), Offset(0.74f, 0.30f), Offset(0.81f, 0.48f), Offset(0.68f, 0.56f), Offset(0.48f, 0.48f)),
            // Africa
            listOf(Offset(0.46f, 0.54f), Offset(0.54f, 0.54f), Offset(0.58f, 0.68f), Offset(0.51f, 0.78f), Offset(0.44f, 0.62f)),
            // Australia
            listOf(Offset(0.76f, 0.68f), Offset(0.84f, 0.68f), Offset(0.84f, 0.78f), Offset(0.76f, 0.78f))
        )

        continentPaths.forEach { vertices ->
            for (idx in vertices.indices) {
                val p1 = vertices[idx]
                val p2 = vertices[(idx + 1) % vertices.size]
                drawLine(
                    color = BorderColor.copy(alpha = 0.5f),
                    start = Offset(p1.x * w, p1.y * h),
                    end = Offset(p2.x * w, p2.y * h),
                    strokeWidth = 1.5f
                )
            }
        }

        // 3. Draw active cyber attack conduits (lines + glowing pulses)
        activeAttacks.take(4).forEachIndexed { index, attack ->
            val start = Offset(attack.attackerLon * w, attack.attackerLat * h)
            val end = Offset(attack.targetLon * w, attack.targetLat * h)

            // Line Color (based on threat level)
            val lineClr = when (attack.threatLevel) {
                "CRITICAL" -> CyberRed
                "HIGH" -> CyberOrange
                "MEDIUM" -> CyberCyan
                else -> CyberGreen
            }

            // High aesthetic arc Bezier calculations
            // Midpoint with curve height offset
            val midX = (start.x + end.x) / 2f
            val midY = ((start.y + end.y) / 2f) - 60f // Control point pushed upwards to make arc
            val ctrl = Offset(midX, midY)

            // Draw line arc using quadratic path
            val path = androidx.compose.ui.graphics.Path().apply {
                moveTo(start.x, start.y)
                quadraticTo(ctrl.x, ctrl.y, end.x, end.y)
            }

            drawPath(
                path = path,
                color = lineClr.copy(alpha = 0.5f),
                style = Stroke(width = 2f)
            )

            // Glow dot traveling along the arc
            // De-Moivre linear interpolation inside quad curve
            val t = (signalFloat + (index * 0.25f)) % 1.0f
            val interpX = (1 - t) * (1 - t) * start.x + 2 * (1 - t) * t * ctrl.x + t * t * end.x
            val interpY = (1 - t) * (1 - t) * start.y + 2 * (1 - t) * t * ctrl.y + t * t * end.y
            
            drawCircle(
                color = lineClr,
                radius = 5.dp.toPx(),
                center = Offset(interpX, interpY)
            )
            drawCircle(
                color = lineClr.copy(alpha = 0.3f),
                radius = (10f + (signalFloat * 10f)).dp.toPx(),
                center = Offset(interpX, interpY)
            )

            // Attack Node Anchors (fades out over progress)
            drawCircle(color = lineClr, radius = 3.dp.toPx(), center = start)
            drawCircle(
                color = lineClr.copy(alpha = 0.4f),
                radius = (4f + (4f * signalFloat)).dp.toPx(),
                center = start,
                style = Stroke(width = 1f)
            )

            drawCircle(color = lineClr, radius = 3.dp.toPx(), center = end)
            drawCircle(
                color = lineClr.copy(alpha = 0.4f),
                radius = (5f + (6f * signalFloat)).dp.toPx(),
                center = end,
                style = Stroke(width = 2f)
            )
        }
    }
}

@Composable
fun AttackTickerItem(
    attack: AttackEvent,
    onFileIncident: (AttackEvent) -> Unit
) {
    var logged by remember { mutableStateOf(false) }

    val threatColor = when (attack.threatLevel) {
        "CRITICAL" -> CyberRed
        "HIGH" -> CyberOrange
        "MEDIUM" -> CyberCyan
        else -> CyberGreen
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0C101E))
            .border(1.dp, BorderColor.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
            .padding(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Red indicator
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(40.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(threatColor)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Attack basic details
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = attack.attackType,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = attack.threatLevel,
                    fontSize = 9.sp,
                    color = threatColor,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(threatColor.copy(alpha = 0.15f), RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                )
            }
            
            Spacer(modifier = Modifier.height(3.dp))
            
            // Route traces
            Text(
                text = "${attack.attackerCountry} (${attack.attackerIp}) ➔ PORT ${attack.port} ➔ ${attack.targetCountry}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(6.dp))

        // File logging action button
        IconButton(
            onClick = {
                onFileIncident(attack)
                logged = true
            },
            enabled = !logged,
            modifier = Modifier
                .size(36.dp)
                .background(
                    if (logged) Color(0xFF1E293B) else CyberRed.copy(alpha = 0.15f),
                    RoundedCornerShape(6.dp)
                )
                .testTag("file_incident_${attack.id}")
        ) {
            Icon(
                imageVector = if (logged) Icons.Default.CheckCircle else Icons.Default.Warning,
                contentDescription = "Log to DB",
                tint = if (logged) CyberGreen else CyberRed,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

// ==========================================
// SCREEN 1: SEC OPERATIONS CENTRE (DATABASE)
// ==========================================
@Composable
fun SecOperationsScreen(
    loggedIncidents: List<IncidentEntity>,
    selectedIncident: IncidentEntity?,
    onSelectIncident: (IncidentEntity?) -> Unit,
    onUpdateNotesAndStatus: (Long, String, String) -> Unit,
    onTriggerAI: (IncidentEntity) -> Unit,
    onPurge: (Long) -> Unit,
    aiState: String,
    aiReply: String
) {
    if (selectedIncident != null) {
        // Detailed Inspector View
        IncidentInspectorView(
            incident = selectedIncident,
            onBack = { onSelectIncident(null) },
            onSaveNotesStatus = onUpdateNotesAndStatus,
            onAskAI = { onTriggerAI(selectedIncident) },
            onPurge = { onPurge(selectedIncident.id) },
            aiState = aiState,
            aiReply = aiReply
        )
    } else {
        // Main list of incidents stored
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp)
        ) {
            Text(
                text = "SECURE OPERATIONS DATABASE INCIDENTS",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = CyberCyan
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Local offline-isolated event records queued for remediation.",
                fontSize = 11.sp,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (loggedIncidents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .clip(RoundedCornerShape(24.dp))
                        .background(CyberDarkSurface)
                        .border(1.dp, BorderColor, RoundedCornerShape(24.dp))
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "No Threats",
                            tint = CyberGreen.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "0 SIGNATURE COLLISION ALERTS DETECTED",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = CyberGreen
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Open Live Map to intercept threat packets and file logs to database.",
                            fontSize = 11.sp,
                            color = TextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(loggedIncidents, key = { it.id }) { incident ->
                        IncidentDbItem(incident = incident, onClick = { onSelectIncident(incident) })
                    }
                }
            }
        }
    }
}

@Composable
fun IncidentDbItem(
    incident: IncidentEntity,
    onClick: () -> Unit
) {
    val statusColor = when (incident.status) {
        "MITIGATED" -> CyberGreen
        "INVESTIGATING" -> CyberCyan
        "ESCALATED" -> CyberRed
        else -> CyberOrange
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(CyberDarkSurface)
            .border(1.dp, BorderColor, RoundedCornerShape(18.dp))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = incident.attackType,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = incident.threatLevel,
                    fontSize = 9.sp,
                    color = if (incident.threatLevel == "CRITICAL" || incident.threatLevel == "HIGH") CyberRed else CyberOrange,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier
                        .background(CyberDarkBg, RoundedCornerShape(2.dp))
                        .padding(horizontal = 4.dp, vertical = 2.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${incident.attackerCountry} (${incident.attackerIp}) ➔ Target Ingress PORT ${incident.port}",
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                color = TextSecondary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(statusColor)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = incident.status,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = statusColor,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
        Icon(
            imageVector = Icons.Default.KeyboardArrowRight,
            contentDescription = "Details",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
fun IncidentInspectorView(
    incident: IncidentEntity,
    onBack: () -> Unit,
    onSaveNotesStatus: (Long, String, String) -> Unit,
    onAskAI: () -> Unit,
    onPurge: () -> Unit,
    aiState: String,
    aiReply: String
) {
    var editedNotes by remember { mutableStateOf(incident.analystNotes) }
    var selectedStatus by remember { mutableStateOf(incident.status) }
    
    val statusOptions = listOf("INVESTIGATING", "MITIGATED", "ESCALATED", "FALSE_ALARM")
    val focusManager = LocalFocusManager.current

    val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    val formattedDate = formatter.format(Date(incident.timestamp))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        item {
            // Header back button
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onBack() }
                    .padding(vertical = 4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = CyberCyan,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "BACK TO OPERATIONS BUFFER",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = CyberCyan,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Attack Details Card
            OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "INCIDENT TARGET DOSSIER",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = incident.attackType,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Black,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    HorizontalDivider(color = BorderColor)
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("THREAT SENSITIVITY", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                incident.threatLevel,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberRed,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("TARGET INTERFACE PORT", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                "PORT ${incident.port}",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = CyberOrange,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("ATTACK CONTAINER ORIGIN", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                "${incident.attackerIp} (${incident.attackerCountryCode})",
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text("INTERCEPT TIMESTAMP", fontSize = 10.sp, color = TextSecondary)
                            Text(
                                formattedDate,
                                fontSize = 12.sp,
                                color = TextPrimary,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Analyst notes & status edits
            OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "ANALYST CONTROL BAY",
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = CyberCyan
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Status Dropdown buttons
                    Text("REMEDIATION DISPOSITION STATUS", fontSize = 10.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        statusOptions.forEach { statusOpt ->
                            val active = selectedStatus == statusOpt
                            val btnColor = when (statusOpt) {
                                "MITIGATED" -> CyberGreen
                                "INVESTIGATING" -> CyberCyan
                                "ESCALATED" -> CyberRed
                                else -> CyberOrange
                            }
                            
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (active) btnColor.copy(alpha = 0.2f) else Color.Transparent)
                                    .border(
                                        1.dp, 
                                        if (active) btnColor else BorderColor, 
                                        RoundedCornerShape(4.dp)
                                    )
                                    .clickable { 
                                        selectedStatus = statusOpt
                                        onSaveNotesStatus(incident.id, editedNotes, statusOpt)
                                    }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = statusOpt.replace("_", " "),
                                    fontSize = 8.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (active) btnColor else TextSecondary,
                                    maxLines = 1
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notes Area
                    Text("ANALYST INVESTIGATION LAB LOG NOTES", fontSize = 10.sp, color = TextSecondary)
                    Spacer(modifier = Modifier.height(6.dp))

                    OutlinedTextField(
                        value = editedNotes,
                        onValueChange = { 
                            editedNotes = it
                            onSaveNotesStatus(incident.id, it, selectedStatus)
                        },
                        textStyle = TextStyle(fontFamily = FontFamily.Monospace, color = TextPrimary, fontSize = 12.sp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .testTag("analyst_notes_field"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberCyan,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = CyberDarkBg,
                            unfocusedContainerColor = CyberDarkBg
                        ),
                        placeholder = { Text("Enter custom response logs...", fontSize = 11.sp, color = TextSecondary) },
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() })
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // AI Sentry Intel Module
            OutlinedCard(
                colors = CardDefaults.cardColors(containerColor = CyberDarkSurface),
                border = BorderStroke(1.dp, CyberCyan.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "AI COGNITIVE INTEL PROCESSOR",
                            fontSize = 10.sp,
                            fontFamily = FontFamily.Monospace,
                            color = CyberCyan,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Button(
                            onClick = { onAskAI() },
                            colors = ButtonDefaults.buttonColors(containerColor = CyberCyan),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("ask_ai_briefing"),
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Text(
                                "QUERY COGNITIVE CORE",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color.Black,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // AI content rendering
                    when (aiState) {
                        "LOADING" -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                CircularProgressIndicator(color = CyberCyan, strokeWidth = 2.dp, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "PROCESSING SIGNAL LOGS THROUGH GEMINI...",
                                    fontSize = 9.sp,
                                    fontFamily = FontFamily.Monospace,
                                    color = CyberCyan
                                )
                            }
                        }
                        "SUCCESS" -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberDarkBg)
                                    .border(1.dp, BorderColor, RoundedCornerShape(6.dp))
                                    .padding(10.dp)
                            ) {
                                Text(
                                    text = aiReply,
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 11.sp,
                                    color = CyberGreen,
                                    lineHeight = 16.sp,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                        "ERROR" -> {
                            Text(
                                text = "COGNITIVE CORE SIGNAL DECAY: UNABLE TO ESTABLISH TELESCOPIC BRIEFING.",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = CyberRed,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }
                        else -> {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(CyberDarkBg)
                                    .padding(12.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "Ready to run Deep Threat Assessment on this vector. Tap button above to initiate satellite telemetry.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Purge dossier button
            Button(
                onClick = { onPurge() },
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.15f)),
                border = BorderStroke(1.dp, CyberRed),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("purge_incident"),
                shape = RoundedCornerShape(6.dp)
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Purge", tint = CyberRed, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    "PURGE INCIDENT DOSSIER FROM BUFFER",
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = CyberRed,
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
