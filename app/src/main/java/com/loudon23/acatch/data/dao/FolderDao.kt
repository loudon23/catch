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
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun getFolders(): Flow<List<FolderItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: FolderItem)

    @Delete
    suspend fun deleteFolder(folder: FolderItem)

    @Query("DELETE FROM folders")
    suspend fun deleteAllFolders()
}