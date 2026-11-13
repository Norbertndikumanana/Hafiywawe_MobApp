package rw.hafiyawe.store.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.SearchOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.repo.AuthRepository
import rw.hafiyawe.store.data.repo.ProductFilters
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ProductCard
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator

@Composable
fun BrowseScreen(nav: Navigator) {
    val app = LocalApp.current
    var filters by remember { mutableStateOf(ProductFilters()) }
    val saved by app.catalog.saved(AuthRepository.KIND_FAVORITE)
        .collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        app.browseRequest?.let {
            filters = it
            app.browseRequest = null
        }
        runCatching { app.catalog.refresh() }
    }

    val sortOptions = listOf(
        "newest" to t("browse.newest"),
        "price_asc" to t("browse.priceLow"),
        "price_desc" to t("browse.priceHigh")
    )
    val resultsFlow = remember(filters) { app.catalog.browse(filters) }
    val results by resultsFlow.collectAsStateWithLifecycle(emptyList())
    val savedIds = remember(saved) { saved.map { it.product.id }.toSet() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(t("browse.title"), style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(10.dp))
                SearchField(
                    value = filters.query,
                    onValueChange = { filters = filters.copy(query = it) }
                )
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionPicker(
                        label = t("sell.category"),
                        value = filters.category,
                        options = Market.CATEGORIES,
                        onChange = { filters = filters.copy(category = it) },
                        modifier = Modifier.weight(1f)
                    )
                    OptionPicker(
                        label = t("sell.condition"),
                        value = filters.condition,
                        options = Market.CONDITIONS,
                        onChange = { filters = filters.copy(condition = it) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OptionPicker(
                        label = t("sell.province"),
                        value = filters.province,
                        options = Market.PROVINCES,
                        onChange = { filters = filters.copy(province = it) },
                        modifier = Modifier.weight(1f)
                    )
                    OptionPicker(
                        label = t("browse.sort"),
                        value = sortOptions.firstOrNull { it.first == filters.sort }?.second,
                        options = sortOptions.map { it.second },
                        allowClear = false,
                        onChange = { picked ->
                            filters = filters.copy(
                                sort = sortOptions.firstOrNull { it.second == picked }?.first ?: "newest"
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "${results.size} ${t("browse.results")}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (results.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.SearchOff,
                    title = t("browse.empty"),
                    actionLabel = t("action.clear"),
                    onAction = { filters = ProductFilters() }
                )
            }
        }

        items(results.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                row.forEach { product ->
                    ProductCard(
                        product = product,
                        saved = savedIds.contains(product.id),
                        modifier = Modifier.weight(1f),
                        onOpen = {
                            scope.launch { app.catalog.recordView(product) }
                            nav.product(product.id)
                        },
                        onToggleSave = {
                            scope.launch {
                                runCatching {
                                    app.catalog.toggleSaved(AuthRepository.KIND_FAVORITE, product)
                                }
                            }
                        }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
