package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Movie
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.VideoItem
import com.example.ui.components.AxionBrandHeader
import com.example.ui.components.LiveConnectionPill
import com.example.ui.components.MovieDetailsBottomSheet
import com.example.ui.components.VideoPreviewDialog
import com.example.ui.screens.AddMovieScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.MoviesScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.VideosScreen
import com.example.ui.theme.AxionBlack
import com.example.ui.theme.AxionBorder
import com.example.ui.theme.AxionError
import com.example.ui.theme.AxionObsidian
import com.example.ui.theme.AxionRed
import com.example.ui.theme.AxionRedContainer
import com.example.ui.theme.AxionSurface
import com.example.ui.theme.AxionSurfaceElevated
import com.example.ui.theme.AxionTextMuted
import com.example.ui.theme.AxionTextPrimary
import com.example.ui.theme.AxionTextSecondary
import com.example.viewmodel.AdminTab
import com.example.viewmodel.AxionAdminViewModel

private data class NavDestination(
    val tab: AdminTab,
    val label: String,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val testTag: String
)

private val AdminDestinations = listOf(
    NavDestination(AdminTab.DASHBOARD, "Dashboard", Icons.Filled.Dashboard, Icons.Outlined.Dashboard, "nav_dashboard"),
    NavDestination(AdminTab.MOVIES, "Movies", Icons.Filled.Movie, Icons.Outlined.Movie, "nav_movies"),
    NavDestination(AdminTab.ADD_MOVIE, "Add", Icons.Filled.AddCircle, Icons.Outlined.AddCircleOutline, "nav_add_movie"),
    NavDestination(AdminTab.VIDEOS, "Videos", Icons.Filled.VideoLibrary, Icons.Outlined.VideoLibrary, "nav_videos"),
    NavDestination(AdminTab.SETTINGS, "Settings", Icons.Filled.Settings, Icons.Outlined.Settings, "nav_settings")
)

@Composable
fun AxionAdminApp(
    viewModel: AxionAdminViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val clipboard = LocalClipboardManager.current
    var pendingSheetDelete by remember { mutableStateOf<VideoItem?>(null) }

    // Navigate back to Dashboard from secondary tabs on Android system Back press
    BackHandler(enabled = uiState.selectedTab != AdminTab.DASHBOARD) {
        viewModel.selectTab(AdminTab.DASHBOARD)
    }

    LaunchedEffect(uiState.snackbarNotice?.id) {
        val notice = uiState.snackbarNotice ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(notice.message)
        viewModel.dismissSnackbar()
    }

    // Native Video Preview Modal
    uiState.previewVideo?.let { video ->
        val streamUrl = viewModel.resolveStreamUrl(video)
        VideoPreviewDialog(
            video = video,
            streamUrl = streamUrl,
            probeStatus = uiState.streamProbes[video.id],
            onDismiss = viewModel::closeVideoPreview,
            onCopyUrl = { url ->
                clipboard.setText(AnnotatedString(url))
                viewModel.notifyMessage("Streaming URL copied to clipboard")
            }
        )
    }

    // Movie Metadata Inspector Bottom Sheet
    uiState.inspectVideo?.let { video ->
        val streamUrl = viewModel.resolveStreamUrl(video)
        MovieDetailsBottomSheet(
            video = video,
            streamUrl = streamUrl,
            probeStatus = uiState.streamProbes[video.id],
            isDeleting = video.id in uiState.deletingVideoIds,
            onDismiss = viewModel::closeMovieInspector,
            onPlayPreview = {
                viewModel.closeMovieInspector()
                viewModel.openVideoPreview(video)
            },
            onDeleteRequest = {
                pendingSheetDelete = video
            }
        )
    }

    // Confirmation dialog when deleting from the details sheet
    pendingSheetDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { pendingSheetDelete = null },
            containerColor = AxionSurfaceElevated,
            titleContentColor = AxionTextPrimary,
            textContentColor = AxionTextSecondary,
            title = { Text("Delete Movie?") },
            text = {
                Text("Remove \"${video.title}\" (ID ${video.id}) via DELETE /api/admin/videos/${video.id}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteMovie(video)
                        pendingSheetDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = AxionRed)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingSheetDelete = null }) {
                    Text("Cancel", color = AxionTextSecondary)
                }
            }
        )
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(AxionBlack)
    ) {
        val isWideScreen = maxWidth >= 700.dp

        if (isWideScreen) {
            // Desktop / Tablet Sidebar Layout
            Scaffold(
                containerColor = AxionBlack,
                contentWindowInsets = WindowInsets.safeDrawing,
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        AxionSnackbar(
                            message = data.visuals.message,
                            isError = uiState.snackbarNotice?.isError == true
                        )
                    }
                }
            ) { innerPadding ->
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    // Persistent Dark Sidebar
                    Column(
                        modifier = Modifier
                            .width(248.dp)
                            .fillMaxHeight()
                            .background(AxionObsidian)
                            .padding(horizontal = 14.dp, vertical = 20.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AxionBrandHeader(
                            compact = true,
                            modifier = Modifier.padding(horizontal = 6.dp)
                        )
                        Spacer(Modifier.height(6.dp))
                        LiveConnectionPill(
                            healthStatus = uiState.healthStatus,
                            isChecking = uiState.isCheckingHealth,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        )
                        Spacer(Modifier.height(10.dp))
                        HorizontalDivider(color = AxionBorder)
                        Spacer(Modifier.height(8.dp))

                        AdminDestinations.forEach { dest ->
                            val selected = uiState.selectedTab == dest.tab
                            NavigationDrawerItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(dest.tab) },
                                icon = {
                                    Icon(
                                        imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                        contentDescription = dest.label
                                    )
                                },
                                label = { Text(dest.label) },
                                shape = RoundedCornerShape(12.dp),
                                colors = NavigationDrawerItemDefaults.colors(
                                    selectedContainerColor = AxionRedContainer,
                                    selectedIconColor = AxionRed,
                                    selectedTextColor = AxionTextPrimary,
                                    unselectedContainerColor = Color.Transparent,
                                    unselectedIconColor = AxionTextSecondary,
                                    unselectedTextColor = AxionTextSecondary
                                ),
                                modifier = Modifier.testTag(dest.testTag)
                            )
                        }
                    }

                    VerticalDivider(color = AxionBorder)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        AdminScreenContent(
                            viewModel = viewModel,
                            uiState = uiState,
                            modifier = Modifier.widthIn(max = 1040.dp)
                        )
                    }
                }
            }
        } else {
            // Mobile-First Bottom Navigation Layout
            Scaffold(
                containerColor = AxionBlack,
                contentWindowInsets = WindowInsets.statusBars,
                snackbarHost = {
                    SnackbarHost(snackbarHostState) { data ->
                        AxionSnackbar(
                            message = data.visuals.message,
                            isError = uiState.snackbarNotice?.isError == true
                        )
                    }
                },
                bottomBar = {
                    Column(modifier = Modifier.background(AxionObsidian)) {
                        HorizontalDivider(color = AxionBorder)
                        NavigationBar(
                            containerColor = AxionObsidian,
                            contentColor = AxionTextPrimary,
                            tonalElevation = 0.dp,
                            windowInsets = WindowInsets.navigationBars
                        ) {
                            AdminDestinations.forEach { dest ->
                                val selected = uiState.selectedTab == dest.tab
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(dest.tab) },
                                    icon = {
                                        Icon(
                                            imageVector = if (selected) dest.selectedIcon else dest.unselectedIcon,
                                            contentDescription = dest.label
                                        )
                                    },
                                    label = {
                                        Text(
                                            text = dest.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1
                                        )
                                    },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = AxionRed,
                                        selectedTextColor = AxionTextPrimary,
                                        indicatorColor = AxionRedContainer,
                                        unselectedIconColor = AxionTextMuted,
                                        unselectedTextColor = AxionTextMuted
                                    ),
                                    modifier = Modifier.testTag(dest.testTag)
                                )
                            }
                        }
                    }
                }
            ) { innerPadding ->
                AdminScreenContent(
                    viewModel = viewModel,
                    uiState = uiState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                )
            }
        }
    }
}

