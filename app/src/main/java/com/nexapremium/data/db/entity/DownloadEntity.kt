package com.nexapremium.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "downloads")
data class DownloadEntity(
    @PrimaryKey val id: String,
    val detailPath: String,
    val title: String,
    val coverUrl: String,
    val filePath: String,
    val fileSizeMb: Double,
    val quality: String,
    val downloadManagerId: Long = 0L,
    val timestamp: Long = System.currentTimeMillis()
)
