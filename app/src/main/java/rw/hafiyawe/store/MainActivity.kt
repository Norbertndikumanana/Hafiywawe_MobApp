package rw.hafiyawe.store

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import rw.hafiyawe.store.ui.AppRoot
import rw.hafiyawe.store.ui.AppState
import rw.hafiyawe.store.ui.navigation.AppDeepLink
import rw.hafiyawe.store.ui.navigation.Routes

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val graph = HafiYaweApp.graph(this)
        val link = parseLink(intent)

        setContent {
            val scope = rememberCoroutineScope()
            val state = remember { AppState(graph, scope) }
            AppRoot(state = state, deepLink = link)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        parseLink(intent)?.let { link ->
            // The running activity keeps its state; deep links are handled on launch.
            setIntent(intent)
        }
    }

    /**
     * Accepts hafiyawe://verify?token=… and hafiyawe://reset?token=…
     * (the same links the API returns in development when SMTP is not configured).
     */
    private fun parseLink(intent: Intent?): AppDeepLink? {
        val data = intent?.data ?: return null
        val token = data.getQueryParameter("token") ?: return null
        return when (data.host?.lowercase()) {
            "verify", "verify-email" -> AppDeepLink(Routes.verifyEmail(token))
            "reset", "reset-password" -> AppDeepLink(Routes.reset(token))
            else -> null
        }
    }
}
