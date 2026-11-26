package com.example.data

import kotlin.math.ln
import kotlin.math.pow

data class VideoItem(
    val id: String,
    val title: String,
    val filename: String,
    val description: String? = null,
    val category: String? = null,
    val posterUrl: String? = null,
    val releaseYear: Int? = null,
    val duration: String? = null,
    val fileSizeBytes: Long? = null,
    val rawStreamUrl: String? = null,
    val createdAt: String? = null
)

data class HealthStatus(
    val isOnline: Boolean,
    val httpCode: Int?,
    val latencyMs: Long?,
    val serverMessage: String?,
    val reportedStorageBytes: Long? = null,
    val serverVersion: String? = null,
    val checkedAtMillis: Long = System.currentTimeMillis()
)

enum class StreamPathMode(val label: String, val patternHint: String) {
    AUTO("Auto-detect", "Checks API stream URL, /videos/{filename}, /api/videos/{id}/stream, /stream/{filename}"),
    VIDEOS_FOLDER("/videos/{filename}", "Serves directly from Termux static videos folder"),
    API_STREAM_ID("/api/videos/{id}/stream", "Streams via Express video ID route"),
    STREAM_FILENAME("/stream/{filename}", "Streams via Express filename stream route")
}

sealed interface StreamProbeStatus {
    data object Unchecked : StreamProbeStatus
    data object Checking : StreamProbeStatus
    data class Available(
        val resolvedUrl: String,
        val contentLengthBytes: Long?,
        val contentType: String?
    ) : StreamProbeStatus

    data class Missing(
        val attemptedUrl: String,
        val httpCode: Int
    ) : StreamProbeStatus

    data class Error(
        val attemptedUrl: String,
        val message: String
    ) : StreamProbeStatus
}

enum class ApiErrorType {
    NETWORK_OFFLINE,
    UNAUTHORIZED,
    NOT_FOUND,
    DUPLICATE_ENTRY,
    BAD_REQUEST,
    SERVER_ERROR,
    INVALID_CONFIG,
    PARSE_ERROR
}

sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(
        val type: ApiErrorType,
        val message: String,
        val httpCode: Int? = null
    ) : ApiResult<Nothing>()
}

data class ConnectionDiagnostics(
    val testedUrl: String,
    val healthResult: HealthStatus,
    val catalogReachable: Boolean,
    val catalogVideoCount: Int?,
    val catalogErrorMessage: String?,
    val hasAdminTokenConfigured: Boolean,
    val testedAtMillis: Long = System.currentTimeMillis()
)

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes < 0L) return "Unknown"
        if (bytes < 1024L) return "$bytes B"
        val unit = 1024.0
        val exp = (ln(bytes.toDouble()) / ln(unit)).toInt().coerceAtMost(4)
        val prefix = "KMGT"[exp - 1]
        val value = bytes / unit.pow(exp.toDouble())
        return String.format(java.util.Locale.US, "%.2f %sB", value, prefix)
    }
}
