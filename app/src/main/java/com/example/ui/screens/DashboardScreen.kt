package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dns
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.FormatUtils
import com.example.data.StreamProbeStatus
import com.example.data.VideoItem
import com.example.ui.components.AxionBrandHeader
import com.example.ui.components.AxionMetricCard
import com.example.ui.components.BackendErrorBanner
import com.example.ui.components.LiveConnectionPill
import com.example.ui.components.MoviePosterThumbnail
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StreamAvailabilityBadge
import com.example.ui.theme.AxionAmber
import com.example.ui.theme.AxionBlack
import com.example.ui.theme.AxionBorder
import com.example.ui.theme.AxionBorderStrong
import com.example.ui.theme.AxionCyan
import com.example.ui.theme.AxionEmerald
import com.example.ui.theme.AxionError
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
fun DashboardScreen(
    uiState: AxionUiState,
    onRefresh: () -> Unit,
    onNavigateTab: (AdminTab) -> Unit,
    onInspectVideo: (VideoItem) -> Unit,
    onPlayPreview: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Cinema Hero Header Banner
        item(key = "hero_banner") {
            DashboardHeroBanner(
                uiState = uiState,
                onRefresh = onRefresh
            )
        }

        // Backend Error Banner (real offline / HTTP error states)
        if (uiState.catalogError != null) {
            item(key = "backend_error_banner") {
                BackendErrorBanner(
                    title = "Termux Backend Connection Issue",
                    message = uiState.catalogError.message,
                    onRetry = onRefresh,
                    onOpenSettings = { onNavigateTab(AdminTab.SETTINGS) }
                )
            }
        }

        // Token Reminder Banner if backend is online but admin token is unset
        if (uiState.healthStatus?.isOnline == true && !uiState.hasAdminToken) {
            item(key = "token_warning_banner") {
                Surface(
                    color = AxionSurfaceElevated,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, AxionAmber.copy(alpha = 0.4f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            Text(
                                text = "Read-Only Mode (No Admin Token)",
                                style = MaterialTheme.typography.titleSmall,
                                color = AxionAmber
                            )
                            Text(
                                text = "Configure X-Admin-Token in Settings to register or delete movies.",
                                style = MaterialTheme.typography.bodySmall,
                                color = AxionTextSecondary
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        OutlinedButton(
                            onClick = { onNavigateTab(AdminTab.SETTINGS) },
                            border = BorderStroke(1.dp, AxionAmber.copy(alpha = 0.6f)),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("dashboard_configure_token_button")
                        ) {
                            Text("Configure", color = AxionAmber)
                        }
                    }
                }
            }
        }

        // Real-Time Metrics Grid
        item(key = "metrics_grid") {
            val isOnline = uiState.healthStatus?.isOnline == true
            val storageValue = uiState.computedStorageBytes?.let { FormatUtils.formatBytes(it) } ?: "N/A"
            val backendValue = when {
                uiState.isCheckingHealth -> "..."
                isOnline -> "Online"
                else -> "Offline"
            }
            val backendSub = when {
                uiState.isCheckingHealth -> "Checking /api/health"
                isOnline -> {
                    val lat = uiState.healthStatus?.latencyMs?.let { "${it}ms" } ?: "OK"
                    val tokenLabel = if (uiState.hasAdminToken) "Token Set" else "No Token"
                    "HTTP ${uiState.healthStatus?.httpCode ?: 200} · $lat · $tokenLabel"
                }
                else -> uiState.healthStatus?.serverMessage ?: "Server unreachable"
            }

            BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                val isWide = maxWidth >= 640.dp
                if (isWide) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AxionMetricCard(
                            title = "Total Movies",
                            value = if (uiState.isLoadingVideos && uiState.videos.isEmpty()) "--" else uiState.totalMovies.toString(),
                            subtitle = "Live count from GET /api/videos",
                            icon = Icons.Filled.Movie,
                            accentColor = AxionRed,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_total_movies"
                        )
                        AxionMetricCard(
                            title = "Registered Catalog",
                            value = if (uiState.isLoadingVideos && uiState.videos.isEmpty()) "--" else uiState.registeredMoviesCount.toString(),
                            subtitle = "Entries with ID & filename",
                            icon = Icons.Filled.LibraryBooks,
                            accentColor = AxionCyan,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_registered_movies"
                        )
                        AxionMetricCard(
                            title = "Video Storage",
                            value = storageValue,
                            subtitle = uiState.storageSourceDescription,
                            icon = Icons.Filled.Storage,
                            accentColor = AxionAmber,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_storage_usage"
                        )
                        AxionMetricCard(
                            title = "Backend Status",
                            value = backendValue,
                            subtitle = backendSub,
                            icon = Icons.Filled.Dns,
                            accentColor = if (isOnline) AxionEmerald else AxionError,
                            modifier = Modifier.weight(1f),
                            testTag = "metric_backend_status"
                        )
                    }
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AxionMetricCard(
                                title = "Total Movies",
                                value = if (uiState.isLoadingVideos && uiState.videos.isEmpty()) "--" else uiState.totalMovies.toString(),
                                subtitle = "Live GET /api/videos",
                                icon = Icons.Filled.Movie,
                                accentColor = AxionRed,
                                modifier = Modifier.weight(1f),
                                testTag = "metric_total_movies"
                            )
                            AxionMetricCard(
                                title = "Registered",
                                value = if (uiState.isLoadingVideos && uiState.videos.isEmpty()) "--" else uiState.registeredMoviesCount.toString(),
                                subtitle = "SQLite catalog items",
                                icon = Icons.Filled.LibraryBooks,
                                accentColor = AxionCyan,
                                modifier = Modifier.weight(1f),
                                testTag = "metric_registered_movies"
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            AxionMetricCard(
                                title = "Video Storage",
                                value = storageValue,
                                subtitle = uiState.storageSourceDescription,
                                icon = Icons.Filled.Storage,
                                accentColor = AxionAmber,
                                modifier = Modifier.weight(1f),
                                testTag = "metric_storage_usage"
                            )
                            AxionMetricCard(
                                title = "Backend",
                                value = backendValue,
                                subtitle = backendSub,
                                icon = Icons.Filled.Dns,
                                accentColor = if (isOnline) AxionEmerald else AxionError,
                                modifier = Modifier.weight(1f),
                                testTag = "metric_backend_status"
                            )
                        }
                    }
                }
            }
        }

        // Recently Added Movies Header
        item(key = "recent_header") {
            SectionHeaderRow(
                title = "Recently Added Movies",
                subtitle = if (uiState.videos.isEmpty()) "Synced directly with Termux SQLite" else "Showing latest ${uiState.recentVideos.size} of ${uiState.totalMovies} registered",
                trailing = {
                    if (uiState.videos.isNotEmpty()) {
                        TextButton(
                            onClick = { onNavigateTab(AdminTab.MOVIES) },
                            modifier = Modifier.testTag("dashboard_view_all_movies")
                        ) {
                            Text("View All", color = AxionRed)
                        }
                    }
                }
            )
        }

        if (uiState.isLoadingVideos && uiState.videos.isEmpty()) {
            item(key = "recent_loading") {
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
                        Text(
                            text = "Fetching real catalog from ${uiState.backendUrl}...",
                            style = MaterialTheme.typography.bodyMedium,
                            color = AxionTextSecondary
                        )
                    }
                }
            }
        } else if (uiState.recentVideos.isEmpty()) {
            item(key = "recent_empty") {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = AxionSurface,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, AxionBorder)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = AxionTextMuted,
                            modifier = Modifier.size(36.dp)
                        )
                        Text(
                            text = if (uiState.catalogError != null) "Catalog Unavailable" else "No Movies Registered Yet",
                            style = MaterialTheme.typography.titleMedium,
                            color = AxionTextPrimary
                        )
                        Text(
                            text = if (uiState.catalogError != null) {
                                "Connect to your running Termux server to load your live catalog."
                            } else {
                                "Register an existing video file from your Termux videos folder to populate AXION STREAM."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AxionTextSecondary
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onNavigateTab(AdminTab.ADD_MOVIE) },
                                colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.testTag("dashboard_empty_add_button")
                            ) {
                                Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Add Movie")
                            }
                            OutlinedButton(
                                onClick = { onNavigateTab(AdminTab.SETTINGS) },
                                border = BorderStroke(1.dp, AxionBorderStrong),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Icon(Icons.Filled.Settings, contentDescription = null, tint = AxionTextPrimary, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Backend URL", color = AxionTextPrimary)
                            }
                        }
                    }
                }
            }
        } else {
            items(uiState.recentVideos, key = { "recent_${it.id}" }) { video ->
                RecentMovieRowCard(
                    video = video,
                    probeStatus = uiState.streamProbes[video.id],
                    onInspect = { onInspectVideo(video) },
                    onPlayPreview = { onPlayPreview(video) }
                )
            }
        }

        // Transparent Backend Integration Audit Card
        item(key = "api_capabilities_audit") {
            BackendCapabilitiesAuditCard()
        }

        item { Spacer(modifier = Modifier.height(12.dp)) }
    }
}

