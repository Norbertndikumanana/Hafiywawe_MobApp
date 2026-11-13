package rw.hafiyawe.store.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import rw.hafiyawe.store.core.daysLeft
import rw.hafiyawe.store.core.rwf
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.ProductRequest
import rw.hafiyawe.store.data.cover
import rw.hafiyawe.store.data.hasCompleteProfile
import rw.hafiyawe.store.data.place
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.AppTextField
import rw.hafiyawe.store.ui.components.BackBar
import rw.hafiyawe.store.ui.components.ConfirmDialog
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ErrorNotice
import rw.hafiyawe.store.ui.components.InfoNote
import rw.hafiyawe.store.ui.components.PrimaryButton
import rw.hafiyawe.store.ui.components.RemoteImage
import rw.hafiyawe.store.ui.components.StatusPill
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes
import rw.hafiyawe.store.ui.rememberScreenState

@Composable
fun SellScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    val user = app.user

    if (user == null) {
        SignInPrompt(nav)
        return
    }

    var images by remember { mutableStateOf<List<Uri>>(emptyList()) }
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var category by remember { mutableStateOf<String?>(null) }
    var condition by remember { mutableStateOf<String?>("Good") }
    var province by remember { mutableStateOf(user.province.ifBlank { null }) }
    var district by remember { mutableStateOf(user.district.ifBlank { null }) }
    var localError by remember { mutableStateOf<String?>(null) }

    val picker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        images = (images + uris).take(6)
    }

    val planDays = daysLeft(user.planExpiresAt)
    val hasAccess = planDays > 0 || user.freePostsRemaining > 0 || user.postCredits > 0
    val canSubmit = user.emailVerified && user.hasCompleteProfile && hasAccess
    val noPhotosLabel = t("sell.noPhotos")
    val categoryLabel = t("sell.category")
    val conditionLabel = t("sell.condition")
    val locationLabel = t("sell.location")

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = t("sell.title"), onBack = { nav.back() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            (state.error ?: localError)?.let { ErrorNotice(it) }

            CrystalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(t("sell.quota"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = when {
                            planDays > 0 -> "Monthly plan · $planDays days left"
                            user.freePostsRemaining > 0 -> "${user.freePostsRemaining} ${t("profile.freePosts")}"
                            user.postCredits > 0 -> "${user.postCredits} ${t("profile.credits")}"
                            else -> "No posting access"
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!hasAccess) {
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { nav.open(Routes.PAYMENTS) }) {
                            Text(t("payments.title"))
                        }
                    }
                }
            }

            if (!user.emailVerified) {
                Spacer(Modifier.height(8.dp))
                InfoNote(t("sell.needEmail"))
                TextButton(onClick = { nav.open(Routes.VERIFY_PENDING) }) {
                    Text(t("profile.verifyEmail"))
                }
            }
            if (!user.hasCompleteProfile) {
                Spacer(Modifier.height(8.dp))
                InfoNote(t("sell.needProfile"))
                TextButton(onClick = { nav.open(Routes.PROFILE) }) { Text(t("profile.edit")) }
            }

            Spacer(Modifier.height(14.dp))
            Text(t("sell.photos"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                images.take(3).forEachIndexed { index, uri ->
                    Box {
                        AsyncImage(
                            model = uri,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(78.dp)
                                .clip(RoundedCornerShape(14.dp))
                        )
                        IconButton(
                            onClick = { images = images.filterIndexed { i, _ -> i != index } },
                            modifier = Modifier
                                .size(22.dp)
                                .align(Alignment.TopEnd)
                        ) {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = t("action.remove"),
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                if (images.size < 6) {
                    OutlinedButton(
                        onClick = { picker.launch("image/*") },
                        modifier = Modifier.height(78.dp)
                    ) {
                        Text(if (images.isEmpty()) t("action.upload") else "+${6 - images.size}")
                    }
                }
            }
            if (images.isNotEmpty()) {
                Text(
                    text = "${images.size}/6",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            if (app.demoMode) {
                Spacer(Modifier.height(4.dp))
                Text(
                    t("sell.demoPhotos"),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(14.dp))
            AppTextField(title, { title = it.take(120) }, t("sell.titleField"))
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = description,
                onValueChange = { description = it.take(4000) },
                label = t("sell.description"),
                singleLine = false,
                minLines = 4
            )
            Spacer(Modifier.height(10.dp))
            AppTextField(
                value = price,
                onValueChange = { price = it.filter { c -> c.isDigit() }.take(9) },
                label = t("sell.price"),
                keyboardType = KeyboardType.Number,
                supporting = price.toLongOrNull()?.let { rwf(it) }
            )
            Spacer(Modifier.height(12.dp))
            OptionPicker(t("sell.category"), category, Market.CATEGORIES, { category = it }, allowClear = false)
            Spacer(Modifier.height(8.dp))
            OptionPicker(t("sell.condition"), condition, Market.CONDITIONS, { condition = it }, allowClear = false)
            Spacer(Modifier.height(8.dp))
            OptionPicker(t("sell.province"), province, Market.PROVINCES, { province = it; district = null }, allowClear = false)
            if (province != null) {
                Spacer(Modifier.height(8.dp))
                OptionPicker(
                    label = t("sell.district"),
                    value = district,
                    options = Market.DISTRICTS[province] ?: emptyList(),
                    onChange = { district = it },
                    allowClear = false
                )
            }

            Spacer(Modifier.height(16.dp))
            PrimaryButton(
                label = t("sell.submit"),
                enabled = canSubmit,
                loading = state.busy,
                onClick = {
                    localError = when {
                        images.isEmpty() -> noPhotosLabel
                        title.trim().length < 4 -> "Add a clear title."
                        description.trim().length < 20 -> "Describe the item in at least 20 characters."
                        (price.toLongOrNull() ?: 0L) <= 0L -> "Enter a price in RWF."
                        category == null -> categoryLabel
                        condition == null -> conditionLabel
                        province == null || district == null -> locationLabel
                        else -> null
                    }
                    if (localError == null) {
                        state.launch {
                            val urls = app.catalog.uploadImages(images)
                            app.catalog.createListing(
                                ProductRequest(
                                    title = title.trim(),
                                    description = description.trim(),
                                    price = price.toLongOrNull() ?: 0L,
                                    category = category!!,
                                    condition = condition!!,
                                    province = province!!,
                                    district = district!!,
                                    images = urls
                                )
                            )
                            runCatching { app.catalog.refreshMine() }
                            app.toast("Submitted for review")
                            nav.open(Routes.MY_LISTINGS)
                        }
                    }
                }
            )
            Spacer(Modifier.height(8.dp))
            Text(
                t("sell.pendingNote"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
fun MyListingsScreen(nav: Navigator) {
    val app = LocalApp.current
    val listings by app.catalog.myListings.collectAsStateWithLifecycle(emptyList())
    val state = rememberScreenState()
    var pendingDelete by remember { mutableStateOf<Product?>(null) }

    if (app.user == null) {
        SignInPrompt(nav)
        return
    }

    LaunchedEffect(Unit) { runCatching { app.catalog.refreshMine() } }

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(
            title = t("mylistings.title"),
            onBack = { nav.back() },
            actions = {
                TextButton(onClick = { nav.open(Routes.SELL) }) { Text(t("action.post")) }
            }
        )
        state.error?.let { ErrorNotice(it) }

        if (listings.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.Storefront,
                title = t("mylistings.empty"),
                actionLabel = t("action.post"),
                onAction = { nav.open(Routes.SELL) }
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(listings, key = { it.id }) { product ->
                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(10.dp)) {
                            RemoteImage(
                                path = product.cover,
                                contentDescription = product.title,
                                label = product.title,
                                modifier = Modifier
                                    .width(84.dp)
                                    .aspectRatio(1f)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    product.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 2
                                )
                                Text(
                                    rwf(product.price),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    product.place,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(Modifier.height(4.dp))
                                StatusPill(product.status)
                                if (product.status == Market.STATUS_REJECTED &&
                                    !product.rejectionReason.isNullOrBlank()
                                ) {
                                    Spacer(Modifier.height(4.dp))
                                    Text(
                                        product.rejectionReason.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 10.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (product.status == Market.STATUS_ACTIVE) {
                                TextButton(onClick = {
                                    state.launch {
                                        app.catalog.setStatus(product.id, Market.STATUS_OFF_MARKET)
                                        app.catalog.refreshMine()
                                    }
                                }) { Text(t("mylistings.offMarket")) }
                            }
                            if (product.status == Market.STATUS_OFF_MARKET) {
                                TextButton(onClick = {
                                    state.launch {
                                        app.catalog.setStatus(product.id, Market.STATUS_ACTIVE)
                                        app.catalog.refreshMine()
                                    }
                                }) { Text(t("mylistings.restore")) }
                            }
                            TextButton(onClick = { pendingDelete = product }) {
                                Text(t("action.delete"), color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }

    pendingDelete?.let { product ->
        ConfirmDialog(
            title = t("mylistings.deleteTitle"),
            body = t("mylistings.deleteBody"),
            confirmLabel = t("action.delete"),
            destructive = true,
            onConfirm = {
                pendingDelete = null
                state.launch {
                    app.catalog.deleteListing(product.id)
                    app.catalog.refreshMine()
                }
            },
            onDismiss = { pendingDelete = null }
        )
    }

    if (state.busy) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(strokeWidth = 2.dp)
        }
    }
}
