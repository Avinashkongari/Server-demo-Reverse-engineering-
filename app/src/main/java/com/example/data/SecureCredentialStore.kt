package com.example.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

private val Context.axionSettingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "axion_stream_settings"
)

object UrlValidator {
    const val DEFAULT_BACKEND_URL = "http://127.0.0.1:3000"

    /**
     * Strictly validates and normalizes a user-supplied backend base URL.
     * Allows only http:// or https:// schemes with a non-blank host and strips trailing slashes.
     */
    fun normalizeAndValidate(rawInput: String): Result<String> {
        val trimmed = rawInput.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Backend URL cannot be empty."))
        }
        if (trimmed.any { it.isWhitespace() || it.isISOControl() }) {
            return Result.failure(IllegalArgumentException("Backend URL must not contain spaces or control characters."))
        }
        val lower = trimmed.lowercase()
        if (!lower.startsWith("http://") && !lower.startsWith("https://")) {
            return Result.failure(IllegalArgumentException("Backend URL must start with http:// or https://"))
        }
        val parsed = trimmed.toHttpUrlOrNull()
            ?: return Result.failure(IllegalArgumentException("Enter a valid HTTP/HTTPS URL (for example http://127.0.0.1:3000)."))
        if (parsed.host.isBlank()) {
            return Result.failure(IllegalArgumentException("Backend URL must include a valid host or IP address."))
        }
        if (parsed.query != null || parsed.fragment != null) {
            return Result.failure(IllegalArgumentException("Backend base URL must not include query parameters or fragments."))
        }
        val normalized = parsed.toString().trimEnd('/')
        return Result.success(normalized)
    }

    fun isSafeHttpUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return false
        val parsed = url.trim().toHttpUrlOrNull() ?: return false
        return parsed.scheme == "http" || parsed.scheme == "https"
    }
}

object FilenameValidator {
    fun validate(rawFilename: String): Result<String> {
        val trimmed = rawFilename.trim()
        if (trimmed.isEmpty()) {
            return Result.failure(IllegalArgumentException("Video filename is required."))
        }
        if (trimmed.contains("..") || trimmed.contains("/") || trimmed.contains("\\") ||
            trimmed.contains("\u0000") || trimmed.contains("?") || trimmed.contains("#")
        ) {
            return Result.failure(
                IllegalArgumentException("Filename must be a plain file name in the Termux videos folder (no paths like '../' or '/').")
            )
        }
        return Result.success(trimmed)
    }
}

/**
 * Stores non-sensitive server config in DataStore Preferences and encrypts the Admin Token
 * at rest using Android Keystore AES-256-GCM before saving to private SharedPreferences.
 * Never logs the raw token or persists it in plaintext.
 */
class SecureCredentialStore(context: Context) {
    private val appContext = context.applicationContext
    private val securePrefs = appContext.getSharedPreferences(SECURE_PREFS_NAME, Context.MODE_PRIVATE)

    @Volatile
    private var inMemoryFallbackToken: String? = null

    val backendUrlFlow: Flow<String> = appContext.axionSettingsDataStore.data.map { prefs ->
        prefs[KEY_BACKEND_URL] ?: UrlValidator.DEFAULT_BACKEND_URL
    }

    val streamPathModeFlow: Flow<StreamPathMode> = appContext.axionSettingsDataStore.data.map { prefs ->
        val stored = prefs[KEY_STREAM_PATH_MODE]
        StreamPathMode.entries.firstOrNull { it.name == stored } ?: StreamPathMode.AUTO
    }

    suspend fun getBackendUrl(): String = backendUrlFlow.first()

    suspend fun getStreamPathMode(): StreamPathMode = streamPathModeFlow.first()

    suspend fun saveBackendUrl(url: String) {
        val normalized = UrlValidator.normalizeAndValidate(url).getOrThrow()
        appContext.axionSettingsDataStore.edit { prefs ->
            prefs[KEY_BACKEND_URL] = normalized
        }
    }

    suspend fun saveStreamPathMode(mode: StreamPathMode) {
        appContext.axionSettingsDataStore.edit { prefs ->
            prefs[KEY_STREAM_PATH_MODE] = mode.name
        }
    }

    /**
     * Encrypts and stores the X-Admin-Token using Android Keystore AES-256-GCM.
     * Returns true if encrypted persistently on disk, false if kept in memory only due to keystore unavailability.
     */
    fun saveAdminToken(token: String): Boolean {
        val cleaned = token.trim()
        if (cleaned.isEmpty()) {
            clearAdminToken()
            return true
        }
        return try {
            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ciphertext = cipher.doFinal(cleaned.toByteArray(StandardCharsets.UTF_8))

            securePrefs.edit()
                .putString(KEY_ENCRYPTED_TOKEN, Base64.encodeToString(ciphertext, Base64.NO_WRAP))
                .putString(KEY_TOKEN_IV, Base64.encodeToString(iv, Base64.NO_WRAP))
                .apply()
            inMemoryFallbackToken = null
            true
        } catch (_: Exception) {
            // Never fall back to plaintext disk storage; hold only in RAM for current session.
            inMemoryFallbackToken = cleaned
            securePrefs.edit().clear().apply()
            false
        }
    }

    fun getAdminToken(): String? {
        inMemoryFallbackToken?.let { if (it.isNotBlank()) return it }
        val encodedCiphertext = securePrefs.getString(KEY_ENCRYPTED_TOKEN, null) ?: return null
        val encodedIv = securePrefs.getString(KEY_TOKEN_IV, null) ?: return null
        return try {
            val secretKey = getExistingSecretKey() ?: return null
            val iv = Base64.decode(encodedIv, Base64.NO_WRAP)
            val ciphertext = Base64.decode(encodedCiphertext, Base64.NO_WRAP)
            val cipher = Cipher.getInstance(AES_GCM_TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            val plainBytes = cipher.doFinal(ciphertext)
            String(plainBytes, StandardCharsets.UTF_8).takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            clearAdminToken()
            null
        }
    }

    fun hasAdminToken(): Boolean = !getAdminToken().isNullOrBlank()

    fun clearAdminToken() {
        inMemoryFallbackToken = null
        securePrefs.edit().clear().apply()
    }

    private fun getExistingSecretKey(): SecretKey? {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        return keyStore.getKey(KEY_ALIAS, null) as? SecretKey
    }

    private fun getOrCreateSecretKey(): SecretKey {
        getExistingSecretKey()?.let { return it }
        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "axion_stream_admin_token_aes_key"
        private const val AES_GCM_TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH_BITS = 128
        private const val SECURE_PREFS_NAME = "axion_secure_vault"
        private const val KEY_ENCRYPTED_TOKEN = "enc_admin_token"
        private const val KEY_TOKEN_IV = "enc_admin_token_iv"

        private val KEY_BACKEND_URL = stringPreferencesKey("backend_url")
        private val KEY_STREAM_PATH_MODE = stringPreferencesKey("stream_path_mode")
    }
}
