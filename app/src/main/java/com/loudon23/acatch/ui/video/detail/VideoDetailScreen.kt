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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
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
    val context = LocalContext.current

    // Create a new ExoPlayer instance for this screen
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE // Set to repeat the current video
        }
    }

    // Release the player when the composable is disposed
    DisposableEffect(Unit) {
        onDispose {
            exoPlayer.release()
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
            videoViewModel = videoViewModel, // Still needed for thumbnails and video list
            isCurrentFolderPage = verticalPagerState.currentPage == folderIndex && !verticalPagerState.isScrollInProgress,
            player = exoPlayer // Pass the new ExoPlayer instance
        )
    }
}