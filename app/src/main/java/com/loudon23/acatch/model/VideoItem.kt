package com.loudon23.acatch.model

import android.net.Uri

data class VideoItem(
    val id: Int,
    val videoUri: Uri,
    val thumbnailUri: Uri? = null // For future use
)