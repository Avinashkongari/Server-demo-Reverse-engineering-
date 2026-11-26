package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.NetworkCheck
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConnectionDiagnostics
import com.example.data.StreamPathMode
import com.example.data.UrlValidator
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AxionAmber
import com.example.ui.theme.AxionAmberContainer
import com.example.ui.theme.AxionBlack
import com.example.ui.theme.AxionBorder
import com.example.ui.theme.AxionBorderStrong
import com.example.ui.theme.AxionEmerald
import com.example.ui.theme.AxionEmeraldContainer
import com.example.ui.theme.AxionError
import com.example.ui.theme.AxionErrorContainer
import com.example.ui.theme.AxionRed
import com.example.ui.theme.AxionRedContainer
import com.example.ui.theme.AxionSurface
import com.example.ui.theme.AxionSurfaceElevated
import com.example.ui.theme.AxionTextMuted
import com.example.ui.theme.AxionTextPrimary
import com.example.ui.theme.AxionTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AxionUiState

@Composable
fun SettingsScreen(
    uiState: AxionUiState,
    onSaveSettings: (url: String, newToken: String, streamMode: StreamPathMode, onSaved: () -> Unit) -> Unit,
    onTestConnection: (url: String) -> Unit,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    var backendUrlInput by rememberSaveable { mutableStateOf(uiState.backendUrl) }
    var tokenInput by rememberSaveable { mutableStateOf("") }
    var showToken by rememberSaveable { mutableStateOf(false) }
    var selectedStreamMode by rememberSaveable { mutableStateOf(uiState.streamPathMode) }

    LaunchedEffect(uiState.backendUrl) {
        if (backendUrlInput.isBlank()) {
            backendUrlInput = uiState.backendUrl
        }
    }
    LaunchedEffect(uiState.streamPathMode) {
        selectedStreamMode = uiState.streamPathMode
    }

    val urlValidationResult = UrlValidator.normalizeAndValidate(backendUrlInput)
    val urlError = urlValidationResult.exceptionOrNull()?.message

    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AxionRed,
        unfocusedBorderColor = AxionBorder,
        focusedContainerColor = AxionSurfaceElevated,
        unfocusedContainerColor = AxionSurfaceElevated,
        focusedTextColor = AxionTextPrimary,
        unfocusedTextColor = AxionTextPrimary,
        focusedLabelColor = AxionRed,
        unfocusedLabelColor = AxionTextSecondary
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("settings_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "settings_header") {
            SectionHeaderRow(
                title = "Backend & Security Settings",
                subtitle = "Configure Termux Express server URL & encrypted X-Admin-Token"
            )
        }

        // Backend URL Configuration Card
        item(key = "backend_url_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AxionSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AxionBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "TERMUX BACKEND URL",
                        style = MaterialTheme.typography.labelMedium,
                        color = AxionRed
                    )

                    OutlinedTextField(
                        value = backendUrlInput,
                        onValueChange = { backendUrlInput = it },
                        label = { Text("Server Base URL") },
                        placeholder = { Text("http://127.0.0.1:3000", color = AxionTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Link, contentDescription = null, tint = AxionTextSecondary)
                        },
                        isError = urlError != null,
                        supportingText = {
                            if (urlError != null) {
                                Text(urlError, color = AxionError)
                            } else {
                                Text(
                                    "Use http://127.0.0.1:3000 on the same Android phone or your Termux LAN IP.",
                                    color = AxionTextMuted
                                )
                            }
                        },
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_backend_url_input")
                    )

                    // Quick Termux URL Presets
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val presets = listOf("http://127.0.0.1:3000", "http://localhost:3000")
                        presets.forEach { preset ->
                            val selected = backendUrlInput.trim().trimEnd('/') == preset
                            FilterChip(
                                selected = selected,
                                onClick = { backendUrlInput = preset },
                                label = {
                                    Text(
                                        text = preset.removePrefix("http://"),
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 11.sp
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AxionRedContainer,
                                    selectedLabelColor = AxionTextPrimary,
                                    containerColor = AxionSurfaceElevated,
                                    labelColor = AxionTextSecondary
                                ),
                                border = FilterChipDefaults.filterChipBorder(
                                    enabled = true,
                                    selected = selected,
                                    borderColor = AxionBorder,
                                    selectedBorderColor = AxionRed
                                )
                            )
                        }
                    }

                    HorizontalDivider(color = AxionBorder)

                    Text(
                        text = "STREAMING URL PATTERN",
                        style = MaterialTheme.typography.labelMedium,
                        color = AxionTextSecondary
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        StreamPathMode.entries.forEach { mode ->
                            val isSelected = selectedStreamMode == mode
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) AxionSurfaceElevated else AxionBlack.copy(alpha = 0.35f))
                                    .clickable { selectedStreamMode = mode }
                                    .padding(horizontal = 10.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = isSelected,
                                    onClick = { selectedStreamMode = mode },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = AxionRed,
                                        unselectedColor = AxionTextMuted
                                    )
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = mode.label,
                                        fontFamily = JetBrainsMonoFontFamily,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AxionTextPrimary
                                    )
                                    Text(
                                        text = mode.patternHint,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = AxionTextMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Secure Admin Token Card
        item(key = "admin_token_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AxionSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AxionBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ADMIN AUTHENTICATION",
                            style = MaterialTheme.typography.labelMedium,
                            color = AxionRed
                        )
                        Surface(
                            color = if (uiState.hasAdminToken) AxionEmeraldContainer else AxionAmberContainer,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(
                                1.dp,
                                if (uiState.hasAdminToken) AxionEmerald.copy(alpha = 0.45f) else AxionAmber.copy(alpha = 0.45f)
                            )
                        ) {
                            Text(
                                text = if (uiState.hasAdminToken) "TOKEN STORED" else "NOT CONFIGURED",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = if (uiState.hasAdminToken) AxionEmerald else AxionAmber,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tokenInput,
                        onValueChange = { tokenInput = it },
                        label = {
                            Text(
                                if (uiState.hasAdminToken) "Replace X-Admin-Token (leave blank to keep)" else "Enter X-Admin-Token"
                            )
                        },
                        placeholder = { Text("Paste your Termux admin secret token", color = AxionTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Key, contentDescription = null, tint = AxionTextSecondary)
                        },
                        trailingIcon = {
                            IconButton(onClick = { showToken = !showToken }) {
                                Icon(
                                    imageVector = if (showToken) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                                    contentDescription = if (showToken) "Hide token" else "Show token",
                                    tint = AxionTextSecondary
                                )
                            }
                        },
                        visualTransformation = if (showToken) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("settings_admin_token_input")
                    )

                    // Security Safeguard Summary
                    Surface(
                        color = AxionSurfaceElevated,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, AxionBorder)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Security,
                                contentDescription = null,
                                tint = AxionEmerald,
                                modifier = Modifier.size(18.dp)
                            )
                            Text(
                                text = "Encrypted at rest via Android Keystore (AES-256-GCM). Sent strictly in the X-Admin-Token HTTP header on /api/admin/* calls—never embedded in URLs, logs, or cloud backups.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AxionTextSecondary
                            )
                        }
                    }

                    // Primary Save + Test Connection Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Button(
                            onClick = {
                                onSaveSettings(backendUrlInput, tokenInput, selectedStreamMode) {
                                    tokenInput = ""
                                    showToken = false
                                }
                            },
                            enabled = urlError == null,
                            colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_save_button")
                        ) {
                            Icon(Icons.Filled.Save, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save")
                        }

                        OutlinedButton(
                            onClick = { onTestConnection(backendUrlInput) },
                            enabled = !uiState.isTestingConnection && urlError == null,
                            border = BorderStroke(1.dp, AxionBorderStrong),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .height(50.dp)
                                .testTag("settings_test_connection_button")
                        ) {
                            if (uiState.isTestingConnection) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp,
                                    color = AxionRed
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.NetworkCheck,
                                    contentDescription = null,
                                    tint = AxionTextPrimary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(Modifier.width(6.dp))
                            Text("Test", color = AxionTextPrimary)
                        }
                    }
                }
            }
        }

        // Live Connection Diagnostics Result Card
        uiState.connectionDiagnostics?.let { diag ->
            item(key = "connection_diagnostics_card") {
                ConnectionDiagnosticsCard(diagnostics = diag)
            }
        }

        // Session / Logout Card
        item(key = "logout_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AxionSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AxionError.copy(alpha = 0.35f))
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "ADMIN SESSION",
                        style = MaterialTheme.typography.labelMedium,
                        color = AxionError
                    )
                    Text(
                        text = "Clearing your session immediately removes the encrypted X-Admin-Token from Android Keystore-backed storage and active memory.",
                        style = MaterialTheme.typography.bodySmall,
                        color = AxionTextSecondary
                    )
                    OutlinedButton(
                        onClick = {
                            tokenInput = ""
                            showToken = false
                            onLogout()
                        },
                        enabled = uiState.hasAdminToken || tokenInput.isNotEmpty(),
                        border = BorderStroke(1.dp, AxionError.copy(alpha = 0.6f)),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = AxionError),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("settings_logout_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("Logout & Clear Admin Token")
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun ConnectionDiagnosticsCard(diagnostics: ConnectionDiagnostics) {
    val healthOk = diagnostics.healthResult.isOnline
    val borderColor = if (healthOk && diagnostics.catalogReachable) AxionEmerald else AxionError

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("connection_diagnostics_card"),
        color = AxionSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, borderColor.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = if (healthOk) Icons.Filled.CheckCircle else Icons.Filled.ErrorOutline,
                    contentDescription = null,
                    tint = if (healthOk) AxionEmerald else AxionError,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = "Connection Test Results",
                    style = MaterialTheme.typography.titleMedium,
                    color = AxionTextPrimary
                )
            }

            DiagnosticLine(
                label = "Tested URL",
                value = diagnostics.testedUrl,
                isGood = healthOk
            )
            DiagnosticLine(
                label = "GET /api/health",
                value = if (healthOk) {
                    val lat = diagnostics.healthResult.latencyMs?.let { " · ${it}ms" } ?: ""
                    "HTTP ${diagnostics.healthResult.httpCode ?: 200}$lat (${diagnostics.healthResult.serverMessage ?: "OK"})"
                } else {
                    diagnostics.healthResult.serverMessage ?: "Offline"
                },
                isGood = healthOk
            )
            DiagnosticLine(
                label = "GET /api/videos",
                value = if (diagnostics.catalogReachable) {
                    "Reachable (${diagnostics.catalogVideoCount ?: 0} movies returned)"
                } else {
                    diagnostics.catalogErrorMessage ?: "Unreachable"
                },
                isGood = diagnostics.catalogReachable
            )
            DiagnosticLine(
                label = "Admin Token",
                value = if (diagnostics.hasAdminTokenConfigured) {
                    "Configured locally (sent on POST/DELETE /api/admin/videos)"
                } else {
                    "Not configured (admin writes disabled)"
                },
                isGood = diagnostics.hasAdminTokenConfigured
            )
        }
    }
}

@Composable
private fun DiagnosticLine(
    label: String,
    value: String,
    isGood: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = AxionTextMuted
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = if (isGood) AxionTextPrimary else AxionError
        )
    }
}
