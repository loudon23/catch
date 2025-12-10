package com.loudon23.acatch.data.item

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "folders")
data class FolderItem(
    @PrimaryKey
    val uri: String,
    val name: String,
    val thumbnailVideoUri: String? = null // Add this field for the representative video
)
