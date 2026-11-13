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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.data.AppNotification
import rw.hafiyawe.store.data.ChatRoom
import rw.hafiyawe.store.data.Message
import rw.hafiyawe.store.data.isUnread
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.Avatar
import rw.hafiyawe.store.ui.components.BackBar
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ErrorNotice
import rw.hafiyawe.store.ui.components.RemoteImage
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes

@Composable
fun ChatsScreen(nav: Navigator) {
    val app = LocalApp.current
    val rooms by app.chats.rooms.collectAsStateWithLifecycle(emptyList())
    var error by remember { mutableStateOf<String?>(null) }

    if (app.user == null) {
        SignInPrompt(nav)
        return
    }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { app.chats.refreshRooms() }.onFailure { error = it.message }
            delay(15_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("chats.title"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
        }
        error?.let { ErrorNotice(it) }

        if (rooms.isEmpty()) {
            EmptyState(
                icon = Icons.Filled.ChatBubbleOutline,
                title = t("chats.empty"),
                actionLabel = t("nav.browse"),
                onAction = { nav.tab(Routes.BROWSE) }
            )
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                items(rooms, key = { it.id }) { room ->
                    RoomRow(room) { nav.chat(room.id) }
                }
            }
        }
    }
}

@Composable
private fun RoomRow(room: ChatRoom, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RemoteImage(
            path = room.productImage,
            contentDescription = room.productTitle,
            label = room.productTitle,
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(14.dp))
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(room.productTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
            Text(
                text = room.lastMessage ?: t("chat.empty"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
            Text(
                text = if (room.side == "buying") "You are buying · ${room.otherName}"
                else "You are selling · ${room.otherName}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(
                Iso.relative(room.updatedAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (room.unreadCount > 0) {
                Spacer(Modifier.height(4.dp))
                Badge { Text(if (room.unreadCount > 9) "9+" else room.unreadCount.toString()) }
            }
        }
    }
}

@Composable
fun ConversationScreen(nav: Navigator, roomId: String) {
    val app = LocalApp.current
    val room by app.chats.room(roomId).collectAsStateWithLifecycle(null)
    val messages by app.chats.messages(roomId).collectAsStateWithLifecycle(emptyList())
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val meId = app.user?.id

    LaunchedEffect(roomId) {
        while (true) {
            runCatching { app.chats.loadMessages(roomId) }.onFailure { error = it.message }
            delay(5_000)
        }
    }

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = room?.otherName ?: t("chats.title"), onBack = { nav.back() })
        room?.let { current ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { nav.product(current.productId) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RemoteImage(
                    path = current.productImage,
                    contentDescription = current.productTitle,
                    label = current.productTitle,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(current.productTitle, style = MaterialTheme.typography.titleMedium, maxLines = 1)
                    Text(
                        t("listing.location") + ": " + (if (current.side == "buying") "you are buying" else "you are selling"),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        error?.let { ErrorNotice(it) }

        Box(modifier = Modifier.weight(1f)) {
            if (messages.isEmpty()) {
                EmptyState(icon = Icons.Filled.ChatBubbleOutline, title = t("chat.empty"))
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(messages, key = { it.id }) { message ->
                        MessageBubble(message = message, mine = message.senderId == meId)
                    }
                }
            }
        }

        Surface(color = MaterialTheme.colorScheme.surface, shadowElevation = 6.dp) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .imePadding()
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = draft,
                    onValueChange = { draft = it.take(4000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(t("chat.sendHint")) },
                    shape = MaterialTheme.shapes.large,
                    maxLines = 4
                )
                IconButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        val body = draft
                        draft = ""
                        scope.launch {
                            runCatching { app.chats.send(roomId, body) }
                                .onFailure { error = it.message }
                        }
                    }
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = t("action.send"))
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: Message, mine: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (mine) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            shape = RoundedCornerShape(
                topStart = 16.dp,
                topEnd = 16.dp,
                bottomStart = if (mine) 16.dp else 4.dp,
                bottomEnd = if (mine) 4.dp else 16.dp
            ),
            color = if (mine) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth(0.82f)
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.body,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (mine) MaterialTheme.colorScheme.onPrimaryContainer
                    else MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = Iso.relative(message.createdAt),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun NotificationsScreen(nav: Navigator) {
    val app = LocalApp.current
    val items by app.notifications.items.collectAsStateWithLifecycle(emptyList())
    val scope = rememberCoroutineScope()

    if (app.user == null) {
        SignInPrompt(nav)
        return
    }

    LaunchedEffect(Unit) {
        while (true) {
            runCatching { app.notifications.refresh() }
            delay(20_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 18.dp, end = 8.dp, top = 14.dp, bottom = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(t("alerts.title"), style = MaterialTheme.typography.headlineSmall, modifier = Modifier.weight(1f))
            if (items.any { it.isUnread }) {
                TextButton(onClick = { scope.launch { runCatching { app.notifications.markAllRead() } } }) {
                    Text(t("action.markAll"))
                }
            }
        }

        if (items.isEmpty()) {
            EmptyState(icon = Icons.Filled.NotificationsNone, title = t("alerts.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(bottom = 20.dp)) {
                items(items, key = { it.id }) { notification ->
                    NotificationRow(notification) {
                        scope.launch {
                            runCatching { app.notifications.markRead(notification.id) }
                        }
                        navigateNotification(nav, notification)
                    }
                }
            }
        }
    }
}

@Composable
private fun NotificationRow(notification: AppNotification, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .background(
                if (notification.isUnread) MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                else MaterialTheme.colorScheme.background
            )
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notificationIcon(notification.type),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                notification.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = if (notification.isUnread) FontWeight.SemiBold else FontWeight.Normal
            )
            Text(
                notification.body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                Iso.relative(notification.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun notificationIcon(type: String): ImageVector = when (type) {
    "message" -> Icons.Filled.ChatBubbleOutline
    "payment_approved", "payment_rejected" -> Icons.Filled.Payments
    "identity_verified", "identity_unverified" -> Icons.Filled.VerifiedUser
    "support_reply" -> Icons.Filled.SupportAgent
    "listing_approved", "listing_rejected" -> Icons.Filled.Storefront
    else -> Icons.Filled.NotificationsNone
}

/** Server hrefs are web paths; map them onto Android destinations. */
private fun navigateNotification(nav: Navigator, notification: AppNotification) {
    val href = notification.href ?: return
    when {
        href.startsWith("/chats/") -> nav.chat(href.removePrefix("/chats/"))
        href.startsWith("/chats") -> nav.tab(Routes.CHATS)
        href.startsWith("/products/") -> nav.product(href.removePrefix("/products/"))
        href.startsWith("/payments") -> nav.open(Routes.PAYMENTS)
        href.startsWith("/my-listings") -> nav.open(Routes.MY_LISTINGS)
        href.startsWith("/profile") -> nav.tab(Routes.PROFILE)
        href.startsWith("/contact") -> nav.open(Routes.CONTACT)
        else -> Unit
    }
}

/** Small helper used by the notifications bell in other screens. */
@Composable
fun NotificationBell(count: Int, onClick: () -> Unit) {
    Box {
        IconButton(onClick = onClick) {
            Icon(
                Icons.Filled.NotificationsNone,
                contentDescription = t("alerts.title"),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (count > 0) {
            Badge(modifier = Modifier.align(Alignment.TopEnd).padding(top = 6.dp, end = 4.dp)) {
                Text(if (count > 9) "9+" else count.toString())
            }
        }
    }
}
