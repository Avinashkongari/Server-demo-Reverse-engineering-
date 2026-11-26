package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Title
import androidx.compose.material.icons.filled.VideoFile
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.SectionHeaderRow
import com.example.ui.theme.AxionAmber
import com.example.ui.theme.AxionAmberContainer
import com.example.ui.theme.AxionBorder
import com.example.ui.theme.AxionEmerald
import com.example.ui.theme.AxionEmeraldContainer
import com.example.ui.theme.AxionError
import com.example.ui.theme.AxionErrorContainer
import com.example.ui.theme.AxionRed
import com.example.ui.theme.AxionSurface
import com.example.ui.theme.AxionSurfaceElevated
import com.example.ui.theme.AxionTextMuted
import com.example.ui.theme.AxionTextPrimary
import com.example.ui.theme.AxionTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AdminTab
import com.example.viewmodel.AxionUiState

@Composable
fun AddMovieScreen(
    uiState: AxionUiState,
    onSubmitMovie: (title: String, filename: String, onSuccessCleared: () -> Unit) -> Unit,
    onClearFeedback: () -> Unit,
    onNavigateTab: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by rememberSaveable { mutableStateOf("") }
    var filename by rememberSaveable { mutableStateOf("") }

    // Extended fields displayed cleanly in locked/schema-pending state so the user is never
    // misled into believing the current Express API persists fields it does not accept yet.
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = AxionRed,
        unfocusedBorderColor = AxionBorder,
        disabledBorderColor = AxionBorder.copy(alpha = 0.55f),
        focusedContainerColor = AxionSurfaceElevated,
        unfocusedContainerColor = AxionSurfaceElevated,
        disabledContainerColor = AxionSurface,
        focusedTextColor = AxionTextPrimary,
        unfocusedTextColor = AxionTextPrimary,
        disabledTextColor = AxionTextMuted,
        focusedLabelColor = AxionRed,
        unfocusedLabelColor = AxionTextSecondary,
        disabledLabelColor = AxionTextMuted
    )

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_movie_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item(key = "add_movie_header") {
            SectionHeaderRow(
                title = "Register New Movie",
                subtitle = "POST ${uiState.backendUrl}/api/admin/videos"
            )
        }

        // Important Termux Server File Prerequisite Notice (No fake upload button!)
        item(key = "termux_file_notice") {
            Surface(
                color = AxionSurfaceElevated,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, AxionBorder)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.FolderOpen,
                        contentDescription = null,
                        tint = AxionRed,
                        modifier = Modifier.size(22.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Video File Must Exist on Termux Server",
                            style = MaterialTheme.typography.titleSmall,
                            color = AxionTextPrimary
                        )
                        Text(
                            text = "Place the media file inside your Termux server's videos folder first, then enter its exact filename below. Direct uploads are intentionally omitted until POST /api/admin/upload is added.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AxionTextSecondary
                        )
                    }
                }
            }
        }

        // Missing Admin Token Gate
        if (!uiState.hasAdminToken) {
            item(key = "missing_token_notice") {
                Surface(
                    color = AxionErrorContainer,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AxionError.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "X-Admin-Token Required",
                                style = MaterialTheme.typography.titleSmall,
                                color = AxionError
                            )
                            Text(
                                text = "Set your admin token in Settings before registering movies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AxionTextSecondary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { onNavigateTab(AdminTab.SETTINGS) },
                            border = BorderStroke(1.dp, AxionError.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("add_movie_goto_settings")
                        ) {
                            Icon(Icons.Filled.Settings, contentDescription = null, tint = AxionTextPrimary, modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Settings", color = AxionTextPrimary)
                        }
                    }
                }
            }
        }

        // Primary Active Backend Fields Card
        item(key = "active_fields_card") {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = AxionSurface,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, AxionBorder)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "ACTIVE BACKEND FIELDS",
                            style = MaterialTheme.typography.labelMedium,
                            color = AxionRed
                        )
                        Text(
                            text = "Sent in JSON body",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 10.sp,
                            color = AxionTextMuted
                        )
                    }

                    OutlinedTextField(
                        value = title,
                        onValueChange = {
                            title = it
                            if (uiState.addMovieFeedback != null) onClearFeedback()
                        },
                        label = { Text("Movie Title *") },
                        placeholder = { Text("e.g. Blade Runner 2049", color = AxionTextMuted) },
                        leadingIcon = {
                            Icon(Icons.Filled.Title, contentDescription = null, tint = AxionTextSecondary)
                        },
                        singleLine = true,
                        enabled = !uiState.isSubmittingMovie,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_movie_title_input")
                    )

                    OutlinedTextField(
                        value = filename,
                        onValueChange = {
                            filename = it
                            if (uiState.addMovieFeedback != null) onClearFeedback()
                        },
                        label = { Text("Server Filename *") },
                        placeholder = { Text("e.g. blade_runner_2049.mp4", color = AxionTextMuted) },
                        supportingText = {
                            Text(
                                text = "Exact file name in Termux videos/ folder (no '../' or path separators).",
                                color = AxionTextMuted
                            )
                        },
                        leadingIcon = {
                            Icon(Icons.Filled.VideoFile, contentDescription = null, tint = AxionTextSecondary)
                        },
                        singleLine = true,
                        enabled = !uiState.isSubmittingMovie,
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_movie_filename_input")
                    )

                    // Live Submission Feedback Banner
                    uiState.addMovieFeedback?.let { feedback ->
                        val bg = if (feedback.isError) AxionErrorContainer else AxionEmeraldContainer
                        val fg = if (feedback.isError) AxionError else AxionEmerald
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_movie_feedback_banner"),
                            color = bg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, fg.copy(alpha = 0.45f))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Icon(
                                    imageVector = if (feedback.isError) Icons.Filled.ErrorOutline else Icons.Filled.CheckCircle,
                                    contentDescription = null,
                                    tint = fg,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = feedback.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AxionTextPrimary
                                )
                            }
                        }
                    }

                    Button(
                        onClick = {
                            onSubmitMovie(title, filename) {
                                title = ""
                                filename = ""
                            }
                        },
                        enabled = !uiState.isSubmittingMovie && uiState.hasAdminToken,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = AxionRed,
                            disabledContainerColor = AxionSurfaceElevated
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("register_movie_submit_button")
                    ) {
                        if (uiState.isSubmittingMovie) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.5.dp,
                                color = AxionTextPrimary
                            )
                            Spacer(Modifier.width(10.dp))
                            Text("Registering with Termux...")
                        } else {
                            Icon(Icons.Filled.AddCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Register Movie via POST /api/admin/videos")
                        }
                    }
                }
            }
        }

        // Extended Metadata Form Blueprint (Honest & Non-Misleading)
        item(key = "extended_metadata_card") {
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
                    Surface(
                        color = AxionAmberContainer,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AxionAmber.copy(alpha = 0.4f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = "Requires backend schema update",
                                tint = AxionAmber,
                                modifier = Modifier.size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text(
                                    text = "Extended Metadata (Backend Schema Pending)",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = AxionAmber
                                )
                                Text(
                                    text = "Your existing POST /api/admin/videos accepts title and filename only. These fields stay locked until your Termux SQLite schema & Express POST route store description, category, poster_url, release_year, and duration.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = AxionTextSecondary
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = "",
                        onValueChange = {},
                        enabled = false,
                        label = { Text("Description (Requires SQLite column)") },
                        colors = fieldColors,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Category") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Release Year") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Duration") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = "",
                            onValueChange = {},
                            enabled = false,
                            label = { Text("Poster Image URL") },
                            colors = fieldColors,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}
