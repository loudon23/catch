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
import androidx.compose.runtime.LaunchedEffect // Add this import
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
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
import com.loudon23.acatch.data.dao.FolderWithVideoCount
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

    // 1. Reacts to changes in the currently playing folder URI to start/stop the player
    LaunchedEffect(currentlyPlayingFolderUri) {
        currentlyPlayingFolderUri?.let { uri ->
            Log.d("AutoPlayback", "Playback target changed to: $uri, calling playVideo.")
            videoViewModel.playVideo(uri)
        } ?: run {
            Log.d("AutoPlayback", "Playback target is null, calling stopPlayback.")
            videoViewModel.stopPlayback()
        }
    }

    // 2. Determines which video to play based on scroll state and visibility
    LaunchedEffect(folderItems, lazyGridState.isScrollInProgress) {
        // Run only when scrolling has stopped
        if (lazyGridState.isScrollInProgress) return@LaunchedEffect

        if (isLoading || folderItems.isEmpty()) {
            if (currentlyPlayingFolderUri != null) {
                videoViewModel.setCurrentlyPlayingFolder(null)
            }
            return@LaunchedEffect
        }

        val visibleItemsInfo = lazyGridState.layoutInfo.visibleItemsInfo
        val viewportHeight = lazyGridState.layoutInfo.viewportSize.height

        if (visibleItemsInfo.isEmpty()) {
            if (currentlyPlayingFolderUri != null) {
                videoViewModel.setCurrentlyPlayingFolder(null)
            }
            return@LaunchedEffect
        }

        // Find the info for the currently playing item, if it's visible
        val currentPlayingItemInfo = currentlyPlayingFolderUri?.let { playingUri ->
            folderItems.indexOfFirst { it.folder.uri == playingUri }
                .takeIf { it != -1 }
                ?.let { index -> visibleItemsInfo.find { it.index == index } }
        }

        var shouldPlayNewItem = false
        if (currentPlayingItemInfo == null) {
            // Case 1: The currently playing item is not in the viewport, or nothing is playing.
            shouldPlayNewItem = true
        } else {
            // Case 2: The currently playing item is in the viewport. Check its visibility.
            val itemTop = currentPlayingItemInfo.offset.y
            val itemHeight = currentPlayingItemInfo.size.height

            // An item's height can be 0 in some edge cases during composition.
            if (itemHeight > 0) {
                // Calculate the visible height of the item
                val visibleTop = itemTop.coerceAtLeast(0)
                val visibleBottom = (itemTop + itemHeight).coerceAtMost(viewportHeight)
                val visibleHeight = visibleBottom - visibleTop

                // Check if less than half is visible
                if ((visibleHeight.toFloat() / itemHeight) < 0.5f) {
                    shouldPlayNewItem = true
                }
            }
        }

        // If we need to play a new item, find the best candidate.
        if (shouldPlayNewItem) {
            // Priority 1: Find the first fully visible item with a video.
            var newFolderToPlay = visibleItemsInfo
                .asSequence()
                .filter { it.offset.y >= 0 && (it.offset.y + it.size.height) <= viewportHeight }
                .sortedBy { it.index }
                .mapNotNull { folderItems.getOrNull(it.index) }
                .firstOrNull { it.folder.thumbnailVideoUri != null }

            // Priority 2: If none are fully visible, find the most visible item.
            if (newFolderToPlay == null) {
                newFolderToPlay = visibleItemsInfo
                    .asSequence()
                    .mapNotNull { itemInfo ->
                        val folderWithCount = folderItems.getOrNull(itemInfo.index)
                        if (folderWithCount?.folder?.thumbnailVideoUri == null) {
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
                                folderWithCount to visibility
                            }
                        }
                    }
                    .maxByOrNull { it.second }
                    ?.first
            }

            // Only update if the new target is different from the current one to avoid re-seeking.
            if (newFolderToPlay?.folder?.uri != currentlyPlayingFolderUri) {
                videoViewModel.setCurrentlyPlayingFolder(newFolderToPlay?.folder?.uri)
            }
        }
    }

    LaunchedEffect(videoViewModel.playbackEnded) {
        videoViewModel.playbackEnded.collectLatest {
            Log.d("AutoPlayback", "Playback ended event received. URI: $currentlyPlayingFolderUri")

            // 1. Get current layout info
            val visibleItemsInfo = lazyGridState.layoutInfo.visibleItemsInfo
            val viewportHeight = lazyGridState.layoutInfo.viewportSize.height
            if (visibleItemsInfo.isEmpty()) {
                videoViewModel.setCurrentlyPlayingFolder(null)
                return@collectLatest
            }

            // 2. Determine the pool of candidate folders for playback.
            // Priority is given to fully visible folders.
            val fullyVisibleFolders = visibleItemsInfo
                .asSequence()
                .filter { it.offset.y >= 0 && (it.offset.y + it.size.height) <= viewportHeight }
                .sortedBy { it.index }
                .mapNotNull { folderItems.getOrNull(it.index) }
                .filter { it.folder.thumbnailVideoUri != null }
                .toList()

            val candidatePool = fullyVisibleFolders.ifEmpty {
                // Fallback: If no folders are fully visible, consider all visible folders.
                visibleItemsInfo
                    .asSequence()
                    .sortedBy { it.index }
                    .mapNotNull { folderItems.getOrNull(it.index) }
                    .filter { it.folder.thumbnailVideoUri != null }
                    .toList()
            }

            if (candidatePool.isEmpty()) {
                videoViewModel.setCurrentlyPlayingFolder(null) // Nothing to play
                return@collectLatest
            }

            // 3. Find the next folder to play from the candidate pool.
            val currentCandidateIndex = candidatePool.indexOfFirst { it.folder.uri == currentlyPlayingFolderUri }

            val nextFolder = if (currentCandidateIndex != -1 && currentCandidateIndex < candidatePool.size - 1) {
                // If the finished item is in the pool and not the last one, play the next one.
                candidatePool[currentCandidateIndex + 1]
            } else {
                // Otherwise, loop back to the first item in the pool.
                candidatePool.first()
            }

            // 4. Set the new playback target.
            // Avoid re-setting if it's the same, unless it's the only item in the pool (to allow replaying)
            if (nextFolder.folder.uri != currentlyPlayingFolderUri) {
                videoViewModel.setCurrentlyPlayingFolder(nextFolder.folder.uri)
            } else if (candidatePool.size == 1) {
                videoViewModel.playVideo(nextFolder.folder.uri!!) // Re-play the single item
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
            containerColor = Color.Transparent
        ) { paddingValues ->
            if (videoPermissionState.status.isGranted) {
                if (folderItems.isEmpty() && !isLoading) {
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
                        state = lazyGridState, // Pass the lazyGridState
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = paddingValues,
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        if (isLoading) {
                            items(4) { // Show 4 skeletons while loading
                                FolderListItemSkeleton()
                            }
                        } else {
                            itemsIndexed(
                                folderItems,
                                key = { _, folderWithCount -> folderWithCount.folder.uri }) { _, folderWithCount ->
                                val folderThumbnailBitmap = folderWithCount.folder.thumbnailVideoUri?.let { uri ->
                                    thumbnails[uri]
                                }

                                val isPlayingThisFolder = currentlyPlayingFolderUri == folderWithCount.folder.uri && folderWithCount.folder.thumbnailVideoUri != null

                                Log.d("FolderListItem", "Folder: ${folderWithCount.folder.name}, isPlaying: $isPlayingThisFolder")

                                FolderListItemComposable(
                                    folderWithVideoCount = folderWithCount,
                                    thumbnailBitmap = folderThumbnailBitmap,
                                    onFolderClick = {
                                        scope.launch {
                                            val videosInFolder =
                                                videoViewModel.getVideosForFolder(it.folder.uri.toUri()).firstOrNull()
                                            if (!videosInFolder.isNullOrEmpty()) {
                                                // Navigate to detail screen when folder is clicked
                                                onNavigateToDetail(
                                                    folderWithCount.folder.uri,
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
                                        if (currentlyPlayingFolderUri == it.folder.uri) {
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
                                    // Add the new onPlayIconClick lambda here
                                    onPlayIconClick = {
                                        videoViewModel.setCurrentlyPlayingFolder(folderWithCount.folder.uri)
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
