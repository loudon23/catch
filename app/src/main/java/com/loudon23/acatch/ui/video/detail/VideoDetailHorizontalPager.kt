package com.loudon23.acatch.ui.video.detail

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.FolderItem
import com.loudon23.acatch.ui.video.VideoViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailHorizontalPager(
    folder: FolderItem,
    initialVideoUri: String?,
    initialVideoIndex: Int?,
    videoViewModel: VideoViewModel,
    isCurrentFolderPage: Boolean,
    controlsVisible: Boolean,
    onControlsVisibleChange: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val allVideoItems by videoViewModel.videoListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    var progress by remember { mutableFloatStateOf(0f) }

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    LaunchedEffect(player, isCurrentFolderPage) {
        if (isCurrentFolderPage) {
            while (true) {
                progress = if (player.duration > 0) {
                    player.currentPosition.toFloat() / player.duration.toFloat()
                } else {
                    0f
                }
                delay(100) // Update progress every 100ms
            }
        }
    }

    var isPlaying by remember { mutableStateOf(player.isPlaying) }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
        }
    }

    LaunchedEffect(controlsVisible, isPlaying) {
        if (isPlaying && controlsVisible) {
            delay(3000L)
            onControlsVisibleChange(false)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    val videoItems = remember(allVideoItems, folder.uri) {
        allVideoItems.filter { it.folderUri == folder.uri }
    }

    if (videoItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos in this folder.", color = Color.White)
        }
        return
    }

    val initialPage = remember(initialVideoUri, initialVideoIndex, videoItems) {
        val indexFromUri = videoItems.indexOfFirst { it.uri == initialVideoUri }
        (initialVideoIndex ?: if (indexFromUri != -1) indexFromUri else 0).coerceIn(0, (videoItems.size - 1).coerceAtLeast(0))
    }

    val horizontalPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videoItems.size }
    )

    LaunchedEffect(horizontalPagerState.settledPage, isCurrentFolderPage) {
        if (isCurrentFolderPage) {
            videoItems.getOrNull(horizontalPagerState.settledPage)?.let {
                val mediaItem = MediaItem.fromUri(it.uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
            } ?: run {
                player.stop()
                player.clearMediaItems()
            }
        } else {
            player.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        player.playWhenReady = !player.playWhenReady
                        onControlsVisibleChange(true)
                    })
                }
        ) { page ->
            val videoItem = videoItems.getOrNull(page)
            if (videoItem != null) {
                val thumbnailBitmap = thumbnails[videoItem.uri]
                VideoDetailPlayer(
                    video = videoItem,
                    thumbnailBitmap = thumbnailBitmap,
                    player = player,
                    isCurrentPage = horizontalPagerState.currentPage == page,
                    controlsVisible = controlsVisible,
                    progress = progress
                )
            } else {
                Text("Loading video...", color = Color.White)
            }
        }

        if (controlsVisible) {
            PagerIndicator(
                currentPage = horizontalPagerState.currentPage,
                pageCount = videoItems.size,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 16.dp)
            )

            val currentVideoItem = videoItems.getOrNull(horizontalPagerState.currentPage)
            if (currentVideoItem != null) {
                VideoDetailActionButtons(
                    videoItem = currentVideoItem,
                    modifier = Modifier.align(Alignment.BottomEnd)
                )
            }
        }
    }
}