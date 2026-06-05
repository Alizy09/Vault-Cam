package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CardSlate
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.PrimaryCyberGreen

@Composable
fun LockScreen(
    hasStoredPin: Boolean,
    onPinSetupComplete: (String) -> Unit,
    onPinVerify: (String, () -> Unit, () -> Unit) -> Unit,
    onTriggerBiometrics: () -> Unit,
    onUnlockSuccess: () -> Unit
) {
    var stepSetupMode by remember { mutableStateOf(!hasStoredPin) }
    var setupFirstPin by remember { mutableStateOf("") }

    var enteredPin by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    var isErrorState by remember { mutableStateOf(false) }

    // Header label
    val titleText = when {
        stepSetupMode && setupFirstPin.isEmpty() -> "Setup Secure Passcode"
        stepSetupMode -> "Confirm Secure Passcode"
        else -> "VaultCam Locked"
    }

    val subtitleText = when {
        stepSetupMode && setupFirstPin.isEmpty() -> "Create a 4-digit numeric code to protect your encrypted videos"
        stepSetupMode -> "Re-enter your custom passcode to finish setup"
        else -> "Authentication required to access the secure media vault"
    }

    fun handleNumberInput(number: String) {
        if (enteredPin.length < 4) {
            enteredPin += number
            isErrorState = false
            statusMessage = ""
        }

        if (enteredPin.length == 4) {
            if (stepSetupMode) {
                if (setupFirstPin.isEmpty()) {
                    // First step of setup complete
                    setupFirstPin = enteredPin
                    enteredPin = ""
                } else {
                    // Check if they match
                    if (setupFirstPin == enteredPin) {
                        onPinSetupComplete(enteredPin)
                        stepSetupMode = false
                        statusMessage = "Passcode saved!"
                        enteredPin = ""
                    } else {
                        // Mismatch
                        isErrorState = true
                        statusMessage = "Passcodes do not match. Restart setup."
                        setupFirstPin = ""
                        enteredPin = ""
                    }
                }
            } else {
                // Verify passcode
                onPinVerify(enteredPin, {
                    // Success callback
                    onUnlockSuccess()
                }, {
                    // Fail callback
                    isErrorState = true
                    statusMessage = "Incorrect Passcode. Try again."
                    enteredPin = ""
                })
            }
        }
    }

    fun handleBackspace() {
        if (enteredPin.isNotEmpty()) {
            enteredPin = enteredPin.dropLast(1)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .drawBehind {
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
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Visual lock icon
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Lock",
                tint = if (isErrorState) MaterialTheme.colorScheme.error else PrimaryCyberGreen,
                modifier = Modifier
                    .size(64.dp)
                    .padding(bottom = 8.dp)
            )

            Text(
                text = titleText,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = subtitleText,
                style = MaterialTheme.typography.bodyMedium.copy(
                    color = MutedSlateLocal,
                    lineHeight = 20.sp
                ),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Pin indicators
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                for (i in 0 until 4) {
                    val active = i < enteredPin.length
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 12.dp)
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(
                                if (active) PrimaryCyberGreen else CardSlate
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Status message
            AnimatedVisibility(
                visible = statusMessage.isNotEmpty(),
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Text(
                    text = statusMessage,
                    color = if (isErrorState) MaterialTheme.colorScheme.error else PrimaryCyberGreen,
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 10-key PIN layout
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .border(
                        width = 1.dp,
                        color = Color.White.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(24.dp)
                    ),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.04f)),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val rows = listOf(
                        listOf("1", "2", "3"),
                        listOf("4", "5", "6"),
                        listOf("7", "8", "9"),
                        listOf("BIO", "0", "BACK")
                    )

                    for (row in rows) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            for (item in row) {
                                when (item) {
                                    "BIO" -> {
                                        if (!stepSetupMode && hasStoredPin) {
                                            Box(
                                                modifier = Modifier
                                                    .size(60.dp)
                                                    .clip(CircleShape)
                                                    .background(PrimaryCyberGreen.copy(alpha = 0.15f))
                                                    .clickable { onTriggerBiometrics() }
                                                    .testTag("biometric_trigger_button"),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Fingerprint",
                                                    tint = PrimaryCyberGreen,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        } else {
                                            Spacer(modifier = Modifier.size(60.dp))
                                        }
                                    }
                                    "BACK" -> {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .clickable { handleBackspace() }
                                                .testTag("pin_backspace_button"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Backspace",
                                                tint = Color.White.copy(alpha = 0.6f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                    }
                                    else -> {
                                        Box(
                                            modifier = Modifier
                                                .size(60.dp)
                                                .clip(CircleShape)
                                                .background(Color.White.copy(alpha = 0.05f))
                                                .border(
                                                    width = 1.dp,
                                                    color = Color.White.copy(alpha = 0.08f),
                                                    shape = CircleShape
                                                )
                                                .clickable { handleNumberInput(item) }
                                                .testTag("pin_btn_$item"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = item,
                                                color = Color.White,
                                                fontSize = 24.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private val MutedSlateLocal = Color(0xFF9CA3AF)
