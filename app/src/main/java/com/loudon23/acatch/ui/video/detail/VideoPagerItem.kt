package com.loudon23.acatch.ui.video.detail

import android.content.ActivityNotFoundException
import android.content.Intent // Add this import
import android.net.Uri // Add this import
import android.graphics.Bitmap
import android.util.Log
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext // Add this import
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.loudon23.acatch.data.VideoItem

@Composable
fun VideoPagerItem(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    player: ExoPlayer,
    onDeleteVideo: (VideoItem) -> Unit,
    onNavigateBack: () -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }

    var currentMediaId by remember { mutableStateOf(player.currentMediaItem?.mediaId) }
    var playbackState by remember { mutableStateOf(player.playbackState) }
    val context = LocalContext.current // Get the current context

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                playbackState = state
            }

            override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
                currentMediaId = mediaItem?.mediaId
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
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
        val isCurrentVideo = currentMediaId == video.uri

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    useController = false
                }
            },
            update = { playerView ->
                playerView.player = if (isCurrentVideo) player else null
            }
        )

        if (thumbnailBitmap != null && (!isCurrentVideo || playbackState != Player.STATE_READY)) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 우측 하단에 버튼 목록 추가
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd) // 우측 하단 정렬
                .navigationBarsPadding() // Add this modifier
                .padding(end = 16.dp, bottom = 16.dp), // 패딩 추가
            horizontalAlignment = Alignment.CenterHorizontally // Column 내 아이콘들을 중앙 정렬
        ) {
            IconButton(onClick = { /* TODO: 좋아요 기능 구현 */ }) {
                Icon(
                    imageVector = Icons.Outlined.PushPin,
                    contentDescription = "Set as Cover Video",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.size(8.dp)) // 버튼 사이 간격
            IconButton(onClick = {
                val folderUri = Uri.parse(video.folderUri) // 비디오의 folderUri 사용
                Log.d("VideoPagerItem", "folderUri: $folderUri")
                val intent = Intent(Intent.ACTION_VIEW).apply {
                    setDataAndType(folderUri, "vnd.android.document/directory")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    setPackage("pl.solidexplorer2")
                }
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    Log.w("VideoPagerItem", "Solid Explorer not found, using chooser.", e)
                    val genericIntent = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(folderUri, "vnd.android.document/directory")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    val chooser = Intent.createChooser(genericIntent, "Open folder")
                    context.startActivity(chooser)
                }
            }) {
                Icon(
                    imageVector = Icons.Outlined.FolderOpen,
                    contentDescription = "Open Explorer",
                    tint = Color.White
                )
            }
            // 여기에 다른 버튼들을 추가할 수 있습니다.
        }
    }
}