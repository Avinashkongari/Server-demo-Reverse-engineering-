package com.example.ui.components

import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.SubcomposeAsyncImage
import com.example.R
import com.example.data.FormatUtils
import com.example.data.HealthStatus
import com.example.data.StreamProbeStatus
import com.example.data.VideoItem
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
import com.example.ui.theme.AxionSurfaceHigh
import com.example.ui.theme.AxionTextMuted
import com.example.ui.theme.AxionTextPrimary
import com.example.ui.theme.AxionTextSecondary
import com.example.ui.theme.JetBrainsMonoFontFamily
import com.example.ui.theme.OutfitFontFamily

@Composable
fun AxionBrandHeader(
    modifier: Modifier = Modifier,
    compact: Boolean = false
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.img_axion_icon_clean),
            contentDescription = "Axion Stream logo",
            modifier = Modifier
                .size(if (compact) 32.dp else 38.dp)
                .clip(RoundedCornerShape(8.dp))
                .border(1.dp, AxionBorder, RoundedCornerShape(8.dp)),
            contentScale = ContentScale.Crop
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "AXION",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 17.sp else 20.sp,
                    letterSpacing = 1.6.sp,
                    color = AxionRed
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = "STREAM",
                    fontFamily = OutfitFontFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = if (compact) 17.sp else 20.sp,
                    letterSpacing = 1.6.sp,
                    color = AxionTextPrimary
                )
            }
            Text(
                text = "ADMIN CONSOLE",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 10.sp,
                letterSpacing = 1.4.sp,
                color = AxionTextMuted
            )
        }
    }
}

@Composable
fun LiveConnectionPill(
    healthStatus: HealthStatus?,
    isChecking: Boolean,
    modifier: Modifier = Modifier
) {
    val isOnline = healthStatus?.isOnline == true
    val bgColor by animateColorAsState(
        targetValue = when {
            isChecking -> AxionAmberContainer
            isOnline -> AxionEmeraldContainer
            else -> AxionErrorContainer
        },
        label = "status_bg"
    )
    val dotColor by animateColorAsState(
        targetValue = when {
            isChecking -> AxionAmber
            isOnline -> AxionEmerald
            else -> AxionError
        },
        label = "status_dot"
    )

    val pulseTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by pulseTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Surface(
        modifier = modifier.testTag("connection_status_pill"),
        color = bgColor,
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, dotColor.copy(alpha = 0.45f))
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(10.dp),
                    strokeWidth = 1.5.dp,
                    color = AxionAmber
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor.copy(alpha = if (isOnline) pulseAlpha else 1f))
                )
            }
            val label = when {
                isChecking -> "CHECKING"
                isOnline -> healthStatus?.latencyMs?.let { "ONLINE · ${it}ms" } ?: "ONLINE"
                else -> "OFFLINE"
            }
            Text(
                text = label,
                fontFamily = JetBrainsMonoFontFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 11.sp,
                color = dotColor
            )
        }
    }
}

@Composable
fun AxionMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    testTag: String? = null
) {
    Surface(
        modifier = modifier.then(if (testTag != null) Modifier.testTag(testTag) else Modifier),
        color = AxionSurface,
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, AxionBorder)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(accentColor, accentColor.copy(alpha = 0.15f))
                        )
                    )
            )
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title.uppercase(),
                        style = MaterialTheme.typography.labelMedium,
                        color = AxionTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        color = accentColor.copy(alpha = 0.14f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier
                                .padding(6.dp)
                                .size(18.dp)
                        )
                    }
                }

                Text(
                    text = value,
                    style = MaterialTheme.typography.displayMedium,
                    color = AxionTextPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AxionTextMuted,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MoviePosterThumbnail(
    posterUrl: String?,
    title: String,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(AxionSurfaceHigh)
            .border(1.dp, AxionBorder, RoundedCornerShape(10.dp)),
        contentAlignment = Alignment.Center
    ) {
        if (!posterUrl.isNullOrBlank()) {
            SubcomposeAsyncImage(
                model = posterUrl,
                contentDescription = "Poster for $title",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                loading = {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = AxionRed
                        )
                    }
                },
                error = {
                    PosterFallbackArt(title = title)
                }
            )
        } else {
            PosterFallbackArt(title = title)
        }
    }
}

@Composable
private fun PosterFallbackArt(title: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF1F1014), AxionSurfaceElevated, AxionBlack)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
            modifier = Modifier.padding(6.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Movie,
                contentDescription = "No poster for $title",
                tint = AxionRed.copy(alpha = 0.7f),
                modifier = Modifier.size(24.dp)
            )
            Text(
                text = "NO POSTER",
                fontFamily = JetBrainsMonoFontFamily,
                fontSize = 8.sp,
                color = AxionTextMuted,
                maxLines = 1
            )
        }
    }
}

