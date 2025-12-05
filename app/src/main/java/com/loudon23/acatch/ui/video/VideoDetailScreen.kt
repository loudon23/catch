package com.loudon23.acatch.ui.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loudon23.acatch.data.VideoItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailScreen(
    folderUri: String,
    videoUri: String?,
    videoIndex: Int?,
    videoViewModel: VideoViewModel = viewModel(),
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val scope = rememberCoroutineScope()

    val allVideoItems by videoViewModel.videoListState.collectAsState()
    val videoItems = remember(allVideoItems, folderUri) {
        allVideoItems.filter { it.folderUri == folderUri }
    }
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    val initialPage = remember(videoUri, videoIndex, videoItems) {
        val indexFromUri = videoItems.indexOfFirst { it.uri == videoUri }
        (videoIndex ?: if (indexFromUri != -1) indexFromUri else 0).coerceIn(0, (videoItems.size - 1).coerceAtLeast(0))
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videoItems.size }
    )

    // When the page changes, play the video for the new page.
    LaunchedEffect(pagerState, videoItems) {
        snapshotFlow { pagerState.settledPage }.collectLatest { page ->
            val item = videoItems.getOrNull(page)
            if (item != null) {
                videoViewModel.playVideo(item.uri)
            } else {
                videoViewModel.stopPlayback()
            }
        }
    }

    // Stop playback when the screen is disposed.
    DisposableEffect(Unit) {
        onDispose {
            videoViewModel.stopPlayback()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        if (videoItems.isEmpty()) {
            Text("No videos available.", color = Color.White)
        } else {
            VerticalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                val videoItem = videoItems.getOrNull(page)
                if (videoItem != null) {
                    val thumbnailBitmap = thumbnails[videoItem.uri]
                    VideoPagerItem(
                        video = videoItem,
                        thumbnailBitmap = thumbnailBitmap,
                        player = videoViewModel.player, // Pass the single player instance
                        onDeleteVideo = { itemToDelete ->
                            onDeleteVideo(itemToDelete)
                            if (videoItems.size == 1) {
                                onNavigateBack()
                            } else if (page >= videoItems.size - 1 && videoItems.isNotEmpty()) {
                                scope.launch {
                                    val newPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(newPage)
                                }
                            }
                        },
                        onNavigateBack = onNavigateBack
                    )
                } else {
                    Text("Loading video...", color = Color.White)
                }
            }
        }
    }
}