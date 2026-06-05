package com.example.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.data.database.Recording
import com.example.data.preferences.PreferenceManager
import com.example.data.security.CryptoEngine
import com.example.service.BackgroundVideoRecorderService
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCyberGreen
import java.io.File
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// Custom modern background modifier to map the theme's glowing violet/indigo/blue blobs
fun Modifier.frostedGlassBackground(): Modifier = this.drawBehind {
    // Midnight background base
    drawRect(color = Color(0xFF0B0E14))

    // Top-left soft glowing violet-indigo blob
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF4F46E5).copy(alpha = 0.16f), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(x = size.width * 0.1f, y = size.height * 0.1f),
            radius = size.width * 0.7f
        ),
        radius = size.width * 0.7f,
        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.1f, y = size.height * 0.1f)
    )

    // Bottom-right soft glowing blue/indigo blob
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color(0xFF3B82F6).copy(alpha = 0.12f), Color.Transparent),
            center = androidx.compose.ui.geometry.Offset(x = size.width * 0.9f, y = size.height * 0.9f),
            radius = size.width * 0.8f
        ),
        radius = size.width * 0.8f,
        center = androidx.compose.ui.geometry.Offset(x = size.width * 0.9f, y = size.height * 0.9f)
    )
}

@Composable
fun MainDashboard(
    viewModel: com.example.ui.viewmodel.VaultViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var selectedTab by remember { mutableIntStateOf(1) } // Default to active capture control panel

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .frostedGlassBackground(),
        containerColor = Color.Transparent, // Let the background modifier show standard base
        bottomBar = {
            // Floating Glass Tab Controller Custom Pill
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .windowInsetsPadding(WindowInsets.navigationBars)
                    .padding(horizontal = 24.dp, vertical = 20.dp)
                    .testTag("main_bottom_nav"),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth(0.95f)
                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(100.dp))
                        .border(
                            width = 1.dp,
                            color = Color.White.copy(alpha = 0.10f),
                            shape = RoundedCornerShape(100.dp)
                        )
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tab 0: Secured Vault
                    IconButton(
                        onClick = { selectedTab = 0 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (selectedTab == 0) Color(0xFF4F46E5) else Color.Transparent)
                            .testTag("nav_item_vault")
                    ) {
                        Icon(
                            imageVector = Icons.Default.VideoLibrary,
                            contentDescription = "Secured Vault",
                            tint = if (selectedTab == 0) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // Tab 1: Monitor Panel
                    IconButton(
                        onClick = { selectedTab = 1 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (selectedTab == 1) Color(0xFF4F46E5) else Color.Transparent)
                            .testTag("nav_item_monitor")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Control Monitor",
                            tint = if (selectedTab == 1) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }

                    // Tab 2: Settings Panel
                    IconButton(
                        onClick = { selectedTab = 2 },
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(if (selectedTab == 2) Color(0xFF4F46E5) else Color.Transparent)
                            .testTag("nav_item_settings")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Safe Settings",
                            tint = if (selectedTab == 2) Color.White else Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Authentic system secure header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Vault Cam",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        letterSpacing = (-0.5).sp
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(Color(0xFF10B981), shape = CircleShape)
                        )
                        Text(
                            text = "SYSTEM SECURE",
                            color = Color.White.copy(alpha = 0.5f),
                            fontWeight = FontWeight.Bold,
                            fontSize = 10.sp,
                            letterSpacing = 1.5.sp
                        )
                    }
                }

                // Header custom accessory item
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(14.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.10f), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Fingerprint check",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                when (selectedTab) {
                    0 -> VaultTabContent(viewModel, context)
                    1 -> CaptureTabContent(viewModel, context)
                    2 -> SettingsTabContent(viewModel, context)
                }
            }
        }
    }
}

// ---------------------- TAB 0: VAULT LIST ----------------------

