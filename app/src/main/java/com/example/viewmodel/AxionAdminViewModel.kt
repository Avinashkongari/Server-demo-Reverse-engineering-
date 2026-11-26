package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ApiErrorType
import com.example.data.ApiResult
import com.example.data.AxionApiService
import com.example.data.ConnectionDiagnostics
import com.example.data.FilenameValidator
import com.example.data.HealthStatus
import com.example.data.SecureCredentialStore
import com.example.data.StreamPathMode
import com.example.data.StreamProbeStatus
import com.example.data.UrlValidator
import com.example.data.VideoItem
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

enum class AdminTab {
    DASHBOARD,
    MOVIES,
    ADD_MOVIE,
    VIDEOS,
    SETTINGS
}

data class SnackbarNotice(
    val id: Long = System.currentTimeMillis(),
    val message: String,
    val isError: Boolean = false
)

data class AddMovieFormFeedback(
    val isError: Boolean,
    val errorType: ApiErrorType? = null,
    val message: String
)

data class AxionUiState(
    val selectedTab: AdminTab = AdminTab.DASHBOARD,
    val backendUrl: String = UrlValidator.DEFAULT_BACKEND_URL,
    val streamPathMode: StreamPathMode = StreamPathMode.AUTO,
    val hasAdminToken: Boolean = false,
    val healthStatus: HealthStatus? = null,
    val isCheckingHealth: Boolean = false,
    val videos: List<VideoItem> = emptyList(),
    val isLoadingVideos: Boolean = false,
    val catalogError: ApiResult.Error? = null,
    val streamProbes: Map<String, StreamProbeStatus> = emptyMap(),
    val isProbingStreams: Boolean = false,
    val searchQuery: String = "",
    val selectedCategory: String? = null,
    val isSubmittingMovie: Boolean = false,
    val addMovieFeedback: AddMovieFormFeedback? = null,
    val deletingVideoIds: Set<String> = emptySet(),
    val isTestingConnection: Boolean = false,
    val connectionDiagnostics: ConnectionDiagnostics? = null,
    val previewVideo: VideoItem? = null,
    val inspectVideo: VideoItem? = null,
    val snackbarNotice: SnackbarNotice? = null
) {
    val totalMovies: Int get() = videos.size

    val registeredMoviesCount: Int
        get() = videos.count { it.id.isNotBlank() && it.filename.isNotBlank() }

    /**
     * Computes real video storage usage from backend data only.
     * Priority:
     * 1. Storage total reported directly by /api/health
     * 2. Sum of per-video fileSizeBytes returned by /api/videos
     * 3. Sum of real Content-Length / Content-Range bytes probed from live streaming endpoints
     * Returns null when unavailable (never fabricates fake storage metrics).
     */
    val computedStorageBytes: Long?
        get() {
            healthStatus?.reportedStorageBytes?.let { if (it >= 0L) return it }
            val metaSizes = videos.mapNotNull { it.fileSizeBytes?.takeIf { size -> size >= 0L } }
            if (metaSizes.isNotEmpty()) return metaSizes.sum()
            val probedSizes = streamProbes.values
                .filterIsInstance<StreamProbeStatus.Available>()
                .mapNotNull { it.contentLengthBytes?.takeIf { size -> size >= 0L } }
            if (probedSizes.isNotEmpty()) return probedSizes.sum()
            return null
        }

    val storageSourceDescription: String
        get() {
            if (healthStatus?.reportedStorageBytes != null) return "Reported by /api/health"
            val metaCount = videos.count { it.fileSizeBytes != null }
            if (metaCount > 0) return "From catalog metadata ($metaCount/${videos.size})"
            val probedWithSize = streamProbes.values
                .filterIsInstance<StreamProbeStatus.Available>()
                .count { it.contentLengthBytes != null }
            if (probedWithSize > 0) return "Stream headers ($probedWithSize/${videos.size} verified)"
            if (healthStatus?.isOnline == false) return "Backend offline"
            if (videos.isEmpty()) return "0 registered videos"
            return "Requires storage endpoint or stream size headers"
        }

    val availableCategories: List<String>
        get() = videos.mapNotNull { it.category?.trim()?.takeIf { c -> c.isNotEmpty() } }
            .distinct()
            .sorted()

    val filteredVideos: List<VideoItem>
        get() {
            val q = searchQuery.trim().lowercase()
            return videos.filter { video ->
                val matchesCategory = when (selectedCategory) {
                    null -> true
                    UNCATEGORIZED_FILTER -> video.category.isNullOrBlank()
                    else -> video.category.equals(selectedCategory, ignoreCase = true)
                }
                val matchesQuery = q.isEmpty() ||
                    video.title.lowercase().contains(q) ||
                    video.filename.lowercase().contains(q) ||
                    video.id.lowercase().contains(q) ||
                    (video.description?.lowercase()?.contains(q) == true) ||
                    (video.category?.lowercase()?.contains(q) == true)
                matchesCategory && matchesQuery
            }
        }

    val recentVideos: List<VideoItem>
        get() {
            if (videos.isEmpty()) return emptyList()
            val hasTimestamps = videos.any { !it.createdAt.isNullOrBlank() }
            val sorted = if (hasTimestamps) {
                videos.sortedByDescending { it.createdAt.orEmpty() }
            } else {
                val allNumericIds = videos.all { it.id.toLongOrNull() != null }
                if (allNumericIds) {
                    videos.sortedByDescending { it.id.toLongOrNull() ?: 0L }
                } else {
                    videos.asReversed()
                }
            }
            return sorted.take(6)
        }

    companion object {
        const val UNCATEGORIZED_FILTER = "__UNCATEGORIZED__"
    }
}

