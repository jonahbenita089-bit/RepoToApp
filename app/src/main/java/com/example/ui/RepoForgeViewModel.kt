package com.example.ui

import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CloudAccountRepository
import com.example.data.firebase.CloudSavedRepo
import com.example.data.firebase.ContactMethod
import com.example.data.firebase.UserProfile
import com.example.data.local.DownloadedPackageEntity
import com.example.data.local.SavedRepoEntity
import com.example.data.local.TokenPreferences
import com.example.data.local.UserPreferencesState
import com.example.data.remote.GitHubArtifactDto
import com.example.data.remote.GitHubJobDto
import com.example.data.remote.GitHubNetworkClient
import com.example.data.remote.GitHubRepoDto
import com.example.data.remote.GitHubUserDto
import com.example.data.repository.InspectedRepository
import com.example.data.repository.RepoForgeRepository
import com.example.domain.ArtifactPackageManager
import com.example.domain.ProjectStack
import com.example.domain.RepoInputParser
import com.example.domain.WorkflowConfig
import com.example.domain.WorkflowGenerator
import com.example.ui.screens.PendingAccountSetup
import com.example.ui.theme.ThemePreference
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface RepoInspectState {
    data object Idle : RepoInspectState
    data object Loading : RepoInspectState
    data class Success(val repo: InspectedRepository) : RepoInspectState
    data class Error(val message: String) : RepoInspectState
}

data class RepoForgeUiState(
    val repoInput: String = "",
    val inspectState: RepoInspectState = RepoInspectState.Idle,
    val workflowConfig: WorkflowConfig = WorkflowConfig(stack = ProjectStack.FLUTTER),
    val generatedYaml: String = WorkflowGenerator.generate(WorkflowConfig(stack = ProjectStack.FLUTTER)),
    val isCommittingWorkflow: Boolean = false,
    val isTriggeringBuild: Boolean = false,
    val isRefreshingRuns: Boolean = false,
    val downloadingItemId: Long? = null,
    val selectedRunJobs: Map<Long, List<GitHubJobDto>> = emptyMap(),
    val loadingJobsRunId: Long? = null,
    val authenticatedUser: GitHubUserDto? = null,
    val myGitHubRepos: List<GitHubRepoDto> = emptyList(),
    val isLoadingMyRepos: Boolean = false,
    val isVerifyingToken: Boolean = false,
    val tokenStatusMessage: String? = null,
    val cloudProfile: UserProfile? = null,
    val isSavingProfile: Boolean = false,
    val cloudError: String? = null,
    val preferences: UserPreferencesState = UserPreferencesState(),
    val savedRepos: List<SavedRepoEntity> = emptyList(),
    val cloudSavedRepos: List<CloudSavedRepo> = emptyList(),
    val downloadedPackages: List<DownloadedPackageEntity> = emptyList(),
)

sealed interface UiEvent {
    data class ShowMessage(val message: String) : UiEvent
    data class NavigateToTab(val route: String) : UiEvent
}