@Composable
fun VaultTabContent(
    viewModel: com.example.ui.viewmodel.VaultViewModel,
    context: Context
) {
    val recordingsList by viewModel.recordingsList.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Text(
            text = "Enclosed Media Vault",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Text(
            text = "Your files are encrypted using dynamic AES-GCM and stored only inside secure internal app sandbox.",
            fontSize = 12.sp,
            color = MutedSlateLocal,
            modifier = Modifier.padding(bottom = 20.dp)
        )

        if (recordingsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = "Secured Database",
                        tint = Color.White.copy(alpha = 0.15f),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "The Chamber is Empty",
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Tap the 'Monitor' tab and start a background capture session to record videos privately.",
                        color = MutedSlateLocal,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.weight(1f)
            ) {
                items(recordingsList, key = { it.id }) { recording ->
                    RecordingItemCard(recording,
                        onPlay = { viewModel.prepareVideoForPlayback(recording) },
                        onShare = { shareEncryptedFile(context, recording) },
                        onDelete = { viewModel.deleteRecording(recording) }
                    )
                }
            }
        }
    }
}

@Composable
fun RecordingItemCard(
    recording: Recording,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(20.dp)
            )
            .testTag("recording_card_${recording.id}"),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(10.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Videocam,
                        contentDescription = "Video file",
                        tint = Color(0xFF4F46E5),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recording.title,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        fontSize = 15.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = getFormattedDate(recording.timestamp),
                            fontSize = 11.sp,
                            color = MutedSlateLocal
                        )
                        Text(
                            text = "•",
                            fontSize = 11.sp,
                            color = MutedSlateLocal
                        )
                        Text(
                            text = getFormattedSize(recording.fileSize),
                            fontSize = 11.sp,
                            color = MutedSlateLocal
                        )
                    }
                }

                Text(
                    text = getFormattedDuration(recording.duration),
                    color = Color.White,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action row buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Play
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4F46E5),
                        contentColor = Color.White
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("play_recording_${recording.id}"),
                    contentPadding = PaddingValues(vertical = 12.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Play icon",
                        tint = Color.White,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Secure Play", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }

                // Share
                IconButton(
                    onClick = onShare,
                    modifier = Modifier
                        .size(44.dp)
                        .background(Color.White.copy(alpha = 0.05f), shape = RoundedCornerShape(12.dp))
                        .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                        .testTag("share_recording_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = "Share",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Delete
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .size(44.dp)
                        .background(
                            MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .testTag("delete_recording_${recording.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteOutline,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

// ---------------------- TAB 1: CAPTURE PANEL ----------------------

@Composable
fun CaptureTabContent(
    viewModel: com.example.ui.viewmodel.VaultViewModel,
    context: Context
) {
    val isRecordingInBg by BackgroundVideoRecorderService.isRecording.collectAsState()
    val recordingDurationSeconds by BackgroundVideoRecorderService.currentRecordDuration.collectAsState()
    val cameraLabel = if (viewModel.useFrontCameraState.collectAsState().value) "Front Mirror Shield Enabled" else "Rear Secure Cam Enabled"

    // Dash rotation configuration
    val infiniteTransition = rememberInfiniteTransition(label = "rotating_dash")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Background Monitor",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Text(
            text = "Tap control button. Capture persists if app gets minimized, lockscreen gets triggered, or app gets swiped closed.",
            fontSize = 12.sp,
            color = MutedSlateLocal,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp, bottom = 36.dp)
        )

        // Spectacular dashed spin trigger core
        Box(
            modifier = Modifier.size(240.dp),
            contentAlignment = Alignment.Center
        ) {
            // Spinning outer dashed stroke
            Canvas(
                modifier = Modifier
                    .size(225.dp)
                    .graphicsLayer { rotationZ = rotationAngle }
            ) {
                drawCircle(
                    color = Color.White.copy(alpha = 0.10f),
                    radius = size.minDimension / 2f,
                    style = Stroke(
                        width = 2.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(20f, 20f), 0f)
                    )
                )
            }

            // Central Frosted glass button circle
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = CircleShape
                    )
                    .clickable {
                        triggerCamServiceToggle(context, isRecordingInBg)
                    }
                    .testTag("service_record_toggle_button"),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(
                                if (isRecordingInBg) Color(0x33EF4444) else Color(0x334F46E5)
                            )
                            .border(
                                width = 1.dp,
                                color = if (isRecordingInBg) Color(0x77EF4444) else Color(0x774F46E5),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isRecordingInBg) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .background(Color(0xFFEF4444), shape = RoundedCornerShape(4.dp))
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .background(Color(0xFF4F46E5), shape = CircleShape)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = if (isRecordingInBg) "RECORD ACTIVE" else "TAP ACTION",
                        color = if (isRecordingInBg) Color(0xFFEF4444) else Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = if (isRecordingInBg) "STOP MONITOR" else "START CAPTURE",
                        color = Color.White.copy(alpha = 0.5f),
                        fontSize = 9.sp,
                        letterSpacing = 0.5.sp,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Glass state tracking card
        Card(
            modifier = Modifier
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.08f),
                    shape = RoundedCornerShape(16.dp)
                ),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(if (isRecordingInBg) Color(0xFFEF4444) else Color(0xFF10B981))
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = if (isRecordingInBg) {
                        "SECURELY RECORDING: ${recordingDurationSeconds}s"
                    } else {
                        "CAPSULE STANDBY • $cameraLabel"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.5.sp
                )
            }
        }
    }
}

