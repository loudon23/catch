package com.loudon23.acatch.data

import kotlinx.coroutines.flow.Flow

class VideoRepository(private val videoDao: VideoDao) {

    val allVideos: Flow<List<VideoItem>> = videoDao.getVideos()

    suspend fun insertVideos(videos: List<VideoItem>) {
        videoDao.insertVideos(videos)
    }

    suspend fun deleteVideo(video: VideoItem) {
        videoDao.delete(video)
    }

    suspend fun deleteAllVideos() {
        videoDao.deleteAllVideos()
    }
}