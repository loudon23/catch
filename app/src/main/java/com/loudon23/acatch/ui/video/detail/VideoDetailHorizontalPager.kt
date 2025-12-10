package com.loudon23.acatch.ui.video.detail

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.loudon23.acatch.data.item.FolderItem
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
    val allVideoItems by videoViewModel.videoListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    var currentPageIsPlaying by remember { mutableStateOf(false) }
    var currentVideoPlayerTogglePlay: (() -> Unit)? by remember { mutableStateOf(null) }

    val view = LocalView.current
    LaunchedEffect(controlsVisible) {
        val window = (view.context as? Activity)?.window ?: return@LaunchedEffect
        val insetsController = WindowCompat.getInsetsController(window, view)
        if (controlsVisible) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.hide(WindowInsetsCompat.Type.systemBars())
            insetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    LaunchedEffect(controlsVisible) {
        if (controlsVisible) {
            delay(3000L)
            onControlsVisibleChange(false)
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
        currentPageIsPlaying = false
        currentVideoPlayerTogglePlay = null
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures(onTap = {
                        onControlsVisibleChange(!controlsVisible)
                    })
                }
        ) { page ->
            val videoItem = videoItems.getOrNull(page)
            if (videoItem != null) {
                val thumbnailBitmap = thumbnails[videoItem.uri]
                VideoDetailPlayer(
                    video = videoItem,
                    thumbnailBitmap = thumbnailBitmap,
                    isCurrentPage = horizontalPagerState.currentPage == page,
                    onIsPlayingChange = { playing ->
                        if (horizontalPagerState.currentPage == page) {
                            currentPageIsPlaying = playing
                        }
                    },
                    onPlayerReady = { togglePlayFunction ->
                        if (horizontalPagerState.currentPage == page) {
                            currentVideoPlayerTogglePlay = togglePlayFunction
                        }
                    }
                )
            } else {
                Text("Loading video...", color = Color.White)
            }
        }

        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150)),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                PlayPauseIcon(
                    isPlaying = currentPageIsPlaying,
                    onTogglePlay = { currentVideoPlayerTogglePlay?.invoke() },
                    modifier = Modifier.align(Alignment.Center)
                )

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
}