// ---------------------- TAB 2: PRIVACY SETTINGS ----------------------

@Composable
fun SettingsTabContent(
    viewModel: com.example.ui.viewmodel.VaultViewModel,
    context: Context
) {
    val frontCameraEnabled by viewModel.useFrontCameraState.collectAsState()
    val biometricsActive by viewModel.biometricsEnabledState.collectAsState()
    val activeAliasName by viewModel.activeIconState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        item {
            Text(
                text = "Privacy Controls",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "Configure camera behaviors and system camouflage methods.",
                fontSize = 12.sp,
                color = MutedSlateLocal
            )
        }

        // Camera settings block
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Camera Core Settings",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Use Front Camera", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Toggle between front self facing and rear capturing sensors.",
                                color = MutedSlateLocal,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(0.95f)
                            )
                        }

                        Switch(
                            checked = frontCameraEnabled,
                            onCheckedChange = { viewModel.toggleCameraSource(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4F46E5),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier.testTag("camera_toggle_switch")
                        )
                    }
                }
            }
        }

        // Biometric Settings
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Biometrics Security",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Unlock with Biometrics", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                            Text(
                                "Use FaceID or Fingerprint instead of passcode PIN for vault access.",
                                color = MutedSlateLocal,
                                fontSize = 11.sp,
                                modifier = Modifier.fillMaxWidth(0.95f)
                            )
                        }

                        Switch(
                            checked = biometricsActive,
                            onCheckedChange = { viewModel.toggleBiometrics(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF4F46E5),
                                uncheckedThumbColor = Color.White.copy(alpha = 0.5f),
                                uncheckedTrackColor = Color.White.copy(alpha = 0.10f)
                            ),
                            modifier = Modifier.testTag("biometrics_toggle_switch")
                        )
                    }
                }
            }
        }

        // Camouflage Icon Changer (Grid list)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(20.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Icon Camouflage (Decoy App)",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "For additional privacy, swap launcher appearance. The app icon and name changes immediately on launcher.",
                        color = MutedSlateLocal,
                        fontSize = 11.sp,
                        modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        AliasOptionItem(
                            title = "VaultCam (Default System)",
                            subtitle = "Show standard security camera emblem",
                            isSelected = activeAliasName == PreferenceManager.ICON_DEFAULT,
                            iconRepresentative = {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = Color(0xFF4F46E5),
                                    modifier = Modifier.size(24.dp)
                                )
                            },
                            onClick = { viewModel.changeAppLauncherIcon(PreferenceManager.ICON_DEFAULT) }
                        )

                        AliasOptionItem(
                            title = "Decoy Calculator",
                            subtitle = "Disguise layout as standard math tool",
                            isSelected = activeAliasName == PreferenceManager.ICON_CALCULATOR,
                            iconRepresentative = {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF2B2D30),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("+ -", color = Color(0xFF00E676), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                                    }
                                }
                            },
                            onClick = { viewModel.changeAppLauncherIcon(PreferenceManager.ICON_CALCULATOR) }
                        )

                        AliasOptionItem(
                            title = "Decoy Notes",
                            subtitle = "Disguise layout as a secure notepad memo",
                            isSelected = activeAliasName == PreferenceManager.ICON_NOTES,
                            iconRepresentative = {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF232E3A),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("✏️", fontSize = 12.sp)
                                    }
                                }
                            },
                            onClick = { viewModel.changeAppLauncherIcon(PreferenceManager.ICON_NOTES) }
                        )

                        AliasOptionItem(
                            title = "Decoy Smart Compass",
                            subtitle = "Disguise layout as responsive orientation tool",
                            isSelected = activeAliasName == PreferenceManager.ICON_COMPASS,
                            iconRepresentative = {
                                Surface(
                                    modifier = Modifier.size(32.dp),
                                    color = Color(0xFF1C2329),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("🧭", fontSize = 12.sp)
                                    }
                                }
                            },
                            onClick = { viewModel.changeAppLauncherIcon(PreferenceManager.ICON_COMPASS) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AliasOptionItem(
    title: String,
    subtitle: String,
    isSelected: Boolean,
    iconRepresentative: @Composable () -> Unit,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = if (isSelected) Color(0xFF4F46E5) else Color.White.copy(alpha = 0.06f),
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) Color(0x114F46E5) else Color.White.copy(alpha = 0.02f))
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        iconRepresentative()
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 14.sp
            )
            Text(
                text = subtitle,
                color = MutedSlateLocal,
                fontSize = 11.sp
            )
        }
        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF4F46E5))
            )
        }
    }
}

