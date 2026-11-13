package rw.hafiyawe.store.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import rw.hafiyawe.store.data.repo.AuthRepository
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.BackBar
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ProductCard
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes

@Composable
fun FavoritesScreen(nav: Navigator) {
    SavedCollection(
        nav = nav,
        kind = AuthRepository.KIND_FAVORITE,
        title = t("profile.favorites"),
        emptyText = "Tap the heart on a listing to keep it here.",
        emptyAction = t("nav.browse")
    )
}

@Composable
fun CartScreen(nav: Navigator) {
    SavedCollection(
        nav = nav,
        kind = AuthRepository.KIND_CART,
        title = t("profile.cart"),
        emptyText = "The cart is a list of sellers to contact — there is nothing to pay in the app.",
        emptyAction = t("nav.browse")
    )
}

@Composable
private fun SavedCollection(
    nav: Navigator,
    kind: String,
    title: String,
    emptyText: String,
    emptyAction: String
) {
    val app = LocalApp.current
    val saved by app.catalog.saved(kind).collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(kind) {
        runCatching { app.catalog.refreshSaved(kind) }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = title, onBack = { nav.back() })

        if (saved.isEmpty()) {
            EmptyState(
                icon = if (kind == AuthRepository.KIND_CART) Icons.Filled.ShoppingCart
                else Icons.Filled.FavoriteBorder,
                title = if (kind == AuthRepository.KIND_CART) t("profile.cart") else t("profile.favorites"),
                body = emptyText,
                actionLabel = emptyAction,
                onAction = { nav.tab(Routes.BROWSE) }
            )
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(saved, key = { it.product.id }) { row ->
                    Column {
                        ProductCard(
                            product = row.product,
                            modifier = Modifier.fillMaxWidth(),
                            saved = kind == AuthRepository.KIND_FAVORITE,
                            onOpen = {
                                scope.launch { app.catalog.recordView(row.product) }
                                nav.product(row.product.id)
                            },
                            onToggleSave = {
                                scope.launch {
                                    runCatching { app.catalog.toggleSaved(kind, row.product) }
                                }
                            }
                        )
                        if (!row.available) {
                            Spacer(Modifier.height(4.dp))
                            Text(
                                text = t("listing.offMarket"),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(start = 6.dp)
                            )
                        }
                    }
                }
                if (app.user == null) {
                    item {
                        Text(
                            t("profile.savedHint"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
