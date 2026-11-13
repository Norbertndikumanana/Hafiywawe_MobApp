package rw.hafiyawe.store.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import rw.hafiyawe.store.ui.i18n.LocalStrings
import rw.hafiyawe.store.ui.i18n.stringsFor
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.AppDeepLink
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes
import rw.hafiyawe.store.ui.screens.AboutScreen
import rw.hafiyawe.store.ui.screens.AccountStatusScreen
import rw.hafiyawe.store.ui.screens.AdminScreen
import rw.hafiyawe.store.ui.screens.BrowseScreen
import rw.hafiyawe.store.ui.screens.CartScreen
import rw.hafiyawe.store.ui.screens.ChatsScreen
import rw.hafiyawe.store.ui.screens.ContactScreen
import rw.hafiyawe.store.ui.screens.ConversationScreen
import rw.hafiyawe.store.ui.screens.FavoritesScreen
import rw.hafiyawe.store.ui.screens.ForgotPasswordScreen
import rw.hafiyawe.store.ui.screens.HelpScreen
import rw.hafiyawe.store.ui.screens.HomeScreen
import rw.hafiyawe.store.ui.screens.LoginScreen
import rw.hafiyawe.store.ui.screens.MyListingsScreen
import rw.hafiyawe.store.ui.screens.NotificationsScreen
import rw.hafiyawe.store.ui.screens.PaymentsScreen
import rw.hafiyawe.store.ui.screens.ProductDetailScreen
import rw.hafiyawe.store.ui.screens.ProfileScreen
import rw.hafiyawe.store.ui.screens.RegisterScreen
import rw.hafiyawe.store.ui.screens.ResetPasswordScreen
import rw.hafiyawe.store.ui.screens.SellScreen
import rw.hafiyawe.store.ui.screens.SettingsScreen
import rw.hafiyawe.store.ui.screens.TicketScreen
import rw.hafiyawe.store.ui.screens.VerifyEmailScreen
import rw.hafiyawe.store.ui.screens.VerifyPendingScreen
import rw.hafiyawe.store.ui.theme.HafiYaweTheme

