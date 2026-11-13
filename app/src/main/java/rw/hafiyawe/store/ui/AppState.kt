package rw.hafiyawe.store.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.material3.SnackbarHostState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import rw.hafiyawe.store.core.AppGraph
import rw.hafiyawe.store.data.AppConfig
import rw.hafiyawe.store.data.User
import rw.hafiyawe.store.data.isAdmin
import rw.hafiyawe.store.data.remote.ApiException

/**
 * App-wide state: who is signed in, which server, theme and language, plus the
 * counters the bottom bar shows. Screens read it through [LocalApp].
 */
@Stable
class AppState(val graph: AppGraph, val scope: CoroutineScope) {

    val snackbar = SnackbarHostState()

    var user by mutableStateOf(graph.session.user.value)
        private set

    var demoMode by mutableStateOf(graph.session.demoMode.value)
        private set

    var language by mutableStateOf(graph.session.language)
        private set

    var themeMode by mutableStateOf(graph.session.themeMode)
        private set

    var config by mutableStateOf(AppConfig())
        private set

    var unreadNotifications by mutableIntStateOf(0)
        private set

    var unreadChats by mutableIntStateOf(0)
        private set

    /** Set by Home or a deep link, consumed once by the Browse screen. */
    var browseRequest by mutableStateOf<rw.hafiyawe.store.data.repo.ProductFilters?>(null)

    /** Development-only links returned by the API when SMTP is not configured. */
    var pendingVerificationUrl by mutableStateOf<String?>(null)
    var pendingResetUrl by mutableStateOf<String?>(null)

    val auth get() = graph.auth
    val admin get() = graph.admin
    val catalog get() = graph.catalog
    val chats get() = graph.chats
    val notifications get() = graph.notifications
    val payments get() = graph.payments
    val support get() = graph.support
    val meta get() = graph.meta

    val isSignedIn: Boolean get() = user != null
    val isAdmin: Boolean get() = user?.isAdmin == true
    val isBanned: Boolean get() = user?.isBanned == true
    val baseUrl: String get() = graph.session.baseUrl

    init {
        scope.launch {
            graph.session.user.collect { user = it }
        }
        scope.launch {
            graph.session.demoMode.collect { demoMode = it }
        }
        scope.launch {
            graph.meta.config.collect { config = it ?: AppConfig() }
        }
        scope.launch {
            graph.notifications.unread.collect { unreadNotifications = it }
        }
        scope.launch {
            graph.chats.unread.collect { unreadChats = it }
        }
    }

    fun toast(message: String) {
        scope.launch { snackbar.showSnackbar(message) }
    }

    fun useLanguage(code: String) {
        graph.session.language = code
        language = code
    }

    fun useTheme(mode: String) {
        graph.session.themeMode = mode
        themeMode = mode
    }

    fun setBaseUrl(url: String) {
        graph.session.baseUrl = url
    }

    fun refreshCounters() {
        scope.launch {
            runCatching { graph.notifications.refresh() }
            runCatching { graph.chats.refreshRooms() }
        }
    }

    fun refreshEverything() {
        scope.launch {
            runCatching { graph.meta.refreshAll() }
            runCatching { graph.catalog.refresh() }
            if (isSignedIn) {
                runCatching { graph.catalog.refreshMine() }
                runCatching { graph.catalog.refreshSaved(rw.hafiyawe.store.data.repo.AuthRepository.KIND_FAVORITE) }
                runCatching { graph.catalog.refreshSaved(rw.hafiyawe.store.data.repo.AuthRepository.KIND_CART) }
                runCatching { graph.notifications.refresh() }
                runCatching { graph.chats.refreshRooms() }
                runCatching { graph.payments.refresh() }
                runCatching { graph.support.refresh() }
                runCatching { graph.auth.refreshMe() }
            }
        }
    }

    fun signOut() {
        scope.launch {
            runCatching { graph.auth.signOut() }
            toast("Signed out")
        }
    }

    fun enterDemo() {
        scope.launch {
            runCatching { graph.auth.enterDemo() }
            refreshEverything()
        }
    }

    fun leaveDemo() {
        scope.launch {
            runCatching { graph.auth.leaveDemo() }
            user = null
        }
    }

    fun <T> collectFlow(flow: Flow<T>): Flow<T> = flow
}

val LocalApp = staticCompositionLocalOf<AppState> {
    error("AppState was not provided")
}

/** Per-screen busy/error handling so every form behaves the same way. */
@Stable
class ScreenState(private val app: AppState, private val scope: CoroutineScope) {
    var busy by mutableStateOf(false)
        private set
    var error by mutableStateOf<String?>(null)
        private set
    var done by mutableStateOf(false)
        private set

    fun clearError() {
        error = null
    }

    fun reset() {
        error = null
        done = false
        busy = false
    }

    fun launch(block: suspend () -> Unit) {
        scope.launch {
            busy = true
            error = null
            try {
                block()
                done = true
            } catch (e: ApiException) {
                error = e.message
            } catch (e: Exception) {
                error = e.message ?: "Something went wrong. Please try again."
            } finally {
                busy = false
            }
        }
    }

    fun toast(message: String) = app.toast(message)
}

@Composable
fun rememberScreenState(): ScreenState {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    return remember(app) { ScreenState(app, scope) }
}

@Composable
fun currentUser(): User? = LocalApp.current.user
