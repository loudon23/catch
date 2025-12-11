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
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.loudon23.acatch.ui.video.VideoViewModel
import com.loudon23.acatch.ui.video.SortOrder
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun FolderListScreen(
    videoViewModel: VideoViewModel = viewModel(),
    onNavigateToDetail: (String, String, Int) -> Unit
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

    var showSortDialog by remember { mutableStateOf(false) }
    val currentSortOrder by videoViewModel.sortOrder.collectAsState()

    val folderInfoList by videoViewModel.folderListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()
    val isLoading by videoViewModel.isLoading.collectAsState()
    val currentlyPlayingVideoUri by videoViewModel.currentlyPlayingFolderUri.collectAsState()

    val lazyGridState = rememberLazyGridState()

    if (showSortDialog) {
        SortDialog(
            currentSortOrder = currentSortOrder,
            onSortOrderChange = { newSortOrder ->
                videoViewModel.setSortOrder(newSortOrder)
                showSortDialog = false
            },
            onDismiss = { showSortDialog = false }
        )
    }

    LaunchedEffect(currentlyPlayingVideoUri) {
        currentlyPlayingVideoUri?.let {
            videoViewModel.playVideo(it)
        } ?: run {
            videoViewModel.stopPlayback()
        }
    }

    LaunchedEffect(folderInfoList, lazyGridState.isScrollInProgress) {
        if (lazyGridState.isScrollInProgress) return@LaunchedEffect

        if (isLoading || folderInfoList.isEmpty()) {
            if (currentlyPlayingVideoUri != null) {
                videoViewModel.setCurrentlyPlayingFolder(null)
            }
            return@LaunchedEffect
        }

        val visibleItemsInfo = lazyGridState.layoutInfo.visibleItemsInfo
        val viewportHeight = lazyGridState.layoutInfo.viewportSize.height

        if (visibleItemsInfo.isEmpty()) {
            if (currentlyPlayingVideoUri != null) {
                videoViewModel.setCurrentlyPlayingFolder(null)
            }
            return@LaunchedEffect
        }

        val currentPlayingItemInfo = currentlyPlayingVideoUri?.let { playingUri ->
            folderInfoList.indexOfFirst { it.coverVideoUri == playingUri }
                .takeIf { it != -1 }
                ?.let { index -> visibleItemsInfo.find { it.index == index } }
        }

        var shouldPlayNewItem = false
        if (currentPlayingItemInfo == null) {
            shouldPlayNewItem = true
        } else {
            val itemTop = currentPlayingItemInfo.offset.y
            val itemHeight = currentPlayingItemInfo.size.height
            if (itemHeight > 0) {
                val visibleTop = itemTop.coerceAtLeast(0)
                val visibleBottom = (itemTop + itemHeight).coerceAtMost(viewportHeight)
                val visibleHeight = visibleBottom - visibleTop
                if ((visibleHeight.toFloat() / itemHeight) < 0.5f) {
                    shouldPlayNewItem = true
                }
            }
        }

        if (shouldPlayNewItem) {
            var newFolderToPlay = visibleItemsInfo
                .asSequence()
                .filter { it.offset.y >= 0 && (it.offset.y + it.size.height) <= viewportHeight }
                .sortedBy { it.index }
                .mapNotNull { folderInfoList.getOrNull(it.index) }
                .firstOrNull { it.coverVideoUri != null }

            if (newFolderToPlay == null) {
                newFolderToPlay = visibleItemsInfo
                    .asSequence()
                    .mapNotNull { itemInfo ->
                        val folderInfo = folderInfoList.getOrNull(itemInfo.index)
                        if (folderInfo?.coverVideoUri == null) {
                            null
                        } else {
                            val itemTop = itemInfo.offset.y
                            val itemHeight = itemInfo.size.height
                            if (itemHeight <= 0) {
                                null
                            } else {
                                val visibleTop = itemTop.coerceAtLeast(0)
                                val visibleBottom = (itemTop + itemHeight).coerceAtMost(viewportHeight)
                                val visibleHeight = visibleBottom - visibleTop
                                val visibility = visibleHeight.toFloat() / itemHeight
                                folderInfo to visibility
                            }
                        }
                    }
                    .maxByOrNull { it.second }
                    ?.first
            }

            if (newFolderToPlay?.coverVideoUri != currentlyPlayingVideoUri) {
                videoViewModel.setCurrentlyPlayingFolder(newFolderToPlay?.coverVideoUri)
            }
        }
    }

    LaunchedEffect(videoViewModel.playbackEnded) {
        videoViewModel.playbackEnded.collectLatest {
            val visibleItemsInfo = lazyGridState.layoutInfo.visibleItemsInfo
            val viewportHeight = lazyGridState.layoutInfo.viewportSize.height
            if (visibleItemsInfo.isEmpty()) {
                videoViewModel.setCurrentlyPlayingFolder(null)
                return@collectLatest
            }

            val fullyVisibleFolders = visibleItemsInfo
                .asSequence()
                .filter { it.offset.y >= 0 && (it.offset.y + it.size.height) <= viewportHeight }
                .sortedBy { it.index }
                .mapNotNull { folderInfoList.getOrNull(it.index) }
                .filter { it.coverVideoUri != null }
                .toList()

            val candidatePool = fullyVisibleFolders.ifEmpty {
                visibleItemsInfo
                    .asSequence()
                    .sortedBy { it.index }
                    .mapNotNull { folderInfoList.getOrNull(it.index) }
                    .filter { it.coverVideoUri != null }
                    .toList()
            }

            if (candidatePool.isEmpty()) {
                videoViewModel.setCurrentlyPlayingFolder(null)
                return@collectLatest
            }

            val currentCandidateIndex = candidatePool.indexOfFirst { it.coverVideoUri == currentlyPlayingVideoUri }

            val nextFolder = if (currentCandidateIndex != -1 && currentCandidateIndex < candidatePool.size - 1) {
                candidatePool[currentCandidateIndex + 1]
            } else {
                candidatePool.first()
            }

            if (nextFolder.coverVideoUri != currentlyPlayingVideoUri) {
                videoViewModel.setCurrentlyPlayingFolder(nextFolder.coverVideoUri)
            } else if (candidatePool.size == 1) {
                nextFolder.coverVideoUri?.let { videoViewModel.playVideo(it) }
            }
        }
    }

    VideoAppDrawer(
        drawerState = drawerState,
        scope = scope,
        directoryPickerLauncher = directoryPickerLauncher,
        onClearAllData = { videoViewModel.clearAllData() },
        onSortMenuClick = { showSortDialog = true }
    ) { 
        Scaffold(
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (videoPermissionState.status.isGranted) {
                if (folderInfoList.isEmpty() && !isLoading) {
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
                        state = lazyGridState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = paddingValues,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isLoading) {
                            items(4) { 
                                FolderListItemSkeleton()
                            }
                        } else {
                            itemsIndexed(
                                folderInfoList,
                                key = { _, folderInfo -> folderInfo.folder.uri }) { _, folderInfo ->
                                val folderThumbnailBitmap = folderInfo.coverVideoUri?.let { uri ->
                                    thumbnails[uri]
                                }

                                val isPlayingThisFolder = currentlyPlayingVideoUri == folderInfo.coverVideoUri && folderInfo.coverVideoUri != null

                                FolderListItemComposable(
                                    folderInfo = folderInfo,
                                    thumbnailBitmap = folderThumbnailBitmap,
                                    onFolderClick = {
                                        scope.launch {
                                            val videosInFolder =
                                                videoViewModel.getVideosForFolder(it.folder.uri.toUri()).firstOrNull()
                                            if (!videosInFolder.isNullOrEmpty()) {
                                                onNavigateToDetail(
                                                    folderInfo.folder.uri,
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
                                        if (currentlyPlayingVideoUri == it.coverVideoUri) {
                                            videoViewModel.setCurrentlyPlayingFolder(null)
                                        }
                                    },
                                    onOpenFolder = {
                                        val folderUri = it.folder.uri.toUri()
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
                                    onPlayIconClick = {
                                        videoViewModel.setCurrentlyPlayingFolder(folderInfo.coverVideoUri)
                                    },
                                    player = videoViewModel.player,
                                    onPlayVideo = { videoUri -> videoViewModel.playVideo(videoUri) },
                                    onStopPlayback = { videoViewModel.stopPlayback() },
                                    isPlaying = isPlayingThisFolder
                                )
                            }
                        }
                    }
                }
            }
            else {
                Column(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
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
