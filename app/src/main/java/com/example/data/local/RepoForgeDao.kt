package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RepoForgeDao {

    @Query("SELECT * FROM saved_repos ORDER BY lastInspectedAt DESC")
    fun observeSavedRepos(): Flow<List<SavedRepoEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertSavedRepo(repo: SavedRepoEntity)

    @Query("DELETE FROM saved_repos WHERE fullName = :fullName")
    suspend fun deleteSavedRepo(fullName: String)

    @Query("SELECT * FROM downloaded_packages ORDER BY downloadedAt DESC")
    fun observeDownloadedPackages(): Flow<List<DownloadedPackageEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDownloadedPackage(pkg: DownloadedPackageEntity)

    @Query("DELETE FROM downloaded_packages WHERE id = :id")
    suspend fun deleteDownloadedPackage(id: Long)
}
