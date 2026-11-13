package rw.hafiyawe.store.ui.screens

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.core.rwf
import rw.hafiyawe.store.data.AdminStats
import rw.hafiyawe.store.data.Announcement
import rw.hafiyawe.store.data.AnnouncementRequest
import rw.hafiyawe.store.data.AuditEntry
import rw.hafiyawe.store.data.HelpArticle
import rw.hafiyawe.store.data.HelpRequest
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.Payment
import rw.hafiyawe.store.data.Product
import rw.hafiyawe.store.data.SupportTicket
import rw.hafiyawe.store.data.User
import rw.hafiyawe.store.data.cover
import rw.hafiyawe.store.data.isAdmin
import rw.hafiyawe.store.data.place
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.AppTextField
import rw.hafiyawe.store.ui.components.Avatar
import rw.hafiyawe.store.ui.components.BackBar
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ErrorNotice
import rw.hafiyawe.store.ui.components.InfoNote
import rw.hafiyawe.store.ui.components.Pill
import rw.hafiyawe.store.ui.components.PillTone
import rw.hafiyawe.store.ui.components.RemoteImage
import rw.hafiyawe.store.ui.components.StatTile
import rw.hafiyawe.store.ui.components.StatusPill
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator

@Composable
fun AdminScreen(nav: Navigator) {
    val app = LocalApp.current
    if (app.user == null) {
        SignInPrompt(nav)
        return
    }
    if (!app.isAdmin) {
        EmptyState(
            icon = Icons.Filled.Lock,
            title = "Administrator access only",
            body = "This account does not have administrator rights."
        )
        return
    }

    var tab by remember { mutableIntStateOf(0) }
    val tabs = listOf(
        "admin.overview", "admin.listings", "admin.payments", "admin.users",
        "admin.support", "admin.announcements", "admin.help", "admin.audit"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = t("admin.title"), onBack = { nav.back() })
        ScrollableTabRow(
            selectedTabIndex = tab,
            edgePadding = 8.dp,
            containerColor = MaterialTheme.colorScheme.background
        ) {
            tabs.forEachIndexed { index, key ->
                Tab(
                    selected = tab == index,
                    onClick = { tab = index },
                    text = { Text(t(key), maxLines = 1) }
                )
            }
        }
        when (tab) {
            0 -> AdminOverview(onOpenTab = { tab = it })
            1 -> AdminListings()
            2 -> AdminPayments()
            3 -> AdminUsers()
            4 -> AdminSupport()
            5 -> AdminAnnouncements()
            6 -> AdminHelp()
            else -> AdminAudit()
        }
    }
}

@Composable
private fun AdminOverview(onOpenTab: (Int) -> Unit) {
    val app = LocalApp.current
    var stats by remember { mutableStateOf<AdminStats?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        runCatching { app.admin.stats() }
            .onSuccess { stats = it }
            .onFailure { error = it.message }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        error?.let { ErrorNotice(it) }
        if (app.demoMode) InfoNote(t("admin.needsServer"))
        Spacer(Modifier.height(8.dp))
        val value = stats
        if (value == null) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(strokeWidth = 2.dp)
            }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(t("admin.pendingProducts"), value.pendingProducts.toString(), Modifier.weight(1f))
                StatTile(t("admin.pendingPayments"), value.pendingPayments.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(t("admin.usersCount"), value.users.toString(), Modifier.weight(1f))
                StatTile(t("admin.activeProducts"), value.activeProducts.toString(), Modifier.weight(1f))
            }
            Spacer(Modifier.height(18.dp))
            TextButton(onClick = { onOpenTab(1) }, modifier = Modifier.fillMaxWidth()) {
                Text("${t("admin.listings")} (${value.pendingProducts})")
            }
            TextButton(onClick = { onOpenTab(2) }, modifier = Modifier.fillMaxWidth()) {
                Text("${t("admin.payments")} (${value.pendingPayments})")
            }
            Spacer(Modifier.height(10.dp))
            InfoNote(t("payments.pendingNote"))
        }
    }
}

