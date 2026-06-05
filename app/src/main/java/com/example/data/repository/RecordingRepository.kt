package com.example.data.repository

import com.example.data.database.Recording
import com.example.data.database.RecordingDao
import kotlinx.coroutines.flow.Flow
import java.io.File

class RecordingRepository(private val recordingDao: RecordingDao) {

    val allRecordings: Flow<List<Recording>> = recordingDao.getAllRecordings()

    suspend fun insertRecording(recording: Recording): Long {
        return recordingDao.insertRecording(recording)
    }

    suspend fun deleteRecording(recording: Recording) {
        // Delete the SQL record
        recordingDao.deleteRecording(recording.id)

        // Delete the secure file on disk
        try {
            val file = File(recording.filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun getRecordingById(id: Int): Recording? {
        return recordingDao.getRecordingById(id)
    }
}
