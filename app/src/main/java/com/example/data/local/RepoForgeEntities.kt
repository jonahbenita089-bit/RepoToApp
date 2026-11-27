package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "saved_repos")
data class SavedRepoEntity(
    @PrimaryKey val fullName: String,
    val owner: String,
    val repo: String,
    val description: String,
    val defaultBranch: String,
    val detectedStack: String,
    val avatarUrl: String,
    val htmlUrl: String,
    val supportsApk: Boolean,
    val supportsIpa: Boolean,
    val lastInspectedAt: Long,
)

@Entity(tableName = "downloaded_packages")
data class DownloadedPackageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val repoFullName: String,
    val fileName: String,
    val filePath: String,
    val packageType: String,
    val sizeBytes: Long,
    val sha256: String,
    val downloadedAt: Long,
)
