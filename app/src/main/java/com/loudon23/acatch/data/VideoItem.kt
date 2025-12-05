package com.loudon23.acatch.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "videos")
data class VideoItem(
    @PrimaryKey
    val uri: String,
    val name: String,
    val duration: Int,
    val size: Int
)