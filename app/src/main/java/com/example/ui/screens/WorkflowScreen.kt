package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.PhoneIphone
import androidx.compose.material.icons.rounded.RocketLaunch
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.BuildMode
import com.example.domain.IosPackagingMode
import com.example.domain.ProjectStack
import com.example.domain.WorkflowConfig
import com.example.ui.RepoForgeUiState
import com.example.ui.RepoInspectState
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.MonospaceCodeBox
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.MonoBadgeStyle

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorkflowScreen(
    uiState: RepoForgeUiState,
    onUpdateConfig: ((WorkflowConfig) -> WorkflowConfig) -> Unit,
    onCopyYaml: () -> Unit,
    onShareYaml: () -> Unit,
    onExportYamlFile: () -> Unit,
    onCommitWorkflowOnly: () -> Unit,
    onCommitAndTriggerBuild: () -> Unit,
    onGoToStudio: () -> Unit,
    onGoToToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspected = (uiState.inspectState as? RepoInspectState.Success)?.repo
    val config = uiState.workflowConfig

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (inspected == null) {
                item {
                    EmptySectionCard(
                        icon = Icons.Rounded.RocketLaunch,
                        title = "Select a GitHub repository first",
                        subtitle = "Inspect a repository in the Studio tab so RepoForge can tailor the APK and IPA build workflow to its stack.",
                        actionContent = {
                            Button(
                                onClick = onGoToStudio,
                                modifier = Modifier.testTag("workflow_go_studio_button"),
                            ) {
                                Text("Go to Studio")
                            }
                        },
                    )
                }
            }

            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        SectionHeaderRow(
                            title = "Build Configuration",
                            trailingText = inspected?.slug?.fullName ?: "Template Mode",
                        )

                        // Stack Selector
                        Text(
                            text = "Framework Stack",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ProjectStack.entries.forEach { stack ->
                                FilterChip(
                                    selected = config.stack == stack,
                                    onClick = { onUpdateConfig { it.copy(stack = stack) } },
                                    label = { Text(stack.displayName) },
                                    modifier = Modifier.testTag("stack_chip_${stack.shortCode}"),
                                )
                            }
                        }

                        // Target outputs: APK & IPA
                        Text(
                            text = "Output Packages",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            FilterChip(
                                selected = config.buildApk,
                                onClick = { onUpdateConfig { it.copy(buildApk = !it.buildApk) } },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.Android,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text("Android APK") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_build_apk"),
                            )
                            FilterChip(
                                selected = config.buildIpa,
                                onClick = { onUpdateConfig { it.copy(buildIpa = !it.buildIpa) } },
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Rounded.PhoneIphone,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                },
                                label = { Text("iOS IPA") },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("toggle_build_ipa"),
                            )
                        }

                        // Branch & Build Mode
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            if (inspected != null && inspected.branches.isNotEmpty()) {
                                var branchExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.weight(1f)) {
                                    OutlinedButton(
                                        onClick = { branchExpanded = true },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .minimumInteractiveComponentSize()
                                            .testTag("branch_selector_button"),
                                    ) {
                                        Text(
                                            text = "Branch: ${config.branch}",
                                            style = MonoBadgeStyle,
                                            maxLines = 1,
                                        )
                                    }
                                    DropdownMenu(
                                        expanded = branchExpanded,
                                        onDismissRequest = { branchExpanded = false },
                                    ) {
                                        inspected.branches.forEach { branchName ->
                                            DropdownMenuItem(
                                                text = { Text(branchName, style = MonoBadgeStyle) },
                                                onClick = {
                                                    branchExpanded = false
                                                    onUpdateConfig { it.copy(branch = branchName) }
                                                },
                                            )
                                        }
                                    }
                                }
                            }

                            BuildMode.entries.forEach { mode ->
                                FilterChip(
                                    selected = config.buildMode == mode,
                                    onClick = { onUpdateConfig { it.copy(buildMode = mode) } },
                                    label = { Text(mode.label) },
                                    modifier = Modifier.testTag("build_mode_${mode.flagValue}"),
                                )
                            }
                        }

                        if (config.buildIpa) {
                            Text(
                                text = "iOS IPA Packaging Mode",
                                style = MaterialTheme.typography.labelLarge,
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                IosPackagingMode.entries.forEach { iosMode ->
                                    val selected = config.iosPackagingMode == iosMode
                                    Surface(
                                        onClick = { onUpdateConfig { it.copy(iosPackagingMode = iosMode) } },
                                        shape = MaterialTheme.shapes.small,
                                        color = if (selected) {
                                            MaterialTheme.colorScheme.tertiaryContainer
                                        } else {
                                            MaterialTheme.colorScheme.surfaceVariant
                                        },
                                        border = BorderStroke(
                                            1.dp,
                                            if (selected) {
                                                MaterialTheme.colorScheme.tertiary
                                            } else {
                                                MaterialTheme.colorScheme.outlineVariant
                                            },
                                        ),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .testTag("ios_mode_${iosMode.name}"),
                                    ) {
                                        Column(modifier = Modifier.padding(12.dp)) {
                                            Text(
                                                text = iosMode.label,
                                                style = MaterialTheme.typography.titleSmall,
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = iosMode.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Publish GitHub Release",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Attach generated APK & IPA files directly to a tag release",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = config.attachToGitHubRelease,
                                onCheckedChange = { checked ->
                                    onUpdateConfig { it.copy(attachToGitHubRelease = checked) }
                                },
                                modifier = Modifier.testTag("toggle_github_release"),
                            )
                        }
                    }
                }
            }

            // Push & Build Actions
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        SectionHeaderRow(title = "Deploy Pipeline to GitHub")

                        if (uiState.authenticatedUser == null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Text(
                                        text = "Add a GitHub PAT with repo + workflow scopes to commit and trigger builds directly.",
                                        style = MaterialTheme.typography.bodySmall,
                                        modifier = Modifier.weight(1f),
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    OutlinedButton(
                                        onClick = onGoToToken,
                                        modifier = Modifier.testTag("workflow_add_token_button"),
                                    ) {
                                        Text("Add Token")
                                    }
                                }
                            }
                        }

                        val isBusy = uiState.isCommittingWorkflow || uiState.isTriggeringBuild
                        Button(
                            onClick = onCommitAndTriggerBuild,
                            enabled = inspected != null && !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("commit_and_build_button"),
                        ) {
                            if (isBusy) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.isCommittingWorkflow) "Pushing Workflow..." else "Dispatching Build...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.RocketLaunch,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Push Workflow & Build APK/IPA")
                            }
                        }

                        OutlinedButton(
                            onClick = onCommitWorkflowOnly,
                            enabled = inspected != null && !isBusy,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("commit_workflow_only_button"),
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.CloudUpload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Commit .github/workflows/${config.workflowFileName}")
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = onCopyYaml,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("copy_yaml_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.ContentCopy,
                                    contentDescription = stringResource(id = R.string.cd_copy_yaml),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Copy")
                            }
                            OutlinedButton(
                                onClick = onExportYamlFile,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("save_yaml_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SaveAlt,
                                    contentDescription = "Save YAML file",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Save")
                            }
                            OutlinedButton(
                                onClick = onShareYaml,
                                modifier = Modifier
                                    .weight(1f)
                                    .minimumInteractiveComponentSize()
                                    .testTag("share_yaml_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Share,
                                    contentDescription = stringResource(id = R.string.cd_share_file),
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Share")
                            }
                        }
                    }
                }
            }

            item {
                SectionHeaderRow(title = "Generated GitHub Actions YAML")
                Spacer(modifier = Modifier.height(8.dp))
                MonospaceCodeBox(code = uiState.generatedYaml)
            }
        }
    }
}
