package com.loudon23.acatch.ui.video

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.media3.common.Player
import com.loudon23.acatch.data.VideoItem

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
        exoPlayer.playWhenReady = isPlaying
        exoPlayer.volume = if (isPlaying) 1f else 0f
        exoPlayer.repeatMode = Player.REPEAT_MODE_ONE // 무한 반복 설정
        // 현재 재생 중인 비디오로 이동하면 처음부터 재생
        if (isPlaying) {
            exoPlayer.seekTo(0)
        }
    }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                currentPlaybackState = playbackState // 플레이어 상태 업데이트
                // 무한루프 설정으로 인해 Player.STATE_ENDED 시점에 다음 페이지로 자동 전환하는 로직은 더 이상 필요 없습니다.
            }
        }
        exoPlayer.addListener(listener)

        onDispose {
            exoPlayer.removeListener(listener)
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
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                androidx.media3.ui.PlayerView(ctx).apply { // 완전히 정규화된 이름 사용
                    player = exoPlayer
                    useController = false // 컨트롤러 사용
                }
            }
        )

        // 플레이어가 준비되지 않았거나 재생 중이 아닐 때 썸네일을 표시
        if (thumbnailBitmap != null && (!isPlaying || currentPlaybackState != Player.STATE_READY)) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
