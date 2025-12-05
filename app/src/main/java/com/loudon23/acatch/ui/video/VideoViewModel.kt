package com.loudon23.acatch.ui.video

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.loudon23.acatch.data.AppDatabase
import com.loudon23.acatch.data.VideoItem
import com.loudon23.acatch.data.VideoRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class VideoViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: VideoRepository

    val videoListState: StateFlow<List<VideoItem>>

    init {
        val videoDao = AppDatabase.getDatabase(application).videoDao()
        repository = VideoRepository(videoDao)
        videoListState = repository.allVideos.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList()
        )
    }

    fun scanVideosFromUri(uri: Uri) {
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
                        duration = 0, // Not easily available from DocumentFile
                        size = file.length().toInt()
                    )
                )
            }
        }
    }
}