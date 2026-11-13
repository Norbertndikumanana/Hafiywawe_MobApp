package rw.hafiyawe.store.data.remote

import android.content.SharedPreferences
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import rw.hafiyawe.store.data.local.AppJson

@Serializable
private data class StoredCookie(
    val name: String,
    val value: String,
    val domain: String,
    val path: String,
    val expiresAt: Long,
    val secure: Boolean,
    val httpOnly: Boolean
)

/**
 * The HAFIYAWE API authenticates with an HttpOnly cookie session, not a bearer
 * token, so the cookie has to be captured from login and replayed on every call —
 * and it must survive app restarts.
 */
class PersistentCookieJar(private val prefs: SharedPreferences) : CookieJar {

    private val cookies: MutableList<StoredCookie> = load().toMutableList()

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        for (cookie in cookies) {
            val stored = StoredCookie(
                name = cookie.name,
                value = cookie.value,
                domain = cookie.domain,
                path = cookie.path,
                expiresAt = cookie.expiresAt,
                secure = cookie.secure,
                httpOnly = cookie.httpOnly
            )
            this.cookies.removeAll {
                it.name == stored.name && it.domain == stored.domain && it.path == stored.path
            }
            if (stored.expiresAt > System.currentTimeMillis()) {
                this.cookies.add(stored)
            }
        }
        persist()
    }

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        val valid = cookies.filter { it.expiresAt > now && matches(it, url) }
        return valid.mapNotNull { stored ->
            runCatching {
                Cookie.Builder()
                    .name(stored.name)
                    .value(stored.value)
                    .domain(stored.domain)
                    .path(stored.path)
                    .expiresAt(stored.expiresAt)
                    .apply {
                        if (stored.secure) secure()
                        if (stored.httpOnly) httpOnly()
                    }
                    .build()
            }.getOrNull()
        }
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        persist()
    }

    private fun matches(cookie: StoredCookie, url: HttpUrl): Boolean {
        val hostMatches = url.host == cookie.domain || url.host.endsWith(".${cookie.domain}")
        val pathMatches = url.encodedPath.startsWith(cookie.path)
        return hostMatches && pathMatches && (!cookie.secure || url.isHttps)
    }

    private fun persist() {
        prefs.edit()
            .putString(KEY, AppJson.encodeToString(cookies.toList()))
            .apply()
    }

    private fun load(): List<StoredCookie> {
        val raw = prefs.getString(KEY, null) ?: return emptyList()
        return runCatching { AppJson.decodeFromString<List<StoredCookie>>(raw) }
            .getOrDefault(emptyList())
    }

    private companion object {
        const val KEY = "cookie_jar"
    }
}
