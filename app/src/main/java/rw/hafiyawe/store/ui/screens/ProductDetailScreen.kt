package rw.hafiyawe.store.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.core.rwf
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.cover
import rw.hafiyawe.store.data.isActive
import rw.hafiyawe.store.data.place
import rw.hafiyawe.store.data.repo.AuthRepository
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.Avatar
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.InfoNote
import rw.hafiyawe.store.ui.components.Pill
import rw.hafiyawe.store.ui.components.PillTone
import rw.hafiyawe.store.ui.components.RemoteImage
import rw.hafiyawe.store.ui.components.StatusPill
import rw.hafiyawe.store.ui.components.VerifiedBadge
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes
import rw.hafiyawe.store.ui.theme.Brand

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(nav: Navigator, productId: String) {
    val app = LocalApp.current
    val product by app.catalog.observeProduct(productId).collectAsStateWithLifecycle(null)
    val saved by app.catalog.isSaved(AuthRepository.KIND_FAVORITE, productId)
        .collectAsStateWithLifecycle(false)
    val inCart by app.catalog.isSaved(AuthRepository.KIND_CART, productId)
        .collectAsStateWithLifecycle(false)
    val scope = rememberCoroutineScope()
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(productId) {
        loading = true
        runCatching { app.catalog.product(productId) }
            .onFailure { error = it.message }
        loading = false
        app.catalog.product(productId)?.let { app.catalog.recordView(it) }
    }

    val owner = app.user?.id == product?.sellerId
    val genericError = t("error.generic")

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.background)
                .padding(horizontal = 6.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = { nav.back() }) {
                Icon(
                    Icons.Filled.ArrowBack,
                    contentDescription = t("action.back")
                )
            }
            Text(
                text = t("listing.description"),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = {
                scope.launch {
                    runCatching {
                        product?.let { app.catalog.toggleSaved(AuthRepository.KIND_FAVORITE, it) }
                    }
                }
            }) {
                Icon(
                    imageVector = if (saved) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                    contentDescription = t("profile.favorites"),
                    tint = if (saved) Brand.Gold else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        when {
            product == null && loading -> Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) { CircularProgressIndicator(strokeWidth = 2.dp) }

            product == null -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(error ?: t("error.generic"))
                Spacer(Modifier.height(12.dp))
                Button(onClick = { nav.back() }) { Text(t("action.back")) }
            }

            else -> {
                val item = product!!
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(bottom = 24.dp)
                ) {
                    item {
                        val images = item.images
                        if (images.isEmpty()) {
                            RemoteImage(
                                path = null,
                                contentDescription = item.title,
                                label = item.title,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .aspectRatio(4f / 3f)
                            )
                        } else {
                            val pager = rememberPagerState(pageCount = { images.size })
                            Box {
                                HorizontalPager(state = pager) { page ->
                                    RemoteImage(
                                        path = images[page],
                                        contentDescription = item.title,
                                        label = item.title,
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(4f / 3f)
                                    )
                                }
                                Row(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(10.dp),
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    repeat(images.size) { index ->
                                        Box(
                                            modifier = Modifier
                                                .size(7.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    if (pager.currentPage == index)
                                                        MaterialTheme.colorScheme.primary
                                                    else MaterialTheme.colorScheme.surface.copy(alpha = 0.7f)
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = rwf(item.price),
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.weight(1f)
                                )
                                if (!item.isActive) StatusPill(item.status)
                            }
                            Spacer(Modifier.height(6.dp))
                            Text(item.title, style = MaterialTheme.typography.titleLarge)
                            Spacer(Modifier.height(10.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Pill(item.condition, PillTone.Gold)
                                Pill(item.category)
                                Pill(item.province)
                            }
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = listOfNotNull(
                                    item.place.ifBlank { null },
                                    Iso.relative(item.createdAt)
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (!item.isActive) {
                                Spacer(Modifier.height(10.dp))
                                InfoNote(
                                    when (item.status) {
                                        Market.STATUS_PENDING -> t("listing.pending")
                                        Market.STATUS_REJECTED -> t("listing.rejected") +
                                            (item.rejectionReason?.let { " $it" } ?: "")
                                        else -> t("listing.offMarket")
                                    }
                                )
                            }
                        }
                    }

                    item {
                        CrystalCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Avatar(item.sellerName.ifBlank { "Seller" })
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        item.sellerName.ifBlank { "Seller" },
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                    if (item.sellerVerified) VerifiedBadge()
                                }
                                Text(
                                    text = t("listing.seller"),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    item {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(t("listing.description"), style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(
                                item.description,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(Modifier.height(14.dp))
                            InfoNote(t("listing.safety"))
                        }
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (owner) {
                            Button(
                                onClick = { nav.open(Routes.MY_LISTINGS) },
                                modifier = Modifier.weight(1f)
                            ) { Text(t("profile.mylistings")) }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        runCatching {
                                            app.catalog.toggleSaved(AuthRepository.KIND_CART, item)
                                        }.onSuccess {
                                            app.toast(
                                                if (!inCart) "Added to cart" else "Removed from cart"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier.height(50.dp)
                            ) {
                                Icon(Icons.Filled.ShoppingCart, contentDescription = t("profile.cart"))
                            }
                            Button(
                                onClick = {
                                    if (app.user == null) {
                                        nav.open(Routes.LOGIN)
                                        return@Button
                                    }
                                    scope.launch {
                                        runCatching { app.chats.openFor(item) }
                                            .onSuccess { roomId -> nav.chat(roomId) }
                                            .onFailure { app.toast(it.message ?: genericError) }
                                    }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(50.dp)
                            ) { Text(t("action.message")) }
                        }
                    }
                }
            }
        }
    }
}
