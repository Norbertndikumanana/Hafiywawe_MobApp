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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.core.daysLeft
import rw.hafiyawe.store.data.Market
import rw.hafiyawe.store.data.hasCompleteProfile
import rw.hafiyawe.store.data.isAdmin
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.AppTextField
import rw.hafiyawe.store.ui.components.Avatar
import rw.hafiyawe.store.ui.components.BackBar
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.ErrorNotice
import rw.hafiyawe.store.ui.components.InfoNote
import rw.hafiyawe.store.ui.components.Pill
import rw.hafiyawe.store.ui.components.PillTone
import rw.hafiyawe.store.ui.components.PrimaryButton
import rw.hafiyawe.store.ui.components.StatTile
import rw.hafiyawe.store.ui.i18n.LANGUAGES
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes
import rw.hafiyawe.store.ui.rememberScreenState

@Composable
fun ProfileScreen(nav: Navigator) {
    val app = LocalApp.current
    val user = app.user
    var editing by remember { mutableStateOf(false) }

    if (user == null) {
        SignInPrompt(nav)
        return
    }

    LaunchedEffect(Unit) { runCatching { app.graph.auth.refreshMe() } }

    val planDays = daysLeft(user.planExpiresAt)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 30.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Avatar(user.name, size = 56.dp)
                Spacer(Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(user.name.ifBlank { "HAFIYAWE member" }, style = MaterialTheme.typography.titleLarge)
                    Text(
                        user.email,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (user.isAdmin) Pill("admin", PillTone.Gold)
                        Pill(
                            if (user.emailVerified) t("profile.emailVerified")
                            else t("profile.emailUnverified"),
                            if (user.emailVerified) PillTone.Positive else PillTone.Warning
                        )
                    }
                }
            }
        }

        if (!user.emailVerified) {
            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                    InfoNote(t("sell.needEmail"))
                    TextButton(onClick = { nav.open(Routes.VERIFY_PENDING) }) {
                        Text(t("profile.verifyEmail"))
                    }
                }
            }
        }

        if (user.isBanned) {
            item {
                Column(modifier = Modifier.padding(horizontal = 18.dp, vertical = 4.dp)) {
                    InfoNote(
                        "${t("accountStatus.banned")}: ${user.banReason}",
                        tone = PillTone.Danger
                    )
                    TextButton(onClick = { nav.open(Routes.ACCOUNT_STATUS) }) {
                        Text(t("profile.status"))
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatTile(
                    label = t("profile.freePosts"),
                    value = user.freePostsRemaining.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = t("profile.credits"),
                    value = user.postCredits.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatTile(
                    label = t("profile.plan"),
                    value = if (planDays > 0) "$planDays d" else "—",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            CrystalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            t("profile.identity"),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.weight(1f)
                        )
                        Pill(
                            if (user.identityVerified) t("profile.identityVerified")
                            else t("profile.identityPending"),
                            if (user.identityVerified) PillTone.Positive else PillTone.Warning
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = listOf(
                            user.phone.ifBlank { "No phone yet" },
                            listOf(user.district, user.province).filter { it.isNotBlank() }
                                .joinToString(", ").ifBlank { "No location yet" }
                        ).joinToString(" · "),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (!user.hasCompleteProfile) {
                        Spacer(Modifier.height(6.dp))
                        Text(
                            t("sell.needProfile"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    TextButton(onClick = { editing = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(t("profile.edit"))
                    }
                }
            }
        }

        item {
            CrystalCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 6.dp)
            ) {
                Column {
                    MenuRow(Icons.Filled.Storefront, t("profile.mylistings")) {
                        nav.open(Routes.MY_LISTINGS)
                    }
                    MenuRow(Icons.Filled.Favorite, t("profile.favorites")) {
                        nav.open(Routes.FAVORITES)
                    }
                    MenuRow(Icons.Filled.ShoppingCart, t("profile.cart")) {
                        nav.open(Routes.CART)
                    }
                    MenuRow(Icons.Filled.Payments, t("profile.payments")) {
                        nav.open(Routes.PAYMENTS)
                    }
                    MenuRow(Icons.Filled.VerifiedUser, t("profile.status")) {
                        nav.open(Routes.ACCOUNT_STATUS)
                    }
                    MenuRow(Icons.Filled.HelpOutline, t("profile.help")) {
                        nav.open(Routes.HELP)
                    }
                    MenuRow(Icons.Filled.SupportAgent, t("profile.support")) {
                        nav.open(Routes.CONTACT)
                    }
                    MenuRow(Icons.Filled.Settings, t("profile.settings")) {
                        nav.open(Routes.SETTINGS)
                    }
                    if (user.isAdmin) {
                        MenuRow(Icons.Filled.AdminPanelSettings, t("profile.admin")) {
                            nav.open(Routes.ADMIN)
                        }
                    }
                }
            }
        }

        item {
            TextButton(
                onClick = { app.signOut(); nav.home() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 12.dp)
            ) {
                Icon(Icons.Filled.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(t("action.signOut"))
            }
        }
    }

    if (editing) {
        EditProfileDialog(
            onDismiss = { editing = false },
            onSaved = {
                editing = false
                app.toast("Profile saved")
            }
        )
    }
}

@Composable
private fun MenuRow(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, modifier = Modifier.size(19.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
        Icon(
            Icons.Filled.ChevronRight,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EditProfileDialog(onDismiss: () -> Unit, onSaved: () -> Unit) {
    val app = LocalApp.current
    val state = rememberScreenState()
    val user = app.user
    var name by remember { mutableStateOf(user?.name.orEmpty()) }
    var phone by remember { mutableStateOf(user?.phone.orEmpty()) }
    var nationalId by remember { mutableStateOf("") }
    var province by remember { mutableStateOf(user?.province.orEmpty().ifBlank { null }) }
    var district by remember { mutableStateOf(user?.district.orEmpty().ifBlank { null }) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(t("profile.edit")) },
        text = {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .height(420.dp)
            ) {
                state.error?.let { ErrorNotice(it) }
                AppTextField(name, { name = it }, t("auth.name"))
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    phone,
                    { phone = it },
                    t("profile.phone"),
                    placeholder = "078 000 0000",
                    keyboardType = KeyboardType.Phone
                )
                Spacer(Modifier.height(8.dp))
                AppTextField(
                    nationalId,
                    { nationalId = it.filter { c -> c.isDigit() }.take(16) },
                    t("profile.nationalId"),
                    supporting = user?.nationalIdLast4?.takeIf { it.isNotBlank() }
                        ?.let { "Saved ID ends with $it" },
                    keyboardType = KeyboardType.Number
                )
                Spacer(Modifier.height(10.dp))
                OptionPicker(
                    label = t("profile.province"),
                    value = province,
                    options = Market.PROVINCES,
                    onChange = { province = it; district = null }
                )
                if (province != null) {
                    Spacer(Modifier.height(6.dp))
                    OptionPicker(
                        label = t("profile.district"),
                        value = district,
                        options = Market.DISTRICTS[province] ?: listOf("Gasabo", "Kicukiro", "Nyarugenge"),
                        onChange = { district = it }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !state.busy,
                onClick = {
                    state.launch {
                        app.graph.auth.updateProfile(
                            name = name.trim(),
                            phone = phone.trim().ifBlank { null },
                            nationalId = nationalId.trim().takeIf { it.length == 16 },
                            province = province,
                            district = district
                        )
                        onSaved()
                    }
                }
            ) { Text(t("action.save")) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(t("action.cancel")) }
        }
    )
}

@Composable
fun SettingsScreen(nav: Navigator) {
    val app = LocalApp.current
    var server by remember { mutableStateOf(app.baseUrl) }
    var savedMessage by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        BackBar(title = t("settings.title"), onBack = { nav.back() })

        Column(modifier = Modifier.padding(18.dp)) {
            Text(t("settings.server"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            AppTextField(
                value = server,
                onValueChange = { server = it },
                label = "http://10.0.2.2:3001",
                supporting = t("settings.serverHint")
            )
            Spacer(Modifier.height(10.dp))
            PrimaryButton(t("action.save"), onClick = {
                val clean = server.trim().trimEnd('/')
                if (clean.startsWith("http://") || clean.startsWith("https://")) {
                    app.setBaseUrl(clean)
                    savedMessage = "Server saved. Pull to refresh to reload."
                } else {
                    savedMessage = "The address must start with http:// or https://"
                }
            })
            savedMessage?.let {
                Spacer(Modifier.height(8.dp))
                InfoNote(it)
            }

            Spacer(Modifier.height(22.dp))
            HorizontalDivider()
            Spacer(Modifier.height(14.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(t("settings.demo"), style = MaterialTheme.typography.titleMedium)
                    Text(
                        t("settings.demoHint"),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = app.demoMode,
                    onCheckedChange = { enabled ->
                        if (enabled) app.enterDemo() else app.leaveDemo()
                    }
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(t("settings.language"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                LANGUAGES.forEach { (code, label) ->
                    FilterChip(
                        selected = app.language == code,
                        onClick = { app.useLanguage(code) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            Text(t("settings.theme"), style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    "system" to t("settings.themeSystem"),
                    "light" to t("settings.themeLight"),
                    "dark" to t("settings.themeDark")
                ).forEach { (mode, label) ->
                    FilterChip(
                        selected = app.themeMode == mode,
                        onClick = { app.useTheme(mode) },
                        label = { Text(label) }
                    )
                }
            }

            Spacer(Modifier.height(22.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            TextButton(onClick = { nav.about("about") }) { Text(t("settings.about")) }
            TextButton(onClick = { nav.about("trust") }) { Text(t("settings.trust")) }
            TextButton(onClick = { nav.about("terms") }) { Text(t("settings.terms")) }
            TextButton(onClick = { nav.about("privacy") }) { Text(t("settings.privacy")) }
        }
    }
}

@Composable
fun AccountStatusScreen(nav: Navigator) {
    val app = LocalApp.current
    val user = app.user
    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = t("accountStatus.title"), onBack = { nav.back() })
        Column(modifier = Modifier.padding(20.dp)) {
            if (user == null) {
                SignInPrompt(nav)
                return@Column
            }
            if (user.isBanned) {
                Pill(t("accountStatus.banned"), PillTone.Danger)
                Spacer(Modifier.height(12.dp))
                Text(t("accountStatus.reason"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(user.banReason, style = MaterialTheme.typography.bodyLarge)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = Iso.dateTime(user.bannedAt),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(16.dp))
                InfoNote("Your listings stay in your account and your posting balances are unchanged.")
                Spacer(Modifier.height(16.dp))
                PrimaryButton(t("accountStatus.appeal"), onClick = { nav.open(Routes.CONTACT) })
            } else {
                Pill(t("accountStatus.ok"), PillTone.Positive)
                Spacer(Modifier.height(14.dp))
                PrimaryButton(t("action.done"), onClick = { nav.back() })
            }
        }
    }
}

@Composable
fun AboutScreen(nav: Navigator, page: String) {
    val title = when (page) {
        "trust" -> t("settings.trust")
        "terms" -> t("settings.terms")
        "privacy" -> t("settings.privacy")
        else -> t("settings.about")
    }
    val body = when (page) {
        "trust" -> TRUST_TEXT
        "terms" -> TERMS_TEXT
        "privacy" -> PRIVACY_TEXT
        else -> ABOUT_TEXT
    }
    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = title, onBack = { nav.back() })
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp)
        ) {
            Text(body, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(20.dp))
            Text(
                text = "HAFIYAWE · Kigali, Rwanda",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private const val ABOUT_TEXT =
    "HAFIYAWE is an independent second-hand marketplace for Rwanda. People list the things they no " +
        "longer need; buyers browse, search, save what they like and start a conversation.\n\n" +
        "The price of an item is always paid directly to the seller after you have seen it. HAFIYAWE " +
        "never holds that money and does not arrange delivery. The only thing the platform charges " +
        "for is posting slots: your first two listings are free, then 100 RWF per listing or " +
        "2,000 RWF for 30 days of unlimited posting."

private const val TRUST_TEXT =
    "Every listing is reviewed by a person before it appears in Browse.\n\n" +
        "Meet in a busy public place during daylight. Take someone with you. Inspect the item " +
        "carefully — check that a phone switches on, that a laptop charges, that furniture is steady — " +
        "before any money changes hands. Never send a deposit for something you have not seen.\n\n" +
        "If something feels wrong, report it through Contact support. Accounts that ask for payment " +
        "before inspection can be suspended."

private const val TERMS_TEXT =
    "Use HAFIYAWE only for lawful sales of items you own. Do not post stolen goods, prohibited " +
        "items, or anything that is not yours to sell.\n\n" +
        "You are responsible for the accuracy of your listing: photos, condition and price. " +
        "Administrators may reject a listing or suspend an account that breaks these rules. " +
        "Rejection and deletion never refund a posting slot.\n\n" +
        "Payment for posting slots is verified manually. A reference number can only be used once."

private const val PRIVACY_TEXT =
    "Your national ID is stored encrypted and only its last four digits are ever shown back to you " +
        "or to an administrator reviewing your identity.\n\n" +
        "Your phone number, district and province are visible on your listings so buyers can reach " +
        "you. Your email address is not published. Payment receipts are private: only you and the " +
        "reviewing administrator can open them.\n\n" +
        "In this Android build, searches and listing views are also stored on your own device to " +
        "order your home feed. That history never leaves the phone."