class RepoForgeViewModel(
    private val networkClient: GitHubNetworkClient,
    private val packageManager: ArtifactPackageManager,
    private val repository: RepoForgeRepository,
    private val cloudRepository: CloudAccountRepository,
    private val tokenPreferences: TokenPreferences,
    private val currentUserId: String,
    initialPendingSetup: PendingAccountSetup?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RepoForgeUiState())
    val uiState: StateFlow<RepoForgeUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<UiEvent>()
    val events: SharedFlow<UiEvent> = _events.asSharedFlow()

    private var pollingJob: Job? = null

    init {
        initializeUserProfile(initialPendingSetup)
        observeCloudData()

        viewModelScope.launch {
            tokenPreferences.preferencesFlow.collect { prefs ->
                val tokenChanged = prefs.githubToken != networkClient.currentToken()
                repository.setAuthToken(prefs.githubToken)
                _uiState.update { it.copy(preferences = prefs) }
                if (tokenChanged && prefs.githubToken.isNotBlank()) {
                    verifySavedTokenSilently(prefs.githubToken)
                } else if (prefs.githubToken.isBlank()) {
                    _uiState.update {
                        it.copy(
                            authenticatedUser = null,
                            myGitHubRepos = emptyList(),
                            tokenStatusMessage = null,
                        )
                    }
                }
            }
        }

        viewModelScope.launch {
            repository.observeSavedRepos().collect { repos ->
                _uiState.update { it.copy(savedRepos = repos) }
            }
        }

        viewModelScope.launch {
            repository.observeDownloadedPackages().collect { packages ->
                _uiState.update { it.copy(downloadedPackages = packages) }
            }
        }
    }

    private fun initializeUserProfile(pendingSetup: PendingAccountSetup?) {
        viewModelScope.launch {
            val firebaseUser = Firebase.auth.currentUser
            if (firebaseUser == null || firebaseUser.uid != currentUserId) return@launch

            val existing = runCatching {
                cloudRepository.observeUserProfile(currentUserId).firstOrNull()
            }.getOrNull()

            if (pendingSetup != null) {
                val resolvedName = pendingSetup.displayName.ifBlank {
                    firebaseUser.displayName ?: "RepoForge Developer"
                }
                val resolvedEmail = pendingSetup.contactEmail.ifBlank {
                    firebaseUser.email.orEmpty()
                }
                cloudRepository.upsertUserProfile(
                    displayName = resolvedName,
                    preferredContactType = pendingSetup.preferredContactType,
                    contactEmail = resolvedEmail.ifBlank { null },
                    phoneNumber = pendingSetup.phoneNumber.ifBlank { null },
                    photoUrl = firebaseUser.photoUrl?.toString(),
                ).onFailure { err ->
                    _uiState.update { it.copy(cloudError = err.message) }
                }
            } else if (existing == null) {
                cloudRepository.upsertUserProfile(
                    displayName = firebaseUser.displayName ?: "RepoForge Developer",
                    preferredContactType = ContactMethod.GOOGLE,
                    contactEmail = firebaseUser.email,
                    phoneNumber = firebaseUser.phoneNumber,
                    photoUrl = firebaseUser.photoUrl?.toString(),
                )
            }
        }
    }

    private fun observeCloudData() {
        viewModelScope.launch {
            cloudRepository.observeUserProfile(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing user profile", rootError)
                    _uiState.update { it.copy(cloudError = rootError.message) }
                }
                .collect { profile ->
                    _uiState.update { it.copy(cloudProfile = profile, cloudError = null) }
                }
        }

        viewModelScope.launch {
            cloudRepository.observeSavedRepos(currentUserId)
                .catch { error ->
                    val firestoreError = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (firestoreError == null && error is CancellationException) throw error
                    val rootError = firestoreError ?: error
                    Log.w(TAG, "Error observing cloud saved repos", rootError)
                    _uiState.update { it.copy(cloudError = rootError.message) }
                }
                .collect { cloudRepos ->
                    _uiState.update { it.copy(cloudSavedRepos = cloudRepos) }
                }
        }
    }

    fun updateCloudProfile(
        displayName: String,
        preferredContactType: ContactMethod,
        contactEmail: String,
        phoneNumber: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSavingProfile = true) }
            val photoUrl = Firebase.auth.currentUser?.photoUrl?.toString()
                ?: _uiState.value.cloudProfile?.photoUrl
            cloudRepository.upsertUserProfile(
                displayName = displayName,
                preferredContactType = preferredContactType,
                contactEmail = contactEmail.ifBlank { null },
                phoneNumber = phoneNumber.ifBlank { null },
                photoUrl = photoUrl,
            ).onSuccess {
                _uiState.update { it.copy(isSavingProfile = false) }
                _events.emit(UiEvent.ShowMessage("Account profile updated"))
            }.onFailure { err ->
                _uiState.update { it.copy(isSavingProfile = false) }
                _events.emit(UiEvent.ShowMessage(err.message ?: "Failed to update profile"))
            }
        }
    }

    private fun verifySavedTokenSilently(token: String) {
        viewModelScope.launch {
            repository.verifyToken(token)
                .onSuccess { user ->
                    _uiState.update {
                        it.copy(
                            authenticatedUser = user,
                            tokenStatusMessage = "Connected as @${user.login}",
                        )
                    }
                    loadMyGitHubRepos()
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            authenticatedUser = null,
                            myGitHubRepos = emptyList(),
                            tokenStatusMessage = "Saved token could not be verified",
                        )
                    }
                }
        }
    }

    fun loadMyGitHubRepos() {
        if (networkClient.currentToken().isBlank()) return
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingMyRepos = true) }
            repository.fetchAuthenticatedUserRepos()
                .onSuccess { repos ->
                    _uiState.update { it.copy(isLoadingMyRepos = false, myGitHubRepos = repos) }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isLoadingMyRepos = false) }
                    _events.emit(UiEvent.ShowMessage(err.message ?: "Failed to load GitHub repositories"))
                }
        }
    }

    fun updateRepoInput(input: String) {
        _uiState.update { it.copy(repoInput = input) }
    }

    fun inspectRepository(rawInput: String = _uiState.value.repoInput) {
        val slug = RepoInputParser.parse(rawInput)
        if (slug == null) {
            _uiState.update {
                it.copy(
                    inspectState = RepoInspectState.Error(
                        "Enter a valid GitHub repository such as owner/repo or https://github.com/owner/repo",
                    ),
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                repoInput = slug.fullName,
                inspectState = RepoInspectState.Loading,
                selectedRunJobs = emptyMap(),
            )
        }

        viewModelScope.launch {
            repository.inspectRepository(slug)
                .onSuccess { inspected ->
                    val stack = inspected.detection.stack
                    val newConfig = WorkflowConfig(
                        stack = stack,
                        branch = inspected.repoDto.defaultBranch,
                        buildApk = stack.supportsApk,
                        buildIpa = stack.supportsIpa,
                        buildMode = _uiState.value.workflowConfig.buildMode,
                        iosPackagingMode = _uiState.value.workflowConfig.iosPackagingMode,
                        isExpoProject = inspected.detection.isExpoProject,
                        attachToGitHubRelease = _uiState.value.workflowConfig.attachToGitHubRelease,
                    )
                    _uiState.update {
                        it.copy(
                            inspectState = RepoInspectState.Success(inspected),
                            workflowConfig = newConfig,
                            generatedYaml = WorkflowGenerator.generate(newConfig),
                        )
                    }
                    cloudRepository.upsertSavedRepo(
                        CloudSavedRepo(
                            fullName = inspected.repoDto.fullName,
                            owner = slug.owner,
                            repo = slug.repo,
                            description = inspected.repoDto.description.orEmpty(),
                            defaultBranch = inspected.repoDto.defaultBranch,
                            detectedStack = stack.name,
                            avatarUrl = inspected.repoDto.owner.avatarUrl.orEmpty(),
                            htmlUrl = inspected.repoDto.htmlUrl,
                            supportsApk = stack.supportsApk,
                            supportsIpa = stack.supportsIpa,
                        ),
                    )
                    restartPollingIfNeeded(inspected)
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            inspectState = RepoInspectState.Error(
                                err.message ?: "Failed to inspect repository.",
                            ),
                        )
                    }
                }
        }
    }

    fun updateWorkflowConfig(transform: (WorkflowConfig) -> WorkflowConfig) {
        _uiState.update { current ->
            val updated = transform(current.workflowConfig)
            val safeConfig = if (!updated.buildApk && !updated.buildIpa) {
                updated.copy(buildApk = true)
            } else {
                updated
            }
            current.copy(
                workflowConfig = safeConfig,
                generatedYaml = WorkflowGenerator.generate(safeConfig),
            )
        }
    }

    fun commitWorkflowToRepo(triggerAfterCommit: Boolean) {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        val config = _uiState.value.workflowConfig
        val yaml = _uiState.value.generatedYaml

        viewModelScope.launch {
            _uiState.update { it.copy(isCommittingWorkflow = true) }
            repository.commitWorkflowToRepo(
                slug = inspected.slug,
                branch = config.branch,
                workflowFileName = config.workflowFileName,
                yamlContent = yaml,
            ).onSuccess {
                _uiState.update { it.copy(isCommittingWorkflow = false) }
                if (triggerAfterCommit) {
                    _events.emit(UiEvent.ShowMessage("Workflow pushed. Dispatching APK & IPA build..."))
                    delay(1800)
                    refreshAndTriggerGeneratedWorkflow(inspected, config)
                } else {
                    _events.emit(UiEvent.ShowMessage("Committed .github/workflows/${config.workflowFileName} to ${config.branch}"))
                    refreshRuns()
                }
            }.onFailure { err ->
                _uiState.update { it.copy(isCommittingWorkflow = false) }
                _events.emit(UiEvent.ShowMessage(err.message ?: "Failed to commit workflow"))
            }
        }
    }

    private suspend fun refreshAndTriggerGeneratedWorkflow(
        inspected: InspectedRepository,
        config: WorkflowConfig,
    ) {
        _uiState.update { it.copy(isTriggeringBuild = true) }
        var targetWorkflowId: Long? = null

        repeat(3) { attempt ->
            val refreshed = repository.refreshActionsData(inspected.slug).getOrNull()
            if (refreshed != null) {
                val (workflows, runs, artifacts) = refreshed
                updateInspectedActions(workflows, runs, artifacts)
                targetWorkflowId = workflows.firstOrNull {
                    it.path.endsWith(config.workflowFileName, ignoreCase = true)
                }?.id
                if (targetWorkflowId != null) return@repeat
            }
            if (attempt < 2) delay(1500)
        }

        if (targetWorkflowId == null) {
            _uiState.update { it.copy(isTriggeringBuild = false) }
            _events.emit(UiEvent.ShowMessage("Workflow committed. Open Runs tab and refresh in a moment to dispatch."))
            _events.emit(UiEvent.NavigateToTab("runs"))
            return
        }

        val inputs = mapOf(
            "build_apk" to config.buildApk.toString(),
            "build_ipa" to config.buildIpa.toString(),
            "build_mode" to config.buildMode.flagValue,
        )
        repository.triggerWorkflowDispatch(
            slug = inspected.slug,
            workflowId = targetWorkflowId!!,
            branch = config.branch,
            inputs = inputs,
        ).onSuccess {
            _uiState.update { it.copy(isTriggeringBuild = false) }
            _events.emit(UiEvent.ShowMessage("Build triggered on ${config.branch}"))
            delay(1500)
            refreshRuns()
            _events.emit(UiEvent.NavigateToTab("runs"))
        }.onFailure { err ->
            _uiState.update { it.copy(isTriggeringBuild = false) }
            _events.emit(UiEvent.ShowMessage(err.message ?: "Failed to dispatch build"))
        }
    }

    fun triggerExistingWorkflow(workflowId: Long, workflowPath: String) {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        val config = _uiState.value.workflowConfig
        val inputs = if (workflowPath.endsWith(config.workflowFileName, ignoreCase = true)) {
            mapOf(
                "build_apk" to config.buildApk.toString(),
                "build_ipa" to config.buildIpa.toString(),
                "build_mode" to config.buildMode.flagValue,
            )
        } else {
            emptyMap()
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTriggeringBuild = true) }
            repository.triggerWorkflowDispatch(
                slug = inspected.slug,
                workflowId = workflowId,
                branch = config.branch,
                inputs = inputs,
            ).onSuccess {
                _uiState.update { it.copy(isTriggeringBuild = false) }
                _events.emit(UiEvent.ShowMessage("Workflow run dispatched on ${config.branch}"))
                delay(1500)
                refreshRuns()
            }.onFailure { err ->
                _uiState.update { it.copy(isTriggeringBuild = false) }
                _events.emit(UiEvent.ShowMessage(err.message ?: "Failed to dispatch workflow"))
            }
        }
    }

    fun refreshRuns() {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshingRuns = true) }
            repository.refreshActionsData(inspected.slug)
                .onSuccess { (workflows, runs, artifacts) ->
                    updateInspectedActions(workflows, runs, artifacts)
                    _uiState.update { it.copy(isRefreshingRuns = false) }
                    restartPollingIfNeeded(
                        inspected.copy(workflows = workflows, recentRuns = runs, artifacts = artifacts),
                    )
                }
                .onFailure { err ->
                    _uiState.update { it.copy(isRefreshingRuns = false) }
                    _events.emit(UiEvent.ShowMessage(err.message ?: "Refresh failed"))
                }
        }
    }

    fun loadJobsForRun(runId: Long) {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingJobsRunId = runId) }
            repository.fetchRunJobs(inspected.slug, runId)
                .onSuccess { jobs ->
                    _uiState.update { state ->
                        state.copy(
                            loadingJobsRunId = null,
                            selectedRunJobs = state.selectedRunJobs + (runId to jobs),
                        )
                    }
                }
                .onFailure { err ->
                    _uiState.update { it.copy(loadingJobsRunId = null) }
                    _events.emit(UiEvent.ShowMessage(err.message ?: "Could not load job steps"))
                }
        }
    }

    fun cancelRun(runId: Long) {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        viewModelScope.launch {
            repository.cancelWorkflowRun(inspected.slug, runId)
                .onSuccess {
                    _events.emit(UiEvent.ShowMessage("Cancellation requested for run #$runId"))
                    delay(1000)
                    refreshRuns()
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage(err.message ?: "Failed to cancel run"))
                }
        }
    }

    fun downloadArtifact(artifact: GitHubArtifactDto) {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingItemId = artifact.id) }
            repository.downloadArtifact(
                repoFullName = inspected.slug.fullName,
                downloadUrl = artifact.archiveDownloadUrl,
                fileName = "${artifact.name}.zip",
                isZipArchive = true,
            ).onSuccess { savedList ->
                _uiState.update { it.copy(downloadingItemId = null) }
                val names = savedList.joinToString(", ") { it.fileName }
                _events.emit(UiEvent.ShowMessage("Downloaded: $names"))
            }.onFailure { err ->
                _uiState.update { it.copy(downloadingItemId = null) }
                _events.emit(UiEvent.ShowMessage(err.message ?: "Download failed"))
            }
        }
    }

    fun downloadReleaseAsset(assetId: Long, assetUrl: String, assetName: String) {
        val inspected = (_uiState.value.inspectState as? RepoInspectState.Success)?.repo ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(downloadingItemId = assetId) }
            repository.downloadArtifact(
                repoFullName = inspected.slug.fullName,
                downloadUrl = assetUrl,
                fileName = assetName,
                isZipArchive = assetName.endsWith(".zip", ignoreCase = true),
            ).onSuccess { savedList ->
                _uiState.update { it.copy(downloadingItemId = null) }
                val names = savedList.joinToString(", ") { it.fileName }
                _events.emit(UiEvent.ShowMessage("Saved package: $names"))
            }.onFailure { err ->
                _uiState.update { it.copy(downloadingItemId = null) }
                _events.emit(UiEvent.ShowMessage(err.message ?: "Download failed"))
            }
        }
    }

    fun exportPackageToUri(pkg: DownloadedPackageEntity, uri: Uri) {
        viewModelScope.launch {
            packageManager.copyFileToUri(pkg.filePath, uri)
                .onSuccess {
                    _events.emit(UiEvent.ShowMessage("Exported ${pkg.fileName} to selected folder"))
                }
                .onFailure { err ->
                    _events.emit(UiEvent.ShowMessage(err.message ?: "Export failed"))
                }
        }
    }

    fun createSharePackageIntent(pkg: DownloadedPackageEntity): Intent? {
        return packageManager.createShareIntent(pkg.filePath, pkg.fileName)
    }

    fun createShareYamlIntent(): Intent {
        val fileName = _uiState.value.workflowConfig.workflowFileName
        return packageManager.createShareYamlIntent(_uiState.value.generatedYaml, fileName)
    }

    fun deleteSavedRepo(fullName: String) {
        viewModelScope.launch {
            repository.removeSavedRepo(fullName)
            cloudRepository.deleteSavedRepo(fullName)
        }
    }

    fun deleteDownloadedPackage(pkg: DownloadedPackageEntity) {
        viewModelScope.launch {
            repository.removeDownloadedPackage(pkg)
            _events.emit(UiEvent.ShowMessage("Removed ${pkg.fileName}"))
        }
    }

    fun saveAndVerifyToken(token: String) {
        val clean = token.trim()
        if (clean.isBlank()) {
            viewModelScope.launch {
                tokenPreferences.clearGithubToken()
                _uiState.update {
                    it.copy(
                        authenticatedUser = null,
                        myGitHubRepos = emptyList(),
                        tokenStatusMessage = "Token cleared",
                    )
                }
                _events.emit(UiEvent.ShowMessage("GitHub token cleared"))
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isVerifyingToken = true, tokenStatusMessage = null) }
            repository.verifyToken(clean)
                .onSuccess { user ->
                    tokenPreferences.saveGithubToken(clean)
                    _uiState.update {
                        it.copy(
                            isVerifyingToken = false,
                            authenticatedUser = user,
                            tokenStatusMessage = "Verified as @${user.login}",
                        )
                    }
                    loadMyGitHubRepos()
                    _events.emit(UiEvent.ShowMessage("GitHub connected as @${user.login}"))
                }
                .onFailure { err ->
                    _uiState.update {
                        it.copy(
                            isVerifyingToken = false,
                            tokenStatusMessage = err.message ?: "Token verification failed",
                        )
                    }
                }
        }
    }

    fun setThemePreference(preference: ThemePreference) {
        viewModelScope.launch {
            tokenPreferences.saveThemePreference(preference)
        }
    }

    fun setAutoPollRuns(enabled: Boolean) {
        viewModelScope.launch {
            tokenPreferences.setAutoPollRuns(enabled)
            if (!enabled) pollingJob?.cancel()
        }
    }

    fun clearToken() {
        saveAndVerifyToken("")
    }

    private fun updateInspectedActions(
        workflows: List<com.example.data.remote.GitHubWorkflowDto>,
        runs: List<com.example.data.remote.GitHubWorkflowRunDto>,
        artifacts: List<GitHubArtifactDto>,
    ) {
        _uiState.update { state ->
            val currentSuccess = state.inspectState as? RepoInspectState.Success ?: return@update state
            state.copy(
                inspectState = RepoInspectState.Success(
                    currentSuccess.repo.copy(
                        workflows = workflows,
                        recentRuns = runs,
                        artifacts = artifacts,
                    ),
                ),
            )
        }
    }

    private fun restartPollingIfNeeded(inspected: InspectedRepository) {
        pollingJob?.cancel()
        if (!_uiState.value.preferences.autoPollRuns) return
        val hasActiveRun = inspected.recentRuns.any {
            it.status.equals("in_progress", ignoreCase = true) ||
                it.status.equals("queued", ignoreCase = true) ||
                it.status.equals("waiting", ignoreCase = true)
        }
        if (!hasActiveRun) return

        pollingJob = viewModelScope.launch {
            while (isActive) {
                delay(12_000)
                val result = repository.refreshActionsData(inspected.slug).getOrNull() ?: continue
                val (workflows, runs, artifacts) = result
                updateInspectedActions(workflows, runs, artifacts)
                val stillActive = runs.any {
                    it.status.equals("in_progress", ignoreCase = true) ||
                        it.status.equals("queued", ignoreCase = true) ||
                        it.status.equals("waiting", ignoreCase = true)
                }
                if (!stillActive) break
            }
        }
    }

    override fun onCleared() {
        pollingJob?.cancel()
        super.onCleared()
    }

    private companion object {
        const val TAG = "RepoForgeVM"
    }
}
