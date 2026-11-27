package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.BookmarkRemove
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.firebase.CloudSavedRepo
import com.example.data.remote.GitHubRepoDto
import com.example.data.repository.InspectedRepository
import com.example.ui.RepoForgeUiState
import com.example.ui.RepoInspectState
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.PlatformPill
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.MonoBadgeStyle

@Composable
fun StudioScreen(
    uiState: RepoForgeUiState,
    onRepoInputChange: (String) -> Unit,
    onInspectRepo: (String) -> Unit,
    onConnectGitHubToken: (String) -> Unit,
    onRefreshMyRepos: () -> Unit,
    onDeleteSavedRepo: (String) -> Unit,
    onOpenWorkflowBuilder: () -> Unit,
    onOpenRuns: () -> Unit,
    onOpenPackages: () -> Unit,
    onOpenUrlInBrowser: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current

    val mergedSavedRepos = remember(uiState.cloudSavedRepos, uiState.savedRepos) {
        if (uiState.cloudSavedRepos.isNotEmpty()) {
            uiState.cloudSavedRepos.sortedByDescending { it.updatedAt?.seconds ?: 0L }
        } else {
            uiState.savedRepos.map {
                CloudSavedRepo(
                    fullName = it.fullName,
                    owner = it.owner,
                    repo = it.repo,
                    description = it.description,
                    defaultBranch = it.defaultBranch,
                    detectedStack = it.detectedStack,
                    avatarUrl = it.avatarUrl,
                    htmlUrl = it.htmlUrl,
                    supportsApk = it.supportsApk,
                    supportsIpa = it.supportsIpa,
                )
            }
        }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            item {
                HeroStudioCard(uiState = uiState)
            }

            item {
                GitHubQuickConnectCard(
                    uiState = uiState,
                    onConnectGitHubToken = onConnectGitHubToken,
                    onRefreshMyRepos = onRefreshMyRepos,
                    onSelectRepo = { slug ->
                        onRepoInputChange(slug)
                        onInspectRepo(slug)
                    },
                    onOpenTokenGenerator = {
                        onOpenUrlInBrowser("https://github.com/settings/tokens/new?scopes=repo,workflow&description=RepoForge+Android")
                    },
                )
            }

            item {
                RepositoryInputCard(
                    repoInput = uiState.repoInput,
                    isLoading = uiState.inspectState is RepoInspectState.Loading,
                    onRepoInputChange = onRepoInputChange,
                    onInspectClick = {
                        keyboard?.hide()
                        onInspectRepo(uiState.repoInput)
                    },
                    onQuickPick = { sampleSlug ->
                        keyboard?.hide()
                        onRepoInputChange(sampleSlug)
                        onInspectRepo(sampleSlug)
                    },
                )
            }

            when (val inspect = uiState.inspectState) {
                is RepoInspectState.Loading -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        ) {
                            Row(
                                modifier = Modifier.padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(28.dp), strokeWidth = 3.dp)
                                Column {
                                    Text(
                                        text = "Inspecting GitHub repository...",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Detecting mobile build stack, branches, workflows, and releases.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }

                is RepoInspectState.Error -> {
                    item {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                )
                                Text(
                                    text = inspect.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }

                is RepoInspectState.Success -> {
                    item {
                        InspectedRepoOverviewCard(
                            inspected = inspect.repo,
                            onOpenWorkflowBuilder = onOpenWorkflowBuilder,
                            onOpenRuns = onOpenRuns,
                            onOpenPackages = onOpenPackages,
                            onOpenInBrowser = { onOpenUrlInBrowser(inspect.repo.repoDto.htmlUrl) },
                        )
                    }
                }

                RepoInspectState.Idle -> Unit
            }

            item {
                SectionHeaderRow(
                    title = "Saved Repositories",
                    trailingText = "${mergedSavedRepos.size} synced",
                )
            }

            if (mergedSavedRepos.isEmpty()) {
                item {
                    EmptySectionCard(
                        icon = Icons.Rounded.FolderOpen,
                        title = "No repositories inspected yet",
                        subtitle = "Sign in to GitHub above to pick one of your repositories or paste any GitHub URL below.",
                    )
                }
            } else {
                items(
                    items = mergedSavedRepos,
                    key = { it.fullName },
                ) { saved ->
                    SavedRepoCard(
                        item = saved,
                        onSelect = { onInspectRepo(saved.fullName) },
                        onDelete = { onDeleteSavedRepo(saved.fullName) },
                    )
                }
            }
        }
    }
}

@Composable
private fun GitHubQuickConnectCard(
    uiState: RepoForgeUiState,
    onConnectGitHubToken: (String) -> Unit,
    onRefreshMyRepos: () -> Unit,
    onSelectRepo: (String) -> Unit,
    onOpenTokenGenerator: () -> Unit,
) {
    var inlineToken by remember { mutableStateOf("") }
    var showToken by remember { mutableStateOf(false) }
    var repoFilter by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            1.dp,
            if (uiState.authenticatedUser != null) {
                MaterialTheme.colorScheme.secondary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        ),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (uiState.authenticatedUser == null) {
                SectionHeaderRow(
                    title = "Sign in to GitHub",
                    trailingText = "Fetch your repos",
                )
                Text(
                    text = "Connect a GitHub Personal Access Token to list all of your public and private repositories in one place and push APK/IPA build workflows.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                OutlinedTextField(
                    value = inlineToken,
                    onValueChange = { inlineToken = it },
                    label = { Text("GitHub Token (ghp_... or github_pat_...)") },
                    placeholder = { Text("Paste token to load your repos") },
                    singleLine = true,
                    leadingIcon = { Icon(Icons.Rounded.Key, contentDescription = null) },
                    trailingIcon = {
                        IconButton(
                            onClick = { showToken = !showToken },
                            modifier = Modifier.testTag("studio_toggle_token_visibility"),
                        ) {
                            Icon(
                                imageVector = if (showToken) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                contentDescription = if (showToken) "Hide token" else "Show token",
                            )
                        }
                    },
                    visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("studio_github_token_input"),
                )

                if (!uiState.tokenStatusMessage.isNullOrBlank()) {
                    Text(
                        text = uiState.tokenStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onConnectGitHubToken(inlineToken) },
                        enabled = !uiState.isVerifyingToken && inlineToken.isNotBlank(),
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                            .testTag("studio_connect_github_button"),
                    ) {
                        if (uiState.isVerifyingToken) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Connecting...")
                        } else {
                            Text("Sign in to GitHub")
                        }
                    }
                    OutlinedButton(
                        onClick = onOpenTokenGenerator,
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("studio_get_github_token_button"),
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Get Token")
                    }
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    AsyncImage(
                        model = uiState.authenticatedUser.avatarUrl,
                        contentDescription = stringResource(id = R.string.cd_repo_avatar),
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Your GitHub Repositories",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        Text(
                            text = "@${uiState.authenticatedUser.login}  |  ${uiState.myGitHubRepos.size} loaded",
                            style = MonoBadgeStyle,
                            color = MaterialTheme.colorScheme.secondary,
                        )
                    }
                    IconButton(
                        onClick = onRefreshMyRepos,
                        enabled = !uiState.isLoadingMyRepos,
                        modifier = Modifier.testTag("refresh_my_github_repos_button"),
                    ) {
                        if (uiState.isLoadingMyRepos) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(id = R.string.cd_refresh),
                            )
                        }
                    }
                }

                if (uiState.myGitHubRepos.isNotEmpty()) {
                    OutlinedTextField(
                        value = repoFilter,
                        onValueChange = { repoFilter = it },
                        placeholder = { Text("Filter your repositories...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("filter_my_repos_input"),
                    )

                    val filtered = remember(uiState.myGitHubRepos, repoFilter) {
                        if (repoFilter.isBlank()) uiState.myGitHubRepos
                        else uiState.myGitHubRepos.filter {
                            it.fullName.contains(repoFilter, ignoreCase = true) ||
                                (it.language?.contains(repoFilter, ignoreCase = true) == true)
                        }
                    }

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(
                            items = filtered,
                            key = { it.id },
                        ) { repo ->
                            MyRepoPickerCard(
                                repo = repo,
                                onSelect = { onSelectRepo(repo.fullName) },
                            )
                        }
                    }
                } else if (!uiState.isLoadingMyRepos) {
                    Text(
                        text = "No repositories returned for this token yet. Tap refresh to reload.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun MyRepoPickerCard(
    repo: GitHubRepoDto,
    onSelect: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .width(210.dp)
            .testTag("my_repo_card_${repo.fullName}"),
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = repo.name,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (repo.isPrivate) {
                    Icon(
                        imageVector = Icons.Rounded.Lock,
                        contentDescription = "Private repository",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp),
                    )
                }
            }
            Text(
                text = repo.fullName,
                style = MonoBadgeStyle,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = listOfNotNull(repo.language, repo.defaultBranch).joinToString("  |  "),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun HeroStudioCard(uiState: RepoForgeUiState) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(176.dp),
        ) {
            Image(
                painter = painterResource(id = R.drawable.img_hero_pipeline),
                contentDescription = stringResource(id = R.string.cd_hero_banner),
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color(0xAA0B0F17),
                                Color(0xEE0B0F17),
                            ),
                        ),
                    ),
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PlatformPill(label = "APK", isAndroid = true)
                        PlatformPill(label = "IPA", isAndroid = false)
                    }
                    Surface(
                        shape = RoundedCornerShape(999.dp),
                        color = if (uiState.authenticatedUser != null) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color(0xFF1E293B)
                        },
                    ) {
                        Text(
                            text = uiState.authenticatedUser?.let { "GitHub: @${it.login}" }
                                ?: (uiState.cloudProfile?.displayName ?: "Cloud Account Active"),
                            style = MonoBadgeStyle,
                            color = if (uiState.authenticatedUser != null) {
                                MaterialTheme.colorScheme.onSecondaryContainer
                            } else {
                                Color(0xFFCBD5E1)
                            },
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        )
                    }
                }

                Column {
                    Text(
                        text = stringResource(id = R.string.hero_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(id = R.string.hero_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color(0xFFCBD5E1),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun RepositoryInputCard(
    repoInput: String,
    isLoading: Boolean,
    onRepoInputChange: (String) -> Unit,
    onInspectClick: () -> Unit,
    onQuickPick: (String) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = repoInput,
                onValueChange = onRepoInputChange,
                label = { Text(stringResource(id = R.string.repo_input_label)) },
                placeholder = { Text(stringResource(id = R.string.repo_input_placeholder)) },
                singleLine = true,
                leadingIcon = {
                    Icon(imageVector = Icons.Rounded.Code, contentDescription = null)
                },
                trailingIcon = {
                    if (repoInput.isNotEmpty()) {
                        IconButton(
                            onClick = { onRepoInputChange("") },
                            modifier = Modifier.testTag("clear_repo_input_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(id = R.string.cd_clear_input),
                            )
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onInspectClick() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("repo_url_input"),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "Inspect public:",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                SuggestionChip(
                    onClick = { onQuickPick("flutter/samples") },
                    label = { Text("flutter/samples", style = MonoBadgeStyle) },
                    modifier = Modifier.testTag("sample_flutter_chip"),
                )
                SuggestionChip(
                    onClick = { onQuickPick("android/nowinandroid") },
                    label = { Text("android/nowinandroid", style = MonoBadgeStyle) },
                    modifier = Modifier.testTag("sample_android_chip"),
                )
            }

            Button(
                onClick = onInspectClick,
                enabled = !isLoading && repoInput.isNotBlank(),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("inspect_repo_button"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (isLoading) "Inspecting..." else stringResource(id = R.string.inspect_repo))
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InspectedRepoOverviewCard(
    inspected: InspectedRepository,
    onOpenWorkflowBuilder: () -> Unit,
    onOpenRuns: () -> Unit,
    onOpenPackages: () -> Unit,
    onOpenInBrowser: () -> Unit,
) {
    val stack = inspected.detection.stack
    val repo = inspected.repoDto

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AsyncImage(
                    model = repo.owner.avatarUrl,
                    contentDescription = stringResource(id = R.string.cd_repo_avatar),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(46.dp)
                        .clip(CircleShape),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = repo.fullName,
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Branch: ${repo.defaultBranch}",
                            style = MonoBadgeStyle,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Star,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp),
                            )
                            Text(
                                text = "${repo.stargazersCount}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
                IconButton(
                    onClick = onOpenInBrowser,
                    modifier = Modifier.testTag("open_repo_browser_button"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = stringResource(id = R.string.cd_open_browser),
                    )
                }
            }

            if (!repo.description.isNullOrBlank()) {
                Text(
                    text = repo.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Detected Stack: ${stack.displayName}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            PlatformPill(label = "APK", isAndroid = true, enabled = stack.supportsApk)
                            PlatformPill(label = "IPA", isAndroid = false, enabled = stack.supportsIpa)
                        }
                    }
                    Text(
                        text = stack.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    AnimatedVisibility(visible = inspected.detection.detectedIndicators.isNotEmpty()) {
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            inspected.detection.detectedIndicators.forEach { marker ->
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = MaterialTheme.colorScheme.surface,
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.Rounded.CheckCircle,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.secondary,
                                            modifier = Modifier.size(12.dp),
                                        )
                                        Text(text = marker, style = MonoBadgeStyle)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                SummaryStatChip(
                    label = "Workflows",
                    value = "${inspected.workflows.size}",
                    modifier = Modifier.weight(1f),
                )
                SummaryStatChip(
                    label = "Runs",
                    value = "${inspected.recentRuns.size}",
                    modifier = Modifier.weight(1f),
                )
                SummaryStatChip(
                    label = "Artifacts",
                    value = "${inspected.artifacts.size}",
                    modifier = Modifier.weight(1f),
                )
            }

            Button(
                onClick = onOpenWorkflowBuilder,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("configure_apk_ipa_pipeline_button"),
            ) {
                Icon(imageVector = Icons.Rounded.Build, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Generate APK & IPA Pipeline")
                Spacer(modifier = Modifier.width(6.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.ArrowForward,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                FilledTonalButton(
                    onClick = onOpenRuns,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("view_repo_runs_button"),
                ) {
                    Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("CI Runs")
                }
                OutlinedButton(
                    onClick = onOpenPackages,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("view_repo_packages_button"),
                ) {
                    Text("Packages (${inspected.artifacts.size + inspected.releases.sumOf { it.assets.size }})")
                }
            }
        }
    }
}

@Composable
private fun SummaryStatChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SavedRepoCard(
    item: CloudSavedRepo,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onSelect)
            .testTag("saved_repo_card_${item.fullName}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            AsyncImage(
                model = item.avatarUrl,
                contentDescription = stringResource(id = R.string.cd_repo_avatar),
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape),
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = item.fullName,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.detectedStack,
                        style = MonoBadgeStyle,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    if (item.supportsApk) PlatformPill(label = "APK", isAndroid = true)
                    if (item.supportsIpa) PlatformPill(label = "IPA", isAndroid = false)
                }
            }
            IconButton(
                onClick = onDelete,
                modifier = Modifier.testTag("remove_saved_repo_${item.fullName}"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.BookmarkRemove,
                    contentDescription = "Remove saved repository",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
