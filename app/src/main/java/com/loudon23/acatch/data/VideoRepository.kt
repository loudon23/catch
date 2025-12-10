package com.loudon23.acatch.data

import com.loudon23.acatch.data.dao.FolderDao
import com.loudon23.acatch.data.dao.FolderInfo
import com.loudon23.acatch.data.dao.VideoDao
import com.loudon23.acatch.data.item.FolderItem
import com.loudon23.acatch.data.item.VideoItem
import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao, private val folderDao: FolderDao) {

    val allVideos: Flow<List<VideoItem>> = videoDao.getVideos()
    val allFolders: Flow<List<FolderInfo>> = folderDao.getFolderInfo()

    suspend fun getFirstVideoInFolder(folderUri: String): VideoItem? {
        return videoDao.getFirstVideoInFolder(folderUri)
    }

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

    suspend fun setFolderCoverVideo(folderUri: String, coverVideoId: Int) {
        folderDao.setFolderCoverVideo(folderUri, coverVideoId)
    }
}