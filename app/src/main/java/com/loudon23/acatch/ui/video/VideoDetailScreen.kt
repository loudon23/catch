package com.loudon23.acatch.ui.video

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loudon23.acatch.data.VideoItem
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
    val folderItems by videoViewModel.folderListState.collectAsState()

    if (folderItems.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text("No folders available.", color = Color.White)
        }
        return
    }

    val initialFolderIndex = remember(folderItems, folderUri) {
        folderItems.indexOfFirst { it.uri == folderUri }.coerceAtLeast(0)
    }

    val verticalPagerState = rememberPagerState(
        initialPage = initialFolderIndex,
        pageCount = { folderItems.size }
    )

    // Stop playback when the screen is disposed.
    DisposableEffect(Unit) {
        onDispose {
            videoViewModel.stopPlayback()
        }
    }

    VerticalPager(
        state = verticalPagerState,
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) { folderIndex ->
        val currentFolder = folderItems[folderIndex]

        val isInitialFolder = currentFolder.uri == folderUri
        val initialVideoForThisPage = if (isInitialFolder) videoUri else null
        val initialIndexForThisPage = if (isInitialFolder) videoIndex else 0

        FolderVideoPager(
            folder = currentFolder,
            initialVideoUri = initialVideoForThisPage,
            initialVideoIndex = initialIndexForThisPage,
            videoViewModel = videoViewModel,
            onDeleteVideo = onDeleteVideo,
            onNavigateBack = onNavigateBack,
            isCurrentFolderPage = verticalPagerState.currentPage == folderIndex && !verticalPagerState.isScrollInProgress
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderVideoPager(
    folder: com.loudon23.acatch.data.FolderItem,
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
}