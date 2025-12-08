package com.loudon23.acatch.ui.video.detail

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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import com.loudon23.acatch.ui.video.VideoViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailScreen(
    folderUri: String,
    videoUri: String?,
    videoIndex: Int?,
    videoViewModel: VideoViewModel = viewModel(),
) {
    val folderItems by videoViewModel.folderListState.collectAsState()
    var controlsVisible by remember { mutableStateOf(false) }
    val systemUiController = rememberSystemUiController()

    DisposableEffect(systemUiController) {
        // Set the navigation bar to be transparent
        systemUiController.setSystemBarsColor(
            color = Color.Transparent,
            darkIcons = false
        )

        onDispose {
            // Restore the original system bar color
            systemUiController.setSystemBarsColor(
                color = Color.Black,
                darkIcons = false
            )
        }
    }

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

        VideoDetailHorizontalPager(
            folder = currentFolder,
            initialVideoUri = initialVideoForThisPage,
            initialVideoIndex = initialIndexForThisPage,
            videoViewModel = videoViewModel,
            isCurrentFolderPage = verticalPagerState.currentPage == folderIndex,
            controlsVisible = controlsVisible,
            onControlsVisibleChange = { controlsVisible = it }
        )
    }
}