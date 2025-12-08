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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.loudon23.acatch.data.VideoItem

@Composable
fun VideoDetailPlayer(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    player: ExoPlayer,
    isCurrentPage: Boolean,
    progress: Float
) {
    var playbackState by remember { mutableStateOf(player.playbackState) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }
        }
        player.addListener(listener)
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = if (isCurrentPage) player else null
            }
        )

        if (thumbnailBitmap != null && (!isCurrentPage || playbackState != Player.STATE_READY)) {
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
