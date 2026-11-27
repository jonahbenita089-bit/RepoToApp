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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.Key
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.firebase.ContactMethod
import com.example.ui.RepoForgeUiState
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.MonoBadgeStyle
import com.example.ui.theme.StatusSuccess
import com.example.ui.theme.ThemePreference

@Composable
fun SettingsScreen(
    uiState: RepoForgeUiState,
    userEmail: String?,
    onSaveCloudProfile: (String, ContactMethod, String, String) -> Unit,
    onSignOut: () -> Unit,
    onSaveAndVerifyToken: (String) -> Unit,
    onClearToken: () -> Unit,
    onThemeChange: (ThemePreference) -> Unit,
    onAutoPollChange: (Boolean) -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val profile = uiState.cloudProfile
    var displayNameDraft by remember(profile?.displayName) {
        mutableStateOf(profile?.displayName.orEmpty())
    }
    var contactMethodDraft by remember(profile?.preferredContactType) {
        mutableStateOf(profile?.resolvedContactMethod() ?: ContactMethod.GOOGLE)
    }
    var contactEmailDraft by remember(profile?.contactEmail, userEmail) {
        mutableStateOf(profile?.contactEmail ?: userEmail.orEmpty())
    }
    var phoneDraft by remember(profile?.phoneNumber) {
        mutableStateOf(profile?.phoneNumber.orEmpty())
    }

    var tokenDraft by remember(uiState.preferences.githubToken) {
        mutableStateOf(uiState.preferences.githubToken)
    }
    var showToken by remember { mutableStateOf(false) }

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
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            SectionHeaderRow(
                                title = "RepoForge Cloud Account",
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(
                                onClick = onSignOut,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("sign_out_button"),
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.Logout,
                                    contentDescription = "Sign out",
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Sign Out")
                            }
                        }

                        if (!profile?.photoUrl.isNullOrBlank()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                AsyncImage(
                                    model = profile?.photoUrl,
                                    contentDescription = "Google account avatar",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape),
                                )
                                Column {
                                    Text(
                                        text = profile?.displayName ?: "RepoForge Account",
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    Text(
                                        text = "Google Verified  |  Synced via Firestore",
                                        style = MonoBadgeStyle,
                                        color = MaterialTheme.colorScheme.secondary,
                                    )
                                }
                            }
                        }

                        Text(
                            text = "Preferred Contact Method",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ContactMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = contactMethodDraft == method,
                                    onClick = { contactMethodDraft = method },
                                    label = { Text(method.label, maxLines = 1) },
                                    modifier = Modifier.testTag("account_contact_method_${method.name}"),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = displayNameDraft,
                            onValueChange = { displayNameDraft = it },
                            label = { Text("Display Name") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_display_name_input"),
                        )

                        OutlinedTextField(
                            value = contactEmailDraft,
                            onValueChange = { contactEmailDraft = it },
                            label = { Text("Account Email") },
                            placeholder = { Text("developer@example.com") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_email_input"),
                        )

                        OutlinedTextField(
                            value = phoneDraft,
                            onValueChange = { phoneDraft = it },
                            label = { Text("Phone Number") },
                            placeholder = { Text("+1 555 014 8821") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("account_phone_input"),
                        )

                        FilledTonalButton(
                            onClick = {
                                onSaveCloudProfile(
                                    displayNameDraft,
                                    contactMethodDraft,
                                    contactEmailDraft,
                                    phoneDraft,
                                )
                            },
                            enabled = !uiState.isSavingProfile,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("save_account_profile_button"),
                        ) {
                            if (uiState.isSavingProfile) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Saving Profile...")
                            } else {
                                Text("Save Account Profile")
                            }
                        }
                    }
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
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        SectionHeaderRow(title = "GitHub Personal Access Token")

                        if (uiState.authenticatedUser != null) {
                            Surface(
                                shape = MaterialTheme.shapes.medium,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                border = BorderStroke(1.dp, StatusSuccess.copy(alpha = 0.45f)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    AsyncImage(
                                        model = uiState.authenticatedUser.avatarUrl,
                                        contentDescription = "Authenticated GitHub avatar",
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape),
                                    )
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = uiState.authenticatedUser.name ?: uiState.authenticatedUser.login,
                                            style = MaterialTheme.typography.titleMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                        Text(
                                            text = "@${uiState.authenticatedUser.login}",
                                            style = MonoBadgeStyle,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.CheckCircle,
                                        contentDescription = "Token verified",
                                        tint = StatusSuccess,
                                    )
                                }
                            }
                        }

                        OutlinedTextField(
                            value = tokenDraft,
                            onValueChange = { tokenDraft = it },
                            label = { Text("GitHub PAT (ghp_... or github_pat_...)") },
                            placeholder = { Text("Paste personal access token") },
                            singleLine = true,
                            leadingIcon = {
                                Icon(imageVector = Icons.Rounded.Key, contentDescription = null)
                            },
                            trailingIcon = {
                                IconButton(
                                    onClick = { showToken = !showToken },
                                    modifier = Modifier.testTag("toggle_token_visibility"),
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
                                .testTag("github_token_input"),
                        )

                        if (!uiState.tokenStatusMessage.isNullOrBlank()) {
                            Text(
                                text = uiState.tokenStatusMessage,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (uiState.authenticatedUser != null) {
                                    MaterialTheme.colorScheme.secondary
                                } else {
                                    MaterialTheme.colorScheme.error
                                },
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = { onSaveAndVerifyToken(tokenDraft) },
                                enabled = !uiState.isVerifyingToken && tokenDraft.isNotBlank(),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                                    .testTag("verify_save_token_button"),
                            ) {
                                if (uiState.isVerifyingToken) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary,
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Text(if (uiState.isVerifyingToken) "Verifying..." else "Verify & Save")
                            }
                            if (uiState.preferences.githubToken.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        tokenDraft = ""
                                        onClearToken()
                                    },
                                    modifier = Modifier
                                        .height(48.dp)
                                        .testTag("clear_token_button"),
                                ) {
                                    Text("Clear")
                                }
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Text(
                                    text = "Required Scopes",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Enable 'repo' and 'workflow' scopes on a Classic PAT (or Actions + Contents Read/Write on a Fine-grained PAT) to list your private repos, commit workflows, dispatch builds, and download APK/IPA artifacts.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                                OutlinedButton(
                                    onClick = {
                                        onOpenUrl("https://github.com/settings/tokens/new?scopes=repo,workflow&description=RepoForge+Android")
                                    },
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("create_github_token_link"),
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.OpenInNew,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp),
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Generate Token on GitHub")
                                }
                            }
                        }
                    }
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
                        SectionHeaderRow(title = "Studio Preferences")

                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            ThemePreference.entries.forEach { pref ->
                                FilterChip(
                                    selected = uiState.preferences.themePreference == pref,
                                    onClick = { onThemeChange(pref) },
                                    label = { Text(pref.name.lowercase().replaceFirstChar { it.uppercase() }) },
                                    modifier = Modifier.testTag("theme_chip_${pref.name}"),
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Live Build Auto-Polling",
                                    style = MaterialTheme.typography.titleSmall,
                                )
                                Text(
                                    text = "Automatically refresh active GitHub Actions runs every 12 seconds",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = uiState.preferences.autoPollRuns,
                                onCheckedChange = onAutoPollChange,
                                modifier = Modifier.testTag("toggle_auto_poll"),
                            )
                        }
                    }
                }
            }
        }
    }
}
