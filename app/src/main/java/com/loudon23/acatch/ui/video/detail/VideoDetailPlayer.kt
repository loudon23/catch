package com.loudon23.acatch.ui.video.detail

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.item.VideoItem
import com.loudon23.acatch.ui.common.CommonVideoPlayerView
import kotlinx.coroutines.delay

@Composable
fun VideoDetailPlayer(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    isCurrentPage: Boolean,
    onIsPlayingChange: (Boolean) -> Unit,
    onPlayerReady: ((() -> Unit) -> Unit)? = null
) {
    val context = LocalContext.current

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE // Detailed view should repeat
        }
    }

    var progress by remember { mutableFloatStateOf(0f) }
    var isPlaying by remember { mutableStateOf(player.isPlaying) }
    var isVideoRendered by remember { mutableStateOf(false) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
                onIsPlayingChange(playing) // Notify parent
            }

            override fun onRenderedFirstFrame() {
                isVideoRendered = true
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                isVideoRendered = false
            }
        }
        player.addListener(listener)

        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, video, isCurrentPage) {
        if (isCurrentPage) {
            val mediaItem = MediaItem.fromUri(video.uri)
            player.setMediaItem(mediaItem)
            player.prepare()
            player.playWhenReady = true // Start playing when current
            onPlayerReady?.invoke { player.playWhenReady = !player.playWhenReady } // Provide toggle function
        } else {
            player.pause() // Pause if not current page
            // When navigating away, reset the player state for consistency
            progress = 0f
            isPlaying = false
            isVideoRendered = false
            onIsPlayingChange(false)
        }
    }

    LaunchedEffect(player, isCurrentPage) {
        if (isCurrentPage) {
            while (true) {
                val currentProgress = if (player.duration > 0) {
                    player.currentPosition.toFloat() / player.duration.toFloat()
                } else {
                    0f
                }
                if (progress != currentProgress) { // Only update if changed to avoid recomposition
                    progress = currentProgress
                }
                delay(100) // Update progress every 100ms
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        CommonVideoPlayerView(player = player, isPlaying = isCurrentPage && isPlaying)

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