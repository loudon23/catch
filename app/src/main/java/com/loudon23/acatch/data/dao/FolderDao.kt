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
        SELECT f.*, (SELECT COUNT(*) FROM videos v WHERE v.folderUri = f.uri) as videoCount
        FROM folders f
        ORDER BY f.name ASC
    """)
    fun getFoldersWithVideoCount(): Flow<List<FolderWithVideoCount>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderItem)

    @Delete
    suspend fun deleteFolder(folder: FolderItem)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()

    @Query("UPDATE folders SET thumbnailVideoUri = :thumbnailVideoUri WHERE uri = :folderUri")
    suspend fun setFolderThumbnail(folderUri: String, thumbnailVideoUri: String)
}
