package com.loudon23.acatch.ui.video

import android.app.Application
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loudon23.acatch.data.AppDatabase
import com.loudon23.acatch.data.VideoItem
import com.loudon23.acatch.data.VideoRepository
import com.loudon23.acatch.utils.ThumbnailExtractor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.media3.exoplayer.ExoPlayer // ExoPlayer import 추가
import androidx.media3.common.MediaItem // MediaItem import 추가
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.flow.collectLatest // collectLatest import 추가

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository
    private val _thumbnails: MutableStateFlow<Map<String, Bitmap>> = MutableStateFlow(emptyMap())
    val thumbnails: StateFlow<Map<String, Bitmap>> = _thumbnails

    val videoListState: StateFlow<List<VideoItem>>

    // ExoPlayer 인스턴스를 관리할 맵
    private val _exoPlayers: ConcurrentHashMap<String, ExoPlayer> = ConcurrentHashMap()
    // 현재 재생 중인 비디오의 인덱스를 추적 (VerticalPager용)
    private val _currentlyPlayingIndex: MutableStateFlow<Int> = MutableStateFlow(-1)
    val currentlyPlayingIndex: StateFlow<Int> = _currentlyPlayingIndex

    init {
        // Log.d("VideoViewModel", "VideoViewModel initialized.") // ViewModel 생성 시 로그 제거
        val videoDao = AppDatabase.getDatabase(application).videoDao()
        repository = VideoRepository(videoDao)

        // 모든 비디오를 구독하고 썸네일을 추출합니다.
        videoListState = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )

        // 기존 비디오 또는 새로 로드된 비디오에 대한 썸네일 추출 로직
        viewModelScope.launch {
            videoListState.collectLatest { videos ->
                if (videos.isNotEmpty()) {
                    // Log.d("VideoViewModel", "init: videoListState updated with ${videos.size} videos. Starting thumbnail extraction for existing videos.") // 로그 제거
                    extractThumbnailsForVideos(videos)
                } else {
                    // Log.d("VideoViewModel", "init: videoListState is empty.") // 로그 제거
                    _thumbnails.value = emptyMap() // 비디오가 없으면 썸네일도 초기화
                }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Log.d("VideoViewModel", "VideoViewModel cleared.") // ViewModel 소멸 시 로그 제거
        _exoPlayers.values.forEach { it.release() }
        _exoPlayers.clear()
    }

    // 특정 URI에 대한 ExoPlayer 인스턴스를 가져오거나 새로 생성합니다.
    fun getOrCreatePlayer(videoUri: String): ExoPlayer {
        return _exoPlayers.getOrPut(videoUri) {
            // Log.d("VideoViewModel", "Creating new ExoPlayer for URI: $videoUri") // ExoPlayer 생성 로그 제거
            ExoPlayer.Builder(getApplication()).build().apply {
                setMediaItem(MediaItem.fromUri(Uri.parse(videoUri)))
                prepare()
                playWhenReady = false // 초기에는 재생하지 않고, UI에서 제어
            }
        }
    }

    // 현재 재생 중인 비디오 인덱스 업데이트 및 인접 비디오 미리 로드
    fun updateCurrentlyPlayingIndex(index: Int) {
        // Log.d("VideoViewModel", "updateCurrentlyPlayingIndex called with index: $index") // 로그 제거
        _currentlyPlayingIndex.value = index
        val videoItems = videoListState.value
        if (index >= 0 && index < videoItems.size) {
            val currentVideoUri = videoItems[index].uri
            // 현재 비디오는 항상 준비 (getOrCreatePlayer가 처리)
            getOrCreatePlayer(currentVideoUri)

            // 이전 비디오 미리 로드 (만약 존재한다면)
            if (index > 0) {
                val prevVideoUri = videoItems[index - 1].uri
                getOrCreatePlayer(prevVideoUri)
            }
            // 다음 비디오 미리 로드 (만약 존재한다면)
            if (index < videoItems.size - 1) {
                val nextVideoUri = videoItems[index + 1].uri
                getOrCreatePlayer(nextVideoUri)
            }

            // 현재 인덱스에서 멀리 떨어진 플레이어는 해제 (예: 현재 인덱스 +- 1 범위를 벗어나는 플레이어)
            val playersToKeep = setOf(
                currentVideoUri,
                videoItems.getOrNull(index - 1)?.uri,
                videoItems.getOrNull(index + 1)?.uri
            ).filterNotNull().toSet()

            _exoPlayers.keys.forEach { uri ->
                if (uri !in playersToKeep) {
                    // Log.d("VideoViewModel", "Releasing ExoPlayer for URI (out of range): $uri") // 로그 제거
                    _exoPlayers.remove(uri)?.release()
                }
            }
        } else {
            // 인덱스가 유효하지 않으면 모든 플레이어 해제
            // Log.d("VideoViewModel", "updateCurrentlyPlayingIndex: Invalid index ($index), releasing all ExoPlayers.") // 로그 제거
            _exoPlayers.values.forEach { it.release() }
            _exoPlayers.clear()
        }
    }


    fun scanVideosFromUri(uri: Uri) {
        // Log.d("VideoViewModel", "scanVideosFromUri called with URI: $uri") // 함수 호출 로그 제거
        val contentResolver = getApplication<Application>().contentResolver
        contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val videoList = mutableListOf<VideoItem>()
                val documentFile = DocumentFile.fromTreeUri(getApplication(), uri)
                if (documentFile != null && documentFile.isDirectory) {
                    scanDirectory(documentFile, videoList)
                }
                repository.insertVideos(videoList)
                // Log.d("VideoViewModel", "scanVideosFromUri: Video list scanned, size: ${videoList.size}") // 로그 제거
                extractThumbnailsForVideos(videoList)

                // 새로운 비디오가 추가되면, 기존 플레이어들을 정리하고 다시 프리로드 로직을 실행
                updateCurrentlyPlayingIndex(_currentlyPlayingIndex.value)
            }
        }
    }

    private fun scanDirectory(directory: DocumentFile, videoList: MutableList<VideoItem>) {
        for (file in directory.listFiles()) {
            if (file.isDirectory) {
                scanDirectory(file, videoList)
            } else if (file.type?.startsWith("video/") == true) {
                videoList.add(
                    VideoItem(
                        uri = file.uri.toString(),
                        name = file.name ?: "Unknown",
                        duration = 0,
                        size = file.length().toInt()
                    )
                )
            }
        }
    }

    private suspend fun extractThumbnailsForVideos(videoList: List<VideoItem>) {
        if (videoList.isEmpty()) {
            // Log.d("VideoViewModel", "extractThumbnailsForVideos: videoList is empty, skipping thumbnail extraction.") // 로그 제거
            return
        }
        // Log.d("VideoViewModel", "Starting thumbnail extraction for ${videoList.size} videos.") // 로그 제거
        val newThumbnails = mutableMapOf<String, Bitmap>()
        for (videoItem in videoList) {
            val uri = Uri.parse(videoItem.uri)
            val bitmap = ThumbnailExtractor.extractThumbnail(getApplication(), uri)
            if (bitmap != null) {
                newThumbnails[videoItem.uri] = bitmap
                // Log.d("VideoViewModel", "Thumbnail extracted and added for URI: ${videoItem.uri}") // 로그 제거
            } else {
                // Log.w("VideoViewModel", "Failed to extract thumbnail for URI: ${videoItem.uri}") // 로그 제거
            }
        }
        _thumbnails.value = _thumbnails.value + newThumbnails
        // Log.d("VideoViewModel", "Finished thumbnail extraction. Total thumbnails: ${_thumbnails.value.size}") // 로그 제거
    }

    fun deleteVideo(videoItem: VideoItem) {
        viewModelScope.launch {
            repository.deleteVideo(videoItem)
            _thumbnails.value = _thumbnails.value.filterKeys { it != videoItem.uri }
            // 비디오 삭제 시 해당 ExoPlayer도 해제
            _exoPlayers.remove(videoItem.uri)?.release()
            // 플레이어 목록 정리 및 재로드 로직 실행
            updateCurrentlyPlayingIndex(_currentlyPlayingIndex.value)
        }
    }

    fun deleteAllVideos() {
        viewModelScope.launch {
            repository.deleteAllVideos()
            _thumbnails.value = emptyMap()
            // 모든 ExoPlayer 해제
            _exoPlayers.values.forEach { it.release() }
            _exoPlayers.clear()
            _currentlyPlayingIndex.value = -1 // 인덱스 초기화
        }
    }
}