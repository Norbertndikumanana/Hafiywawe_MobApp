package rw.hafiyawe.store.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Send
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.data.SupportMessage
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.AppTextField
import rw.hafiyawe.store.ui.components.BackBar
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.EmptyState
import rw.hafiyawe.store.ui.components.ErrorNotice
import rw.hafiyawe.store.ui.components.InfoNote
import rw.hafiyawe.store.ui.components.Pill
import rw.hafiyawe.store.ui.components.PillTone
import rw.hafiyawe.store.ui.components.PrimaryButton
import rw.hafiyawe.store.ui.components.StatusPill
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes
import rw.hafiyawe.store.ui.rememberScreenState

@Composable
fun HelpScreen(nav: Navigator) {
    val app = LocalApp.current
    val articles by app.meta.help.collectAsStateWithLifecycle(emptyList())
    var expanded by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) { runCatching { app.meta.refreshHelp() } }

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = t("help.title"), onBack = { nav.back() })
        if (articles.isEmpty()) {
            EmptyState(icon = Icons.Filled.HelpOutline, title = t("help.empty"))
        } else {
            LazyColumn(contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(articles, key = { it.id }) { article ->
                    CrystalCard(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { expanded = if (expanded == article.id) null else article.id }
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Pill(article.category, PillTone.Gold)
                            Spacer(Modifier.height(6.dp))
                            Text(article.title, style = MaterialTheme.typography.titleMedium)
                            if (expanded == article.id) {
                                Spacer(Modifier.height(8.dp))
                                Text(article.body, style = MaterialTheme.typography.bodyMedium)
                                Spacer(Modifier.height(6.dp))
                                Text(
                                    "Updated ${Iso.date(article.updatedAt)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                item {
                    TextButton(onClick = { nav.open(Routes.CONTACT) }, modifier = Modifier.fillMaxWidth()) {
                        Text(t("profile.support"))
                    }
                }
            }
        }
    }
}

@Composable
fun ContactScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    val tickets by app.support.tickets.collectAsStateWithLifecycle(emptyList())
    var subject by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(app.user?.email.orEmpty()) }
    var showForm by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { runCatching { app.support.refresh() } }

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(
            title = t("support.title"),
            onBack = { nav.back() },
            actions = {
                if (!showForm) {
                    TextButton(onClick = { showForm = true }) { Text(t("support.new")) }
                }
            }
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            state.error?.let { ErrorNotice(it) }

            if (showForm) {
                CrystalCard(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(t("support.new"), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        if (app.user == null) {
                            AppTextField(
                                email,
                                { email = it },
                                t("auth.email"),
                                keyboardType = KeyboardType.Email
                            )
                            Spacer(Modifier.height(8.dp))
                        }
                        AppTextField(subject, { subject = it.take(150) }, t("support.subject"))
                        Spacer(Modifier.height(8.dp))
                        AppTextField(
                            value = description,
                            onValueChange = { description = it.take(5000) },
                            label = t("support.description"),
                            singleLine = false,
                            minLines = 4
                        )
                        Spacer(Modifier.height(10.dp))
                        PrimaryButton(t("action.send"), loading = state.busy, onClick = {
                            state.launch {
                                val result = app.support.create(
                                    email = email,
                                    subject = subject,
                                    description = description
                                )
                                description = ""
                                subject = ""
                                showForm = false
                                nav.ticket(result.ticket.id)
                            }
                        })
                        TextButton(onClick = { showForm = false }) { Text(t("action.cancel")) }
                    }
                }
                Spacer(Modifier.height(14.dp))
            }

            InfoNote(
                "Support is a private conversation inside HAFIYAWE. " +
                    "Replies appear here${if (app.user == null) " and in this request's private link" else ""}."
            )

            Spacer(Modifier.height(16.dp))
            Text(t("support.title"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (tickets.isEmpty()) {
                Text(
                    t("support.empty"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                tickets.forEach { ticket ->
                    CrystalCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp),
                        onClick = { nav.ticket(ticket.id) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    ticket.subject ?: ticket.description.take(48),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Pill(
                                    ticket.status,
                                    if (ticket.status == "open") PillTone.Warning else PillTone.Positive
                                )
                            }
                            Text(
                                ticket.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2
                            )
                            Text(
                                Iso.relative(ticket.updatedAt),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TicketScreen(nav: Navigator, ticketId: String) {
    val app = LocalApp.current
    val ticket by app.support.ticket(ticketId).collectAsStateWithLifecycle(null)
    val scope = rememberCoroutineScope()
    var draft by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(ticketId) {
        while (true) {
            runCatching { app.support.refreshTicket(ticketId) }.onFailure { error = it.message }
            delay(15_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = t("support.title"), onBack = { nav.back() })
        error?.let { ErrorNotice(it) }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            ticket?.let { current ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        current.subject ?: "Request",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f)
                    )
                    Pill(current.status, if (current.status == "open") PillTone.Warning else PillTone.Positive)
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    current.email,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(12.dp))

                val messages = if (current.messages.isEmpty()) {
                    listOf(
                        SupportMessage(
                            id = "root",
                            body = current.description,
                            authorRole = "user",
                            createdAt = current.createdAt
                        )
                    )
                } else current.messages

                messages.forEach { message ->
                    SupportBubble(message)
                    Spacer(Modifier.height(8.dp))
                }
            } ?: Text(t("common.loading"))
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
                    onValueChange = { draft = it.take(5000) },
                    modifier = Modifier.weight(1f),
                    placeholder = { Text(t("support.reply")) },
                    shape = MaterialTheme.shapes.large,
                    maxLines = 4
                )
                IconButton(
                    enabled = draft.isNotBlank(),
                    onClick = {
                        val body = draft
                        draft = ""
                        scope.launch {
                            runCatching { app.support.reply(ticketId, body) }
                                .onFailure { error = it.message }
                        }
                    }
                ) {
                    Icon(Icons.Filled.Send, contentDescription = t("action.send"))
                }
            }
        }
    }
}

@Composable
private fun SupportBubble(message: SupportMessage) {
    val fromAdmin = message.authorRole == "admin"
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (fromAdmin) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                if (fromAdmin) "HAFIYAWE support" else "You",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(4.dp))
            Text(message.body, style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                Iso.relative(message.createdAt),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
