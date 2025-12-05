package com.loudon23.acatch.ui.video

import android.net.Uri
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.loudon23.acatch.data.VideoItem

@Composable
fun VideoScreen(videoItems: List<VideoItem>) {
    var currentlyPlayingUri by remember { mutableStateOf<String?>(null) }
    val videoUris = remember(videoItems) { videoItems.map { it.uri } }

    LaunchedEffect(videoUris) {
        if (currentlyPlayingUri == null && videoUris.isNotEmpty()) {
            currentlyPlayingUri = videoUris.first()
        }
    }

    val onVideoEnded: () -> Unit = {
        val currentIndex = videoUris.indexOf(currentlyPlayingUri)
        val nextIndex = if (currentIndex != -1 && currentIndex < videoUris.size - 1) {
            currentIndex + 1
        } else {
            0 // Loop back to the beginning
        }
        if (videoUris.isNotEmpty()) {
            currentlyPlayingUri = videoUris[nextIndex]
        }
    }

    LazyVerticalGrid(columns = GridCells.Fixed(2)) {
        items(videoItems, key = { it.uri }) { video ->
            VideoPlayerView(
                video = video,
                isPlaying = video.uri == currentlyPlayingUri,
                onVideoEnded = onVideoEnded
            )
        }
    }
}

@Composable
fun VideoPlayerView(
    video: VideoItem,
    isPlaying: Boolean,
    onVideoEnded: () -> Unit
) {
    val context = LocalContext.current
    val exoPlayer = remember(video.uri) {
        ExoPlayer.Builder(context).build().apply {
            setMediaItem(MediaItem.fromUri(Uri.parse(video.uri)))
            prepare()
        }
    }

    LaunchedEffect(isPlaying) {
        exoPlayer.playWhenReady = isPlaying
        exoPlayer.volume = if (isPlaying) 1f else 0f
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    onVideoEnded()
                }
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    AndroidView(
        modifier = Modifier.aspectRatio(9f / 16f),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = exoPlayer
                useController = false
            }
        }
    )
}
