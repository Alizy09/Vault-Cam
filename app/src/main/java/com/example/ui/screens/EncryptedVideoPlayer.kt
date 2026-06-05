package com.example.ui.screens

import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.delay
import java.io.File
import java.util.Locale

@Composable
fun EncryptedVideoPlayer(
    videoFile: File,
    videoTitle: String,
    onDismissRequest: () -> Unit
) {
    var videoView by remember { mutableStateOf<VideoView?>(null) }
    var isPlaying by remember { mutableStateOf(false) }
    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }

    // Safe unified dismiss handler that halts player before physical file deletion
    val handleDismiss = {
        try {
            videoView?.stopPlayback()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        onDismissRequest()
    }

    // Safely stop playback when the composable is disposed of in the hierarchy
    androidx.compose.runtime.DisposableEffect(videoFile) {
        onDispose {
            try {
                videoView?.stopPlayback()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // Read timeline updates periodically if video is active (fully guarded against IllegalStateExceptions)
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            videoView?.let { vv ->
                try {
                    currentPosition = vv.currentPosition.toLong()
                    duration = vv.duration.toLong()
                    isPlaying = vv.isPlaying
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            delay(250)
        }
    }

    // Single-digit helper for time formatting (e.g. 05:12)
    fun formatTime(ms: Long): String {
        val totalSecs = ms / 1000
        val minutes = totalSecs / 60
        val seconds = totalSecs % 60
        return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }

    Dialog(
        onDismissRequest = handleDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Header of Player
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Secure Unlocked Playback",
                            color = MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = videoTitle,
                            color = Color.White,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1
                        )
                    }

                    IconButton(
                        onClick = handleDismiss
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close Player",
                            tint = Color.White
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Built-in Native Video View in Android wrapped with Custom Compose Controller Controls
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.Black),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        AndroidView(
                            factory = { context ->
                                VideoView(context).apply {
                                    try {
                                        setVideoURI(android.net.Uri.fromFile(videoFile))
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                    setOnPreparedListener { mp ->
                                        try {
                                            mp.isLooping = false
                                            duration = mp.duration.toLong()
                                            isPlaying = true
                                            this.start()
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    setOnCompletionListener {
                                        try {
                                            isPlaying = false
                                            currentPosition = 0L
                                            this.seekTo(0)
                                        } catch (e: Exception) {
                                            e.printStackTrace()
                                        }
                                    }
                                    setOnErrorListener { mp, what, extra ->
                                        android.util.Log.e("EncryptedVideoPlayer", "Playback error: $what, $extra")
                                        true
                                    }
                                    videoView = this
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(16.dp))
                        )

                        // Glass cover Overlay containing the play controls
                        Box(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            // High contrast central play button
                            IconButton(
                                onClick = {
                                    try {
                                        videoView?.let { vv ->
                                            if (vv.isPlaying) {
                                                vv.pause()
                                                isPlaying = false
                                            } else {
                                                vv.start()
                                                isPlaying = true
                                            }
                                        }
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                },
                                modifier = Modifier
                                    .size(64.dp)
                                    .align(Alignment.Center)
                                    .background(Color.Black.copy(alpha = 0.6f), shape = CircleShape)
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }

                            // Bottom seek controls
                            Column(
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .fillMaxWidth()
                                    .background(
                                        Brush.verticalGradient(
                                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.8f))
                                        )
                                    )
                                    .padding(horizontal = 16.dp, vertical = 12.dp)
                            ) {
                                // Time strings
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatTime(currentPosition),
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = formatTime(if (duration > 0) duration else videoView?.duration?.toLong() ?: 0L),
                                        color = Color.White.copy(alpha = 0.7f),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                Spacer(modifier = Modifier.height(4.dp))

                                // Dynamic Seek Slider
                                val maxDurationVal = if (duration > 0) duration else videoView?.duration?.toLong() ?: 1L
                                Slider(
                                    value = currentPosition.coerceIn(0L, maxDurationVal).toFloat(),
                                    onValueChange = { newValue ->
                                        videoView?.seekTo(newValue.toInt())
                                        currentPosition = newValue.toLong()
                                    },
                                    valueRange = 0f..maxDurationVal.toFloat(),
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary,
                                        inactiveTrackColor = Color.White.copy(alpha = 0.24f)
                                    )
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Decrypted on-the-fly inside memory-safe private cache. File is securely destroyed when playing ends.",
                    color = Color.White.copy(alpha = 0.5f),
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}
