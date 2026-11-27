package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [SavedRepoEntity::class, DownloadedPackageEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class RepoForgeDatabase : RoomDatabase() {
    abstract fun repoForgeDao(): RepoForgeDao

    companion object {
        @Volatile
        private var instance: RepoForgeDatabase? = null

        fun getInstance(context: Context): RepoForgeDatabase {
            return instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    RepoForgeDatabase::class.java,
                    "repoforge.db",
                ).build().also { instance = it }
            }
        }
    }
}
