package com.loudon23.acatch.ui.video

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.loudon23.acatch.data.VideoItem
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailScreen(
    videoUri: String?, // 시작 동영상의 URI (초기 페이지를 찾기 위함)
    videoIndex: Int?, // 시작 동영상의 인덱스 (VerticalPager의 initialPage)
    videoViewModel: VideoViewModel = viewModel(),
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val videoItems by videoViewModel.videoListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    // 초기 페이지 인덱스를 찾습니다.
    val initialPage = remember(videoUri, videoIndex, videoItems) {
        Log.d("VideoDetailScreen", "videoUri: $videoUri, videoIndex: $videoIndex")
        val indexFromUri = videoItems.indexOfFirst { it.uri == videoUri }
        Log.d("VideoDetailScreen", "indexFromUri: $indexFromUri")
        (videoIndex ?: indexFromUri).coerceIn(0, (videoItems.size - 1).coerceAtLeast(0))
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videoItems.size }
    )

    // 페이지 변경 시 현재 재생 중인 비디오 인덱스를 ViewModel에 전달
    LaunchedEffect(pagerState, videoItems.size) {
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            videoViewModel.updateCurrentlyPlayingIndex(page)
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
            ) {
                page ->
                Log.d("VideoDetailScreen", "videoItems: $videoItems")
                Log.d("VideoDetailScreen", "Rendering page: $page")
                val videoItem = videoItems.getOrNull(page)
                if (videoItem != null) {
                    val thumbnailBitmap = thumbnails[videoItem.uri]
                    VideoPagerItem(
                        video = videoItem,
                        thumbnailBitmap = thumbnailBitmap,
                        isPlaying = pagerState.currentPage == page,
                        videoViewModel = videoViewModel,
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