@Composable
private fun AdminScreenContent(
    viewModel: AxionAdminViewModel,
    uiState: com.example.viewmodel.AxionUiState,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = uiState.selectedTab,
        animationSpec = tween(durationMillis = 180),
        modifier = modifier,
        label = "admin_tab_transition"
    ) { tab ->
        when (tab) {
            AdminTab.DASHBOARD -> DashboardScreen(
                uiState = uiState,
                onRefresh = viewModel::refreshAll,
                onNavigateTab = viewModel::selectTab,
                onInspectVideo = viewModel::openMovieInspector,
                onPlayPreview = viewModel::openVideoPreview
            )

            AdminTab.MOVIES -> MoviesScreen(
                uiState = uiState,
                onSearchQueryChange = viewModel::updateSearchQuery,
                onCategorySelected = viewModel::selectCategory,
                onRefresh = viewModel::refreshAll,
                onNavigateTab = viewModel::selectTab,
                onInspectVideo = viewModel::openMovieInspector,
                onPlayPreview = viewModel::openVideoPreview,
                onConfirmDelete = viewModel::deleteMovie
            )

            AdminTab.ADD_MOVIE -> AddMovieScreen(
                uiState = uiState,
                onSubmitMovie = viewModel::registerMovie,
                onClearFeedback = viewModel::clearAddMovieFeedback,
                onNavigateTab = viewModel::selectTab
            )

            AdminTab.VIDEOS -> VideosScreen(
                uiState = uiState,
                resolveStreamUrl = viewModel::resolveStreamUrl,
                onVerifyAllStreams = viewModel::verifyAllStreams,
                onProbeSingleStream = viewModel::probeSingleStream,
                onPlayPreview = viewModel::openVideoPreview,
                onCopyStreamUrl = { viewModel.notifyMessage("Streaming URL copied to clipboard") },
                onRefreshCatalog = viewModel::refreshAll,
                onNavigateTab = viewModel::selectTab
            )

            AdminTab.SETTINGS -> SettingsScreen(
                uiState = uiState,
                onSaveSettings = viewModel::saveSettings,
                onTestConnection = viewModel::testConnection,
                onLogout = viewModel::logout
            )
        }
    }
}

@Composable
private fun AxionSnackbar(
    message: String,
    isError: Boolean
) {
    Surface(
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .fillMaxWidth(),
        color = AxionSurfaceElevated,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, if (isError) AxionError else AxionRed.copy(alpha = 0.6f))
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = AxionTextPrimary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )
    }
}