// ----------------------- HELPER UTILITIES -----------------------

private fun triggerCamServiceToggle(context: Context, isRunning: Boolean) {
    val serviceIntent = Intent(context, BackgroundVideoRecorderService::class.java).apply {
        action = if (isRunning) BackgroundVideoRecorderService.ACTION_STOP_RECORDING
        else BackgroundVideoRecorderService.ACTION_START_RECORDING
    }

    if (!isRunning) {
        ContextCompat.startForegroundService(context, serviceIntent)
    } else {
        context.startService(serviceIntent)
    }
}

private fun shareEncryptedFile(context: Context, recording: Recording) {
    try {
        val file = File(recording.filePath)
        if (file.exists()) {
            // Decrypt or copy temporarily in cache so it can be shared through normal share channels
            val tempSharedFile = File(context.cacheDir, "shared_vaultcam_recording.mp4")
            if (recording.filePath.endsWith(".enc")) {
                CryptoEngine.decryptFile(file, tempSharedFile)
            } else {
                file.inputStream().use { input ->
                    tempSharedFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Let's copy/provide content provider URI
            val authorities = "${context.packageName}.provider"
            // Use FileProvider to send
            val uri: Uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                authorities,
                tempSharedFile
            )

            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "video/mp4"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Secure Video"))
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
}

private fun getFormattedDate(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}

private fun getFormattedSize(bytes: Long): String {
    if (bytes <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB")
    val digitGroups = (Math.log10(bytes.toDouble()) / Math.log10(1024.0)).toInt()
    return DecimalFormat("#,##0.#").format(bytes / Math.pow(1024.0, digitGroups.toDouble())) + " " + units[digitGroups]
}

private fun getFormattedDuration(durationMs: Long): String {
    val seconds = (durationMs / 1000) % 60
    val minutes = (durationMs / (1000 * 60)) % 60
    val hours = (durationMs / (1000 * 60 * 60)) % 24
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

private val MutedSlateLocal = Color(0xFF9CA3AF)
