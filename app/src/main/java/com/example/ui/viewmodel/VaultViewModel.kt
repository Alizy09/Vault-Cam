package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.database.AppDatabase
import com.example.data.database.Recording
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.RecordingRepository
import com.example.data.security.CryptoEngine
import com.example.data.security.IconChanger
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    private val context = application.applicationContext
    private val repository: RecordingRepository
    private val preferenceManager: PreferenceManager

    // App state locking
    private val _isAppLocked = MutableStateFlow(true)
    val isAppLocked: StateFlow<Boolean> = _isAppLocked.asStateFlow()

    // Recording lists
    val recordingsList: StateFlow<List<Recording>>

    // Preference States
    val pinHashState: StateFlow<String?>
    val biometricsEnabledState: StateFlow<Boolean>
    val useFrontCameraState: StateFlow<Boolean>
    val activeIconState: StateFlow<String>

    // Temporary playback file
    private var tempPlaybackFile: File? = null
    private val _playbackFileState = MutableStateFlow<File?>(null)
    val playbackFileState: StateFlow<File?> = _playbackFileState.asStateFlow()

    init {
        val database = AppDatabase.getDatabase(context)
        repository = RecordingRepository(database.recordingDao())
        preferenceManager = PreferenceManager(context)

        // Bind DB lists
        recordingsList = repository.allRecordings.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

        // Bind preference data flows
        pinHashState = preferenceManager.pinHashFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = null
        )

        biometricsEnabledState = preferenceManager.biometricsEnabledFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

        useFrontCameraState = preferenceManager.useFrontCameraFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

        activeIconState = preferenceManager.activeIconFlow.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = PreferenceManager.ICON_DEFAULT
        )
    }

    // Lock controls
    fun unlockApp() {
        _isAppLocked.value = false
    }

    fun lockApp() {
        _isAppLocked.value = true
    }

    // Preferences configuration updates
    fun setupPin(newPin: String) {
        viewModelScope.launch {
            preferenceManager.savePin(newPin)
        }
    }

    fun verifyPin(pin: String, onSuccess: () -> Unit, onFailure: () -> Unit) {
        viewModelScope.launch {
            if (preferenceManager.verifyPin(pin)) {
                _isAppLocked.value = false
                onSuccess()
            } else {
                onFailure()
            }
        }
    }

    fun toggleBiometrics(enabled: Boolean) {
        viewModelScope.launch {
            preferenceManager.setBiometricsEnabled(enabled)
        }
    }

    fun toggleCameraSource(frontCamera: Boolean) {
        viewModelScope.launch {
            preferenceManager.setUseFrontCamera(frontCamera)
        }
    }

    fun changeAppLauncherIcon(iconAliasName: String) {
        viewModelScope.launch {
            preferenceManager.setActiveIcon(iconAliasName)
            IconChanger.applyIconAlias(context, iconAliasName)
        }
    }

    // Secure playback controls
    fun prepareVideoForPlayback(recording: Recording) {
        viewModelScope.launch {
            try {
                // Clear any previous playback file safely
                cleanUpPlaybackFile()

                val sourceFile = File(recording.filePath)
                if (sourceFile.exists()) {
                    if (recording.filePath.endsWith(".enc")) {
                        // Decrypt AES encrypted file only to local private cache directory
                        val playFile = File(context.cacheDir, "play_view_${System.currentTimeMillis()}.mp4")
                        CryptoEngine.decryptFile(sourceFile, playFile)
                        tempPlaybackFile = playFile
                        _playbackFileState.value = playFile
                    } else {
                        // Play direct unencrypted mp4 file
                        _playbackFileState.value = sourceFile
                        tempPlaybackFile = null
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun cleanUpPlaybackFile() {
        try {
            tempPlaybackFile?.let {
                if (it.exists()) {
                    it.delete()
                }
            }
            tempPlaybackFile = null
            _playbackFileState.value = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // Shred files permanently
    fun deleteRecording(recording: Recording) {
        viewModelScope.launch {
            repository.deleteRecording(recording)
        }
    }
}
