package com.loudon23.acatch.ui.video.list

import android.graphics.Bitmap
import android.view.HapticFeedbackConstants
import androidx.annotation.OptIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import com.loudon23.acatch.data.dao.FolderInfo
import com.loudon23.acatch.ui.common.CommonVideoPlayerView

@OptIn(UnstableApi::class)
@Composable
fun FolderListItemComposable(
    folderInfo: FolderInfo,
    thumbnailBitmap: Bitmap?,
    onFolderClick: (FolderInfo) -> Unit,
    onDeleteFolder: (FolderInfo) -> Unit,
    onOpenFolder: (FolderInfo) -> Unit,
    onRefreshFolder: (FolderInfo) -> Unit,
    player: ExoPlayer,
    onPlayVideo: (String) -> Unit,
    onStopPlayback: () -> Unit,
    isPlaying: Boolean,
    onPlayIconClick: () -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current
    var isVideoRendered by remember { mutableStateOf(false) }

    val folder = folderInfo.folder

    LaunchedEffect(isPlaying, folderInfo.coverVideoUri) {
        val videoUri = folderInfo.coverVideoUri
        if (isPlaying && videoUri != null) {
            onPlayVideo(videoUri)
        } else {
            isVideoRendered = false
        }
    }

    DisposableEffect(player, isPlaying) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                super.onRenderedFirstFrame()
                isVideoRendered = true
            }
        }

        if (isPlaying) {
            player.addListener(listener)
        }

        onDispose {
            player.removeListener(listener)
        }
    }

    Box(
        modifier = Modifier
            .aspectRatio(464f / 688f) 
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .pointerInput(folder) {
                detectTapGestures(
                    onTap = {
                        onFolderClick(folderInfo)
                    },
                    onLongPress = { offset ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        pressOffset = with(density) {
                            DpOffset(offset.x.toDp(), offset.y.toDp())
                        }
                        showContextMenu = true
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        if (isPlaying && folderInfo.coverVideoUri != null) {
            CommonVideoPlayerView(player = player, isPlaying = isPlaying, resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM)
        }

        if (!isVideoRendered) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap.asImageBitmap(),
                    contentDescription = folder.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize(),
                    contentAlignment = Alignment.BottomStart
                ) {
                    IconButton(onClick = onPlayIconClick, modifier = Modifier.padding(0.dp)) {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Play Icon",
                            tint = Color.White
                        )
                    }
                }
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        imageVector = Icons.Filled.Folder,
                        contentDescription = "Folder Icon",
                        tint = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                    Text(folder.name, color = Color.White, modifier = Modifier.padding(top = 8.dp))
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = folderInfo.videoCount.toString(),
                color = Color.White,
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier
                    .background(color = Color.Black.copy(alpha = 0.5f), shape = RoundedCornerShape(4.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = pressOffset
        ) {
            DropdownMenuItem(
                text = { Text("Refresh") },
                onClick = {
                    onRefreshFolder(folderInfo)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Refresh,
                        contentDescription = "Refresh"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDeleteFolder(folderInfo)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.Delete,
                        contentDescription = "Delete"
                    )
                }
            )
            DropdownMenuItem(
                text = { Text("Open Explorer") },
                onClick = {
                    onOpenFolder(folderInfo)
                    showContextMenu = false
                },
                leadingIcon = {
                    Icon(
                        Icons.Outlined.FolderOpen,
                        contentDescription = "Open Explorer"
                    )
                }
            )
        }
    }
}
