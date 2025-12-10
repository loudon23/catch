package com.loudon23.acatch.data.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.loudon23.acatch.data.item.FolderItem
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("""
        SELECT f.*, 
               (SELECT COUNT(*) FROM videos v WHERE v.folderUri = f.uri) as videoCount,
               (SELECT v.uri FROM videos v WHERE v.id = f.coverVideoId) as coverVideoUri
        FROM folders f
        ORDER BY f.name ASC
    """)
    fun getFolderInfo(): Flow<List<FolderInfo>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderItem)

    @Delete
    suspend fun deleteFolder(folder: FolderItem)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("UPDATE folders SET coverVideoId = :coverVideoId WHERE uri = :folderUri")
    suspend fun setFolderCoverVideo(folderUri: String, coverVideoId: Int)
}
