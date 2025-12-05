package com.loudon23.acatch.ui.video

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun VideoListScreen(
    videoViewModel: VideoViewModel = viewModel(),
    onNavigateToDetail: (String, String, Int) -> Unit // 상세 화면으로 이동하기 위한 콜백 시그니처 변경 (String, String, Int)
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
    val isLoading by videoViewModel.isLoading.collectAsState()

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
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isLoading) {
                        items(4) { // Show 4 skeletons while loading
                            VideoItemSkeleton()
                        }
                    } else if (folderItems.isEmpty()) {
                        item { // Use item scope for single element
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(paddingValues),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("폴더가 없습니다. 새 폴더를 추가해주세요.")
                            }
                        }
                    } else {
                        itemsIndexed(folderItems, key = { _, folderItem -> folderItem.uri }) { _, folderItem ->
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
                                            onNavigateToDetail(folderItem.uri, videosInFolder.first().uri, 0)
                                        } else {
                                            Toast.makeText(context, "이 폴더에는 비디오가 없습니다.", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                onDeleteFolder = {
                                    videoViewModel.deleteFolder(it)
                                },
                                onOpenFolder = { 
                                    val folderUri = it.uri.toUri()
                                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(folderUri, DocumentsContract.getTreeDocumentId(folderUri))

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(documentUri, "vnd.android.document/directory")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        setPackage("pl.solidexplorer2")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(context, "폴더를 열 수 있는 앱을 찾을 수 없습니다.", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onRefreshFolder = {
                                    videoViewModel.refreshFolder(it)
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