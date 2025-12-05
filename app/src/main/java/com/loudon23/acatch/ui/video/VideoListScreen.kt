package com.loudon23.acatch.ui.video

// import android.util.Log // Log import 제거
import android.Manifest
import android.graphics.Bitmap
import android.os.Build
import android.view.HapticFeedbackConstants
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
import androidx.compose.material.icons.filled.CreateNewFolder
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.loudon23.acatch.data.VideoItem
import kotlinx.coroutines.launch

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

    ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            ModalDrawerSheet {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Catch Menu", modifier = Modifier.padding(bottom = 16.dp))
                    NavigationDrawerItem(
                        label = { Text("Add Folder") },
                        selected = false,
                        onClick = {
                            directoryPickerLauncher.launch(null)
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.CreateNewFolder, contentDescription = "Add Folder") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Export Videos") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Export functionality not yet implemented", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.Upload, contentDescription = "Export Videos") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Import Videos") },
                        selected = false,
                        onClick = {
                            Toast.makeText(context, "Import functionality not yet implemented", Toast.LENGTH_SHORT).show()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.Download, contentDescription = "Import Videos") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                    NavigationDrawerItem(
                        label = { Text("Delete All Videos") },
                        selected = false,
                        onClick = {
                            videoViewModel.deleteAllVideos()
                            scope.launch { drawerState.close() }
                        },
                        icon = { Icon(Icons.Filled.DeleteForever, contentDescription = "Delete All Videos") },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            // FAB는 Drawer 내부로 기능이 이동했으므로 제거
        ) { paddingValues ->
            if (videoPermissionState.status.isGranted) {
                val videoItems by videoViewModel.videoListState.collectAsState()
                val thumbnails by videoViewModel.thumbnails.collectAsState()

                if (videoItems.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(paddingValues),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("비디오가 없습니다.")
                    }
                } else {
                    // VideoListScreen에서 직접 LazyVerticalGrid를 렌더링
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.padding(paddingValues)
                    ) {
                        itemsIndexed(videoItems, key = { _, videoItem -> videoItem.uri }) { index, videoItem -> // itemsIndexed 사용
                            VideoThumbnailItem(
                                video = videoItem,
                                thumbnailBitmap = thumbnails[videoItem.uri],
                                onItemClick = { uri -> onNavigateToDetail(uri, index) }, // 클릭 시 상세 화면으로 이동 (index 추가)
                                onDeleteVideo = { item -> videoViewModel.deleteVideo(item) }
                            )
                        }

                        // 스켈레톤 UI 추가 (VideoListScreen에서 관리)
                        val maxVisibleItems = 6
                        val skeletonsToAdd = if (videoItems.size < maxVisibleItems) {
                            maxVisibleItems - videoItems.size
                        } else {
                            0
                        }
                        items(skeletonsToAdd) {
                            VideoItemSkeleton()
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

@Composable
fun VideoThumbnailItem(
    video: VideoItem,
    thumbnailBitmap: Bitmap?,
    onItemClick: (String) -> Unit, // 아이템 클릭 콜백 (URI 전달) - 이 부분은 MainActivity에서 인덱스를 추가하므로 여기서는 String만 받도록 유지
    onDeleteVideo: (VideoItem) -> Unit
) {
    val view = LocalView.current
    var showContextMenu by remember { androidx.compose.runtime.mutableStateOf(false) }
    var pressOffset by remember { androidx.compose.runtime.mutableStateOf(DpOffset.Zero) }

    // VideoThumbnailItem이 구성될 때 썸네일 상태 로깅
    // LaunchedEffect(video.uri, thumbnailBitmap) { // 로그 제거
    //     if (thumbnailBitmap != null) { // 로그 제거
    //         Log.d("VideoThumbnailItem", "Thumbnail loaded for URI: ${video.uri}") // 로그 제거
    //     } else { // 로그 제거
    //         Log.d("VideoThumbnailItem", "Thumbnail NOT loaded for URI: ${video.uri}") // 로그 제거
    //     } // 로그 제거
    // } // 로그 제거

    Box(
        modifier = Modifier
            .aspectRatio(9f / 16f)
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { // onTap 콜백 추가
                        onItemClick(video.uri)
                    },
                    onLongPress = { offset ->
                        view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                        pressOffset = DpOffset(offset.x.dp, offset.y.dp)
                        showContextMenu = true
                    }
                )
            }
    ) {
        if (thumbnailBitmap != null) {
            Image(
                bitmap = thumbnailBitmap.asImageBitmap(),
                contentDescription = video.name,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Gray.copy(alpha = 0.3f)),
                contentAlignment = Alignment.Center
            ) {
                Text("Loading Thumbnail...", color = Color.White)
            }
        }

        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { showContextMenu = false },
            offset = pressOffset
        ) {
            DropdownMenuItem(
                text = { Text("Delete") },
                onClick = {
                    onDeleteVideo(video)
                    showContextMenu = false
                }
            )
        }
    }
}