package com.loudon23.acatch.ui.video

import android.Manifest
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.loudon23.acatch.data.FolderItem
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@Composable
fun FolderItemComposable(
    folder: FolderItem,
    thumbnailBitmap: Bitmap?,
    onFolderClick: (Uri) -> Unit
) {
    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f) // 비디오와 동일한 비율 유지
            .padding(4.dp)
            .background(Color.DarkGray)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { // onTap 콜백 추가
                        onFolderClick(Uri.parse(folder.uri))
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
    }
}

@Composable
fun VideoItemSkeleton(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .aspectRatio(9f / 16f)
            .padding(4.dp)
            .background(Color.Gray.copy(alpha = 0.3f))
    )
}

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoListScreen(
    videoViewModel: VideoViewModel = viewModel(),
    onNavigateToDetail: (String, Int) -> Unit // 상세 화면으로 이동하기 위한 콜백 시그니처 변경 (String, Int)
) {
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        Manifest.permission.READ_MEDIA_VIDEO
    } else {
        Manifest.permission.READ_EXTERNAL_STORAGE
    }
    val videoPermissionState = rememberPermissionState(permission)

    val directoryPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree(),
        onResult = { uri ->
            if (uri != null) {
                videoViewModel.scanVideosFromUri(uri)
            }
        }
    )

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val folderItems by videoViewModel.folderListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    VideoAppDrawer(
        drawerState = drawerState,
        scope = scope,
        directoryPickerLauncher = directoryPickerLauncher,
        onClearAllData = { videoViewModel.clearAllData() }
    ) { // content lambda starts here
        Scaffold(
            // TopAppBar가 제거되었으므로 topBar 파라미터도 제거합니다.
        ) { paddingValues ->
            if (videoPermissionState.status.isGranted) {
                if (folderItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("폴더가 없습니다. 새 폴더를 추가해주세요.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(paddingValues),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        itemsIndexed(folderItems, key = { _, folderItem -> folderItem.uri }) { index, folderItem ->
                            val folderThumbnailBitmap = folderItem.thumbnailVideoUri?.let { uri ->
                                thumbnails[uri]
                            }
                            FolderItemComposable(
                                folder = folderItem,
                                thumbnailBitmap = folderThumbnailBitmap,
                                onFolderClick = {
                                    scope.launch {
                                        val videosInFolder = videoViewModel.getVideosForFolder(it).firstOrNull()
                                        if (!videosInFolder.isNullOrEmpty()) {
                                            onNavigateToDetail(videosInFolder.first().uri, 0)
                                        } else {
                                            Toast.makeText(context, "이 폴더에는 비디오가 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = "동영상 라이브러리에 접근하려면 권한이 필요합니다.")
                    Button(onClick = { videoPermissionState.launchPermissionRequest() }) {
                        Text("권한 요청")
                    }
                }
            }
        }
    }
}