@Composable
fun AppRoot(state: AppState, deepLink: AppDeepLink? = null) {
    val strings = remember(state.language) { stringsFor(state.language) }
    CompositionLocalProvider(
        LocalStrings provides strings,
        LocalApp provides state
    ) {
        HafiYaweTheme(themeMode = state.themeMode) {
            val controller = rememberNavController()
            val nav = remember(controller) { Navigator(controller) }
            val entry by controller.currentBackStackEntryAsState()
            val route = entry?.destination?.route ?: Routes.HOME
            val showBottomBar = route in Routes.bottomRoutes

            Scaffold(
                snackbarHost = { SnackbarHost(state.snackbar) },
                bottomBar = {
                    if (showBottomBar) BottomBar(route = route, state = state, nav = nav)
                },
                containerColor = MaterialTheme.colorScheme.background
            ) { padding ->
                NavHost(
                    navController = controller,
                    startDestination = Routes.HOME,
                    modifier = Modifier.padding(padding)
                ) {
                    composable(Routes.HOME) { HomeScreen(nav) }
                    composable(Routes.BROWSE) { BrowseScreen(nav) }
                    composable(Routes.CHATS) { ChatsScreen(nav) }
                    composable(Routes.NOTIFICATIONS) { NotificationsScreen(nav) }
                    composable(Routes.PROFILE) { ProfileScreen(nav) }

                    composable(Routes.LOGIN) { LoginScreen(nav) }
                    composable(Routes.REGISTER) { RegisterScreen(nav) }
                    composable(Routes.FORGOT) { ForgotPasswordScreen(nav) }
                    composable(Routes.VERIFY_PENDING) { VerifyPendingScreen(nav) }

                    composable(Routes.SELL) { SellScreen(nav) }
                    composable(Routes.MY_LISTINGS) { MyListingsScreen(nav) }
                    composable(Routes.FAVORITES) { FavoritesScreen(nav) }
                    composable(Routes.CART) { CartScreen(nav) }
                    composable(Routes.PAYMENTS) { PaymentsScreen(nav) }
                    composable(Routes.HELP) { HelpScreen(nav) }
                    composable(Routes.CONTACT) { ContactScreen(nav) }
                    composable(Routes.ACCOUNT_STATUS) { AccountStatusScreen(nav) }
                    composable(Routes.SETTINGS) { SettingsScreen(nav) }
                    composable(Routes.ADMIN) { AdminScreen(nav) }

                    composable(
                        route = Routes.PRODUCT,
                        arguments = listOf(navArgument("productId") { type = NavType.StringType })
                    ) { backEntry ->
                        ProductDetailScreen(nav, backEntry.arguments?.getString("productId").orEmpty())
                    }

                    composable(
                        route = Routes.CHAT,
                        arguments = listOf(navArgument("roomId") { type = NavType.StringType })
                    ) { backEntry ->
                        ConversationScreen(nav, backEntry.arguments?.getString("roomId").orEmpty())
                    }

                    composable(
                        route = Routes.TICKET,
                        arguments = listOf(navArgument("ticketId") { type = NavType.StringType })
                    ) { backEntry ->
                        TicketScreen(nav, backEntry.arguments?.getString("ticketId").orEmpty())
                    }

                    composable(
                        route = Routes.RESET,
                        arguments = listOf(navArgument("token") { type = NavType.StringType })
                    ) { backEntry ->
                        ResetPasswordScreen(nav, backEntry.arguments?.getString("token").orEmpty())
                    }

                    composable(
                        route = Routes.VERIFY_EMAIL,
                        arguments = listOf(navArgument("token") { type = NavType.StringType })
                    ) { backEntry ->
                        VerifyEmailScreen(nav, backEntry.arguments?.getString("token").orEmpty())
                    }

                    composable(
                        route = Routes.ABOUT,
                        arguments = listOf(navArgument("page") { type = NavType.StringType })
                    ) { backEntry ->
                        AboutScreen(nav, backEntry.arguments?.getString("page").orEmpty())
                    }
                }
            }

            LaunchedEffect(Unit) {
                state.refreshEverything()
                if (deepLink != null) {
                    controller.navigate(deepLink.route) { launchSingleTop = true }
                }
            }
        }
    }
}

@Composable
private fun BottomBar(route: String, state: AppState, nav: Navigator) {
    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        BottomItem(
            selected = route == Routes.HOME,
            label = t("nav.home"),
            icon = Icons.Filled.Home,
            onClick = { nav.tab(Routes.HOME) }
        )
        BottomItem(
            selected = route == Routes.BROWSE,
            label = t("nav.browse"),
            icon = Icons.Filled.Search,
            onClick = { nav.tab(Routes.BROWSE) }
        )
        BottomItem(
            selected = route == Routes.CHATS,
            label = t("nav.chats"),
            icon = Icons.Filled.ChatBubbleOutline,
            badge = state.unreadChats,
            onClick = { nav.tab(Routes.CHATS) }
        )
        BottomItem(
            selected = route == Routes.NOTIFICATIONS,
            label = t("nav.alerts"),
            icon = Icons.Filled.NotificationsNone,
            badge = state.unreadNotifications,
            onClick = { nav.tab(Routes.NOTIFICATIONS) }
        )
        BottomItem(
            selected = route == Routes.PROFILE,
            label = t("nav.account"),
            icon = Icons.Filled.Person,
            onClick = { nav.tab(Routes.PROFILE) }
        )
    }
}

@Composable
private fun androidx.compose.foundation.layout.RowScope.BottomItem(
    selected: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    badge: Int = 0
) {
    NavigationBarItem(
        selected = selected,
        onClick = onClick,
        label = { Text(label, maxLines = 1) },
        icon = {
            if (badge > 0) {
                BadgedBox(
                    badge = { Badge { Text(if (badge > 9) "9+" else badge.toString()) } }
                ) {
                    Icon(icon, contentDescription = label)
                }
            } else {
                Icon(icon, contentDescription = label)
            }
        }
    )
}
