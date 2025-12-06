package com.loudon23.acatch.ui.video.list

import android.graphics.Bitmap
import android.net.Uri
import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.loudon23.acatch.data.FolderItem
import androidx.core.net.toUri

@Composable
fun FolderListItemComposable(
    folder: FolderItem,
    thumbnailBitmap: Bitmap?,
    onFolderClick: (Uri) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit,
    onOpenFolder: (FolderItem) -> Unit,
    onRefreshFolder: (FolderItem) -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { mutableStateOf(false) }
    var pressOffset by remember { mutableStateOf(DpOffset.Zero) }
    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f) // 비디오와 동일한 비율 유지
            .padding(1.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color.DarkGray)
            .pointerInput(folder) { // Pass folder to restart gesture detection on change
                detectTapGestures(
                    onTap = { 
                        onFolderClick(folder.uri.toUri())
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
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = folder.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop // 썸네일이 잘 보이도록 Crop 스케일 사용
            )
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Filled.Folder,
                    contentDescription = "Folder Icon",
                    tint = Color.White,
                    modifier = Modifier.weight(1f) // 아이콘이 공간을 채우도록
                )
                Text(folder.name, color = Color.White, modifier = Modifier.padding(top = 8.dp))
            }
        }

        // 파일 개수 표시
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            Text(
                text = folder.videoCount.toString(),
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
                    onRefreshFolder(folder)
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
                    onDeleteFolder(folder)
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
                    onOpenFolder(folder)
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