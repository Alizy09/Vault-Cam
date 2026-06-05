package com.example.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.camera.core.CameraSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.FileOutputOptions
import androidx.camera.video.Quality
import androidx.camera.video.QualitySelector
import androidx.camera.video.Recorder
import androidx.camera.video.Recording as CameraXRecording
import androidx.camera.video.VideoCapture
import androidx.camera.video.VideoRecordEvent
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleService
import com.example.MainActivity
import com.example.data.database.AppDatabase
import com.example.data.database.Recording
import com.example.data.preferences.PreferenceManager
import com.example.data.repository.RecordingRepository
import com.example.data.security.CryptoEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class BackgroundVideoRecorderService : LifecycleService() {

    companion object {
        private const val CHANNEL_ID = "vaultcam_recorder_service"
        private const val NOTIFICATION_ID = 4829

        const val ACTION_START_RECORDING = "com.example.service.action.START_RECORDING"
        const val ACTION_STOP_RECORDING = "com.example.service.action.STOP_RECORDING"

        private val _isRecording = MutableStateFlow(false)
        val isRecording: StateFlow<Boolean> = _isRecording

        private val _currentRecordDuration = MutableStateFlow(0L) // In seconds
        val currentRecordDuration: StateFlow<Long> = _currentRecordDuration
    }

    private var cameraProvider: ProcessCameraProvider? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var activeRecording: CameraXRecording? = null
    private var tempRecordingFile: File? = null
    private var recordingStartTime = 0L

    private lateinit var repository: RecordingRepository
    private lateinit var preferenceManager: PreferenceManager
    private val serviceScope = CoroutineScope(Dispatchers.IO)

    // Duration timer
    private var durationThread: Thread? = null
    private var isTimerRunning = false

    override fun onCreate() {
        super.onCreate()
        val database = AppDatabase.getDatabase(this)
        repository = RecordingRepository(database.recordingDao())
        preferenceManager = PreferenceManager(this)

        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)

        when (intent?.action) {
            ACTION_START_RECORDING -> {
                if (!_isRecording.value) {
                    startForeground(NOTIFICATION_ID, buildNotification("Preparing camera..."))
                    initializeAndStartRecording()
                }
            }
            ACTION_STOP_RECORDING -> {
                stopRecording()
            }
        }
        return START_STICKY
    }

    private fun initializeAndStartRecording() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraProviderFuture.addListener({
            try {
                cameraProvider = cameraProviderFuture.get()
                serviceScope.launch {
                    val useFront = preferenceManager.useFrontCameraFlow.first()
                    launch(Dispatchers.Main) {
                        bindCameraUseCases(useFront)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                updateNotificationError("Failed to initialize camera")
                stopSelf()
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases(useFrontCamera: Boolean) {
        val provider = cameraProvider ?: return
        provider.unbindAll()

        val cameraSelector = if (useFrontCamera) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }

        // Configure Recorder for high security & device compatibility
        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.SD)) // SD uses less memory & processes extremely fast
            .build()
        videoCapture = VideoCapture.withOutput(recorder)

        try {
            provider.bindToLifecycle(
                this, // This Service is subclass of LifecycleService which implements LifecycleOwner
                cameraSelector,
                videoCapture
            )
            triggerRecording()
        } catch (e: Exception) {
            e.printStackTrace()
            updateNotificationError("Camera binding failed")
            stopSelf()
        }
    }

    private fun triggerRecording() {
        val capture = videoCapture ?: return

        // Create temporary raw recording file
        val tempDir = cacheDir
        tempRecordingFile = File.createTempFile("vault_temp_", ".mp4", tempDir)

        val fileOutputOptions = FileOutputOptions.Builder(tempRecordingFile!!).build()

        val recordBuilder = capture.output
            .prepareRecording(this, fileOutputOptions)

        // Request audio if permission granted
        val audioPermission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (audioPermission == PackageManager.PERMISSION_GRANTED) {
            recordBuilder.withAudioEnabled()
        }

        try {
            _isRecording.value = true
            recordingStartTime = System.currentTimeMillis()
            startTimer()

            activeRecording = recordBuilder.start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when (recordEvent) {
                    is VideoRecordEvent.Start -> {
                        updateNotification("Recording active...")
                    }
                    is VideoRecordEvent.Finalize -> {
                        _isRecording.value = false
                        stopTimer()

                        val error = recordEvent.error
                        val tempFile = tempRecordingFile

                        if (error == VideoRecordEvent.Finalize.ERROR_NONE && tempFile != null && tempFile.exists() && tempFile.length() > 0) {
                            val duration = System.currentTimeMillis() - recordingStartTime
                            encryptAndSave(tempFile, duration)
                        } else {
                            // Cleanup temp file in case of failure
                            tempFile?.delete()
                        }
                        stopSelf()
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            _isRecording.value = false
            stopTimer()
            stopSelf()
        }
    }

    private fun encryptAndSave(tempFile: File, durationMs: Long) {
        serviceScope.launch {
            try {
                // Post-record high performance AES-GCM encryption
                val secureDir = File(filesDir, "vault_recordings")
                if (!secureDir.exists()) secureDir.mkdirs()

                val finalFileName = "rec_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())}.enc"
                val encryptedFile = File(secureDir, finalFileName)

                updateNotification("Securing recording in progress...")

                // File encryption process
                CryptoEngine.encryptFile(tempFile, encryptedFile)

                // Delete the raw, unencrypted temp file
                tempFile.delete()

                // Insert into secure Room SQLite DB
                val recording = Recording(
                    title = "Recording ${SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault()).format(Date())}",
                    filePath = encryptedFile.absolutePath,
                    duration = durationMs,
                    fileSize = encryptedFile.length()
                )
                repository.insertRecording(recording)

                // Notify completion via notification
                showCompletionNotification()
            } catch (e: Exception) {
                e.printStackTrace()
                tempFile.delete()
            }
        }
    }

    private fun stopRecording() {
        activeRecording?.stop()
        activeRecording = null
        _isRecording.value = false
        stopTimer()
    }

    private fun startTimer() {
        _currentRecordDuration.value = 0L
        isTimerRunning = true
        durationThread = Thread {
            while (isTimerRunning) {
                try {
                    Thread.sleep(1000)
                    if (isTimerRunning) {
                        _currentRecordDuration.value += 1
                        updateNotification("Recording Active: ${_currentRecordDuration.value}s")
                    }
                } catch (e: InterruptedException) {
                    break
                }
            }
        }
        durationThread?.start()
    }

    private fun stopTimer() {
        isTimerRunning = false
        durationThread?.interrupt()
        durationThread = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "VaultCam Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun buildNotification(contentText: String): Notification {
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VaultCam Security Active")
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.presence_video_online)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, buildNotification(text))
    }

    private fun updateNotificationError(errorMsg: String) {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("VaultCam Error")
            .setContentText(errorMsg)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .build()
        manager.notify(NOTIFICATION_ID + 1, notification)
    }

    private fun showCompletionNotification() {
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationIntent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            notificationIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Recording Saved Securely")
            .setContentText("The background capture has been fully encrypted & vaulted.")
            .setSmallIcon(android.R.drawable.presence_video_busy)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        manager.notify(NOTIFICATION_ID + 2, notification)
    }

    override fun onDestroy() {
        stopRecording()
        super.onDestroy()
    }
}
