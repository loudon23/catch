package com.loudon23.acatch.ui.video

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import com.loudon23.acatch.data.FolderItem

@Composable
fun FolderItemComposable(
    folder: FolderItem,
    thumbnailBitmap: Bitmap?,
    onFolderClick: (Uri) -> Unit,
    onDeleteFolder: (FolderItem) -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f) // 비디오와 동일한 비율 유지
            .padding(4.dp)
            .background(Color.DarkGray)
            .pointerInput(folder) { // Pass folder to restart gesture detection on change
                detectTapGestures(
                    onTap = { 
                        onFolderClick(Uri.parse(folder.uri))
                    },
                    onLongPress = {
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
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

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDeleteFolder(folder)
                    showContextMenu = false
                }
            )
        }
    }
}