package com.nexapremium.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "my_list")
data class MyListEntity(
    @PrimaryKey val detailPath: String,
    val title: String,
    val coverUrl: String,
    val subjectType: String,
    val releaseYear: Int = 0,
    val score: Double = 0.0,
    val addedTimestamp: Long = System.currentTimeMillis()
)
