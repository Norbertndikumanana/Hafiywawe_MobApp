package rw.hafiyawe.store.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import rw.hafiyawe.store.core.daysLeft
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.repo.AuthRepository
import rw.hafiyawe.store.data.repo.ProductFilters
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.categoryIcon
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ProductCard
import rw.hafiyawe.store.ui.components.SectionHeader
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes

@Composable
fun HomeScreen(nav: Navigator) {
    val app = LocalApp.current
    val products by app.catalog.learnedFeed.collectAsStateWithLifecycle(emptyList())
    val saved by app.catalog.saved(AuthRepository.KIND_FAVORITE)
        .collectAsStateWithLifecycle(emptyList())
    val announcements by app.meta.announcements.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()
    var search by remember { mutableStateOf("") }

    val savedIds = remember(saved) { saved.map { it.product.id }.toSet() }

    LaunchedEffect(Unit) {
        search = ""
        runCatching { app.catalog.refresh() }
        runCatching { app.meta.refreshAnnouncements() }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 28.dp)
    ) {
        item { BrandHeader() }

        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                SearchField(
                    value = search,
                    onValueChange = { search = it },
                    onSubmit = {
                        app.browseRequest = ProductFilters(query = search)
                        nav.tab(Routes.BROWSE)
                    }
                )
            }
        }

        app.user?.let { user ->
            item {
                val planDays = daysLeft(user.planExpiresAt)
                val access = when {
                    planDays > 0 -> "Monthly plan · $planDays days left"
                    user.freePostsRemaining > 0 -> "${user.freePostsRemaining} free listings left"
                    user.postCredits > 0 -> "${user.postCredits} posting credits"
                    else -> "Add a posting credit to publish"
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = access,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.weight(1f)
                        )
                        Button(onClick = { nav.open(Routes.SELL) }) {
                            Text(t("action.post"), style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }
            }
        }

        items(announcements.take(2), key = { it.id }) { announcement ->
            AnnouncementCard(announcement) { href ->
                when {
                    href == null -> Unit
                    href.startsWith("/payments") -> nav.open(Routes.PAYMENTS)
                    href.startsWith("/browse") -> nav.tab(Routes.BROWSE)
                    href.startsWith("/sell") -> nav.open(Routes.SELL)
                    href.startsWith("/help") -> nav.open(Routes.HELP)
                    else -> nav.tab(Routes.BROWSE)
                }
            }
        }

        item { SectionHeader(title = t("home.categories")) }

        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(Market.CATEGORIES) { category ->
                    CategoryChip(category) {
                        app.browseRequest = ProductFilters(category = category)
                        nav.tab(Routes.BROWSE)
                    }
                }
            }
        }

        item {
            SectionHeader(
                title = t("home.forYou"),
                actionLabel = t("action.viewAll"),
                onAction = { nav.tab(Routes.BROWSE) }
            )
        }

        if (products.isEmpty()) {
            item {
                EmptyState(
                    icon = Icons.Outlined.Explore,
                    title = t("home.empty"),
                    actionLabel = t("action.post"),
                    onAction = { nav.open(Routes.SELL) }
                )
            }
        }

        items(products.chunked(2), key = { row -> row.joinToString("-") { it.id } }) { row ->
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

        item {
            Column(modifier = Modifier.padding(20.dp)) {
                Text(
                    text = t("listing.safety"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BrandHeader() {
    val app = LocalApp.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.linearGradient(
                        listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.tertiary)
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Filled.Storefront,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.size(21.dp)
            )
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text("HAFIYAWE", style = MaterialTheme.typography.titleLarge)
            Text(
                t("home.welcome"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (app.demoMode) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.secondaryContainer
            ) {
                Text(
                    t("common.demoBadge"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                )
            }
            Spacer(Modifier.width(4.dp))
        }
        IconButton(onClick = { app.refreshEverything() }) {
            Icon(
                Icons.Filled.Refresh,
                contentDescription = t("action.refresh"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AnnouncementCard(announcement: Announcement, onCta: (String?) -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = announcement.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = announcement.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
            )
            if (!announcement.ctaLabel.isNullOrBlank() && announcement.href != null) {
                Spacer(Modifier.height(10.dp))
                Button(onClick = { onCta(announcement.href) }) {
                    Text(announcement.ctaLabel.orEmpty())
                }
            }
        }
    }
}

@Composable
private fun CategoryChip(category: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier
                .width(86.dp)
                .padding(vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    categoryIcon(category),
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            Spacer(Modifier.height(6.dp))
            Text(
                text = category,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/** Kept for the badge in other screens. */
@Composable
fun UnreadBadge(count: Int, content: @Composable () -> Unit) {
    if (count > 0) {
        BadgedBox(badge = { Badge { Text(if (count > 9) "9+" else count.toString()) } }) {
            content()
        }
    } else {
        content()
    }
}

private fun noop(product: Product) = product
