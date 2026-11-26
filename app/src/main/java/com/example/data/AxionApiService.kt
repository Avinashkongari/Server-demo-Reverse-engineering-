package com.example.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit

/**
 * Centralized API service for communicating with the existing Termux Node.js + Express backend.
 *
 * Supported endpoints:
 * - GET /api/health
 * - GET /api/videos
 * - POST /api/admin/videos (requires X-Admin-Token header)
 * - DELETE /api/admin/videos/:id (requires X-Admin-Token header)
 *
 * Security invariants:
 * - X-Admin-Token is sent ONLY in the HTTP header of /api/admin/ requests.
 * - Admin tokens are NEVER embedded in URLs or logged.
 */
class AxionApiService {

    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(false)
        .build()

    private val probeClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .followRedirects(true)
        .retryOnConnectionFailure(false)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun checkHealth(baseUrl: String): ApiResult<HealthStatus> = withContext(Dispatchers.IO) {
        val validBase = UrlValidator.normalizeAndValidate(baseUrl).getOrElse {
            return@withContext ApiResult.Error(
                type = ApiErrorType.INVALID_CONFIG,
                message = it.message ?: "Invalid backend URL."
            )
        }

        val url = "$validBase/api/health"
        val request = Request.Builder()
            .url(url)
            .get()
            .header("Accept", "application/json")
            .build()

        val startNanos = System.nanoTime()
        try {
            client.newCall(request).execute().use { response ->
                val latencyMs = (System.nanoTime() - startNanos) / 1_000_000L
                val bodyString = response.body?.string().orEmpty()
                val parsedJson = parseJsonObjectOrNull(bodyString)

                val storageBytes = parsedJson?.let { json ->
                    readOptionalLong(
                        json,
                        "totalStorageBytes",
                        "storageBytes",
                        "storage_bytes",
                        "total_storage_bytes",
                        "diskUsageBytes",
                        "videosSizeBytes"
                    )
                }
                val serverMessage = parsedJson?.let { json ->
                    readOptionalString(json, "status", "message", "state")
                } ?: bodyString.take(120).ifBlank { if (response.isSuccessful) "OK" else "HTTP ${response.code}" }

                val version = parsedJson?.let { readOptionalString(it, "version", "nodeVersion") }

                if (response.isSuccessful) {
                    ApiResult.Success(
                        HealthStatus(
                            isOnline = true,
                            httpCode = response.code,
                            latencyMs = latencyMs,
                            serverMessage = serverMessage,
                            reportedStorageBytes = storageBytes,
                            serverVersion = version
                        )
                    )
                } else {
                    ApiResult.Success(
                        HealthStatus(
                            isOnline = false,
                            httpCode = response.code,
                            latencyMs = latencyMs,
                            serverMessage = "Server responded with HTTP ${response.code}: $serverMessage"
                        )
                    )
                }
            }
        } catch (e: IOException) {
            ApiResult.Error(
                type = ApiErrorType.NETWORK_OFFLINE,
                message = describeNetworkError(e, validBase)
            )
        }
    }

