package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.screens.EncryptedVideoPlayer
import com.example.ui.screens.LockScreen
import com.example.ui.screens.MainDashboard
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.VaultViewModel

class MainActivity : FragmentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MyApplicationTheme {
                val vaultViewModel: VaultViewModel = viewModel()
                val isLocked by vaultViewModel.isAppLocked.collectAsState()
                val hasPinSet by vaultViewModel.pinHashState.collectAsState()
                val biometricsEnabled by vaultViewModel.biometricsEnabledState.collectAsState()
                val activePlaybackFile by vaultViewModel.playbackFileState.collectAsState()
                val recordings by vaultViewModel.recordingsList.collectAsState()

                // Secure App Lock & Permissions Handling
                val permissionsToRequest = mutableListOf(
                    Manifest.permission.CAMERA,
                    Manifest.permission.RECORD_AUDIO
                ).apply {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        add(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                // Modern Jetpack Compose Lifecycle Observer to safely auto-lock on pause
                val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
                androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
                    val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
                        if (event == androidx.lifecycle.Lifecycle.Event.ON_PAUSE) {
                            vaultViewModel.lockApp()
                        }
                    }
                    lifecycleOwner.lifecycle.addObserver(observer)
                    onDispose {
                        lifecycleOwner.lifecycle.removeObserver(observer)
                    }
                }

                LaunchedEffect(Unit) {
                    val notGranted = permissionsToRequest.filter {
                        ContextCompat.checkSelfPermission(this@MainActivity, it) != PackageManager.PERMISSION_GRANTED
                    }
                    if (notGranted.isNotEmpty()) {
                        androidx.core.app.ActivityCompat.requestPermissions(
                            this@MainActivity,
                            notGranted.toTypedArray(),
                            4819
                        )
                    }
                }

                // Auto prompt biometrics on startup if enabled
                LaunchedEffect(isLocked, hasPinSet, biometricsEnabled) {
                    if (isLocked && hasPinSet != null && biometricsEnabled) {
                        triggerBiometricPrompt(
                            onSuccess = { vaultViewModel.unlockApp() },
                            onFailure = { /* Hold locked for PIN keyboard input */ }
                        )
                    }
                }

                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    if (isLocked) {
                        LockScreen(
                            hasStoredPin = hasPinSet != null,
                            onPinSetupComplete = { chosenPin ->
                                vaultViewModel.setupPin(chosenPin)
                            },
                            onPinVerify = { pin, onSuccess, onFailure ->
                                vaultViewModel.verifyPin(pin, onSuccess, onFailure)
                            },
                            onTriggerBiometrics = {
                                triggerBiometricPrompt(
                                    onSuccess = { vaultViewModel.unlockApp() },
                                    onFailure = {
                                        Toast.makeText(this@MainActivity, "Biometric Auth Failed", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            },
                            onUnlockSuccess = {
                                vaultViewModel.unlockApp()
                            }
                        )
                    } else {
                        MainDashboard(
                            viewModel = vaultViewModel
                        )
                    }

                    // Floating secure video sandboxed player dialog
                    activePlaybackFile?.let { playFile ->
                        // Retrieve the title of the video being played dynamically from list
                        val runningVideo = recordings.firstOrNull { it.filePath == playFile.absolutePath || playFile.name.contains(it.filePath.substringAfterLast("/")) }
                        val videoTitle = runningVideo?.title ?: "Secure Recording"

                        EncryptedVideoPlayer(
                            videoFile = playFile,
                            videoTitle = videoTitle,
                            onDismissRequest = {
                                vaultViewModel.cleanUpPlaybackFile()
                            }
                        )
                    }
                }
            }
        }
    }

    private fun triggerBiometricPrompt(onSuccess: () -> Unit, onFailure: () -> Unit) {
        val biometricManager = BiometricManager.from(this)
        val authenticators = BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL
        
        when (biometricManager.canAuthenticate(authenticators)) {
            BiometricManager.BIOMETRIC_SUCCESS -> {
                val executor = ContextCompat.getMainExecutor(this)
                val biometricPrompt = BiometricPrompt(this, executor, object : BiometricPrompt.AuthenticationCallback() {
                    override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                        super.onAuthenticationSucceeded(result)
                        onSuccess()
                    }

                    override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                        super.onAuthenticationError(errorCode, errString)
                        onFailure()
                    }

                    override fun onAuthenticationFailed() {
                        super.onAuthenticationFailed()
                        onFailure()
                    }
                })

                val promptInfo = BiometricPrompt.PromptInfo.Builder()
                    .setTitle("VaultCam Security Verifier")
                    .setSubtitle("Prove your identity to view secure recordings")
                    .setAllowedAuthenticators(authenticators)
                    .build()

                biometricPrompt.authenticate(promptInfo)
            }
            else -> {
                // Biometrics aren't configured or active on container, trigger passcode keyboard
                onFailure()
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 4819) {
            val cameraIndex = permissions.indexOf(Manifest.permission.CAMERA)
            if (cameraIndex != -1 && grantResults.getOrNull(cameraIndex) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(
                    this,
                    "Camera permission is necessary for background security recording.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
}
