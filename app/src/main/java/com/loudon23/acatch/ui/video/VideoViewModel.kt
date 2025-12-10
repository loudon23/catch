package com.loudon23.acatch.ui.video

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.AppDatabase
import com.loudon23.acatch.data.VideoRepository
import com.loudon23.acatch.data.dao.FolderInfo
import com.loudon23.acatch.data.item.FolderItem
import com.loudon23.acatch.data.item.VideoItem
import com.loudon23.acatch.utils.ThumbnailExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    private val _thumbnails: MutableStateFlow<Map<String, Bitmap>> = MutableStateFlow(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails

    val videoListState: StateFlow<List<VideoItem>>
    val folderListState: StateFlow<List<FolderInfo>>

    private val _currentFolderUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    private val _currentlyPlayingFolderUri: MutableStateFlow<String?> = MutableStateFlow(null)
    val currentlyPlayingFolderUri: StateFlow<String?> = _currentlyPlayingFolderUri

    private val _playbackEnded: MutableSharedFlow<Unit> = MutableSharedFlow()
    val playbackEnded: SharedFlow<Unit> = _playbackEnded

    private val _isLoading = MutableStateFlow(true)
    val isLoading: StateFlow<Boolean> = _isLoading

    val player: ExoPlayer

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VideoRepository(database.videoDao(), database.folderDao())

        player = ExoPlayer.Builder(application).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF // Set to OFF for sequential playback
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    if (state == Player.STATE_ENDED) {
                        viewModelScope.launch {
                            _playbackEnded.emit(Unit)
                        }
                    }
                }
            })
        }

        folderListState = repository.allFolders
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = emptyList()
            )

        videoListState = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

        viewModelScope.launch {
            folderListState.first()
            _isLoading.value = false
        }

        viewModelScope.launch {
            videoListState.collectLatest { videos ->
                if (videos.isNotEmpty()) {
                    extractThumbnailsForVideos(videos)
                } else {
                    _thumbnails.value = emptyMap()
                }
            }
        }

        viewModelScope.launch {
            folderListState.collectLatest { folders ->
                if (_currentFolderUri.value == null && folders.isNotEmpty()) {
                    _currentFolderUri.value = null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        player.release()
        _currentlyPlayingFolderUri.value = null
    }

    fun playVideo(videoUri: String) {
        if (player.currentMediaItem?.mediaId == videoUri) {
            return
        }
        val mediaItem = MediaItem.Builder()
            .setUri(videoUri)
            .setMediaId(videoUri)
            .build()
        player.setMediaItem(mediaItem)
        player.prepare()
        player.playWhenReady = true
    }

    fun stopPlayback() {
        player.stop()
        player.clearMediaItems()
    }

    fun setCurrentlyPlayingFolder(folderUri: String?) {
        _currentlyPlayingFolderUri.value = folderUri
        if (folderUri == null) {
            stopPlayback()
        }
    }

    fun scanVideosFromUri(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val documentFile = DocumentFile.fromTreeUri(getApplication(), uri)
                if (documentFile != null && documentFile.isDirectory) {
                    val topLevelFolderUri = uri.toString()
                    val folderName = documentFile.name ?: "Unknown Folder"

                    val videoList = mutableListOf<VideoItem>()
                    scanDirectory(documentFile, videoList, topLevelFolderUri)

                    repository.insertFolder(FolderItem(uri = topLevelFolderUri, name = folderName, coverVideoId = null))
                    repository.insertVideos(videoList)
                    extractThumbnailsForVideos(videoList)

                    val firstVideo = repository.getFirstVideoInFolder(topLevelFolderUri)
                    if (firstVideo != null) {
                        repository.setFolderCoverVideo(topLevelFolderUri, firstVideo.id)
                    }
                }

                _currentFolderUri.value = null
            }
        }
    }

    fun refreshFolder(folderInfo: FolderInfo) {
        viewModelScope.launch {
            repository.deleteVideosByFolderUri(folderInfo.folder.uri)
            scanVideosFromUri(folderInfo.folder.uri.toUri())
        }
    }

    private fun scanDirectory(directory: DocumentFile, videoList: MutableList<VideoItem>, topLevelFolderUri: String) {
        for (file in directory.listFiles()) {
            if (file.isDirectory) {
                scanDirectory(file, videoList, topLevelFolderUri)
            } else if (file.type?.startsWith("video/") == true) {
                videoList.add(
                    VideoItem(
                        uri = file.uri.toString(),
                        name = file.name ?: "Unknown",
                        duration = 0,
                        size = file.length().toInt(),
                        folderUri = topLevelFolderUri
                    )
                )
            }
        }
    }

    private suspend fun extractThumbnailsForVideos(videoList: List<VideoItem>) {
        if (videoList.isEmpty()) {
            return
        }
        val newThumbnails = mutableMapOf<String, Bitmap>()
        for (videoItem in videoList) {
            val uri = videoItem.uri.toUri()
            val bitmap = ThumbnailExtractor.extractThumbnail(getApplication(), uri)
            if (bitmap != null) {
                newThumbnails[videoItem.uri] = bitmap
            }
        }
        _thumbnails.value += newThumbnails
    }

    fun deleteVideo(videoItem: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(videoItem)
            _thumbnails.value = _thumbnails.value.filterKeys { it != videoItem.uri }
            if (player.currentMediaItem?.mediaId == videoItem.uri) {
                stopPlayback()
            }
        }
    }

    fun deleteFolder(folderInfo: FolderInfo) {
        viewModelScope.launch {
            repository.deleteFolder(folderInfo.folder)
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            repository.deleteAllFolders()
            repository.deleteAllVideos()
            _thumbnails.value = emptyMap()
            stopPlayback()
        }
    }

    fun getVideosForFolder(folderUri: Uri): Flow<List<VideoItem>> {
        return repository.allVideos.map {
            it.filter { video -> video.folderUri == folderUri.toString() }
        }
    }

    fun setFolderCover(folderUri: String, videoId: Int) {
        viewModelScope.launch {
            repository.setFolderCoverVideo(folderUri, videoId)
        }
    }
}