    suspend fun fetchVideos(baseUrl: String): ApiResult<List<VideoItem>> = withContext(Dispatchers.IO) {
        val validBase = UrlValidator.normalizeAndValidate(baseUrl).getOrElse {
            return@withContext ApiResult.Error(
                type = ApiErrorType.INVALID_CONFIG,
                message = it.message ?: "Invalid backend URL."
            )
        }

        val request = Request.Builder()
            .url("$validBase/api/videos")
            .get()
            .header("Accept", "application/json")
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext mapHttpError(response.code, body, "Failed to load catalog")
                }
                val items = parseVideoList(body, validBase)
                    ?: return@withContext ApiResult.Error(
                        type = ApiErrorType.PARSE_ERROR,
                        message = "Unexpected JSON format returned by GET /api/videos."
                    )
                ApiResult.Success(items)
            }
        } catch (e: IOException) {
            ApiResult.Error(
                type = ApiErrorType.NETWORK_OFFLINE,
                message = describeNetworkError(e, validBase)
            )
        }
    }

    suspend fun registerVideo(
        baseUrl: String,
        adminToken: String,
        title: String,
        filename: String
    ): ApiResult<VideoItem?> = withContext(Dispatchers.IO) {
        val validBase = UrlValidator.normalizeAndValidate(baseUrl).getOrElse {
            return@withContext ApiResult.Error(
                type = ApiErrorType.INVALID_CONFIG,
                message = it.message ?: "Invalid backend URL."
            )
        }
        if (adminToken.isBlank()) {
            return@withContext ApiResult.Error(
                type = ApiErrorType.UNAUTHORIZED,
                message = "Admin token is missing. Configure X-Admin-Token in Settings first."
            )
        }
        val cleanTitle = title.trim()
        if (cleanTitle.isEmpty()) {
            return@withContext ApiResult.Error(
                type = ApiErrorType.BAD_REQUEST,
                message = "Movie title is required."
            )
        }
        val cleanFilename = FilenameValidator.validate(filename).getOrElse {
            return@withContext ApiResult.Error(
                type = ApiErrorType.BAD_REQUEST,
                message = it.message ?: "Invalid filename."
            )
        }

        // Send strictly the fields supported by the existing Termux backend: title & filename.
        val payload = JSONObject().apply {
            put("title", cleanTitle)
            put("filename", cleanFilename)
        }

        val request = Request.Builder()
            .url("$validBase/api/admin/videos")
            .post(payload.toString().toRequestBody(jsonMediaType))
            .header("Accept", "application/json")
            .header("Content-Type", "application/json")
            .header(HEADER_ADMIN_TOKEN, adminToken.trim())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext mapHttpError(response.code, body, "Failed to register movie")
                }
                val parsedObj = parseJsonObjectOrNull(body)
                val videoObj = parsedObj?.optJSONObject("video")
                    ?: parsedObj?.optJSONObject("data")
                    ?: parsedObj
                val created = videoObj?.let { parseVideoObject(it, validBase) }
                ApiResult.Success(created)
            }
        } catch (e: IOException) {
            ApiResult.Error(
                type = ApiErrorType.NETWORK_OFFLINE,
                message = describeNetworkError(e, validBase)
            )
        }
    }

    suspend fun deleteVideo(
        baseUrl: String,
        adminToken: String,
        videoId: String
    ): ApiResult<Unit> = withContext(Dispatchers.IO) {
        val validBase = UrlValidator.normalizeAndValidate(baseUrl).getOrElse {
            return@withContext ApiResult.Error(
                type = ApiErrorType.INVALID_CONFIG,
                message = it.message ?: "Invalid backend URL."
            )
        }
        if (adminToken.isBlank()) {
            return@withContext ApiResult.Error(
                type = ApiErrorType.UNAUTHORIZED,
                message = "Admin token is missing. Configure X-Admin-Token in Settings first."
            )
        }
        val baseHttpUrl = validBase.toHttpUrlOrNull()
            ?: return@withContext ApiResult.Error(
                type = ApiErrorType.INVALID_CONFIG,
                message = "Invalid backend URL."
            )

        val deleteUrl = baseHttpUrl.newBuilder()
            .addPathSegment("api")
            .addPathSegment("admin")
            .addPathSegment("videos")
            .addPathSegment(videoId)
            .build()

        val request = Request.Builder()
            .url(deleteUrl)
            .delete()
            .header("Accept", "application/json")
            .header(HEADER_ADMIN_TOKEN, adminToken.trim())
            .build()

        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext mapHttpError(response.code, body, "Failed to delete movie")
                }
                ApiResult.Success(Unit)
            }
        } catch (e: IOException) {
            ApiResult.Error(
                type = ApiErrorType.NETWORK_OFFLINE,
                message = describeNetworkError(e, validBase)
            )
        }
    }

    /**
     * Resolves the display streaming URL for a video according to API metadata and StreamPathMode.
     * Never appends any admin token to the URL.
     */
    fun resolvePreferredStreamUrl(
        baseUrl: String,
        video: VideoItem,
        mode: StreamPathMode
    ): String {
        val validBase = UrlValidator.normalizeAndValidate(baseUrl).getOrNull() ?: baseUrl.trimEnd('/')
        val candidates = buildStreamCandidates(validBase, video, mode)
        return candidates.firstOrNull() ?: "$validBase/videos/${video.filename}"
    }

    /**
     * Probes the Termux backend to verify whether the video file is actually reachable for streaming
     * and inspects real Content-Length / Content-Range bytes when available.
     */
    suspend fun probeVideoStream(
        baseUrl: String,
        video: VideoItem,
        mode: StreamPathMode
    ): StreamProbeStatus = withContext(Dispatchers.IO) {
        val validBase = UrlValidator.normalizeAndValidate(baseUrl).getOrElse {
            return@withContext StreamProbeStatus.Error(
                attemptedUrl = baseUrl,
                message = it.message ?: "Invalid backend URL"
            )
        }

        val candidates = buildStreamCandidates(validBase, video, mode)
        var lastMissing: StreamProbeStatus.Missing? = null
        var lastError: StreamProbeStatus.Error? = null

        for (candidateUrl in candidates) {
            val probe = probeSingleUrl(candidateUrl)
            when (probe) {
                is StreamProbeStatus.Available -> return@withContext probe
                is StreamProbeStatus.Missing -> {
                    if (lastMissing == null) lastMissing = probe
                }
                is StreamProbeStatus.Error -> {
                    lastError = probe
                }
                else -> Unit
            }
        }

        lastMissing ?: lastError ?: StreamProbeStatus.Error(
            attemptedUrl = candidates.firstOrNull().orEmpty(),
            message = "Unable to verify video stream"
        )
    }

    private fun probeSingleUrl(url: String): StreamProbeStatus {
        // Prefer lightweight Range GET bytes=0-0 because Express video stream endpoints often support GET Range
        // even when HEAD is unimplemented or disabled.
        val rangeRequest = Request.Builder()
            .url(url)
            .get()
            .header("Range", "bytes=0-0")
            .build()

        return try {
            probeClient.newCall(rangeRequest).execute().use { response ->
                val code = response.code
                if (code == 200 || code == 206) {
                    val contentRangeTotal = parseTotalFromContentRange(response.header("Content-Range"))
                    val contentLength = contentRangeTotal
                        ?: response.header("Content-Length")?.toLongOrNull()?.takeIf { it > 1L }
                    StreamProbeStatus.Available(
                        resolvedUrl = url,
                        contentLengthBytes = contentLength,
                        contentType = response.header("Content-Type")
                    )
                } else if (code == 404 || code == 410) {
                    StreamProbeStatus.Missing(attemptedUrl = url, httpCode = code)
                } else if (code == 416) {
                    // Range not satisfiable (e.g., 0-byte file), try HEAD to confirm existence.
                    probeWithHead(url)
                } else {
                    StreamProbeStatus.Error(attemptedUrl = url, message = "HTTP $code")
                }
            }
        } catch (e: IOException) {
            StreamProbeStatus.Error(
                attemptedUrl = url,
                message = e.message ?: "Network unreachable"
            )
        }
    }

    private fun probeWithHead(url: String): StreamProbeStatus {
        val headRequest = Request.Builder().url(url).head().build()
        return try {
            probeClient.newCall(headRequest).execute().use { response ->
                when (response.code) {
                    in 200..299 -> StreamProbeStatus.Available(
                        resolvedUrl = url,
                        contentLengthBytes = response.header("Content-Length")?.toLongOrNull(),
                        contentType = response.header("Content-Type")
                    )
                    404, 410 -> StreamProbeStatus.Missing(attemptedUrl = url, httpCode = response.code)
                    else -> StreamProbeStatus.Error(attemptedUrl = url, message = "HTTP ${response.code}")
                }
            }
        } catch (e: IOException) {
            StreamProbeStatus.Error(attemptedUrl = url, message = e.message ?: "Network unreachable")
        }
    }

    private fun buildStreamCandidates(
        validBase: String,
        video: VideoItem,
        mode: StreamPathMode
    ): List<String> {
        val baseHttp = validBase.toHttpUrlOrNull() ?: return emptyList()
        val list = linkedSetOf<String>()

        fun addApiResolvedRaw() {
            val raw = video.rawStreamUrl?.trim().orEmpty()
            if (raw.isNotEmpty()) {
                val resolved = resolveUrl(validBase, raw)
                if (resolved != null) list.add(resolved)
            }
        }

        val videosFolderUrl = baseHttp.newBuilder()
            .addPathSegment("videos")
            .addPathSegment(video.filename)
            .build()
            .toString()

        val apiStreamIdUrl = baseHttp.newBuilder()
            .addPathSegment("api")
            .addPathSegment("videos")
            .addPathSegment(video.id)
            .addPathSegment("stream")
            .build()
            .toString()

        val streamFilenameUrl = baseHttp.newBuilder()
            .addPathSegment("stream")
            .addPathSegment(video.filename)
            .build()
            .toString()

        when (mode) {
            StreamPathMode.AUTO -> {
                addApiResolvedRaw()
                if (video.filename.isNotBlank()) list.add(videosFolderUrl)
                if (video.id.isNotBlank()) list.add(apiStreamIdUrl)
                if (video.filename.isNotBlank()) list.add(streamFilenameUrl)
            }
            StreamPathMode.VIDEOS_FOLDER -> {
                if (video.filename.isNotBlank()) list.add(videosFolderUrl)
                addApiResolvedRaw()
            }
            StreamPathMode.API_STREAM_ID -> {
                if (video.id.isNotBlank()) list.add(apiStreamIdUrl)
                addApiResolvedRaw()
            }
            StreamPathMode.STREAM_FILENAME -> {
                if (video.filename.isNotBlank()) list.add(streamFilenameUrl)
                addApiResolvedRaw()
            }
        }
        return list.toList()
    }

    private fun parseTotalFromContentRange(header: String?): Long? {
        if (header.isNullOrBlank()) return null
        // Format: bytes 0-0/12345678
        val slashIndex = header.indexOf('/')
        if (slashIndex == -1 || slashIndex + 1 >= header.length) return null
        return header.substring(slashIndex + 1).trim().toLongOrNull()
    }

    private fun parseVideoList(body: String, baseUrl: String): List<VideoItem>? {
        val trimmed = body.trim()
        if (trimmed.isEmpty()) return emptyList()
        return try {
            val array: JSONArray = when {
                trimmed.startsWith("[") -> JSONArray(trimmed)
                trimmed.startsWith("{") -> {
                    val obj = JSONObject(trimmed)
                    obj.optJSONArray("videos")
                        ?: obj.optJSONArray("data")
                        ?: obj.optJSONArray("items")
                        ?: obj.optJSONArray("movies")
                        ?: return null
                }
                else -> return null
            }

            buildList(array.length()) {
                for (i in 0 until array.length()) {
                    val obj = array.optJSONObject(i) ?: continue
                    parseVideoObject(obj, baseUrl)?.let { add(it) }
                }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun parseVideoObject(obj: JSONObject, baseUrl: String): VideoItem? {
        val id = readOptionalString(obj, "id", "_id", "video_id", "videoId") ?: return null
        val title = readOptionalString(obj, "title", "name") ?: "Untitled"
        val filename = readOptionalString(obj, "filename", "file_name", "file", "videoFile") ?: ""
        val description = readOptionalString(obj, "description", "synopsis", "overview")
        val category = readOptionalString(obj, "category", "genre")
        val rawPoster = readOptionalString(obj, "posterUrl", "poster_url", "poster", "thumbnail", "thumbnailUrl")
        val resolvedPoster = rawPoster?.let { resolveUrl(baseUrl, it) }
        val releaseYear = readOptionalInt(obj, "releaseYear", "release_year", "year")
        val duration = readOptionalString(obj, "duration", "runtime")
        val fileSizeBytes = readOptionalLong(obj, "fileSizeBytes", "file_size_bytes", "fileSize", "file_size", "size")
        val rawStreamUrl = readOptionalString(obj, "streamUrl", "stream_url", "url", "videoUrl", "video_url")
        val createdAt = readOptionalString(obj, "createdAt", "created_at", "addedAt", "added_at")

        return VideoItem(
            id = id,
            title = title,
            filename = filename,
            description = description,
            category = category,
            posterUrl = resolvedPoster,
            releaseYear = releaseYear,
            duration = duration,
            fileSizeBytes = fileSizeBytes,
            rawStreamUrl = rawStreamUrl,
            createdAt = createdAt
        )
    }

    private fun resolveUrl(baseUrl: String, candidate: String): String? {
        val trimmed = candidate.trim()
        if (trimmed.isEmpty()) return null
        if (trimmed.startsWith("http://", ignoreCase = true) ||
            trimmed.startsWith("https://", ignoreCase = true)
        ) {
            return trimmed.takeIf { UrlValidator.isSafeHttpUrl(it) }
        }
        val baseHttp = baseUrl.toHttpUrlOrNull() ?: return null
        val resolved = baseHttp.resolve(trimmed)?.toString()
        return resolved?.takeIf { UrlValidator.isSafeHttpUrl(it) }
    }

    private fun mapHttpError(code: Int, rawBody: String, fallbackPrefix: String): ApiResult.Error {
        val serverMsg = extractErrorMessage(rawBody)
        val combinedLower = serverMsg.lowercase()

        return when (code) {
            401, 403 -> ApiResult.Error(
                type = ApiErrorType.UNAUTHORIZED,
                message = serverMsg.ifBlank { "Unauthorized (HTTP $code). Verify your X-Admin-Token in Settings." },
                httpCode = code
            )
            404 -> ApiResult.Error(
                type = ApiErrorType.NOT_FOUND,
                message = serverMsg.ifBlank { "Resource or video file not found on Termux server (HTTP 404)." },
                httpCode = code
            )
            409 -> ApiResult.Error(
                type = ApiErrorType.DUPLICATE_ENTRY,
                message = serverMsg.ifBlank { "Duplicate movie entry. This title or filename is already registered." },
                httpCode = code
            )
            400, 422 -> {
                val type = when {
                    combinedLower.contains("duplicate") || combinedLower.contains("already exists") ||
                        combinedLower.contains("unique") -> ApiErrorType.DUPLICATE_ENTRY
                    combinedLower.contains("not found") || combinedLower.contains("missing") ||
                        combinedLower.contains("does not exist") -> ApiErrorType.NOT_FOUND
                    else -> ApiErrorType.BAD_REQUEST
                }
                ApiResult.Error(
                    type = type,
                    message = serverMsg.ifBlank { "$fallbackPrefix (HTTP $code)." },
                    httpCode = code
                )
            }
            in 500..599 -> ApiResult.Error(
                type = ApiErrorType.SERVER_ERROR,
                message = serverMsg.ifBlank { "Termux server error (HTTP $code)." },
                httpCode = code
            )
            else -> ApiResult.Error(
                type = ApiErrorType.SERVER_ERROR,
                message = serverMsg.ifBlank { "$fallbackPrefix (HTTP $code)." },
                httpCode = code
            )
        }
    }

    private fun extractErrorMessage(rawBody: String): String {
        val json = parseJsonObjectOrNull(rawBody)
        if (json != null) {
            val msg = readOptionalString(json, "error", "message", "detail", "reason")
            if (!msg.isNullOrBlank()) return msg
        }
        return rawBody.trim().take(200)
    }

    private fun parseJsonObjectOrNull(raw: String): JSONObject? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{")) return null
        return try {
            JSONObject(trimmed)
        } catch (_: Exception) {
            null
        }
    }

    private fun readOptionalString(obj: JSONObject, vararg keys: String): String? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)?.toString()?.trim()
            if (!value.isNullOrEmpty() && value != "null") return value
        }
        return null
    }

    private fun readOptionalInt(obj: JSONObject, vararg keys: String): Int? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            when (value) {
                is Number -> return value.toInt()
                is String -> value.trim().toIntOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun readOptionalLong(obj: JSONObject, vararg keys: String): Long? {
        for (key in keys) {
            if (!obj.has(key) || obj.isNull(key)) continue
            val value = obj.opt(key)
            when (value) {
                is Number -> return value.toLong()
                is String -> value.trim().toLongOrNull()?.let { return it }
            }
        }
        return null
    }

    private fun describeNetworkError(e: IOException, baseUrl: String): String {
        return when (e) {
            is ConnectException -> "Connection refused at $baseUrl. Start your Termux Node.js server and check the port."
            is SocketTimeoutException -> "Connection timed out reaching $baseUrl. Verify Termux is awake and on the same network."
            is UnknownHostException -> "Host not found for $baseUrl. Check the IP address or hostname in Settings."
            else -> "Backend unreachable at $baseUrl (${e.message ?: "network error"})."
        }
    }

    companion object {
        private const val HEADER_ADMIN_TOKEN = "X-Admin-Token"
    }
}
