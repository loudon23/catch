package com.loudon23.acatch.data

import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao, private val folderDao: FolderDao) {

    val allVideos: Flow<List<VideoItem>> = videoDao.getVideos()
    val allFolders: Flow<List<FolderItem>> = folderDao.getFolders()

    suspend fun insertVideos(videos: List<VideoItem>) {
        videoDao.insertVideos(videos)
    }

    suspend fun deleteVideo(video: VideoItem) {
        videoDao.delete(video)
    }

    suspend fun deleteVideosByFolderUri(folderUri: String) { // Add this function
        videoDao.deleteVideosByFolderUri(folderUri)
    }

    suspend fun deleteAllVideos() {
        videoDao.deleteAllVideos()
    }

    suspend fun insertFolder(folder: FolderItem) {
        folderDao.insertFolder(folder)
    }

    suspend fun deleteFolder(folder: FolderItem) { 
        folderDao.deleteFolder(folder)
    }

    suspend fun deleteAllFolders() {
        folderDao.deleteAllFolders()
    }
}