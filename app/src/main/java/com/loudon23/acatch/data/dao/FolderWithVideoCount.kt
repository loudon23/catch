package com.loudon23.acatch.data.dao

import androidx.room.Embedded
import com.loudon23.acatch.data.item.FolderItem

data class FolderWithVideoCount(
    @Embedded
    val folder: FolderItem,
    val videoCount: Int
)