@Composable
private fun AdminListings() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("pending") }
    var items by remember { mutableStateOf<List<Product>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var rejecting by remember { mutableStateOf<Product?>(null) }

    fun reload() {
        scope.launch {
            busy = true
            runCatching { app.admin.products(status) }
                .onSuccess { items = it; error = null }
                .onFailure { error = it.message }
            busy = false
        }
    }

    LaunchedEffect(status) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("pending", "active", "rejected", "all").forEach { option ->
                FilterChip(
                    selected = status == option,
                    onClick = { status = option },
                    label = { Text(option) }
                )
            }
        }
        error?.let { ErrorNotice(it, onRetry = { reload() }) }

        if (items.isEmpty() && !busy) {
            EmptyState(icon = Icons.Filled.Storefront, title = t("admin.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { product ->
                    AdminListingCard(
                        product = product,
                        onApprove = {
                            scope.launch {
                                runCatching { app.admin.decideProduct(product.id, "active", null) }
                                    .onSuccess { reload() }
                                    .onFailure { error = it.message }
                            }
                        },
                        onReject = { rejecting = product }
                    )
                }
            }
        }
    }

    rejecting?.let { product ->
        ReasonDialog(
            title = t("action.reject"),
            label = t("admin.reason"),
            confirmLabel = t("action.reject"),
            onDismiss = { rejecting = null },
            onConfirm = { reason ->
                rejecting = null
                scope.launch {
                    runCatching { app.admin.decideProduct(product.id, "rejected", reason) }
                        .onSuccess { reload() }
                        .onFailure { error = it.message }
                }
            }
        )
    }
}

