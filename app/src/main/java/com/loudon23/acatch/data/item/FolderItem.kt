package com.loudon23.acatch.data.item

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "folders",
    foreignKeys = [
        ForeignKey(
            entity = VideoItem::class,
            parentColumns = ["id"],
            childColumns = ["coverVideoId"],
            onDelete = ForeignKey.SET_NULL
        )
    ],
    indices = [Index(value = ["uri"], unique = true), Index(value = ["coverVideoId"])]
)
data class FolderItem(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val uri: String,
    val name: String,
    val coverVideoId: Int? = null
)
