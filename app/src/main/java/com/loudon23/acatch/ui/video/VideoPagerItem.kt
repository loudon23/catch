package com.loudon23.acatch.ui.video

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.VideoItem

@Composable
fun VideoPagerItem(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    player: ExoPlayer,
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    
    var currentMediaId by remember { mutableStateOf(player.currentMediaItem?.mediaId) }
    var playbackState by remember { mutableStateOf(player.playbackState) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaId = mediaItem?.mediaId
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
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        pressOffset = DpOffset(offset.x.dp, offset.y.dp)
                        showContextMenu = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        val isCurrentVideo = currentMediaId == video.uri

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply {
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = if (isCurrentVideo) player else null
            }
        )

        if (thumbnailBitmap != null && (!isCurrentVideo || playbackState != Player.STATE_READY)) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}