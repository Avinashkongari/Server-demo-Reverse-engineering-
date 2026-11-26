package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Radar
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StreamProbeStatus
import com.example.data.VideoItem
import com.example.ui.components.BackendErrorBanner
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StreamAvailabilityBadge
import com.example.ui.theme.AxionAmber
import com.example.ui.theme.AxionBlack
import com.example.ui.theme.AxionBorder
import com.example.ui.theme.AxionBorderStrong
import com.example.ui.theme.AxionEmerald
import com.example.ui.theme.AxionError
import com.example.ui.theme.AxionRed
import com.example.ui.theme.AxionSurface
import com.example.ui.theme.AxionSurfaceElevated
import com.example.ui.theme.AxionSurfaceHigh
import com.example.ui.theme.AxionTextMuted
import com.example.ui.theme.AxionTextPrimary
import com.example.ui.theme.AxionTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.viewmodel.AdminTab
import com.example.viewmodel.AxionUiState

@Composable
fun VideosScreen(
    uiState: AxionUiState,
    resolveStreamUrl: (VideoItem) -> String,
    onVerifyAllStreams: () -> Unit,
    onProbeSingleStream: (VideoItem) -> Unit,
    onPlayPreview: (VideoItem) -> Unit,
    onCopyStreamUrl: (String) -> Unit,
    onRefreshCatalog: () -> Unit,
    onNavigateTab: (AdminTab) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboard = LocalClipboardManager.current
    val availableCount = uiState.streamProbes.values.count { it is StreamProbeStatus.Available }
    val missingCount = uiState.streamProbes.values.count { it is StreamProbeStatus.Missing }
    val unreachableCount = uiState.streamProbes.values.count { it is StreamProbeStatus.Error }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("videos_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "videos_header") {
            SectionHeaderRow(
                title = "Video Streams",
                subtitle = "Route mode: ${uiState.streamPathMode.label}",
                trailing = {
                    Button(
                        onClick = onVerifyAllStreams,
                        enabled = !uiState.isProbingStreams && uiState.videos.isNotEmpty(),
                        colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 14.dp),
                        modifier = Modifier
                            .height(48.dp)
                            .testTag("verify_all_streams_button")
                    ) {
                        if (uiState.isProbingStreams) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = AxionTextPrimary
                            )
                        } else {
                            Icon(Icons.Filled.Radar, contentDescription = null, modifier = Modifier.size(18.dp))
                        }
                        Spacer(Modifier.width(6.dp))
                        Text(if (uiState.isProbingStreams) "Probing..." else "Verify")
                    }
                }
            )
        }

        // Real Stream Availability Summary Strip
        if (uiState.videos.isNotEmpty()) {
            item(key = "stream_summary_strip") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StreamSummaryChip(
                        label = "AVAILABLE",
                        count = availableCount,
                        accent = AxionEmerald,
                        modifier = Modifier.weight(1f)
                    )
                    StreamSummaryChip(
                        label = "MISSING",
                        count = missingCount,
                        accent = AxionError,
                        modifier = Modifier.weight(1f)
                    )
                    StreamSummaryChip(
                        label = "ERROR",
                        count = unreachableCount,
                        accent = AxionAmber,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        if (uiState.catalogError != null) {
            item(key = "videos_error") {
                BackendErrorBanner(
                    title = "Backend Streams Unreachable",
                    message = uiState.catalogError.message,
                    onRetry = onRefreshCatalog,
                    onOpenSettings = { onNavigateTab(AdminTab.SETTINGS) }
                )
            }
        }

        if (uiState.isLoadingVideos && uiState.videos.isEmpty()) {
            item(key = "videos_loading") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AxionSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AxionBorder)
                ) {
                    Row(
                        modifier = Modifier.padding(24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(22.dp),
                            strokeWidth = 2.5.dp,
                            color = AxionRed
                        )
                        Spacer(Modifier.width(12.dp))
                        Text("Loading video streams from Termux...", color = AxionTextSecondary)
                    }
                }
            }
        } else if (uiState.videos.isEmpty()) {
            item(key = "videos_empty") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AxionSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AxionBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.VideoLibrary,
                            contentDescription = null,
                            tint = AxionTextMuted,
                            modifier = Modifier.size(38.dp)
                        )
                        Text(
                            text = "No Registered Video Streams",
                            style = MaterialTheme.typography.titleMedium,
                            color = AxionTextPrimary
                        )
                        Text(
                            text = "Register video files from your Termux videos folder to verify availability and preview playback.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AxionTextSecondary
                        )
                    }
                }
            }
        } else {
            items(uiState.videos, key = { "stream_${it.id}" }) { video ->
                val streamUrl = resolveStreamUrl(video)
                val probeStatus = uiState.streamProbes[video.id]
                VideoStreamCard(
                    video = video,
                    streamUrl = streamUrl,
                    probeStatus = probeStatus,
                    onPlayPreview = { onPlayPreview(video) },
                    onProbeSingle = { onProbeSingleStream(video) },
                    onCopyUrl = {
                        clipboard.setText(AnnotatedString(streamUrl))
                        onCopyStreamUrl(streamUrl)
                    }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun StreamSummaryChip(
    label: String,
    count: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = AxionSurface,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AxionBorder)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                color = AxionTextMuted
            )
            Text(
                text = count.toString(),
                style = MaterialTheme.typography.headlineSmall,
                color = accent
            )
        }
    }
}

