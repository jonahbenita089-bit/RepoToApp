package com.example.data.remote

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class GitHubUserDto(
    val login: String,
    val name: String? = null,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubOwnerDto(
    val login: String,
    @Json(name = "avatar_url") val avatarUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubRepoDto(
    val id: Long,
    val name: String,
    @Json(name = "full_name") val fullName: String,
    val description: String? = null,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "default_branch") val defaultBranch: String,
    val language: String? = null,
    @Json(name = "stargazers_count") val stargazersCount: Int = 0,
    @Json(name = "private") val isPrivate: Boolean = false,
    @Json(name = "pushed_at") val pushedAt: String? = null,
    val owner: GitHubOwnerDto,
)

@JsonClass(generateAdapter = true)
data class GitHubBranchDto(
    val name: String,
)

@JsonClass(generateAdapter = true)
data class GitHubContentItemDto(
    val name: String,
    val path: String,
    val type: String,
    val sha: String? = null,
    val size: Long = 0,
    val content: String? = null,
    val encoding: String? = null,
    @Json(name = "download_url") val downloadUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class PutFileRequestDto(
    val message: String,
    val content: String,
    val branch: String? = null,
    val sha: String? = null,
)

@JsonClass(generateAdapter = true)
data class PutFileCommitDto(
    val sha: String? = null,
    @Json(name = "html_url") val htmlUrl: String? = null,
)

@JsonClass(generateAdapter = true)
data class PutFileResponseDto(
    val content: GitHubContentItemDto? = null,
    val commit: PutFileCommitDto? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowsResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    val workflows: List<GitHubWorkflowDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowDto(
    val id: Long,
    val name: String,
    val path: String,
    val state: String,
    @Json(name = "html_url") val htmlUrl: String,
)

@JsonClass(generateAdapter = true)
data class WorkflowDispatchRequestDto(
    val ref: String,
    val inputs: Map<String, String> = emptyMap(),
)

@JsonClass(generateAdapter = true)
data class GitHubRunsResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    @Json(name = "workflow_runs") val workflowRuns: List<GitHubWorkflowRunDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubWorkflowRunDto(
    val id: Long,
    val name: String? = null,
    @Json(name = "display_title") val displayTitle: String? = null,
    val status: String? = null,
    val conclusion: String? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
    val event: String? = null,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "updated_at") val updatedAt: String? = null,
    @Json(name = "run_number") val runNumber: Int = 0,
    @Json(name = "workflow_id") val workflowId: Long = 0,
)

@JsonClass(generateAdapter = true)
data class GitHubJobsResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    val jobs: List<GitHubJobDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubJobDto(
    val id: Long,
    val name: String,
    val status: String? = null,
    val conclusion: String? = null,
    @Json(name = "started_at") val startedAt: String? = null,
    @Json(name = "completed_at") val completedAt: String? = null,
    val steps: List<GitHubStepDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubStepDto(
    val name: String,
    val status: String? = null,
    val conclusion: String? = null,
    val number: Int = 0,
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactsResponseDto(
    @Json(name = "total_count") val totalCount: Int = 0,
    val artifacts: List<GitHubArtifactDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactWorkflowRunDto(
    val id: Long? = null,
    @Json(name = "head_branch") val headBranch: String? = null,
    @Json(name = "head_sha") val headSha: String? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubArtifactDto(
    val id: Long,
    val name: String,
    @Json(name = "size_in_bytes") val sizeInBytes: Long = 0,
    @Json(name = "archive_download_url") val archiveDownloadUrl: String,
    val expired: Boolean = false,
    @Json(name = "created_at") val createdAt: String? = null,
    @Json(name = "workflow_run") val workflowRun: GitHubArtifactWorkflowRunDto? = null,
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseDto(
    val id: Long,
    @Json(name = "tag_name") val tagName: String,
    val name: String? = null,
    @Json(name = "html_url") val htmlUrl: String,
    @Json(name = "published_at") val publishedAt: String? = null,
    val assets: List<GitHubReleaseAssetDto> = emptyList(),
)

@JsonClass(generateAdapter = true)
data class GitHubReleaseAssetDto(
    val id: Long,
    val name: String,
    val size: Long = 0,
    @Json(name = "browser_download_url") val browserDownloadUrl: String,
    @Json(name = "content_type") val contentType: String? = null,
)