@Composable
private fun DashboardHeroBanner(
    uiState: AxionUiState,
    onRefresh: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .border(1.dp, AxionBorder, RoundedCornerShape(20.dp))
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_cinema_backdrop_1795667766858),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.matchParentSize()
        )
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            AxionBlack.copy(alpha = 0.65f),
                            AxionBlack.copy(alpha = 0.88f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                AxionBrandHeader()
                IconButton(
                    onClick = onRefresh,
                    enabled = !uiState.isCheckingHealth && !uiState.isLoadingVideos,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(AxionSurface.copy(alpha = 0.85f))
                        .border(1.dp, AxionBorder, RoundedCornerShape(12.dp))
                        .testTag("dashboard_refresh_button")
                ) {
                    if (uiState.isCheckingHealth || uiState.isLoadingVideos) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AxionRed
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Refresh dashboard data",
                            tint = AxionTextPrimary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LiveConnectionPill(
                    healthStatus = uiState.healthStatus,
                    isChecking = uiState.isCheckingHealth
                )
                Text(
                    text = uiState.backendUrl,
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = AxionTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(start = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun RecentMovieRowCard(
    video: VideoItem,
    probeStatus: StreamProbeStatus?,
    onInspect: () -> Unit,
    onPlayPreview: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect)
            .testTag("recent_movie_card_${video.id}"),
        color = AxionSurface,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AxionBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            MoviePosterThumbnail(
                posterUrl = video.posterUrl,
                title = video.title,
                modifier = Modifier
                    .width(54.dp)
                    .height(74.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AxionTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = video.filename.ifBlank { "No filename" },
                    fontFamily = JetBrainsMonoFontFamily,
                    fontSize = 11.sp,
                    color = AxionTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "#${video.id}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = AxionTextSecondary
                    )
                    StreamAvailabilityBadge(status = probeStatus)
                }
            }
            IconButton(
                onClick = onPlayPreview,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(AxionRed.copy(alpha = 0.16f))
                    .testTag("recent_play_button_${video.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayArrow,
                    contentDescription = "Play preview of ${video.title}",
                    tint = AxionRed
                )
            }
        }
    }
}

