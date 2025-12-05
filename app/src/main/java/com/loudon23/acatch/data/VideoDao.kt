package com.loudon23.acatch.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {
    @Query("SELECT * FROM videos")
    fun getVideos(): Flow<List<VideoItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideos(videos: List<VideoItem>)

    @Delete
    suspend fun delete(video: VideoItem)

    @Query("DELETE FROM videos WHERE folderUri = :folderUri")
    suspend fun deleteVideosByFolderUri(folderUri: String)

    @Query("DELETE FROM videos")
    suspend fun deleteAllVideos()
}