@Composable
fun StreamAvailabilityBadge(
    status: StreamProbeStatus?,
    modifier: Modifier = Modifier
) {
    val (label, fg, bg) = when (status) {
        is StreamProbeStatus.Available -> {
            val sizeTag = status.contentLengthBytes?.let { " · ${FormatUtils.formatBytes(it)}" } ?: ""
            Triple("AVAILABLE$sizeTag", AxionEmerald, AxionEmeraldContainer)
        }
        is StreamProbeStatus.Missing -> Triple("MISSING (${status.httpCode})", AxionError, AxionErrorContainer)
        is StreamProbeStatus.Checking -> Triple("CHECKING...", AxionAmber, AxionAmberContainer)
        is StreamProbeStatus.Error -> Triple("UNREACHABLE", AxionAmber, AxionAmberContainer)
        StreamProbeStatus.Unchecked, null -> Triple("NOT CHECKED", AxionTextSecondary, AxionSurfaceHigh)
    }

    Surface(
        modifier = modifier,
        color = bg,
        shape = RoundedCornerShape(6.dp),
        border = BorderStroke(1.dp, fg.copy(alpha = 0.35f))
    ) {
        Text(
            text = label,
            fontFamily = JetBrainsMonoFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 10.sp,
            color = fg,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun BackendErrorBanner(
    title: String,
    message: String,
    onRetry: (() -> Unit)? = null,
    onOpenSettings: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = AxionErrorContainer,
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, AxionError.copy(alpha = 0.45f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.ErrorOutline,
                    contentDescription = "Backend error",
                    tint = AxionError,
                    modifier = Modifier.size(22.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = AxionTextPrimary
                )
            }
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = AxionTextSecondary
            )
            if (onRetry != null || onOpenSettings != null) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (onRetry != null) {
                        Button(
                            onClick = onRetry,
                            colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("banner_retry_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                    if (onOpenSettings != null) {
                        OutlinedButton(
                            onClick = onOpenSettings,
                            border = BorderStroke(1.dp, AxionBorderStrong),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.testTag("banner_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = null,
                                tint = AxionTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Settings", color = AxionTextPrimary)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun VideoPreviewDialog(
    video: VideoItem,
    streamUrl: String,
    probeStatus: StreamProbeStatus?,
    onDismiss: () -> Unit,
    onCopyUrl: (String) -> Unit
) {
    var isBuffering by remember(streamUrl) { mutableStateOf(true) }
    var playbackError by remember(streamUrl) { mutableStateOf<String?>(null) }
    var reloadNonce by remember { mutableStateOf(0) }
    var videoViewRef by remember { mutableStateOf<VideoView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            runCatching { videoViewRef?.stopPlayback() }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            color = AxionSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, AxionBorderStrong)
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Header
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = video.title,
                            style = MaterialTheme.typography.titleMedium,
                            color = AxionTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "ID ${video.id} · ${video.filename}",
                            fontFamily = JetBrainsMonoFontFamily,
                            fontSize = 11.sp,
                            color = AxionTextMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.testTag("close_preview_dialog_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "Close preview",
                            tint = AxionTextPrimary
                        )
                    }
                }

                // Native Android VideoView streaming player
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .background(AxionBlack),
                    contentAlignment = Alignment.Center
                ) {
                    if (playbackError == null) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { ctx ->
                                VideoView(ctx).apply {
                                    val controller = MediaController(ctx)
                                    controller.setAnchorView(this)
                                    setMediaController(controller)
                                    setOnPreparedListener { mp ->
                                        isBuffering = false
                                        mp.isLooping = false
                                        start()
                                    }
                                    setOnErrorListener { _, what, extra ->
                                        isBuffering = false
                                        playbackError =
                                            "Stream playback failed (code $what/$extra). Verify the file exists in Termux videos folder and the streaming route returns valid video bytes."
                                        true
                                    }
                                    videoViewRef = this
                                    setVideoURI(Uri.parse(streamUrl))
                                }
                            },
                            update = { view ->
                                // Reload when user taps Retry
                                if (view.tag != reloadNonce) {
                                    view.tag = reloadNonce
                                    isBuffering = true
                                    view.setVideoURI(Uri.parse(streamUrl))
                                }
                            }
                        )
                    }

                    if (isBuffering && playbackError == null) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            CircularProgressIndicator(color = AxionRed, strokeWidth = 2.5.dp)
                            Text(
                                text = "Connecting to Termux stream...",
                                style = MaterialTheme.typography.bodySmall,
                                color = AxionTextSecondary
                            )
                        }
                    }

                    playbackError?.let { err ->
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = "Playback error",
                                tint = AxionError,
                                modifier = Modifier.size(32.dp)
                            )
                            Text(
                                text = err,
                                style = MaterialTheme.typography.bodySmall,
                                color = AxionTextSecondary
                            )
                            Button(
                                onClick = {
                                    playbackError = null
                                    isBuffering = true
                                    reloadNonce += 1
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = AxionRed)
                            ) {
                                Icon(Icons.Filled.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("Retry Stream")
                            }
                        }
                    }
                }

                // Stream URL & probe details footer
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "STREAMING URL",
                            style = MaterialTheme.typography.labelMedium,
                            color = AxionTextMuted
                        )
                        StreamAvailabilityBadge(status = probeStatus)
                    }
                    Surface(
                        color = AxionBlack,
                        shape = RoundedCornerShape(10.dp),
                        border = BorderStroke(1.dp, AxionBorder)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = streamUrl,
                                fontFamily = JetBrainsMonoFontFamily,
                                fontSize = 11.sp,
                                color = AxionTextSecondary,
                                modifier = Modifier.weight(1f),
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            IconButton(
                                onClick = { onCopyUrl(streamUrl) },
                                modifier = Modifier.testTag("copy_preview_stream_url")
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
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MovieDetailsBottomSheet(
    video: VideoItem,
    streamUrl: String,
    probeStatus: StreamProbeStatus?,
    isDeleting: Boolean,
    onDismiss: () -> Unit,
    onPlayPreview: () -> Unit,
    onDeleteRequest: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val clipboard = LocalClipboardManager.current

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = AxionSurface,
        contentColor = AxionTextPrimary
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalAlignment = Alignment.Top
            ) {
                MoviePosterThumbnail(
                    posterUrl = video.posterUrl,
                    title = video.title,
                    modifier = Modifier
                        .width(84.dp)
                        .height(120.dp)
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = video.title,
                        style = MaterialTheme.typography.headlineSmall,
                        color = AxionTextPrimary
                    )
                    Text(
                        text = "ID: ${video.id}",
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = AxionTextMuted
                    )
                    Text(
                        text = video.filename.ifBlank { "No filename" },
                        fontFamily = JetBrainsMonoFontFamily,
                        fontSize = 12.sp,
                        color = AxionTextSecondary
                    )
                    StreamAvailabilityBadge(status = probeStatus)
                }
            }

            HorizontalDivider(color = AxionBorder)

            // Real metadata rows returned by backend
            MetadataRow(label = "Description", value = video.description ?: "Not provided by backend")
            MetadataRow(label = "Category", value = video.category ?: "Not provided by backend")
            MetadataRow(label = "Release Year", value = video.releaseYear?.toString() ?: "Not provided by backend")
            MetadataRow(label = "Duration", value = video.duration ?: "Not provided by backend")
            MetadataRow(
                label = "File Size",
                value = video.fileSizeBytes?.let { FormatUtils.formatBytes(it) }
                    ?: (probeStatus as? StreamProbeStatus.Available)?.contentLengthBytes?.let {
                        "${FormatUtils.formatBytes(it)} (from stream header)"
                    }
                    ?: "Not provided by backend"
            )
            MetadataRow(label = "Streaming URL", value = streamUrl, isMono = true)

            // Transparent notice regarding metadata editing (no fake edit button!)
            Surface(
                color = AxionAmberContainer,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, AxionAmber.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = Icons.Filled.Lock,
                        contentDescription = "Backend endpoint required",
                        tint = AxionAmber,
                        modifier = Modifier.size(18.dp)
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = "Metadata Editing Requires Backend Update",
                            style = MaterialTheme.typography.titleSmall,
                            color = AxionAmber
                        )
                        Text(
                            text = "Your current Termux API supports GET /api/videos, POST /api/admin/videos, and DELETE /api/admin/videos/:id. Add PATCH /api/admin/videos/:id with SQLite metadata columns to enable editing.",
                            style = MaterialTheme.typography.bodySmall,
                            color = AxionTextSecondary
                        )
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onPlayPreview,
                    colors = ButtonDefaults.buttonColors(containerColor = AxionRed),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("sheet_play_preview_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Play Preview")
                }
                OutlinedButton(
                    onClick = { clipboard.setText(AnnotatedString(streamUrl)) },
                    border = BorderStroke(1.dp, AxionBorderStrong),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("sheet_copy_url_button")
                ) {
                    Icon(Icons.Filled.ContentCopy, contentDescription = "Copy URL", tint = AxionTextPrimary, modifier = Modifier.size(18.dp))
                }
                OutlinedButton(
                    onClick = onDeleteRequest,
                    enabled = !isDeleting,
                    border = BorderStroke(1.dp, AxionError.copy(alpha = 0.6f)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = AxionError),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("sheet_delete_movie_button")
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = AxionError)
                    } else {
                        Icon(Icons.Filled.Delete, contentDescription = "Delete movie", modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun MetadataRow(
    label: String,
    value: String,
    isMono: Boolean = false
) {
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelMedium,
            color = AxionTextMuted
        )
        Text(
            text = value,
            style = if (isMono) {
                MaterialTheme.typography.bodySmall.copy(fontFamily = JetBrainsMonoFontFamily)
            } else {
                MaterialTheme.typography.bodyMedium
            },
            color = if (value == "Not provided by backend") AxionTextMuted else AxionTextPrimary
        )
    }
}

@Composable
fun SectionHeaderRow(
    title: String,
    subtitle: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = AxionTextPrimary
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = AxionTextMuted
                )
            }
        }
        trailing?.invoke()
    }
}

@Composable
fun InfoPillNotice(
    text: String,
    accent: Color = AxionRed,
    bg: Color = AxionRedContainer,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = bg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Outlined.Info,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = text,
                style = MaterialTheme.typography.bodySmall,
                color = AxionTextSecondary
            )
        }
    }
}
