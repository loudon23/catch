package com.loudon23.acatch.ui.video.detail

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VideoProgressBar(progress: Float, modifier: Modifier = Modifier) {
    LinearProgressIndicator(
        progress = { progress },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp) // Add horizontal padding to avoid rounded corners
            .height(1.dp),
        color = Color.Gray,
        trackColor = Color.Gray.copy(alpha = 0.3f)
    )
}
