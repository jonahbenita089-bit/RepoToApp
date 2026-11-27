package com.example.data.repository

import android.util.Base64
import com.example.data.local.DownloadedPackageEntity
import com.example.data.local.RepoForgeDao
import com.example.data.local.SavedRepoEntity
import com.example.data.remote.GitHubArtifactDto
import com.example.data.remote.GitHubJobDto
import com.example.data.remote.GitHubNetworkClient
import com.example.data.remote.GitHubReleaseDto
import com.example.data.remote.GitHubRepoDto
import com.example.data.remote.GitHubUserDto
import com.example.data.remote.GitHubWorkflowDto
import com.example.data.remote.GitHubWorkflowRunDto
import com.example.data.remote.PutFileRequestDto
import com.example.data.remote.WorkflowDispatchRequestDto
import com.example.domain.ArtifactPackageManager
import com.example.domain.ProjectStack
import com.example.domain.RepoSlug
import com.example.domain.StackDetectionResult
import com.example.domain.StackDetector
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject
import retrofit2.HttpException
import java.nio.charset.StandardCharsets

data class InspectedRepository(
    val slug: RepoSlug,
    val repoDto: GitHubRepoDto,
    val branches: List<String>,
    val detection: StackDetectionResult,
    val workflows: List<GitHubWorkflowDto>,
    val recentRuns: List<GitHubWorkflowRunDto>,
    val artifacts: List<GitHubArtifactDto>,
    val releases: List<GitHubReleaseDto>,
)

