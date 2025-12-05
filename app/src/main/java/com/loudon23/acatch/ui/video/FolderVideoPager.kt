package com.loudon23.acatch.ui.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.loudon23.acatch.data.FolderItem
import com.loudon23.acatch.data.VideoItem
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun FolderVideoPager(
    folder: FolderItem,
    initialVideoUri: String?,
    initialVideoIndex: Int?,
    videoViewModel: VideoViewModel,
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit,
    isCurrentFolderPage: Boolean
) {
    val scope = rememberCoroutineScope()
    val allVideoItems by videoViewModel.videoListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

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

    // When the horizontal page changes, play the video, but only if it's the active folder.
    LaunchedEffect(horizontalPagerState.settledPage, isCurrentFolderPage) {
        if (isCurrentFolderPage) {
            videoItems.getOrNull(horizontalPagerState.settledPage)?.let {
                videoViewModel.playVideo(it.uri)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val videoItem = videoItems.getOrNull(page)
            if (videoItem != null) {
                val thumbnailBitmap = thumbnails[videoItem.uri]
                VideoPagerItem(
                    video = videoItem,
                    thumbnailBitmap = thumbnailBitmap,
                    player = videoViewModel.player,
                    onDeleteVideo = { itemToDelete ->
                        onDeleteVideo(itemToDelete)
                        if (videoItems.size == 1) {
                            onNavigateBack()
                        } else if (page >= videoItems.size - 1 && videoItems.isNotEmpty()) {
                            scope.launch {
                                val newPage = (horizontalPagerState.currentPage - 1).coerceAtLeast(0)
                                horizontalPagerState.animateScrollToPage(newPage)
                            }
                        }
                    },
                    onNavigateBack = onNavigateBack
                )
            } else {
                Text("Loading video...", color = Color.White)
            }
        }

        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 16.dp)
                .zIndex(1f),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(videoItems.size) {
                val color = if (horizontalPagerState.currentPage == it) Color.White else Color.Gray
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(color)
                )
            }
        }
    }
}