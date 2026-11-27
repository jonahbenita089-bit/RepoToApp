package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.DownloadedPackageEntity
import com.example.data.remote.GitHubArtifactDto
import com.example.data.remote.GitHubReleaseAssetDto
import com.example.ui.RepoForgeUiState
import com.example.ui.RepoInspectState
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.PlatformPill
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.formatFileSize
import com.example.ui.theme.MonoBadgeStyle

@Composable
fun ArtifactsScreen(
    uiState: RepoForgeUiState,
    onDownloadArtifact: (GitHubArtifactDto) -> Unit,
    onDownloadReleaseAsset: (Long, String, String) -> Unit,
    onExportPackageToDevice: (DownloadedPackageEntity) -> Unit,
    onSharePackage: (DownloadedPackageEntity) -> Unit,
    onDeletePackage: (DownloadedPackageEntity) -> Unit,
    onOpenUrl: (String) -> Unit,
    onGoToStudio: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspected = (uiState.inspectState as? RepoInspectState.Success)?.repo
    val releaseAssets = inspected?.releases.orEmpty().flatMap { release ->
        release.assets.map { asset -> release.tagName to asset }
    }

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                SectionHeaderRow(
                    title = "Downloaded Packages (APK & IPA)",
                    trailingText = "${uiState.downloadedPackages.size} on device",
                )
            }

            if (uiState.downloadedPackages.isEmpty()) {
                item {
                    EmptySectionCard(
                        icon = Icons.Rounded.Inventory2,
                        title = "No APK or IPA packages downloaded yet",
                        subtitle = "Download any GitHub Actions artifact or Release asset below. RepoForge automatically extracts .apk and .ipa binaries from build zips.",
                    )
                }
            } else {
                items(
                    items = uiState.downloadedPackages,
                    key = { it.id },
                ) { pkg ->
                    DownloadedPackageCard(
                        pkg = pkg,
                        onSaveToDevice = { onExportPackageToDevice(pkg) },
                        onShare = { onSharePackage(pkg) },
                        onDelete = { onDeletePackage(pkg) },
                    )
                }
            }

            item {
                SectionHeaderRow(
                    title = "GitHub Actions Artifacts",
                    trailingText = inspected?.let { "${it.artifacts.size} available" } ?: "No repo selected",
                )
            }

            if (inspected == null) {
                item {
                    EmptySectionCard(
                        icon = Icons.Rounded.Archive,
                        title = "Inspect a repository to fetch artifacts",
                        subtitle = "Connect a repository in Studio to download its APK and IPA workflow artifacts.",
                        actionContent = {
                            Button(
                                onClick = onGoToStudio,
                                modifier = Modifier.testTag("artifacts_go_studio_button"),
                            ) {
                                Text("Go to Studio")
                            }
                        },
                    )
                }
            } else {
                val activeArtifacts = inspected.artifacts.filter { !it.expired }
                if (activeArtifacts.isEmpty()) {
                    item {
                        EmptySectionCard(
                            icon = Icons.Rounded.Archive,
                            title = "No Actions artifacts yet",
                            subtitle = "Trigger an APK & IPA workflow run in the Pipeline or Runs tab. Completed builds appear here automatically.",
                        )
                    }
                } else {
                    items(
                        items = activeArtifacts,
                        key = { it.id },
                    ) { artifact ->
                        RemoteArtifactCard(
                            artifact = artifact,
                            isDownloading = uiState.downloadingItemId == artifact.id,
                            onDownload = { onDownloadArtifact(artifact) },
                        )
                    }
                }

                if (releaseAssets.isNotEmpty()) {
                    item {
                        SectionHeaderRow(
                            title = "GitHub Release Binaries",
                            trailingText = "${releaseAssets.size} files",
                        )
                    }
                    items(
                        items = releaseAssets,
                        key = { it.second.id },
                    ) { (tagName, asset) ->
                        ReleaseAssetCard(
                            tagName = tagName,
                            asset = asset,
                            isDownloading = uiState.downloadingItemId == asset.id,
                            onDownload = {
                                onDownloadReleaseAsset(asset.id, asset.browserDownloadUrl, asset.name)
                            },
                            onOpenBrowser = { onOpenUrl(asset.browserDownloadUrl) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadedPackageCard(
    pkg: DownloadedPackageEntity,
    onSaveToDevice: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                when (pkg.packageType) {
                    "APK" -> PlatformPill(label = "APK", isAndroid = true)
                    "IPA" -> PlatformPill(label = "IPA", isAndroid = false)
                    else -> PlatformPill(label = "ZIP", isAndroid = true, enabled = false)
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = pkg.fileName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${pkg.repoFullName}  |  ${formatFileSize(pkg.sizeBytes)}",
                        style = MonoBadgeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.testTag("delete_package_${pkg.id}"),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.DeleteOutline,
                        contentDescription = "Delete downloaded package",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Text(
                text = "SHA-256: ${pkg.sha256}",
                style = MonoBadgeStyle,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(
                    onClick = onSaveToDevice,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("export_package_${pkg.id}"),
                ) {
                    Icon(imageVector = Icons.Rounded.SaveAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save Copy")
                }
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("share_package_${pkg.id}"),
                ) {
                    Icon(imageVector = Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}

@Composable
private fun RemoteArtifactCard(
    artifact: GitHubArtifactDto,
    isDownloading: Boolean,
    onDownload: () -> Unit,
) {
    val lower = artifact.name.lowercase()
    val isApkHint = lower.contains("apk") || lower.contains("android")
    val isIpaHint = lower.contains("ipa") || lower.contains("ios")

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = artifact.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    if (isApkHint) PlatformPill(label = "APK", isAndroid = true)
                    if (isIpaHint) PlatformPill(label = "IPA", isAndroid = false)
                }
                Text(
                    text = "${formatFileSize(artifact.sizeInBytes)}  |  Branch: ${artifact.workflowRun?.headBranch ?: "main"}",
                    style = MonoBadgeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Button(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("download_artifact_${artifact.id}"),
            ) {
                if (isDownloading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Download,
                        contentDescription = stringResource(id = R.string.cd_download_artifact),
                        modifier = Modifier.size(16.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDownloading) "Saving..." else "Get")
            }
        }
    }
}

@Composable
private fun ReleaseAssetCard(
    tagName: String,
    asset: GitHubReleaseAssetDto,
    isDownloading: Boolean,
    onDownload: () -> Unit,
    onOpenBrowser: () -> Unit,
) {
    val isApk = asset.name.endsWith(".apk", ignoreCase = true)
    val isIpa = asset.name.endsWith(".ipa", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    if (isApk) PlatformPill(label = "APK", isAndroid = true)
                    if (isIpa) PlatformPill(label = "IPA", isAndroid = false)
                    Text(
                        text = asset.name,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Text(
                    text = "Release $tagName  |  ${formatFileSize(asset.size)}",
                    style = MonoBadgeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = onOpenBrowser,
                modifier = Modifier.testTag("open_release_asset_${asset.id}"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(id = R.string.cd_open_browser),
                )
            }
            FilledTonalButton(
                onClick = onDownload,
                enabled = !isDownloading,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("download_release_asset_${asset.id}"),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Download,
                    contentDescription = stringResource(id = R.string.cd_download_artifact),
                    modifier = Modifier.size(16.dp),
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(if (isDownloading) "..." else "Get")
            }
        }
    }
}
