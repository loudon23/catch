package com.loudon23.acatch.ui.video.list

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.DocumentsContract
import android.util.Log
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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState // Add this import
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect // Add this import
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.loudon23.acatch.ui.video.VideoViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FolderListScreen(
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
    val currentlyPlayingFolderUri by videoViewModel.currentlyPlayingFolderUri.collectAsState()

    val lazyGridState = rememberLazyGridState()

    // --- Auto Playback Logic --- //
    LaunchedEffect(folderItems, lazyGridState.firstVisibleItemIndex, currentlyPlayingFolderUri) {
        if (isLoading || folderItems.isEmpty()) return@LaunchedEffect

        val firstVisibleItem = lazyGridState.layoutInfo.visibleItemsInfo.firstOrNull()?.index
        Log.d("AutoPlayback", "First visible item index: $firstVisibleItem, currentlyPlayingFolderUri: $currentlyPlayingFolderUri")

        // If no video is currently playing, start playing the first visible item's thumbnail video
        if (currentlyPlayingFolderUri == null && firstVisibleItem != null) {
            val firstFolder = folderItems.getOrNull(firstVisibleItem)
            if (firstFolder != null && firstFolder.thumbnailVideoUri != null) {
                Log.d("AutoPlayback", "Starting initial playback for: ${firstFolder.name}")
                videoViewModel.setCurrentlyPlayingFolder(firstFolder.uri)
            } else if (firstFolder != null && firstFolder.thumbnailVideoUri == null) {
                // If first visible has no thumbnail, try to find the next one
                Log.d("AutoPlayback", "First visible folder has no thumbnail, looking for next playable.")
                val nextPlayable = folderItems.drop(firstVisibleItem).firstOrNull { it.thumbnailVideoUri != null }
                if (nextPlayable != null) {
                    videoViewModel.setCurrentlyPlayingFolder(nextPlayable.uri)
                } else {
                    videoViewModel.setCurrentlyPlayingFolder(null)
                }
            } else {
                videoViewModel.setCurrentlyPlayingFolder(null) // Ensure nothing plays if no valid first item
            }
        }

        // Update player when currentlyPlayingFolderUri changes
        currentlyPlayingFolderUri?.let { uri ->
            Log.d("AutoPlayback", "currentlyPlayingFolderUri changed to: $uri, calling playVideo.")
            videoViewModel.playVideo(uri)
        } ?: run {
            Log.d("AutoPlayback", "currentlyPlayingFolderUri is null, calling stopPlayback.")
            videoViewModel.stopPlayback()
        }
    }

    LaunchedEffect(videoViewModel.playbackEnded) {
        videoViewModel.playbackEnded.collectLatest { 
            Log.d("AutoPlayback", "Playback ended event received. currentlyPlayingFolderUri: $currentlyPlayingFolderUri")
            // Find the current playing folder and then the next visible one
            val currentPlayingIndex = folderItems.indexOfFirst { it.uri == currentlyPlayingFolderUri }
            if (currentPlayingIndex != -1) {
                val nextPlayableIndex = (currentPlayingIndex + 1 until folderItems.size).firstOrNull { index ->
                    val folder = folderItems[index]
                    folder.thumbnailVideoUri != null
                    // Removed: && lazyGridState.layoutInfo.visibleItemsInfo.any { it.index == index } // Only consider visible items
                }

                if (nextPlayableIndex != null) {
                    val nextFolder = folderItems[nextPlayableIndex]
                    Log.d("AutoPlayback", "Playing next folder: ${nextFolder.name}")
                    videoViewModel.setCurrentlyPlayingFolder(nextFolder.uri)
                } else {
                    Log.d("AutoPlayback", "No next playable folder found. Stopping playback.")
                    videoViewModel.setCurrentlyPlayingFolder(null)
                }
            } else {
                Log.d("AutoPlayback", "Currently playing folder not found in list. Stopping playback.")
                videoViewModel.setCurrentlyPlayingFolder(null)
            }
        }
    }

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
                    state = lazyGridState, // Pass the lazyGridState
                    modifier = Modifier.padding(paddingValues),
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                    horizontalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                    if (isLoading) {
                        items(4) { // Show 4 skeletons while loading
                            FolderListItemSkeleton()
                        }
                    }
                    else if (folderItems.isEmpty()) {
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
                    }
                    else {
                        itemsIndexed(
                            folderItems,
                            key = { _, folderItem -> folderItem.uri }) { _, folderItem ->
                            val folderThumbnailBitmap = folderItem.thumbnailVideoUri?.let { uri ->
                                thumbnails[uri]
                            }

                            val isPlayingThisFolder = currentlyPlayingFolderUri == folderItem.uri && folderItem.thumbnailVideoUri != null

                            Log.d("FolderListItem", "Folder: ${folderItem.name}, isPlaying: $isPlayingThisFolder")

                            FolderListItemComposable(
                                folder = folderItem,
                                thumbnailBitmap = folderThumbnailBitmap,
                                onFolderClick = {
                                    scope.launch {
                                        val videosInFolder =
                                            videoViewModel.getVideosForFolder(it).firstOrNull()
                                        if (!videosInFolder.isNullOrEmpty()) {
                                            // Navigate to detail screen when folder is clicked
                                            onNavigateToDetail(
                                                folderItem.uri,
                                                videosInFolder.first().uri,
                                                0
                                            )
                                        } else {
                                            Toast.makeText(
                                                context,
                                                "이 폴더에는 비디오가 없습니다.",
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                },
                                onDeleteFolder = {
                                    videoViewModel.deleteFolder(it)
                                    // If the deleted folder was playing, stop playback
                                    if (currentlyPlayingFolderUri == it.uri) {
                                        videoViewModel.setCurrentlyPlayingFolder(null)
                                    }
                                },
                                onOpenFolder = {
                                    val folderUri = it.uri.toUri()
                                    val documentUri = DocumentsContract.buildDocumentUriUsingTree(
                                        folderUri,
                                        DocumentsContract.getTreeDocumentId(folderUri)
                                    )

                                    val intent = Intent(Intent.ACTION_VIEW).apply {
                                        setDataAndType(
                                            documentUri,
                                            "vnd.android.document/directory"
                                        )
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                        setPackage("pl.solidexplorer2")
                                    }
                                    try {
                                        context.startActivity(intent)
                                    } catch (_: ActivityNotFoundException) {
                                        Toast.makeText(
                                            context,
                                            "폴더를 열 수 있는 앱을 찾을 수 없습니다.",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                },
                                onRefreshFolder = {
                                    videoViewModel.refreshFolder(it)
                                },
                                // Pass the new parameters
                                player = videoViewModel.player,
                                onPlayVideo = { videoUri -> videoViewModel.playVideo(videoUri) },
                                onStopPlayback = { videoViewModel.stopPlayback() },
                                isPlaying = isPlayingThisFolder
                            )
                        }
                    }
                }
            }
            else {
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