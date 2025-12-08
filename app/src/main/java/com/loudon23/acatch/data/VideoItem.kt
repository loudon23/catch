package com.loudon23.acatch.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "videos",
    foreignKeys = [
        ForeignKey(
            entity = FolderItem::class,
            parentColumns = ["uri"],
            childColumns = ["folderUri"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["folderUri"])]
)
data class VideoItem(
    @PrimaryKey
    val uri: String,
    val name: String,
    val duration: Int,
    val size: Int,
    val folderUri: String // Add this field to link to FolderItem
)

@Entity(tableName = "folders")
data class FolderItem(
    @PrimaryKey
    val uri: String,
    val name: String,
    val videoCount: Int = 0,
    val thumbnailVideoUri: String? = null // Add this field for the representative video
)