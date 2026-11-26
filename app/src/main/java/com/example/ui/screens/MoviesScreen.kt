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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.StreamProbeStatus
import com.example.data.VideoItem
import com.example.ui.components.BackendErrorBanner
import com.example.ui.components.InfoPillNotice
import com.example.ui.components.MoviePosterThumbnail
import com.example.ui.components.SectionHeaderRow
import com.example.ui.components.StreamAvailabilityBadge
import com.example.ui.theme.AxionAmber
import com.example.ui.theme.AxionAmberContainer
import com.example.ui.theme.AxionBorder
import com.example.ui.theme.AxionBorderStrong
import com.example.ui.theme.AxionError
import com.example.ui.theme.AxionRed
import com.example.ui.theme.AxionRedContainer
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
fun MoviesScreen(
    uiState: AxionUiState,
    onSearchQueryChange: (String) -> Unit,
    onCategorySelected: (String?) -> Unit,
    onRefresh: () -> Unit,
    onNavigateTab: (AdminTab) -> Unit,
    onInspectVideo: (VideoItem) -> Unit,
    onPlayPreview: (VideoItem) -> Unit,
    onConfirmDelete: (VideoItem) -> Unit,
    modifier: Modifier = Modifier
) {
    var pendingDeleteVideo by remember { mutableStateOf<VideoItem?>(null) }

    pendingDeleteVideo?.let { videoToDelete ->
        AlertDialog(
            onDismissRequest = { pendingDeleteVideo = null },
            containerColor = AxionSurfaceElevated,
            titleContentColor = AxionTextPrimary,
            textContentColor = AxionTextSecondary,
            title = { Text("Delete Movie from Catalog?") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "This calls DELETE /api/admin/videos/${videoToDelete.id} on your Termux server to remove \"${videoToDelete.title}\" from SQLite."
                    )
                    Text(
                        text = "Filename: ${videoToDelete.filename}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = AxionTextMuted
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onConfirmDelete(videoToDelete)
                        pendingDeleteVideo = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                    modifier = Modifier.testTag("confirm_delete_movie_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { pendingDeleteVideo = null },
                    modifier = Modifier.testTag("cancel_delete_movie_button")
                ) {
                    Text("Cancel", color = AxionTextSecondary)
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("movies_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "movies_header") {
            SectionHeaderRow(
                title = "Movie Catalog",
                subtitle = "${uiState.filteredVideos.size} of ${uiState.totalMovies} movies shown",
                trailing = {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        IconButton(
                            onClick = onRefresh,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(AxionSurface)
                                .testTag("movies_refresh_button")
                        ) {
                            if (uiState.isLoadingVideos) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp,
                                    color = AxionRed
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Filled.Refresh,
                                    contentDescription = "Refresh movies",
                                    tint = AxionTextPrimary
                                )
                            }
                        }
                        Button(
                            onClick = { onNavigateTab(AdminTab.ADD_MOVIE) },
                            colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                            shape = RoundedCornerShape(12.dp),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
                            modifier = Modifier
                                .height(48.dp)
                                .testTag("movies_add_button")
                        ) {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
            )
        }

        // Search Bar
        item(key = "movies_search_bar") {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = onSearchQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("movies_search_input"),
                placeholder = {
                    Text("Search title, filename, ID, or category...", color = AxionTextMuted)
                },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = "Search movies", tint = AxionTextSecondary)
                },
                trailingIcon = {
                    if (uiState.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear search", tint = AxionTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AxionRed,
                    unfocusedBorderColor = AxionBorder,
                    focusedContainerColor = AxionSurface,
                    unfocusedContainerColor = AxionSurface,
                    focusedTextColor = AxionTextPrimary,
                    unfocusedTextColor = AxionTextPrimary
                )
            )
        }

        // Dynamic Category Filter Chips (strictly derived from real backend data)
        item(key = "category_filters") {
            val categories = uiState.availableCategories
            val hasUncategorized = uiState.videos.any { it.category.isNullOrBlank() }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item(key = "cat_all") {
                        FilterChip(
                            selected = uiState.selectedCategory == null,
                            onClick = { onCategorySelected(null) },
                            label = { Text("All (${uiState.totalMovies})") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AxionRedContainer,
                                selectedLabelColor = AxionTextPrimary,
                                containerColor = AxionSurface,
                                labelColor = AxionTextSecondary
                            ),
                            border = FilterChipDefaults.filterChipBorder(
                                enabled = true,
                                selected = uiState.selectedCategory == null,
                                borderColor = AxionBorder,
                                selectedBorderColor = AxionRed
                            ),
                            modifier = Modifier.testTag("category_chip_all")
                        )
                    }
                    items(categories, key = { "cat_$it" }) { cat ->
                        val selected = uiState.selectedCategory.equals(cat, ignoreCase = true)
                        FilterChip(
                            selected = selected,
                            onClick = { onCategorySelected(if (selected) null else cat) },
                            label = { Text(cat) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = AxionRedContainer,
                                selectedLabelColor = AxionTextPrimary,
                                containerColor = AxionSurface,
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
                    if (hasUncategorized && uiState.videos.isNotEmpty()) {
                        val selected = uiState.selectedCategory == AxionUiState.UNCATEGORIZED_FILTER
                        item(key = "cat_uncategorized") {
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    onCategorySelected(if (selected) null else AxionUiState.UNCATEGORIZED_FILTER)
                                },
                                label = { Text("Uncategorized") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = AxionRedContainer,
                                    selectedLabelColor = AxionTextPrimary,
                                    containerColor = AxionSurface,
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
                }

                if (uiState.videos.isNotEmpty() && categories.isEmpty()) {
                    InfoPillNotice(
                        text = "Categories, posters, descriptions, release year, and duration appear automatically once returned by GET /api/videos. Metadata editing requires adding PATCH /api/admin/videos/:id on your backend.",
                        accent = AxionAmber,
                        bg = AxionAmberContainer
                    )
                }
            }
        }

        // Error state
        if (uiState.catalogError != null) {
            item(key = "movies_error_banner") {
                BackendErrorBanner(
                    title = "Unable to Load Movie Catalog",
                    message = uiState.catalogError.message,
                    onRetry = onRefresh,
                    onOpenSettings = { onNavigateTab(AdminTab.SETTINGS) }
                )
            }
        }

        // Loading / Empty / Movie List
        if (uiState.isLoadingVideos && uiState.videos.isEmpty()) {
            item(key = "movies_loading") {
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
                        Text("Loading movies from Termux...", color = AxionTextSecondary)
                    }
                }
            }
        } else if (uiState.filteredVideos.isEmpty()) {
            item(key = "movies_empty") {
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
                            imageVector = Icons.Filled.Movie,
                            contentDescription = null,
                            tint = AxionTextMuted,
                            modifier = Modifier.size(38.dp)
                        )
                        val isFiltered = uiState.searchQuery.isNotBlank() || uiState.selectedCategory != null
                        Text(
                            text = if (isFiltered) "No Matching Movies" else "Catalog Is Empty",
                            style = MaterialTheme.typography.titleMedium,
                            color = AxionTextPrimary
                        )
                        Text(
                            text = if (isFiltered) {
                                "Try clearing your search query or category filter."
                            } else {
                                "No registered records returned from GET /api/videos."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = AxionTextSecondary
                        )
                        if (isFiltered) {
                            OutlinedButton(
                                onClick = {
                                    onSearchQueryChange("")
                                    onCategorySelected(null)
                                },
                                border = BorderStroke(1.dp, AxionBorderStrong)
                            ) {
                                Text("Clear Filters", color = AxionTextPrimary)
                            }
                        }
                    }
                }
            }
        } else {
            items(uiState.filteredVideos, key = { "movie_${it.id}" }) { video ->
                MovieCatalogCard(
                    video = video,
                    probeStatus = uiState.streamProbes[video.id],
                    isDeleting = video.id in uiState.deletingVideoIds,
                    onInspect = { onInspectVideo(video) },
                    onPlayPreview = { onPlayPreview(video) },
                    onDeleteClick = { pendingDeleteVideo = video }
                )
            }
        }

        item { Spacer(modifier = Modifier.height(16.dp)) }
    }
}

