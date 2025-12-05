package com.loudon23.acatch.ui.video

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VideoItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .aspectRatio(9f / 16f)
            .padding(4.dp)
            .background(Color.Gray.copy(alpha = 0.3f))
    )
}