class RepoForgeRepository(
    private val networkClient: GitHubNetworkClient,
    private val dao: RepoForgeDao,
    private val packageManager: ArtifactPackageManager,
) {
    private val api get() = networkClient.apiService

    fun observeSavedRepos(): Flow<List<SavedRepoEntity>> = dao.observeSavedRepos()

    fun observeDownloadedPackages(): Flow<List<DownloadedPackageEntity>> = dao.observeDownloadedPackages()

    fun setAuthToken(token: String) {
        networkClient.updateToken(token)
    }

    suspend fun verifyToken(token: String): Result<GitHubUserDto> = runCatching {
        networkClient.updateToken(token)
        api.getAuthenticatedUser()
    }

    suspend fun fetchAuthenticatedUserRepos(): Result<List<GitHubRepoDto>> = runCatching {
        api.getAuthenticatedUserRepos()
    }.recoverCatching { throwable ->
        throw IllegalStateException(formatGitHubError(throwable), throwable)
    }

    suspend fun inspectRepository(slug: RepoSlug): Result<InspectedRepository> = runCatching {
        val repoDto = api.getRepository(slug.owner, slug.repo)
        val branches = runCatching {
            api.getBranches(slug.owner, slug.repo).map { it.name }
        }.getOrDefault(listOf(repoDto.defaultBranch))
            .ifEmpty { listOf(repoDto.defaultBranch) }

        val rootContents = runCatching {
            api.getContentsList(slug.owner, slug.repo, path = "", ref = repoDto.defaultBranch)
        }.getOrDefault(emptyList())

        val rootNames = rootContents.map { it.name }
        val packageJsonContent = if (rootNames.any { it.equals("package.json", ignoreCase = true) }) {
            runCatching {
                val fileDto = api.getFileContent(slug.owner, slug.repo, "package.json", repoDto.defaultBranch)
                decodeBase64Content(fileDto.content)
            }.getOrNull()
        } else {
            null
        }

        val detection = StackDetector.detect(
            rootFiles = rootNames,
            packageJsonContent = packageJsonContent,
            repoLanguage = repoDto.language,
        )

        val workflows = runCatching {
            api.getWorkflows(slug.owner, slug.repo).workflows
        }.getOrDefault(emptyList())

        val runs = runCatching {
            api.getWorkflowRuns(slug.owner, slug.repo).workflowRuns
        }.getOrDefault(emptyList())

        val artifacts = runCatching {
            api.getRepositoryArtifacts(slug.owner, slug.repo).artifacts
        }.getOrDefault(emptyList())

        val releases = runCatching {
            api.getReleases(slug.owner, slug.repo)
        }.getOrDefault(emptyList())

        dao.upsertSavedRepo(
            SavedRepoEntity(
                fullName = repoDto.fullName,
                owner = slug.owner,
                repo = slug.repo,
                description = repoDto.description.orEmpty(),
                defaultBranch = repoDto.defaultBranch,
                detectedStack = detection.stack.name,
                avatarUrl = repoDto.owner.avatarUrl.orEmpty(),
                htmlUrl = repoDto.htmlUrl,
                supportsApk = detection.stack.supportsApk,
                supportsIpa = detection.stack.supportsIpa,
                lastInspectedAt = System.currentTimeMillis(),
            ),
        )

        InspectedRepository(
            slug = slug,
            repoDto = repoDto,
            branches = branches,
            detection = detection,
            workflows = workflows,
            recentRuns = runs,
            artifacts = artifacts,
            releases = releases,
        )
    }.recoverCatching { throwable ->
        throw IllegalStateException(formatGitHubError(throwable), throwable)
    }

    suspend fun commitWorkflowToRepo(
        slug: RepoSlug,
        branch: String,
        workflowFileName: String,
        yamlContent: String,
    ): Result<String> = runCatching {
        require(networkClient.currentToken().isNotBlank()) {
            "A GitHub Personal Access Token with 'repo' and 'workflow' scopes is required to push workflow files."
        }
        val path = ".github/workflows/$workflowFileName"
        val existingSha = runCatching {
            api.getFileContent(slug.owner, slug.repo, path = path, ref = branch).sha
        }.getOrNull()

        val encoded = Base64.encodeToString(
            yamlContent.toByteArray(StandardCharsets.UTF_8),
            Base64.NO_WRAP,
        )
        val resp = api.putFileContent(
            owner = slug.owner,
            repo = slug.repo,
            path = path,
            request = PutFileRequestDto(
                message = if (existingSha == null) {
                    "Add RepoForge APK & IPA build workflow"
                } else {
                    "Update RepoForge APK & IPA build workflow"
                },
                content = encoded,
                branch = branch,
                sha = existingSha,
            ),
        )
        resp.commit?.htmlUrl ?: "${slug.htmlUrl}/blob/$branch/$path"
    }.recoverCatching { throwable ->
        throw IllegalStateException(formatGitHubError(throwable), throwable)
    }

    suspend fun triggerWorkflowDispatch(
        slug: RepoSlug,
        workflowId: Long,
        branch: String,
        inputs: Map<String, String>,
    ): Result<Unit> = runCatching {
        require(networkClient.currentToken().isNotBlank()) {
            "A GitHub Personal Access Token with 'workflow' scope is required to trigger builds."
        }
        val resp = api.dispatchWorkflow(
            owner = slug.owner,
            repo = slug.repo,
            workflowId = workflowId,
            body = WorkflowDispatchRequestDto(ref = branch, inputs = inputs),
        )
        if (!resp.isSuccessful) {
            val errorText = resp.errorBody()?.string().orEmpty()
            error(parseGitHubErrorBody(resp.code(), errorText, resp.message()))
        }
    }.recoverCatching { throwable ->
        throw IllegalStateException(formatGitHubError(throwable), throwable)
    }

    suspend fun refreshActionsData(slug: RepoSlug): Result<Triple<List<GitHubWorkflowDto>, List<GitHubWorkflowRunDto>, List<GitHubArtifactDto>>> =
        runCatching {
            val workflows = api.getWorkflows(slug.owner, slug.repo).workflows
            val runs = api.getWorkflowRuns(slug.owner, slug.repo).workflowRuns
            val artifacts = api.getRepositoryArtifacts(slug.owner, slug.repo).artifacts
            Triple(workflows, runs, artifacts)
        }.recoverCatching { throwable ->
            throw IllegalStateException(formatGitHubError(throwable), throwable)
        }

    suspend fun fetchRunJobs(slug: RepoSlug, runId: Long): Result<List<GitHubJobDto>> = runCatching {
        api.getRunJobs(slug.owner, slug.repo, runId).jobs
    }.recoverCatching { throwable ->
        throw IllegalStateException(formatGitHubError(throwable), throwable)
    }

    suspend fun cancelWorkflowRun(slug: RepoSlug, runId: Long): Result<Unit> = runCatching {
        val resp = api.cancelRun(slug.owner, slug.repo, runId)
        if (!resp.isSuccessful) {
            error("Unable to cancel run: HTTP ${resp.code()}")
        }
    }

    suspend fun downloadArtifact(
        repoFullName: String,
        downloadUrl: String,
        fileName: String,
        isZipArchive: Boolean,
    ): Result<List<DownloadedPackageEntity>> = runCatching {
        val packages = packageManager.downloadAndExtractArtifact(
            downloadUrl = downloadUrl,
            suggestedName = fileName,
            repoFullName = repoFullName,
            isZipArchive = isZipArchive,
        ).getOrThrow()

        val now = System.currentTimeMillis()
        packages.map { info ->
            val entity = DownloadedPackageEntity(
                repoFullName = repoFullName,
                fileName = info.fileName,
                filePath = info.filePath,
                packageType = info.packageType,
                sizeBytes = info.sizeBytes,
                sha256 = info.sha256,
                downloadedAt = now,
            )
            dao.insertDownloadedPackage(entity)
            entity
        }
    }

    suspend fun removeSavedRepo(fullName: String) {
        dao.deleteSavedRepo(fullName)
    }

    suspend fun removeDownloadedPackage(pkg: DownloadedPackageEntity) {
        packageManager.deleteLocalFile(pkg.filePath)
        dao.deleteDownloadedPackage(pkg.id)
    }

    private fun decodeBase64Content(encoded: String?): String? {
        if (encoded.isNullOrBlank()) return null
        return runCatching {
            val cleaned = encoded.replace("\n", "").replace("\r", "")
            val bytes = Base64.decode(cleaned, Base64.DEFAULT)
            String(bytes, StandardCharsets.UTF_8)
        }.getOrNull()
    }

    private fun formatGitHubError(throwable: Throwable): String {
        if (throwable is HttpException) {
            val code = throwable.code()
            val body = runCatching { throwable.response()?.errorBody()?.string() }.getOrNull().orEmpty()
            return parseGitHubErrorBody(code, body, throwable.message())
        }
        return throwable.message ?: "Unexpected network error"
    }

    private fun parseGitHubErrorBody(code: Int, body: String, fallback: String): String {
        val apiMessage = runCatching {
            if (body.isNotBlank()) JSONObject(body).optString("message") else ""
        }.getOrDefault("")
        return when (code) {
            401 -> "GitHub token rejected (401). Check your Personal Access Token in the Token tab."
            403 -> if (apiMessage.contains("rate limit", ignoreCase = true)) {
                "GitHub API rate limit reached for unauthenticated requests. Add a GitHub token in the Token tab."
            } else {
                "Permission denied (403): ${apiMessage.ifBlank { "Ensure your token has 'repo' and 'workflow' scopes." }}"
            }
            404 -> "Repository, branch, or workflow not found (404). For private repos, add a GitHub token first."
            422 -> "GitHub rejected workflow input (422): ${apiMessage.ifBlank { fallback }}"
            else -> "GitHub API error ($code): ${apiMessage.ifBlank { fallback }}"
        }
    }
}
