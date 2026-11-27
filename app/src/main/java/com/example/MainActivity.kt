package com.example

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.Code
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.data.firebase.CloudAccountRepository
import com.example.data.local.DownloadedPackageEntity
import com.example.data.local.RepoForgeDatabase
import com.example.data.local.TokenPreferences
import com.example.data.remote.GitHubNetworkClient
import com.example.data.repository.RepoForgeRepository
import com.example.domain.ArtifactPackageManager
import com.example.ui.RepoForgeViewModel
import com.example.ui.UiEvent
import com.example.ui.auth.attemptAutoSignIn
import com.example.ui.auth.authStateFlow
import com.example.ui.auth.signOut
import com.example.ui.screens.ArtifactsScreen
import com.example.ui.screens.AuthScreen
import com.example.ui.screens.PendingAccountSetup
import com.example.ui.screens.PipelinesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.StudioScreen
import com.example.ui.screens.WorkflowScreen
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.RepoForgeTheme
import com.example.ui.theme.ThemePreference
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch

const val ROUTE_STUDIO = "studio"
const val ROUTE_WORKFLOW = "workflow"
const val ROUTE_RUNS = "runs"
const val ROUTE_ARTIFACTS = "artifacts"
const val ROUTE_SETTINGS = "settings"

private data class NavTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String,
)

private val navTabs = listOf(
    NavTab(ROUTE_STUDIO, R.string.nav_studio, Icons.Rounded.Code, Icons.Outlined.Code, "nav_studio"),
    NavTab(ROUTE_WORKFLOW, R.string.nav_workflow, Icons.Rounded.Build, Icons.Outlined.Build, "nav_workflow"),
    NavTab(ROUTE_RUNS, R.string.nav_runs, Icons.Rounded.AccountTree, Icons.Outlined.AccountTree, "nav_runs"),
    NavTab(ROUTE_ARTIFACTS, R.string.nav_artifacts, Icons.Rounded.Inventory2, Icons.Outlined.Inventory2, "nav_artifacts"),
    NavTab(ROUTE_SETTINGS, R.string.nav_settings, Icons.Rounded.AccountCircle, Icons.Outlined.AccountCircle, "nav_settings"),
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            AppContent()
        }
    }
}

