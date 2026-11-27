package com.example.ui.screens

import android.util.Patterns
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Email
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Phone
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.data.firebase.ContactMethod
import com.example.ui.auth.onGoogleSignInClicked
import com.example.ui.components.PlatformPill

data class PendingAccountSetup(
    val displayName: String,
    val preferredContactType: ContactMethod,
    val contactEmail: String,
    val phoneNumber: String,
)

@Composable
fun AuthScreen(
    onAuthSuccess: (PendingAccountSetup) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var preferredMethod by remember { mutableStateOf(ContactMethod.GOOGLE) }
    var displayName by remember { mutableStateOf("") }
    var contactEmail by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets.safeDrawing,
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 560.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 24.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(168.dp),
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
                                        colors = listOf(Color(0x880B0F17), Color(0xF00B0F17)),
                                    ),
                                ),
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                PlatformPill(label = "APK", isAndroid = true)
                                PlatformPill(label = "IPA", isAndroid = false)
                            }
                            Column {
                                Text(
                                    text = stringResource(id = R.string.auth_title),
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = Color.White,
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = stringResource(id = R.string.auth_subtitle),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFFCBD5E1),
                                )
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = "Account Contact Preference",
                            style = MaterialTheme.typography.titleMedium,
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            ContactMethod.entries.forEach { method ->
                                FilterChip(
                                    selected = preferredMethod == method,
                                    onClick = {
                                        preferredMethod = method
                                        errorMessage = null
                                    },
                                    label = { Text(method.label, maxLines = 1) },
                                    modifier = Modifier.testTag("auth_method_${method.name}"),
                                )
                            }
                        }

                        OutlinedTextField(
                            value = displayName,
                            onValueChange = { displayName = it },
                            label = { Text("Display Name (optional)") },
                            placeholder = { Text("e.g. Alex Rivera") },
                            singleLine = true,
                            leadingIcon = { Icon(Icons.Rounded.Person, contentDescription = null) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("auth_display_name_input"),
                        )

                        if (preferredMethod == ContactMethod.EMAIL || preferredMethod == ContactMethod.GOOGLE) {
                            OutlinedTextField(
                                value = contactEmail,
                                onValueChange = {
                                    contactEmail = it
                                    errorMessage = null
                                },
                                label = {
                                    Text(
                                        if (preferredMethod == ContactMethod.EMAIL) {
                                            "Account Email Address"
                                        } else {
                                            "Recovery / Contact Email (optional)"
                                        },
                                    )
                                },
                                placeholder = { Text("developer@example.com") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                                leadingIcon = { Icon(Icons.Rounded.Email, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_email_input"),
                            )
                        }

                        if (preferredMethod == ContactMethod.PHONE || preferredMethod == ContactMethod.GOOGLE) {
                            OutlinedTextField(
                                value = phoneNumber,
                                onValueChange = {
                                    phoneNumber = it
                                    errorMessage = null
                                },
                                label = {
                                    Text(
                                        if (preferredMethod == ContactMethod.PHONE) {
                                            "Mobile Phone Number"
                                        } else {
                                            "Phone Number (optional)"
                                        },
                                    )
                                },
                                placeholder = { Text("+1 555 014 8821") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                leadingIcon = { Icon(Icons.Rounded.Phone, contentDescription = null) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("auth_phone_input"),
                            )
                        }

                        if (errorMessage != null) {
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.45f)),
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.ErrorOutline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.size(18.dp),
                                    )
                                    Text(
                                        text = errorMessage!!,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }

                        Button(
                            onClick = {
                                val trimmedEmail = contactEmail.trim()
                                val trimmedPhone = phoneNumber.trim()
                                if (preferredMethod == ContactMethod.EMAIL &&
                                    (trimmedEmail.isEmpty() || !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches())
                                ) {
                                    errorMessage = "Enter a valid email address before continuing."
                                    return@Button
                                }
                                if (trimmedEmail.isNotEmpty() && !Patterns.EMAIL_ADDRESS.matcher(trimmedEmail).matches()) {
                                    errorMessage = "The email address format is invalid."
                                    return@Button
                                }
                                if (preferredMethod == ContactMethod.PHONE && trimmedPhone.length < 7) {
                                    errorMessage = "Enter a valid phone number with country or area code."
                                    return@Button
                                }

                                isLoading = true
                                errorMessage = null
                                onGoogleSignInClicked(
                                    context = context,
                                    credentialManager = credentialManager,
                                    onAuthSuccess = {
                                        isLoading = false
                                        onAuthSuccess(
                                            PendingAccountSetup(
                                                displayName = displayName.trim(),
                                                preferredContactType = preferredMethod,
                                                contactEmail = trimmedEmail,
                                                phoneNumber = trimmedPhone,
                                            ),
                                        )
                                    },
                                    onAuthError = { msg ->
                                        isLoading = false
                                        errorMessage = msg
                                    },
                                    scope = scope,
                                    onAuthCancelled = {
                                        isLoading = false
                                    },
                                )
                            },
                            enabled = !isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("google_sign_in_button"),
                        ) {
                            if (isLoading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    strokeWidth = 2.dp,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text("Signing in...")
                            } else {
                                Icon(
                                    imageVector = Icons.Rounded.AccountCircle,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(stringResource(id = R.string.sign_in_google))
                            }
                        }

                        Surface(
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant,
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Info,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp),
                                )
                                Text(
                                    text = "Google Sign-In authenticates your cloud account immediately and saves your chosen email or phone number to your profile. Standalone Email/Password or SMS OTP sign-in requires enabling those providers in the Firebase Console.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
