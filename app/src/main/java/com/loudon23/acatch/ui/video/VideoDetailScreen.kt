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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loudon23.acatch.data.VideoItem

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