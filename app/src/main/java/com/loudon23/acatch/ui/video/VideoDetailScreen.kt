package com.loudon23.acatch.ui.video

import android.net.Uri
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.VerticalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.loudon23.acatch.data.VideoItem
import android.graphics.Bitmap
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
// import android.util.Log // Log import 제거

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailScreen(
    videoUri: String?, // 시작 동영상의 URI (초기 페이지를 찾기 위함)
    videoIndex: Int?, // 시작 동영상의 인덱스 (VerticalPager의 initialPage)
    videoViewModel: VideoViewModel = viewModel(),
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit
    // onNavigateToNextVideo 콜백은 VerticalPager가 처리하므로 더 이상 필요 없습니다.
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val videoItems by videoViewModel.videoListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    // 초기 페이지 인덱스를 찾습니다.
    val initialPage = remember(videoUri, videoIndex, videoItems) {
        // videoIndex가 유효하면 사용하고, 아니면 videoUri로 찾습니다.
        // 유효한 값이 없으면 0으로 설정합니다。
        val indexFromUri = videoItems.indexOfFirst { it.uri == videoUri }
        (videoIndex ?: indexFromUri).coerceIn(0, (videoItems.size - 1).coerceAtLeast(0))
    }

    val pagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videoItems.size }
    )

    // 페이지 변경 시 현재 재생 중인 비디오 인덱스를 ViewModel에 전달
    LaunchedEffect(pagerState, videoItems.size) {
        // videoItems.size가 변경될 때도 LaunchedEffect가 다시 실행되도록 의존성 추가
        // 이를 통해 비디오 목록이 변경(삭제 등)되었을 때 pagerState.pageCount가 업데이트되고,
        // 현재 페이지가 유효한지 다시 확인하며 ViewModel에 현재 인덱스를 전달할 수 있습니다.
        snapshotFlow { pagerState.currentPage }.collectLatest { page ->
            // Log.d("VideoDetailScreen", "Current page changed to: $page") // 로그 제거
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
                val videoItem = videoItems.getOrNull(page)
                if (videoItem != null) {
                    val thumbnailBitmap = thumbnails[videoItem.uri]
                    VideoPagerItem(
                        video = videoItem,
                        thumbnailBitmap = thumbnailBitmap,
                        isPlaying = pagerState.currentPage == page, // 현재 페이지일 때만 재생
                        videoViewModel = videoViewModel, // ViewModel 전달
                        onDeleteVideo = { itemToDelete ->
                            onDeleteVideo(itemToDelete)
                            // 비디오 삭제 후, 현재 페이지가 유효하지 않으면 뒤로 가기
                            // 이 로직은 삭제 후 Composable 재구성 시 PagerState가 새로운 pageCount를 반영하도록 돕습니다.
                            if (videoItems.size == 1) { // 마지막 비디오를 삭제하면 목록으로 돌아감
                                onNavigateBack()
                            } else if (page >= videoItems.size - 1 && videoItems.isNotEmpty()) {
                                // 마지막 페이지를 삭제하면 이전 페이지로 이동
                                scope.launch {
                                    val newPage = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(newPage)
                                }
                            }
                            // pagerState.pageCount는 videoItems.size에 따라 자동으로 업데이트됩니다.
                        },
                        onNavigateBack = onNavigateBack // 삭제 후 목록으로 돌아가는 용도
                    )
                } else {
                    Text("Loading video...", color = Color.White)
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoPagerItem(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    isPlaying: Boolean, // 이 아이템이 현재 재생 중인지 여부
    videoViewModel: VideoViewModel, // ViewModel 전달
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit // 삭제 후 뒤로가기 처리를 위함
) {
    val context = LocalContext.current
    val view = LocalView.current
    val scope = rememberCoroutineScope()
    var showContextMenu by remember { mutableStateOf(false) }
    var currentPlaybackState by remember { mutableStateOf(Player.STATE_IDLE) } // 현재 플레이어 상태 추가
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    // ViewModel에서 ExoPlayer 인스턴스를 가져옵니다.
    val exoPlayer = remember(video.uri) { // isPlaying에 따라 ExoPlayer 생성/해제 대신, isPlaying에 따라 playWhenReady만 변경
        videoViewModel.getOrCreatePlayer(video.uri)
    }

    // isPlaying 상태가 변경될 때만 playWhenReady와 volume을 업데이트합니다.
    LaunchedEffect(isPlaying, exoPlayer) {
        if (exoPlayer != null) {
            exoPlayer.playWhenReady = isPlaying
            exoPlayer.volume = if (isPlaying) 1f else 0f
            exoPlayer.repeatMode = Player.REPEAT_MODE_ONE // 무한 반복 설정
            // 현재 재생 중인 비디오로 이동하면 처음부터 재생
            if (isPlaying) {
                exoPlayer.seekTo(0)
            }
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                currentPlaybackState = playbackState // 플레이어 상태 업데이트
                // 무한루프 설정으로 인해 Player.STATE_ENDED 시점에 다음 페이지로 자동 전환하는 로직은 더 이상 필요 없습니다.
            }
        }
        if (exoPlayer != null) {
            exoPlayer.addListener(listener)
        }

        onDispose {
            if (exoPlayer != null) {
                // ExoPlayer는 ViewModel에서 관리하므로 여기서 release()하지 않습니다.
                // 다만, 리스너는 제거해야 합니다.
                exoPlayer.removeListener(listener)
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = { offset ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        pressOffset = DpOffset(offset.x.dp, offset.y.dp)
                        showContextMenu = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // ExoPlayer가 존재하면 항상 PlayerView를 렌더링하지만, 썸네일 뒤에 숨겨 둡니다.
        // 비디오가 준비되면 썸네일을 숨깁니다.
        if (exoPlayer != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    androidx.media3.ui.PlayerView(ctx).apply { // 완전히 정규화된 이름 사용
                        player = exoPlayer
                        useController = false // 컨트롤러 사용
                    }
                }
            )
        }

        // 플레이어가 준비되지 않았거나 재생 중이 아닐 때 썸네일을 표시
        if (thumbnailBitmap != null && (!isPlaying || currentPlaybackState != Player.STATE_READY)) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize()
            )
        } else if (exoPlayer == null && thumbnailBitmap == null) {
            // ExoPlayer도 썸네일도 없을 경우 로딩 메시지
            Text(
                text = "Loading video...",
                color = Color.White
            )
        }
    }
}