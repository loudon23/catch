package com.loudon23.acatch.ui.video.detail

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.VideoItem
import com.loudon23.acatch.ui.common.CommonVideoPlayerView

@Composable
fun VideoDetailPlayer(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    player: ExoPlayer,
    isCurrentPage: Boolean,
    progress: Float
) {
    var isVideoRendered by remember { mutableStateOf(false) }

    DisposableEffect(player, isCurrentPage) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                if (isCurrentPage) {
                    isVideoRendered = true
                }
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                if (isCurrentPage) {
                    isVideoRendered = false
                }
            }
        }
        player.addListener(listener)

        // Reset isVideoRendered immediately when this composable becomes current or player changes
        if (isCurrentPage) {
            isVideoRendered = false
        }

        onDispose {
            player.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CommonVideoPlayerView(player = player, isPlaying = isCurrentPage)

        if (thumbnailBitmap != null && (!isCurrentPage || !isVideoRendered)) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize()
            )
        }

        VideoProgressBar(
            progress = progress,
            modifier = Modifier.align(Alignment.TopCenter)
        )
    }
}