@Composable
fun AppContent() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val auth = remember { Firebase.auth }
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val credentialManager = remember(context) { CredentialManager.create(context) }
    val tokenPreferences = remember(context) { TokenPreferences(context.applicationContext) }
    val prefsState by tokenPreferences.preferencesFlow.collectAsStateWithLifecycle(
        initialValue = com.example.data.local.UserPreferencesState(),
    )

    var pendingAccountSetup by remember { mutableStateOf<PendingAccountSetup?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = {},
            onUnauthenticated = {},
            scope = scope,
        )
    }

    RepoForgeTheme(themePreference = prefsState.themePreference) {
        val user = currentUser
        if (user == null) {
            AuthScreen(
                onAuthSuccess = { pending ->
                    pendingAccountSetup = pending
                },
            )
        } else {
            val uid = user.uid
            key(uid) {
                val viewModel: RepoForgeViewModel = viewModel(
                    key = uid,
                    factory = viewModelFactory {
                        initializer {
                            val app = checkNotNull(this[APPLICATION_KEY]) {
                                "APPLICATION_KEY missing from CreationExtras"
                            }
                            val databaseId = app.getString(R.string.firestore_database_id)
                            val firestore = FirebaseFirestore.getInstance(databaseId)
                            val networkClient = GitHubNetworkClient()
                            val packageManager = ArtifactPackageManager(app, networkClient)
                            val localDb = RepoForgeDatabase.getInstance(app)
                            val repository = RepoForgeRepository(
                                networkClient = networkClient,
                                dao = localDb.repoForgeDao(),
                                packageManager = packageManager,
                            )
                            val cloudRepository = CloudAccountRepository(firestore)
                            val setupSnapshot = pendingAccountSetup
                            pendingAccountSetup = null
                            RepoForgeViewModel(
                                networkClient = networkClient,
                                packageManager = packageManager,
                                repository = repository,
                                cloudRepository = cloudRepository,
                                tokenPreferences = TokenPreferences(app),
                                currentUserId = uid,
                                initialPendingSetup = setupSnapshot,
                            )
                        }
                    },
                )

                RepoForgeApp(
                    viewModel = viewModel,
                    userEmail = user.email,
                    onSignOut = {
                        signOut(
                            credentialManager = credentialManager,
                            scope = scope,
                        )
                    },
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RepoForgeApp(
    viewModel: RepoForgeViewModel,
    userEmail: String?,
    onSignOut: () -> Unit,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ROUTE_STUDIO

    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var pendingPackageExport by remember { mutableStateOf<DownloadedPackageEntity?>(null) }

    val savePackageLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream"),
    ) { uri: Uri? ->
        val pkg = pendingPackageExport
        pendingPackageExport = null
        if (uri != null && pkg != null) {
            viewModel.exportPackageToUri(pkg, uri)
        }
    }

    val saveYamlLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/yaml"),
    ) { uri: Uri? ->
        if (uri != null) {
            runCatching {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(uiState.generatedYaml.toByteArray(Charsets.UTF_8))
                }
            }.onSuccess {
                scope.launch {
                    snackbarHostState.showSnackbar("Saved ${uiState.workflowConfig.workflowFileName}")
                }
            }.onFailure { err ->
                scope.launch {
                    snackbarHostState.showSnackbar(err.message ?: "Could not save YAML file")
                }
            }
        }
    }

    fun navigateToTab(route: String) {
        navController.navigate(route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun openUrlInBrowser(url: String) {
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }.onFailure {
            scope.launch { snackbarHostState.showSnackbar("No browser available to open URL") }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is UiEvent.ShowMessage -> snackbarHostState.showSnackbar(event.message)
                is UiEvent.NavigateToTab -> navigateToTab(event.route)
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useNavRail = maxWidth >= 600.dp

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = stringResource(id = R.string.app_name),
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    actions = {
                        val activeTitle = navTabs.firstOrNull { it.route == currentRoute }?.let {
                            stringResource(id = it.labelRes)
                        } ?: ""
                        Text(
                            text = activeTitle.uppercase(),
                            style = MonoBadgeStyle,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.padding(end = 16.dp),
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                    ),
                )
            },
            bottomBar = {
                if (!useNavRail) {
                    NavigationBar {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            ) {
                if (useNavRail) {
                    NavigationRail {
                        navTabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { navigateToTab(tab.route) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label, maxLines = 1) },
                                modifier = Modifier.testTag(tab.testTag),
                            )
                        }
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_STUDIO,
                    ) {
                        composable(ROUTE_STUDIO) {
                            StudioScreen(
                                uiState = uiState,
                                onRepoInputChange = viewModel::updateRepoInput,
                                onInspectRepo = { viewModel.inspectRepository(it) },
                                onConnectGitHubToken = viewModel::saveAndVerifyToken,
                                onRefreshMyRepos = viewModel::loadMyGitHubRepos,
                                onDeleteSavedRepo = viewModel::deleteSavedRepo,
                                onOpenWorkflowBuilder = { navigateToTab(ROUTE_WORKFLOW) },
                                onOpenRuns = { navigateToTab(ROUTE_RUNS) },
                                onOpenPackages = { navigateToTab(ROUTE_ARTIFACTS) },
                                onOpenUrlInBrowser = ::openUrlInBrowser,
                            )
                        }
                        composable(ROUTE_WORKFLOW) {
                            WorkflowScreen(
                                uiState = uiState,
                                onUpdateConfig = viewModel::updateWorkflowConfig,
                                onCopyYaml = {
                                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                    clipboard.setPrimaryClip(
                                        ClipData.newPlainText("RepoForge Workflow YAML", uiState.generatedYaml),
                                    )
                                    scope.launch {
                                        snackbarHostState.showSnackbar("Copied workflow YAML to clipboard")
                                    }
                                },
                                onShareYaml = {
                                    val shareIntent = viewModel.createShareYamlIntent()
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Workflow YAML"))
                                },
                                onExportYamlFile = {
                                    saveYamlLauncher.launch(uiState.workflowConfig.workflowFileName)
                                },
                                onCommitWorkflowOnly = {
                                    viewModel.commitWorkflowToRepo(triggerAfterCommit = false)
                                },
                                onCommitAndTriggerBuild = {
                                    viewModel.commitWorkflowToRepo(triggerAfterCommit = true)
                                },
                                onGoToStudio = { navigateToTab(ROUTE_STUDIO) },
                                onGoToToken = { navigateToTab(ROUTE_SETTINGS) },
                            )
                        }
                        composable(ROUTE_RUNS) {
                            PipelinesScreen(
                                uiState = uiState,
                                onRefresh = viewModel::refreshRuns,
                                onTriggerWorkflow = viewModel::triggerExistingWorkflow,
                                onLoadRunJobs = viewModel::loadJobsForRun,
                                onCancelRun = viewModel::cancelRun,
                                onOpenUrl = ::openUrlInBrowser,
                                onGoToStudio = { navigateToTab(ROUTE_STUDIO) },
                                onGoToWorkflowBuilder = { navigateToTab(ROUTE_WORKFLOW) },
                            )
                        }
                        composable(ROUTE_ARTIFACTS) {
                            ArtifactsScreen(
                                uiState = uiState,
                                onDownloadArtifact = viewModel::downloadArtifact,
                                onDownloadReleaseAsset = viewModel::downloadReleaseAsset,
                                onExportPackageToDevice = { pkg ->
                                    pendingPackageExport = pkg
                                    savePackageLauncher.launch(pkg.fileName)
                                },
                                onSharePackage = { pkg ->
                                    val shareIntent = viewModel.createSharePackageIntent(pkg)
                                    if (shareIntent != null) {
                                        context.startActivity(Intent.createChooser(shareIntent, "Share ${pkg.fileName}"))
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar("Package file missing on device")
                                        }
                                    }
                                },
                                onDeletePackage = viewModel::deleteDownloadedPackage,
                                onOpenUrl = ::openUrlInBrowser,
                                onGoToStudio = { navigateToTab(ROUTE_STUDIO) },
                            )
                        }
                        composable(ROUTE_SETTINGS) {
                            SettingsScreen(
                                uiState = uiState,
                                userEmail = userEmail,
                                onSaveCloudProfile = viewModel::updateCloudProfile,
                                onSignOut = onSignOut,
                                onSaveAndVerifyToken = viewModel::saveAndVerifyToken,
                                onClearToken = viewModel::clearToken,
                                onThemeChange = viewModel::setThemePreference,
                                onAutoPollChange = viewModel::setAutoPollRuns,
                                onOpenUrl = ::openUrlInBrowser,
                            )
                        }
                    }
                }
            }
        }
    }
}
