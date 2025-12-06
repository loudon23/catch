package com.loudon23.acatch.ui.video.detail

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FolderOpen
import androidx.compose.material.icons.outlined.PushPin
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.FolderItem
import com.loudon23.acatch.ui.video.VideoViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun VideoDetailHorizontalPager(
    folder: FolderItem,
    initialVideoUri: String?,
    initialVideoIndex: Int?,
    videoViewModel: VideoViewModel,
    isCurrentFolderPage: Boolean
) {
    val context = LocalContext.current
    val allVideoItems by videoViewModel.videoListState.collectAsState()
    val thumbnails by videoViewModel.thumbnails.collectAsState()

    val player = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ONE
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player.release()
        }
    }

    val videoItems = remember(allVideoItems, folder.uri) {
        allVideoItems.filter { it.folderUri == folder.uri }
    }

    if (videoItems.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No videos in this folder.", color = Color.White)
        }
        return
    }

    val initialPage = remember(initialVideoUri, initialVideoIndex, videoItems) {
        val indexFromUri = videoItems.indexOfFirst { it.uri == initialVideoUri }
        (initialVideoIndex ?: if (indexFromUri != -1) indexFromUri else 0).coerceIn(0, (videoItems.size - 1).coerceAtLeast(0))
    }

    val horizontalPagerState = rememberPagerState(
        initialPage = initialPage,
        pageCount = { videoItems.size }
    )

    LaunchedEffect(horizontalPagerState.settledPage, isCurrentFolderPage) {
        if (isCurrentFolderPage) {
            videoItems.getOrNull(horizontalPagerState.settledPage)?.let {
                val mediaItem = MediaItem.fromUri(it.uri)
                player.setMediaItem(mediaItem)
                player.prepare()
                player.playWhenReady = true
            } ?: run {
                player.stop()
                player.clearMediaItems()
            }
        } else {
            player.pause()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = horizontalPagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            val videoItem = videoItems.getOrNull(page)
            if (videoItem != null) {
                val thumbnailBitmap = thumbnails[videoItem.uri]
                VideoPagerItem(
                    video = videoItem,
                    thumbnailBitmap = thumbnailBitmap,
                    player = player,
                    isCurrentPage = horizontalPagerState.currentPage == page
                )
            } else {
                Text("Loading video...", color = Color.White)
            }
        }

        PagerIndicator(
            currentPage = horizontalPagerState.currentPage,
            pageCount = videoItems.size,
            modifier = Modifier.align(Alignment.BottomCenter)
        )

        val currentVideoItem = videoItems.getOrNull(horizontalPagerState.currentPage)
        if (currentVideoItem != null) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .navigationBarsPadding()
                    .padding(end = 16.dp, bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                IconButton(onClick = { /* TODO: 좋아요 기능 구현 */ }) {
                    Icon(
                        imageVector = Icons.Outlined.PushPin,
                        contentDescription = "Set as Cover Video",
                        tint = Color.White
                    )
                }
                Spacer(modifier = Modifier.size(8.dp))
                IconButton(onClick = {
                    val folderUri = Uri.parse(currentVideoItem.folderUri)
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
            }
        }
    }
}