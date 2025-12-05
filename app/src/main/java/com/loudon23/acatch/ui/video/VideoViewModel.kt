package com.loudon23.acatch.ui.video

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import com.loudon23.acatch.data.AppDatabase
import com.loudon23.acatch.data.FolderItem
import com.loudon23.acatch.data.VideoItem
import com.loudon23.acatch.data.VideoRepository
import com.loudon23.acatch.utils.ThumbnailExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    private val _thumbnails: MutableStateFlow<Map<String, Bitmap>> = MutableStateFlow(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails

    val videoListState: StateFlow<List<VideoItem>>
    val folderListState: StateFlow<List<FolderItem>>

    // 현재 선택된 폴더 URI를 저장하는 StateFlow
    private val _currentFolderUri: MutableStateFlow<Uri?> = MutableStateFlow(null)
    val currentFolderUri: StateFlow<Uri?> = _currentFolderUri

    // ExoPlayer 인스턴스를 관리할 맵
    private val _exoPlayers: ConcurrentHashMap<String, ExoPlayer> = ConcurrentHashMap()
    // 현재 재생 중인 비디오의 인덱스를 추적 (VerticalPager용)
    private val _currentlyPlayingIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentlyPlayingIndex: StateFlow<Int> = _currentlyPlayingIndex

    init {
        val database = AppDatabase.getDatabase(application)
        repository = VideoRepository(database.videoDao(), database.folderDao()) // folderDao 추가

        // folderListState는 모든 폴더를 구독합니다.
        folderListState = repository.allFolders.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

        // videoListState는 _currentFolderUri와 repository.allVideos를 결합하여 필터링된 비디오 목록을 제공합니다.
        // 이 StateFlow는 이제 FolderItem을 탭하여 비디오를 표시하는 데 사용되지 않습니다.
        // 대신, FolderItem을 탭하면 VideoDetailScreen으로 직접 이동합니다.
        videoListState = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

        // 기존 비디오 또는 새로 로드된 비디오에 대한 썸네일 추출 로직
        viewModelScope.launch {
            videoListState.collectLatest { videos ->
                if (videos.isNotEmpty()) {
                    extractThumbnailsForVideos(videos)
                } else {
                    _thumbnails.value = emptyMap() // 비디오가 없으면 썸네일도 초기화
                }
            }
        }

        // 초기 currentFolderUri 설정 (더 이상 첫 번째 폴더로 자동 이동하지 않음, 폴더 목록 뷰 유지)
        viewModelScope.launch {
            folderListState.collectLatest { folders ->
                if (_currentFolderUri.value == null && folders.isNotEmpty()) {
                    // 이 로직은 이제 첫 번째 폴더로 자동 이동하지 않도록 비활성화되거나 변경되어야 합니다。
                    // 현재는 기본적으로 null을 유지하여 폴더 목록을 보여줍니다.
                    _currentFolderUri.value = null
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        _exoPlayers.values.forEach { it.release() }
        _exoPlayers.clear()
    }

    // 특정 URI에 대한 ExoPlayer 인스턴스를 가져오거나 새로 생성합니다.
    fun getOrCreatePlayer(videoUri: String): ExoPlayer {
        return _exoPlayers.getOrPut(videoUri) {
            ExoPlayer.Builder(getApplication()).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
                prepare()
                playWhenReady = false // 초기에는 재생하지 않고, UI에서 제어
            }
        }
    }

    // 현재 재생 중인 비디오 인덱스 업데이트 및 인접 비디오 미리 로드
    fun updateCurrentlyPlayingIndex(index: Int) {
        _currentlyPlayingIndex.value = index
        val videoItems = videoListState.value
        if (index >= 0 && index < videoItems.size) {
            val currentVideoUri = videoItems[index].uri
            getOrCreatePlayer(currentVideoUri)

            if (index > 0) {
                val prevVideoUri = videoItems[index - 1].uri
                getOrCreatePlayer(prevVideoUri)
            }
            if (index < videoItems.size - 1) {
                val nextVideoUri = videoItems[index + 1].uri
                getOrCreatePlayer(nextVideoUri)
            }

            val playersToKeep = setOf(
                currentVideoUri,
                videoItems.getOrNull(index - 1)?.uri,
                videoItems.getOrNull(index + 1)?.uri
            ).filterNotNull().toSet()

            _exoPlayers.keys.forEach { uri ->
                if (uri !in playersToKeep) {
                    _exoPlayers.remove(uri)?.release()
                }
            }
        } else {
            _exoPlayers.values.forEach { it.release() }
            _exoPlayers.clear()
        }
    }

    fun scanVideosFromUri(uri: Uri) {
        val contentResolver = getApplication<Application>().contentResolver // Changed here
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val documentFile = DocumentFile.fromTreeUri(getApplication(), uri)
                if (documentFile != null && documentFile.isDirectory) {
                    val topLevelFolderUri = uri.toString()
                    val folderName = documentFile.name ?: "Unknown Folder"

                    val videoList = mutableListOf<VideoItem>()
                    scanDirectory(documentFile, videoList, topLevelFolderUri)
                    
                    // Determine the thumbnail video URI
                    val thumbnailUri = videoList.firstOrNull()?.uri

                    repository.insertFolder(FolderItem(uri = topLevelFolderUri, name = folderName, thumbnailVideoUri = thumbnailUri))
                    repository.insertVideos(videoList)
                    extractThumbnailsForVideos(videoList)
                }

                _currentFolderUri.value = null // After adding, return to folder list view
            }
        }
    }

    // Modified to accept topLevelFolderUri
    private fun scanDirectory(directory: DocumentFile, videoList: MutableList<VideoItem>, topLevelFolderUri: String) {
        for (file in directory.listFiles()) {
            if (file.isDirectory) {
                // Recursively call for subdirectories, but still associate videos with the topLevelFolderUri
                scanDirectory(file, videoList, topLevelFolderUri)
            } else if (file.type?.startsWith("video/") == true) {
                videoList.add(
                    VideoItem(
                        uri = file.uri.toString(),
                        name = file.name ?: "Unknown",
                        duration = 0,
                        size = file.length().toInt(),
                        folderUri = topLevelFolderUri // All videos link to the top-level selected folder
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
            val uri = Uri.parse(videoItem.uri)
            val bitmap = ThumbnailExtractor.extractThumbnail(getApplication(), uri)
            if (bitmap != null) {
                newThumbnails[videoItem.uri] = bitmap
            }
        }
        _thumbnails.value = _thumbnails.value + newThumbnails
    }

    fun deleteVideo(videoItem: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(videoItem)
            _thumbnails.value = _thumbnails.value.filterKeys { it != videoItem.uri }
            _exoPlayers.remove(videoItem.uri)?.release()
            updateCurrentlyPlayingIndex(_currentlyPlayingIndex.value)
        }
    }

    fun clearAllData() { // 함수 이름 변경
        viewModelScope.launch {
            repository.deleteAllFolders()
            repository.deleteAllVideos()
            _thumbnails.value = emptyMap()
            _exoPlayers.values.forEach { it.release() }
            _exoPlayers.clear()
            _currentlyPlayingIndex.value = -1
        }
    }

    // 폴더를 선택하는 새로운 함수 추가 (이제 UI에서 직접 비디오 목록을 표시하지 않음)
    fun selectFolder(uri: Uri?) {
        _currentFolderUri.value = uri
        // 폴더가 변경되면 현재 재생 인덱스 초기화
        _currentlyPlayingIndex.value = -1
        _exoPlayers.values.forEach { it.release() }
        _exoPlayers.clear()
    }

    // 특정 폴더의 비디오 목록을 가져오는 새로운 함수 추가
    fun getVideosForFolder(folderUri: Uri): Flow<List<VideoItem>> {
        return repository.allVideos.map {
            it.filter { video -> video.folderUri == folderUri.toString() }
        }
    }
}