class AxionAdminViewModel(application: Application) : AndroidViewModel(application) {
    private val credentialStore = SecureCredentialStore(application)
    private val apiService = AxionApiService()

    private val _uiState = MutableStateFlow(AxionUiState())
    val uiState: StateFlow<AxionUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val savedUrl = credentialStore.getBackendUrl()
            val savedMode = credentialStore.getStreamPathMode()
            val hasToken = credentialStore.hasAdminToken()
            _uiState.update {
                it.copy(
                    backendUrl = savedUrl,
                    streamPathMode = savedMode,
                    hasAdminToken = hasToken
                )
            }
            refreshAll()
        }
    }

    fun selectTab(tab: AdminTab) {
        _uiState.update {
            it.copy(
                selectedTab = tab,
                addMovieFeedback = if (tab != AdminTab.ADD_MOVIE) null else it.addMovieFeedback
            )
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: String?) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun openVideoPreview(video: VideoItem) {
        _uiState.update { it.copy(previewVideo = video) }
        val existingProbe = _uiState.value.streamProbes[video.id]
        if (existingProbe == null || existingProbe is StreamProbeStatus.Unchecked) {
            probeSingleStream(video)
        }
    }

    fun closeVideoPreview() {
        _uiState.update { it.copy(previewVideo = null) }
    }

    fun openMovieInspector(video: VideoItem) {
        _uiState.update { it.copy(inspectVideo = video) }
    }

    fun closeMovieInspector() {
        _uiState.update { it.copy(inspectVideo = null) }
    }

    fun dismissSnackbar() {
        _uiState.update { it.copy(snackbarNotice = null) }
    }

    fun clearAddMovieFeedback() {
        _uiState.update { it.copy(addMovieFeedback = null) }
    }

    fun resolveStreamUrl(video: VideoItem): String {
        val state = _uiState.value
        val probed = state.streamProbes[video.id]
        if (probed is StreamProbeStatus.Available) {
            return probed.resolvedUrl
        }
        return apiService.resolvePreferredStreamUrl(state.backendUrl, video, state.streamPathMode)
    }

    fun refreshAll() {
        viewModelScope.launch {
            val url = _uiState.value.backendUrl
            _uiState.update {
                it.copy(
                    isCheckingHealth = true,
                    isLoadingVideos = true,
                    catalogError = null
                )
            }

            coroutineScope {
                val healthDeferred = async { apiService.checkHealth(url) }
                val videosDeferred = async { apiService.fetchVideos(url) }

                val healthRes = healthDeferred.await()
                val videosRes = videosDeferred.await()

                val resolvedHealth = when (healthRes) {
                    is ApiResult.Success -> healthRes.data
                    is ApiResult.Error -> HealthStatus(
                        isOnline = false,
                        httpCode = healthRes.httpCode,
                        latencyMs = null,
                        serverMessage = healthRes.message
                    )
                }

                when (videosRes) {
                    is ApiResult.Success -> {
                        val fetchedVideos = videosRes.data
                        _uiState.update { state ->
                            val validIds = fetchedVideos.map { it.id }.toSet()
                            state.copy(
                                isCheckingHealth = false,
                                isLoadingVideos = false,
                                healthStatus = resolvedHealth,
                                videos = fetchedVideos,
                                catalogError = null,
                                streamProbes = state.streamProbes.filterKeys { it in validIds }
                            )
                        }
                        if (fetchedVideos.isNotEmpty()) {
                            verifyAllStreams()
                        }
                    }
                    is ApiResult.Error -> {
                        _uiState.update {
                            it.copy(
                                isCheckingHealth = false,
                                isLoadingVideos = false,
                                healthStatus = resolvedHealth,
                                catalogError = videosRes
                            )
                        }
                    }
                }
            }
        }
    }

    fun verifyAllStreams() {
        val currentVideos = _uiState.value.videos
        if (currentVideos.isEmpty()) return
        val baseUrl = _uiState.value.backendUrl
        val mode = _uiState.value.streamPathMode

        viewModelScope.launch {
            _uiState.update { state ->
                val checkingMap = state.streamProbes.toMutableMap()
                currentVideos.forEach { checkingMap[it.id] = StreamProbeStatus.Checking }
                state.copy(isProbingStreams = true, streamProbes = checkingMap)
            }

            val semaphore = Semaphore(permits = 3)
            coroutineScope {
                val results = currentVideos.map { video ->
                    async {
                        semaphore.withPermit {
                            video.id to apiService.probeVideoStream(baseUrl, video, mode)
                        }
                    }
                }.awaitAll()

                _uiState.update { state ->
                    val merged = state.streamProbes.toMutableMap()
                    results.forEach { (id, status) -> merged[id] = status }
                    state.copy(isProbingStreams = false, streamProbes = merged)
                }
            }
        }
    }

    fun probeSingleStream(video: VideoItem) {
        val baseUrl = _uiState.value.backendUrl
        val mode = _uiState.value.streamPathMode
        viewModelScope.launch {
            _uiState.update { state ->
                state.copy(streamProbes = state.streamProbes + (video.id to StreamProbeStatus.Checking))
            }
            val result = apiService.probeVideoStream(baseUrl, video, mode)
            _uiState.update { state ->
                state.copy(streamProbes = state.streamProbes + (video.id to result))
            }
        }
    }

    fun registerMovie(title: String, filename: String, onSuccessCleared: () -> Unit) {
        val trimmedTitle = title.trim()
        val filenameValidation = FilenameValidator.validate(filename)

        if (trimmedTitle.isEmpty()) {
            _uiState.update {
                it.copy(
                    addMovieFeedback = AddMovieFormFeedback(
                        isError = true,
                        errorType = ApiErrorType.BAD_REQUEST,
                        message = "Enter a movie title."
                    )
                )
            }
            return
        }

        val cleanFilename = filenameValidation.getOrElse { err ->
            _uiState.update {
                it.copy(
                    addMovieFeedback = AddMovieFormFeedback(
                        isError = true,
                        errorType = ApiErrorType.BAD_REQUEST,
                        message = err.message ?: "Enter a valid filename."
                    )
                )
            }
            return
        }

        val token = credentialStore.getAdminToken()
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    hasAdminToken = false,
                    addMovieFeedback = AddMovieFormFeedback(
                        isError = true,
                        errorType = ApiErrorType.UNAUTHORIZED,
                        message = "Admin token is not configured. Open Settings to enter your X-Admin-Token."
                    )
                )
            }
            return
        }

        // Guard against obvious duplicate registration already present in live catalog
        val duplicateInCatalog = _uiState.value.videos.firstOrNull {
            it.filename.equals(cleanFilename, ignoreCase = true)
        }
        if (duplicateInCatalog != null) {
            _uiState.update {
                it.copy(
                    addMovieFeedback = AddMovieFormFeedback(
                        isError = true,
                        errorType = ApiErrorType.DUPLICATE_ENTRY,
                        message = "Duplicate entry: '$cleanFilename' is already registered as \"${duplicateInCatalog.title}\" (ID ${duplicateInCatalog.id})."
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isSubmittingMovie = true, addMovieFeedback = null) }
            val result = apiService.registerVideo(
                baseUrl = _uiState.value.backendUrl,
                adminToken = token,
                title = trimmedTitle,
                filename = cleanFilename
            )

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingMovie = false,
                            addMovieFeedback = AddMovieFormFeedback(
                                isError = false,
                                message = "Registered \"$trimmedTitle\" ($cleanFilename) in Termux SQLite catalog."
                            ),
                            snackbarNotice = SnackbarNotice(
                                message = "Added \"$trimmedTitle\" to catalog"
                            )
                        )
                    }
                    onSuccessCleared()
                    refreshAll()
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isSubmittingMovie = false,
                            addMovieFeedback = AddMovieFormFeedback(
                                isError = true,
                                errorType = result.type,
                                message = result.message
                            )
                        )
                    }
                }
            }
        }
    }

    fun deleteMovie(video: VideoItem) {
        val token = credentialStore.getAdminToken()
        if (token.isNullOrBlank()) {
            _uiState.update {
                it.copy(
                    hasAdminToken = false,
                    snackbarNotice = SnackbarNotice(
                        message = "Configure X-Admin-Token in Settings before deleting movies.",
                        isError = true
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(deletingVideoIds = it.deletingVideoIds + video.id) }
            val result = apiService.deleteVideo(
                baseUrl = _uiState.value.backendUrl,
                adminToken = token,
                videoId = video.id
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingVideoIds = state.deletingVideoIds - video.id,
                            videos = state.videos.filterNot { it.id == video.id },
                            streamProbes = state.streamProbes - video.id,
                            inspectVideo = if (state.inspectVideo?.id == video.id) null else state.inspectVideo,
                            previewVideo = if (state.previewVideo?.id == video.id) null else state.previewVideo,
                            snackbarNotice = SnackbarNotice(message = "Deleted \"${video.title}\" from catalog")
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { state ->
                        state.copy(
                            deletingVideoIds = state.deletingVideoIds - video.id,
                            snackbarNotice = SnackbarNotice(
                                message = result.message,
                                isError = true
                            )
                        )
                    }
                }
            }
        }
    }

    fun saveSettings(
        rawUrl: String,
        newTokenInput: String,
        streamMode: StreamPathMode,
        onSaved: () -> Unit
    ) {
        val validatedUrl = UrlValidator.normalizeAndValidate(rawUrl).getOrElse { err ->
            _uiState.update {
                it.copy(
                    snackbarNotice = SnackbarNotice(
                        message = err.message ?: "Invalid backend URL",
                        isError = true
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            credentialStore.saveBackendUrl(validatedUrl)
            credentialStore.saveStreamPathMode(streamMode)

            var encryptionNote = ""
            if (newTokenInput.isNotBlank()) {
                val persistedEncrypted = credentialStore.saveAdminToken(newTokenInput)
                encryptionNote = if (persistedEncrypted) {
                    " Token encrypted via Android Keystore."
                } else {
                    " Token held in memory for this session."
                }
            }

            val hasToken = credentialStore.hasAdminToken()
            _uiState.update {
                it.copy(
                    backendUrl = validatedUrl,
                    streamPathMode = streamMode,
                    hasAdminToken = hasToken,
                    streamProbes = emptyMap(),
                    snackbarNotice = SnackbarNotice(
                        message = "Saved backend configuration.$encryptionNote"
                    )
                )
            }
            onSaved()
            refreshAll()
        }
    }

    fun testConnection(rawUrl: String) {
        val validatedUrl = UrlValidator.normalizeAndValidate(rawUrl).getOrElse { err ->
            _uiState.update {
                it.copy(
                    snackbarNotice = SnackbarNotice(
                        message = err.message ?: "Invalid backend URL",
                        isError = true
                    )
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isTestingConnection = true) }

            val healthRes = apiService.checkHealth(validatedUrl)
            val videosRes = apiService.fetchVideos(validatedUrl)

            val healthStatus = when (healthRes) {
                is ApiResult.Success -> healthRes.data
                is ApiResult.Error -> HealthStatus(
                    isOnline = false,
                    httpCode = healthRes.httpCode,
                    latencyMs = null,
                    serverMessage = healthRes.message
                )
            }

            val diagnostics = when (videosRes) {
                is ApiResult.Success -> ConnectionDiagnostics(
                    testedUrl = validatedUrl,
                    healthResult = healthStatus,
                    catalogReachable = true,
                    catalogVideoCount = videosRes.data.size,
                    catalogErrorMessage = null,
                    hasAdminTokenConfigured = credentialStore.hasAdminToken()
                )
                is ApiResult.Error -> ConnectionDiagnostics(
                    testedUrl = validatedUrl,
                    healthResult = healthStatus,
                    catalogReachable = false,
                    catalogVideoCount = null,
                    catalogErrorMessage = videosRes.message,
                    hasAdminTokenConfigured = credentialStore.hasAdminToken()
                )
            }

            _uiState.update {
                it.copy(
                    isTestingConnection = false,
                    connectionDiagnostics = diagnostics
                )
            }
        }
    }

    fun logout() {
        credentialStore.clearAdminToken()
        _uiState.update {
            it.copy(
                hasAdminToken = false,
                connectionDiagnostics = it.connectionDiagnostics?.copy(hasAdminTokenConfigured = false),
                snackbarNotice = SnackbarNotice(
                    message = "Admin token cleared from secure storage. Signed out."
                )
            )
        }
    }

    fun notifyMessage(message: String, isError: Boolean = false) {
        _uiState.update { it.copy(snackbarNotice = SnackbarNotice(message = message, isError = isError)) }
    }
}
