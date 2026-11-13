package rw.hafiyawe.store.core

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import rw.hafiyawe.store.data.User
import rw.hafiyawe.store.data.local.AppJson

/**
 * Small persistent store for everything that must survive a restart:
 * the server address, the signed-in account, theme/language, the demo switch
 * and guest support tokens. The session cookie is kept by [PersistentCookieJar].
 */
class SessionStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("hafiyawe.settings", Context.MODE_PRIVATE)

    val cookiePrefs: SharedPreferences =
        context.getSharedPreferences("hafiyawe.cookies", Context.MODE_PRIVATE)

    private val _user = MutableStateFlow(readUser())
    val user: StateFlow<User?> = _user.asStateFlow()

    private val _demoMode = MutableStateFlow(prefs.getBoolean(KEY_DEMO, false))
    val demoMode: StateFlow<Boolean> = _demoMode.asStateFlow()

    var baseUrl: String
        get() {
            val stored = prefs.getString(KEY_BASE, null)?.trim().orEmpty()
            return if (stored.isBlank()) DEFAULT_BASE else stored.trimEnd('/')
        }
        set(value) {
            val clean = value.trim().trimEnd('/')
            prefs.edit().putString(KEY_BASE, if (clean.isBlank()) DEFAULT_BASE else clean).apply()
        }

    var language: String
        get() = prefs.getString(KEY_LANGUAGE, "en") ?: "en"
        set(value) {
            prefs.edit().putString(KEY_LANGUAGE, value).apply()
        }

    var themeMode: String
        get() = prefs.getString(KEY_THEME, "system") ?: "system"
        set(value) {
            prefs.edit().putString(KEY_THEME, value).apply()
        }

    var lastEmail: String
        get() = prefs.getString(KEY_EMAIL, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_EMAIL, value).apply()
        }

    var searchMemory: String
        get() = prefs.getString(KEY_SEARCH, "") ?: ""
        set(value) {
            prefs.edit().putString(KEY_SEARCH, value).apply()
        }

    val isSignedIn: Boolean get() = _user.value != null

    fun setUser(user: User?) {
        _user.value = user
        if (user == null) {
            prefs.edit().remove(KEY_USER).apply()
        } else {
            prefs.edit().putString(KEY_USER, AppJson.encodeToString(User.serializer(), user)).apply()
        }
    }

    fun patchUser(transform: (User) -> User) {
        val current = _user.value ?: return
        setUser(transform(current))
    }

    fun setDemoMode(enabled: Boolean) {
        _demoMode.value = enabled
        prefs.edit().putBoolean(KEY_DEMO, enabled).apply()
    }

    /** Absolute URL for a server-relative image path such as "/uploads/a.jpg". */
    fun absoluteUrl(path: String?): String? {
        if (path.isNullOrBlank()) return null
        // Absolute URLs and device-local content (demo photos) are used as they are.
        if (path.startsWith("http://") || path.startsWith("https://") ||
            path.startsWith("content://") || path.startsWith("file://")
        ) return path
        val origin = baseUrl
        return if (path.startsWith("/")) "$origin$path" else "$origin/$path"
    }

    fun saveSupportToken(ticketId: String, token: String) {
        val tokens = readTokens().toMutableMap()
        tokens[ticketId] = token
        prefs.edit().putString(KEY_SUPPORT_TOKENS, AppJson.encodeToString(tokens)).apply()
    }

    fun supportToken(ticketId: String): String? = readTokens()[ticketId]

    fun dropSupportToken(ticketId: String) {
        val tokens = readTokens().toMutableMap()
        if (tokens.remove(ticketId) != null) {
            prefs.edit().putString(KEY_SUPPORT_TOKENS, AppJson.encodeToString(tokens)).apply()
        }
    }

    private fun readTokens(): Map<String, String> {
        val raw = prefs.getString(KEY_SUPPORT_TOKENS, null) ?: return emptyMap()
        return runCatching {
            Json.decodeFromString<Map<String, String>>(raw)
        }.getOrDefault(emptyMap())
    }

    private fun readUser(): User? {
        val raw = prefs.getString(KEY_USER, null) ?: return null
        return runCatching { AppJson.decodeFromString(User.serializer(), raw) }.getOrNull()
    }

    companion object {
        /** 10.0.2.2 is the host machine as seen from the Android emulator. */
        const val DEFAULT_BASE = "http://10.0.2.2:3001"

        private const val KEY_BASE = "base_url"
        private const val KEY_USER = "session_user"
        private const val KEY_DEMO = "demo_mode"
        private const val KEY_LANGUAGE = "language"
        private const val KEY_THEME = "theme"
        private const val KEY_EMAIL = "last_email"
        private const val KEY_SEARCH = "search_memory"
        private const val KEY_SUPPORT_TOKENS = "support_tokens"
    }
}