@Composable
private fun BackendCapabilitiesAuditCard() {
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
                text = "Termux Backend Integration Map",
                style = MaterialTheme.typography.titleMedium,
                color = AxionTextPrimary
            )
            Text(
                text = "Connected endpoints vs. features requiring additional Express/SQLite routes.",
                style = MaterialTheme.typography.bodySmall,
                color = AxionTextMuted
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EndpointAuditRow(
                    supported = true,
                    endpoint = "GET /api/health",
                    detail = "Live server status & latency"
                )
                EndpointAuditRow(
                    supported = true,
                    endpoint = "GET /api/videos",
                    detail = "Catalog listing, search & filtering"
                )
                EndpointAuditRow(
                    supported = true,
                    endpoint = "POST /api/admin/videos",
                    detail = "Register existing file (title, filename)"
                )
                EndpointAuditRow(
                    supported = true,
                    endpoint = "DELETE /api/admin/videos/:id",
                    detail = "Delete catalog entry via X-Admin-Token"
                )
            }

            Spacer(Modifier.height(2.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                EndpointAuditRow(
                    supported = false,
                    endpoint = "PATCH /api/admin/videos/:id",
                    detail = "Required for editing movie metadata"
                )
                EndpointAuditRow(
                    supported = false,
                    endpoint = "POST /api/admin/upload",
                    detail = "Required for uploading video files directly"
                )
                EndpointAuditRow(
                    supported = false,
                    endpoint = "GET /api/admin/storage",
                    detail = "Optional: exact Termux videos folder disk usage"
                )
                EndpointAuditRow(
                    supported = false,
                    endpoint = "SQLite metadata columns",
                    detail = "description, category, poster_url, release_year, duration"
                )
            }
        }
    }
}

@Composable
private fun EndpointAuditRow(
    supported: Boolean,
    endpoint: String,
    detail: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(AxionSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (supported) Icons.Filled.CheckCircle else Icons.Outlined.Schedule,
            contentDescription = if (supported) "Supported" else "Requires backend addition",
            tint = if (supported) AxionEmerald else AxionAmber,
            modifier = Modifier.size(16.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = endpoint,
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = AxionTextPrimary
            )
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = AxionTextSecondary
            )
        }
    }
}
