package com.example.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "recordings")
data class Recording(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val filePath: String, // Path to the secure encrypted file
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long, // in milliseconds
    val fileSize: Long // in bytes
)
