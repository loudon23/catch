package com.loudon23.acatch.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.loudon23.acatch.data.dao.FolderDao
import com.loudon23.acatch.data.dao.VideoDao
import com.loudon23.acatch.data.item.FolderItem
import com.loudon23.acatch.data.item.VideoItem

@Database(entities = [VideoItem::class, FolderItem::class], version = 6, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao
    abstract fun folderDao(): FolderDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "video_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}