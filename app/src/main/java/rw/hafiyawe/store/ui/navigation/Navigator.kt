package rw.hafiyawe.store.ui.navigation

import android.net.Uri
import androidx.navigation.NavHostController

object Routes {
    const val HOME = "home"
    const val BROWSE = "browse"
    const val CHATS = "chats"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val FORGOT = "forgot-password"
    const val SELL = "sell"
    const val MY_LISTINGS = "my-listings"
    const val FAVORITES = "favorites"
    const val CART = "cart"
    const val PAYMENTS = "payments"
    const val HELP = "help"
    const val CONTACT = "contact"
    const val ACCOUNT_STATUS = "account-status"
    const val SETTINGS = "settings"
    const val ADMIN = "admin"
    const val VERIFY_PENDING = "verify-pending"

    const val PRODUCT = "product/{productId}"
    const val CHAT = "chat/{roomId}"
    const val TICKET = "ticket/{ticketId}"
    const val RESET = "reset-password/{token}"
    const val VERIFY_EMAIL = "verify-email/{token}"
    const val ABOUT = "about/{page}"

    fun product(id: String) = "product/${Uri.encode(id)}"
    fun chat(id: String) = "chat/${Uri.encode(id)}"
    fun ticket(id: String) = "ticket/${Uri.encode(id)}"
    fun reset(token: String) = "reset-password/${Uri.encode(token)}"
    fun verifyEmail(token: String) = "verify-email/${Uri.encode(token)}"
    fun about(page: String) = "about/$page"

    val bottomRoutes = setOf(HOME, BROWSE, CHATS, NOTIFICATIONS, PROFILE)
}

class Navigator(private val controller: NavHostController) {

    fun open(route: String) {
        controller.navigate(route) { launchSingleTop = true }
    }

    fun tab(route: String) {
        controller.navigate(route) {
            popUpTo(Routes.HOME) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun product(id: String) = open(Routes.product(id))
    fun chat(id: String) = open(Routes.chat(id))
    fun ticket(id: String) = open(Routes.ticket(id))
    fun about(page: String = "about") = open(Routes.about(page))

    fun back() {
        if (!controller.popBackStack()) controller.navigate(Routes.HOME) { launchSingleTop = true }
    }

    fun home() {
        controller.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
        }
    }
}

/** Where a hafiyawe:// or https:// link should take the person. */
data class AppDeepLink(val route: String)