@Composable
private fun MovieCatalogCard(
    video: VideoItem,
    probeStatus: StreamProbeStatus?,
    isDeleting: Boolean,
    onInspect: () -> Unit,
    onPlayPreview: () -> Unit,
    onDeleteClick: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onInspect)
            .testTag("movie_card_${video.id}"),
        color = AxionSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AxionBorder)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                MoviePosterThumbnail(
                    posterUrl = video.posterUrl,
                    title = video.title,
                    modifier = Modifier
                        .width(78.dp)
                        .height(112.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(5.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = AxionTextPrimary,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    // Real metadata chips when available
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        MetaTagChip(text = "ID ${video.id}")
                        video.category?.let { MetaTagChip(text = it, highlight = true) }
                        video.releaseYear?.let { MetaTagChip(text = it.toString()) }
                        video.duration?.let { MetaTagChip(text = it) }
                    }

                    Text(
                        text = video.description ?: "No description metadata returned by backend.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (video.description != null) AxionTextSecondary else AxionTextMuted,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = video.filename.ifBlank { "No filename" },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 11.sp,
                        color = AxionTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    StreamAvailabilityBadge(status = probeStatus)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlayPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("movie_play_button_${video.id}")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview")
                }

                OutlinedButton(
                    onClick = onInspect,
                    border = BorderStroke(1.dp, AxionBorderStrong),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("movie_details_button_${video.id}")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Inspect metadata", tint = AxionTextPrimary, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Details", color = AxionTextPrimary)
                }

                OutlinedButton(
                    onClick = onDeleteClick,
                    enabled = !isDeleting,
                    border = BorderStroke(1.dp, AxionError.copy(alpha = 0.55f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AxionError),
                    shape = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("movie_delete_button_${video.id}")
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = AxionError
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Delete,
                            contentDescription = "Delete ${video.title}",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetaTagChip(
    text: String,
    highlight: Boolean = false
) {
    Surface(
        color = if (highlight) AxionRedContainer else AxionSurfaceHigh,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, if (highlight) AxionRed.copy(alpha = 0.45f) else AxionBorder)
    ) {
        Text(
            text = text,
            fontFamily = JetBrainsMonoFontFamily,
            fontSize = 10.sp,
            color = if (highlight) AxionTextPrimary else AxionTextSecondary,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            maxLines = 1
        )
    }
}