@Composable
private fun VideoStreamCard(
    video: VideoItem,
    streamUrl: String,
    probeStatus: StreamProbeStatus?,
    onPlayPreview: () -> Unit,
    onProbeSingle: () -> Unit,
    onCopyUrl: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("video_stream_card_${video.id}"),
        color = AxionSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AxionBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Title + Movie ID + Availability Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = AxionTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = AxionSurfaceHigh,
                            shape = RoundedCornerShape(6.dp),
                            border = BorderStroke(1.dp, AxionBorder)
                        ) {
                            Text(
                                text = "ID ${video.id}",
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AxionTextPrimary,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                            )
                        }
                        Text(
                            text = video.filename.ifBlank { "No filename" },
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = AxionTextSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Spacer(Modifier.width(8.dp))
                StreamAvailabilityBadge(status = probeStatus)
            }

            // Streaming URL Monospace Box
            Surface(
                color = AxionBlack,
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, AxionBorder)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, top = 6.dp, bottom = 6.dp, end = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "STREAMING URL",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 9.sp,
                            color = AxionTextMuted
                        )
                        Text(
                            text = streamUrl,
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = AxionTextSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onCopyUrl,
                        modifier = Modifier.testTag("copy_stream_url_${video.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ContentCopy,
                            contentDescription = "Copy streaming URL",
                            tint = AxionTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            if (probeStatus is StreamProbeStatus.Missing) {
                Text(
                    text = "Termux server returned HTTP ${probeStatus.httpCode}. Confirm '${video.filename}' exists in the videos folder or adjust the Stream Route Pattern in Settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxionError
                )
            } else if (probeStatus is StreamProbeStatus.Error) {
                Text(
                    text = "Probe error: ${probeStatus.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = AxionAmber
                )
            }

            // Actions Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onPlayPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("play_preview_button_${video.id}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play Preview")
                }

                OutlinedButton(
                    onClick = onProbeSingle,
                    enabled = probeStatus !is StreamProbeStatus.Checking,
                    border = BorderStroke(1.dp, AxionBorderStrong),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("check_stream_button_${video.id}")
                ) {
                    if (probeStatus is StreamProbeStatus.Checking) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AxionAmber
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Check availability for ${video.title}",
                            tint = AxionTextPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text("Check", color = AxionTextPrimary)
                }
            }
        }
    }
}