@Composable
private fun AdminListingCard(product: Product, onApprove: () -> Unit, onReject: () -> Unit) {
    CrystalCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(10.dp)) {
            RemoteImage(
                path = product.cover,
                contentDescription = product.title,
                label = product.title,
                modifier = Modifier
                    .size(74.dp)
                    .padding(end = 10.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(product.title, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Text(rwf(product.price), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                Text(
                    "${product.category} · ${product.condition} · ${product.place}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${product.sellerName} · ${Iso.relative(product.createdAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                StatusPill(product.status)
                if (!product.rejectionReason.isNullOrBlank()) {
                    Text(
                        product.rejectionReason.orEmpty(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        if (product.status == Market.STATUS_PENDING) {
            Row(modifier = Modifier.padding(horizontal = 8.dp)) {
                TextButton(onClick = onApprove) { Text(t("action.approve")) }
                TextButton(onClick = onReject) {
                    Text(t("action.reject"), color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun AdminPayments() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("pending") }
    var items by remember { mutableStateOf<List<Payment>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var rejecting by remember { mutableStateOf<Payment?>(null) }
    var preview by remember { mutableStateOf<String?>(null) }

    fun reload() {
        scope.launch {
            runCatching { app.admin.payments(status) }
                .onSuccess { items = it; error = null }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(status) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("pending", "approved", "rejected", "all").forEach { option ->
                FilterChip(selected = status == option, onClick = { status = option }, label = { Text(option) })
            }
        }
        error?.let { ErrorNotice(it, onRetry = { reload() }) }

        if (items.isEmpty()) {
            EmptyState(icon = Icons.Filled.Payments, title = t("admin.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(items, key = { it.id }) { payment ->
                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${payment.userName} · ${rwf(payment.amount)}",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "${payment.kind} · ${payment.reference}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    Iso.dateTime(payment.createdAt),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (!payment.rejectionReason.isNullOrBlank()) {
                                    Text(
                                        payment.rejectionReason.orEmpty(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(Modifier.height(4.dp))
                                StatusPill(payment.status)
                                if (payment.status == "pending") {
                                    Row {
                                        TextButton(onClick = {
                                            scope.launch {
                                                runCatching { app.admin.decidePayment(payment.id, "approved", null) }
                                                    .onSuccess { reload() }
                                                    .onFailure { error = it.message }
                                            }
                                        }) { Text(t("action.approve")) }
                                        TextButton(onClick = { rejecting = payment }) {
                                            Text(t("action.reject"), color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                            if (payment.proofUrl != null) {
                                RemoteImage(
                                    path = payment.proofUrl,
                                    contentDescription = t("payments.receipt"),
                                    modifier = Modifier
                                        .size(64.dp)
                                        .padding(start = 6.dp)
                                )
                                TextButton(onClick = { preview = payment.proofUrl }) {
                                    Text(t("payments.receipt"))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    rejecting?.let { payment ->
        ReasonDialog(
            title = t("action.reject"),
            label = t("admin.reason"),
            confirmLabel = t("action.reject"),
            onDismiss = { rejecting = null },
            onConfirm = { reason ->
                rejecting = null
                scope.launch {
                    runCatching { app.admin.decidePayment(payment.id, "rejected", reason) }
                        .onSuccess { reload() }
                        .onFailure { error = it.message }
                }
            }
        )
    }

    preview?.let { url ->
        AlertDialog(
            onDismissRequest = { preview = null },
            title = { Text(t("payments.receipt")) },
            text = { RemoteImage(path = url, contentDescription = null, modifier = Modifier.fillMaxWidth()) },
            confirmButton = { TextButton(onClick = { preview = null }) { Text(t("action.close")) } }
        )
    }
}

@Composable
private fun AdminUsers() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var users by remember { mutableStateOf<List<User>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var banTarget by remember { mutableStateOf<User?>(null) }

    fun reload() {
        scope.launch {
            runCatching { app.admin.users() }
                .onSuccess { users = it; error = null }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        error?.let { ErrorNotice(it, onRetry = { reload() }) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(users, key = { it.id }) { person ->
                CrystalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Avatar(person.name)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(person.name, style = MaterialTheme.typography.titleMedium)
                                Text(
                                    person.email,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (person.isAdmin) Pill("admin", PillTone.Gold)
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Pill(
                                if (person.identityVerified) "identity ✓" else "identity pending",
                                if (person.identityVerified) PillTone.Positive else PillTone.Warning
                            )
                            if (person.isBanned) Pill("banned", PillTone.Danger)
                        }
                        if (person.banReason.isNotBlank()) {
                            Text(
                                person.banReason,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                        Row {
                            if (!person.isAdmin) {
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { app.admin.verifyIdentity(person.id, !person.identityVerified) }
                                            .onSuccess { reload() }
                                            .onFailure { error = it.message }
                                    }
                                }) {
                                    Text(if (person.identityVerified) "Unverify" else t("action.verify"))
                                }
                                if (person.isBanned) {
                                    TextButton(onClick = {
                                        scope.launch {
                                            runCatching { app.admin.setAccess(person.id, false, null) }
                                                .onSuccess { reload() }
                                                .onFailure { error = it.message }
                                        }
                                    }) { Text(t("action.unban")) }
                                } else {
                                    TextButton(onClick = { banTarget = person }) {
                                        Text(t("action.ban"), color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    banTarget?.let { person ->
        ReasonDialog(
            title = t("action.ban"),
            label = t("admin.banReason"),
            confirmLabel = t("action.ban"),
            onDismiss = { banTarget = null },
            onConfirm = { reason ->
                banTarget = null
                scope.launch {
                    runCatching { app.admin.setAccess(person.id, true, reason) }
                        .onSuccess { reload() }
                        .onFailure { error = it.message }
                }
            }
        )
    }
}

@Composable
private fun AdminSupport() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var status by remember { mutableStateOf("open") }
    var query by remember { mutableStateOf("") }
    var tickets by remember { mutableStateOf<List<SupportTicket>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var open by remember { mutableStateOf<SupportTicket?>(null) }

    fun reload() {
        scope.launch {
            runCatching { app.admin.support(status, query) }
                .onSuccess { tickets = it; error = null }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(status, query) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("open", "resolved", "all").forEach { option ->
                FilterChip(selected = status == option, onClick = { status = option }, label = { Text(option) })
            }
        }
        SearchField(value = query, onValueChange = { query = it }, onSubmit = { reload() })
        error?.let { ErrorNotice(it, onRetry = { reload() }) }

        if (tickets.isEmpty()) {
            EmptyState(icon = Icons.Filled.SupportAgent, title = t("admin.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(tickets, key = { it.id }) { ticket ->
                    CrystalCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { open = ticket }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    ticket.subject ?: "Request",
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Pill(ticket.status, if (ticket.status == "open") PillTone.Warning else PillTone.Positive)
                            }
                            Text(
                                ticket.email,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                ticket.description,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 2
                            )
                        }
                    }
                }
            }
        }
    }

    open?.let { ticket ->
        AdminTicketDialog(
            ticket = ticket,
            onDismiss = { open = null },
            onChanged = { reload() }
        )
    }
}

@Composable
private fun AdminTicketDialog(
    ticket: SupportTicket,
    onDismiss: () -> Unit,
    onChanged: () -> Unit
) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var reply by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var current by remember { mutableStateOf(ticket) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(current.subject ?: "Support request") },
        text = {
            Column(
                modifier = Modifier
                    .height(380.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                error?.let { ErrorNotice(it) }
                Text(current.email, style = MaterialTheme.typography.labelSmall)
                Spacer(Modifier.height(6.dp))
                val messages = if (current.messages.isEmpty()) {
                    listOf(
                        rw.hafiyawe.store.data.SupportMessage(
                            id = "root",
                            body = current.description,
                            authorRole = "user",
                            createdAt = current.createdAt
                        )
                    )
                } else current.messages
                messages.forEach { message ->
                    Text(
                        text = if (message.authorRole == "admin") "HAFIYAWE: ${message.body}"
                        else "Member: ${message.body}",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        Iso.relative(message.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                }
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = reply,
                    onValueChange = { reply = it.take(5000) },
                    label = t("support.reply"),
                    singleLine = false,
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = reply.isNotBlank(),
                onClick = {
                    val body = reply
                    reply = ""
                    scope.launch {
                        runCatching { app.admin.replySupport(current.id, body) }
                            .onSuccess { current = it; onChanged() }
                            .onFailure { error = it.message }
                    }
                }
            ) { Text(t("action.send")) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    val next = if (current.status == "open") "resolved" else "open"
                    scope.launch {
                        runCatching { app.admin.setSupportStatus(current.id, next) }
                            .onSuccess { current = it; onChanged() }
                            .onFailure { error = it.message }
                    }
                }) {
                    Text(if (current.status == "open") "Resolve" else "Reopen")
                }
                TextButton(onClick = onDismiss) { Text(t("action.close")) }
            }
        }
    )
}

@Composable
private fun AdminAnnouncements() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<Announcement>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<Announcement?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            runCatching { app.admin.announcements() }
                .onSuccess { items = it; error = null }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            TextButton(onClick = { creating = true }) { Text(t("support.new")) }
        }
        error?.let { ErrorNotice(it, onRetry = { reload() }) }
        if (items.isEmpty()) {
            EmptyState(icon = Icons.Filled.NotificationsNone, title = t("admin.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(items, key = { it.id }) { announcement ->
                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    announcement.title,
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Pill(
                                    if (announcement.enabled) "published" else "draft",
                                    if (announcement.enabled) PillTone.Positive else PillTone.Neutral
                                )
                            }
                            Text(announcement.body, style = MaterialTheme.typography.bodySmall)
                            Text(
                                "${announcement.kind} · order ${announcement.sortOrder} · ${Iso.date(announcement.updatedAt)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row {
                                TextButton(onClick = { editing = announcement }) { Text(t("action.edit")) }
                                TextButton(onClick = {
                                    scope.launch {
                                        runCatching { app.admin.deleteAnnouncement(announcement.id) }
                                            .onSuccess { reload() }
                                            .onFailure { error = it.message }
                                    }
                                }) { Text(t("action.delete"), color = MaterialTheme.colorScheme.error) }
                            }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        AnnouncementDialog(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSaved = { creating = false; editing = null; reload() }
        )
    }
}

@Composable
private fun AnnouncementDialog(
    existing: Announcement?,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var kind by remember { mutableStateOf(existing?.kind ?: "info") }
    var ctaLabel by remember { mutableStateOf(existing?.ctaLabel.orEmpty()) }
    var href by remember { mutableStateOf(existing?.href.orEmpty()) }
    var sortOrder by remember { mutableStateOf((existing?.sortOrder ?: 100).toString()) }
    var enabled by remember { mutableStateOf(existing?.enabled ?: false) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) t("support.new") else t("action.edit")) },
        text = {
            Column(
                modifier = Modifier
                    .height(420.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                error?.let { ErrorNotice(it) }
                AppTextField(title, { title = it.take(120) }, t("support.subject"))
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = body,
                    onValueChange = { body = it.take(600) },
                    label = t("support.description"),
                    singleLine = false,
                    minLines = 3
                )
                Spacer(Modifier.height(8.dp))
                OptionPicker(
                    label = "Kind",
                    value = kind,
                    options = listOf("info", "feature", "offer", "event"),
                    allowClear = false,
                    onChange = { kind = it ?: "info" }
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(ctaLabel, { ctaLabel = it.take(50) }, "Button label")
                Spacer(Modifier.height(8.dp))
                AppTextField(href, { href = it.take(200) }, "Button link", placeholder = "/browse")
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it.filter { c -> c.isDigit() }.take(4) },
                    label = "Display order",
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("Publish now", modifier = Modifier.weight(1f))
                    Switch(checked = enabled, onCheckedChange = { enabled = it })
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().length < 3 || body.trim().length < 3) {
                    error = "Title and body need at least 3 characters."
                    return@TextButton
                }
                scope.launch {
                    runCatching {
                        app.admin.saveAnnouncement(
                            existing,
                            AnnouncementRequest(
                                title = title.trim(),
                                body = body.trim(),
                                kind = kind,
                                ctaLabel = ctaLabel.trim().ifBlank { null },
                                href = href.trim().ifBlank { null },
                                enabled = enabled,
                                sortOrder = sortOrder.toIntOrNull() ?: 100
                            )
                        )
                    }.onSuccess { onSaved() }.onFailure { error = it.message }
                }
            }) { Text(t("action.save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("action.cancel")) } }
    )
}

@Composable
private fun AdminHelp() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var items by remember { mutableStateOf<List<HelpArticle>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var editing by remember { mutableStateOf<HelpArticle?>(null) }
    var creating by remember { mutableStateOf(false) }

    fun reload() {
        scope.launch {
            runCatching { app.admin.helpArticles() }
                .onSuccess { items = it; error = null }
                .onFailure { error = it.message }
        }
    }

    LaunchedEffect(Unit) { reload() }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)) {
            TextButton(onClick = { creating = true }) { Text(t("support.new")) }
        }
        error?.let { ErrorNotice(it, onRetry = { reload() }) }
        LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items, key = { it.id }) { article ->
                CrystalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Pill(article.category, PillTone.Gold)
                        Spacer(Modifier.height(4.dp))
                        Text(article.title, style = MaterialTheme.typography.titleMedium)
                        Text(
                            article.body,
                            style = MaterialTheme.typography.bodySmall,
                            maxLines = 2,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row {
                            TextButton(onClick = { editing = article }) { Text(t("action.edit")) }
                            TextButton(onClick = {
                                scope.launch {
                                    runCatching { app.admin.deleteHelp(article.id) }
                                        .onSuccess { reload() }
                                        .onFailure { error = it.message }
                                }
                            }) { Text(t("action.delete"), color = MaterialTheme.colorScheme.error) }
                        }
                    }
                }
            }
        }
    }

    if (creating || editing != null) {
        HelpDialog(
            existing = editing,
            onDismiss = { creating = false; editing = null },
            onSaved = { creating = false; editing = null; reload() }
        )
    }
}

@Composable
private fun HelpDialog(existing: HelpArticle?, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var title by remember { mutableStateOf(existing?.title.orEmpty()) }
    var body by remember { mutableStateOf(existing?.body.orEmpty()) }
    var category by remember { mutableStateOf(existing?.category.orEmpty().ifBlank { "Buying" }) }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) t("support.new") else t("action.edit")) },
        text = {
            Column(
                modifier = Modifier
                    .height(400.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                error?.let { ErrorNotice(it) }
                AppTextField(title, { title = it.take(120) }, t("support.subject"))
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    value = body,
                    onValueChange = { body = it.take(5000) },
                    label = t("support.description"),
                    singleLine = false,
                    minLines = 6
                )
                Spacer(Modifier.height(8.dp))
                OptionPicker(
                    label = t("sell.category"),
                    value = category,
                    options = listOf("Buying", "Selling", "Safety", "Payments", "Account"),
                    allowClear = false,
                    onChange = { category = it ?: "Buying" }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                if (title.trim().length < 3 || body.trim().length < 10) {
                    error = "Add a title and a fuller explanation."
                    return@TextButton
                }
                scope.launch {
                    runCatching {
                        app.admin.saveHelp(existing, HelpRequest(title.trim(), body.trim(), category))
                    }.onSuccess { onSaved() }.onFailure { error = it.message }
                }
            }) { Text(t("action.save")) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("action.cancel")) } }
    )
}

@Composable
private fun AdminAudit() {
    val app = LocalApp.current
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var entries by remember { mutableStateOf<List<AuditEntry>>(emptyList()) }
    var total by remember { mutableIntStateOf(0) }
    var error by remember { mutableStateOf<String?>(null) }
    var searched by remember { mutableStateOf(false) }

    fun search(page: Int = 1) {
        scope.launch {
            runCatching { app.admin.auditSearch(query, "", "", "", page) }
                .onSuccess { entries = it.items; total = it.total; searched = true; error = null }
                .onFailure { error = it.message; searched = true }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            SearchField(
                value = query,
                onValueChange = { query = it },
                placeholder = "Search actions, accounts, references",
                onSubmit = { search() }
            )
        }
        error?.let { ErrorNotice(it, onRetry = { search() }) }
        if (app.demoMode) {
            InfoNote(t("admin.auditDemo"))
        }
        if (searched && entries.isEmpty() && error == null) {
            EmptyState(icon = Icons.Filled.List, title = t("admin.empty"))
        } else {
            if (total > 0) {
                Text(
                    "$total records",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)
                )
            }
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                items(entries, key = { it.id }) { entry ->
                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(entry.action, style = MaterialTheme.typography.titleSmall)
                            Text(
                                listOfNotNull(
                                    entry.actorName ?: entry.actorEmail,
                                    entry.userName ?: entry.userEmail,
                                    entry.targetId
                                ).joinToString(" · "),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                Iso.dateTime(entry.createdAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            entry.details?.let {
                                Text(
                                    it.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    maxLines = 3,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReasonDialog(
    title: String,
    label: String,
    confirmLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (String?) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                AppTextField(
                    value = reason,
                    onValueChange = { reason = it.take(1000) },
                    label = label,
                    singleLine = false,
                    minLines = 3,
                    supporting = "${reason.length}/1000"
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(reason.trim().ifBlank { null }) }) { Text(confirmLabel) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(t("action.cancel")) } }
    )
}
