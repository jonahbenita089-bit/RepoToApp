package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.remote.GitHubJobDto
import com.example.data.remote.GitHubWorkflowDto
import com.example.data.remote.GitHubWorkflowRunDto
import com.example.ui.RepoForgeUiState
import com.example.ui.RepoInspectState
import com.example.ui.components.EmptySectionCard
import com.example.ui.components.RunStatusPill
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.StatusError
import com.example.ui.theme.StatusRunning
import com.example.ui.theme.StatusSuccess

@Composable
fun PipelinesScreen(
    uiState: RepoForgeUiState,
    onRefresh: () -> Unit,
    onTriggerWorkflow: (Long, String) -> Unit,
    onLoadRunJobs: (Long) -> Unit,
    onCancelRun: (Long) -> Unit,
    onOpenUrl: (String) -> Unit,
    onGoToStudio: () -> Unit,
    onGoToWorkflowBuilder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val inspected = (uiState.inspectState as? RepoInspectState.Success)?.repo

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter,
    ) {
        if (inspected == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp)
                    .padding(16.dp),
            ) {
                EmptySectionCard(
                    icon = Icons.Rounded.AccountTree,
                    title = "No active repository",
                    subtitle = "Inspect a GitHub repository in Studio to view its GitHub Actions workflows and live APK/IPA build runs.",
                    actionContent = {
                        Button(
                            onClick = onGoToStudio,
                            modifier = Modifier.testTag("runs_go_studio_button"),
                        ) {
                            Text("Go to Studio")
                        }
                    },
                )
            }
            return
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 720.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = inspected.slug.fullName,
                            style = MaterialTheme.typography.titleLarge,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = if (uiState.preferences.autoPollRuns) {
                                "Auto-polling active builds every 12s"
                            } else {
                                "Manual refresh mode"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FilledTonalButton(
                        onClick = onRefresh,
                        enabled = !uiState.isRefreshingRuns,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("refresh_runs_button"),
                    ) {
                        if (uiState.isRefreshingRuns) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Refresh,
                                contentDescription = stringResource(id = R.string.cd_refresh),
                                modifier = Modifier.size(16.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Refresh")
                    }
                }
            }

            item {
                SectionHeaderRow(
                    title = "Repository Workflows",
                    trailingText = "${inspected.workflows.size} total",
                )
            }

            if (inspected.workflows.isEmpty()) {
                item {
                    EmptySectionCard(
                        icon = Icons.Rounded.PlayArrow,
                        title = "No GitHub Actions workflows found",
                        subtitle = "Generate and push RepoForge's APK & IPA workflow from the Pipeline tab to enable cloud builds.",
                        actionContent = {
                            Button(
                                onClick = onGoToWorkflowBuilder,
                                modifier = Modifier.testTag("runs_create_workflow_button"),
                            ) {
                                Text("Create APK & IPA Workflow")
                            }
                        },
                    )
                }
            } else {
                items(
                    items = inspected.workflows,
                    key = { it.id },
                ) { workflow ->
                    WorkflowItemCard(
                        workflow = workflow,
                        isDispatching = uiState.isTriggeringBuild,
                        branch = uiState.workflowConfig.branch,
                        onTrigger = { onTriggerWorkflow(workflow.id, workflow.path) },
                        onOpenUrl = { onOpenUrl(workflow.htmlUrl) },
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(4.dp))
                SectionHeaderRow(
                    title = "Workflow Runs",
                    trailingText = "${inspected.recentRuns.size} recent",
                )
            }

            if (inspected.recentRuns.isEmpty()) {
                item {
                    EmptySectionCard(
                        icon = Icons.Rounded.HourglassTop,
                        title = "No workflow runs recorded",
                        subtitle = "Trigger a workflow above or push a commit to start building APK and IPA outputs.",
                    )
                }
            } else {
                items(
                    items = inspected.recentRuns,
                    key = { it.id },
                ) { run ->
                    WorkflowRunCard(
                        run = run,
                        jobs = uiState.selectedRunJobs[run.id],
                        isLoadingJobs = uiState.loadingJobsRunId == run.id,
                        onExpandJobs = { onLoadRunJobs(run.id) },
                        onCancelRun = { onCancelRun(run.id) },
                        onOpenLogs = { onOpenUrl(run.htmlUrl) },
                    )
                }
            }
        }
    }
}

@Composable
private fun WorkflowItemCard(
    workflow: GitHubWorkflowDto,
    isDispatching: Boolean,
    branch: String,
    onTrigger: () -> Unit,
    onOpenUrl: () -> Unit,
) {
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
                Text(
                    text = workflow.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = workflow.path,
                    style = MonoBadgeStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = onOpenUrl,
                modifier = Modifier.testTag("open_workflow_${workflow.id}"),
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                    contentDescription = stringResource(id = R.string.cd_open_browser),
                )
            }
            FilledTonalButton(
                onClick = onTrigger,
                enabled = !isDispatching,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("run_workflow_${workflow.id}"),
            ) {
                Icon(imageVector = Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Run ($branch)", maxLines = 1)
            }
        }
    }
}

@Composable
private fun WorkflowRunCard(
    run: GitHubWorkflowRunDto,
    jobs: List<GitHubJobDto>?,
    isLoadingJobs: Boolean,
    onExpandJobs: () -> Unit,
    onCancelRun: () -> Unit,
    onOpenLogs: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val isActive = run.status.equals("in_progress", ignoreCase = true) ||
        run.status.equals("queued", ignoreCase = true) ||
        run.status.equals("waiting", ignoreCase = true)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = run.displayTitle ?: run.name ?: "Build #${run.runNumber}",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "#${run.runNumber}  |  ${run.headBranch ?: "main"}  |  ${run.headSha?.take(7) ?: ""}",
                        style = MonoBadgeStyle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                RunStatusPill(status = run.status, conclusion = run.conclusion)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                OutlinedButton(
                    onClick = {
                        expanded = !expanded
                        if (expanded && jobs == null) {
                            onExpandJobs()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("toggle_run_jobs_${run.id}"),
                ) {
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (expanded) "Hide Steps" else "Job Steps")
                }

                OutlinedButton(
                    onClick = onOpenLogs,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_run_logs_${run.id}"),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                        contentDescription = stringResource(id = R.string.cd_open_browser),
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Logs")
                }

                if (isActive) {
                    OutlinedButton(
                        onClick = onCancelRun,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("cancel_run_${run.id}"),
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Cancel,
                            contentDescription = "Cancel run",
                            tint = StatusError,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    if (isLoadingJobs) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(vertical = 8.dp),
                        ) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            Text("Loading job steps...", style = MaterialTheme.typography.bodySmall)
                        }
                    } else if (jobs.isNullOrEmpty()) {
                        Text(
                            text = "No job telemetry returned yet.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        jobs.forEach { job ->
                            JobStepsBlock(job = job)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun JobStepsBlock(job: GitHubJobDto) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = job.name,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
                RunStatusPill(status = job.status, conclusion = job.conclusion)
            }
            job.steps.forEach { step ->
                val stepColor = when {
                    step.conclusion == "success" -> StatusSuccess
                    step.conclusion == "failure" -> StatusError
                    step.status == "in_progress" -> StatusRunning
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val stepIcon = when {
                    step.conclusion == "success" -> Icons.Rounded.CheckCircle
                    step.conclusion == "failure" -> Icons.Rounded.ErrorOutline
                    step.status == "in_progress" -> Icons.Rounded.Sync
                    else -> Icons.Rounded.HourglassTop
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(
                        imageVector = stepIcon,
                        contentDescription = null,
                        tint = stepColor,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        text = "${step.number}. ${step.name}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
