package rw.hafiyawe.store.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import rw.hafiyawe.store.ui.LocalApp
import rw.hafiyawe.store.ui.components.AppTextField
import rw.hafiyawe.store.ui.components.CrystalCard
import rw.hafiyawe.store.ui.components.ErrorNotice
import rw.hafiyawe.store.ui.components.InfoNote
import rw.hafiyawe.store.ui.components.Pill
import rw.hafiyawe.store.ui.components.PillTone
import rw.hafiyawe.store.ui.components.PrimaryButton
import rw.hafiyawe.store.ui.components.SuccessNote
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.navigation.Routes
import rw.hafiyawe.store.ui.rememberScreenState

@Composable
private fun AuthShell(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Text("HAFIYAWE", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(6.dp))
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(4.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(18.dp))
        content()
    }
}

@Composable
fun LoginScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    var email by remember { mutableStateOf(app.graph.session.lastEmail) }
    var password by remember { mutableStateOf("") }

    AuthShell(t("auth.signInTitle"), t("auth.signInSubtitle")) {
        state.error?.let { ErrorNotice(it) }
        AppTextField(
            value = email,
            onValueChange = { email = it },
            label = t("auth.email"),
            keyboardType = KeyboardType.Email
        )
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = t("auth.password"),
            password = true
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            label = t("action.signIn"),
            loading = state.busy,
            enabled = email.isNotBlank() && password.isNotBlank(),
            onClick = {
                state.launch {
                    app.graph.auth.signIn(email, password)
                    app.refreshEverything()
                    nav.home()
                }
            }
        )
        Spacer(Modifier.height(6.dp))
        TextButton(
            onClick = { nav.open(Routes.FORGOT) },
            modifier = Modifier.fillMaxWidth()
        ) { Text(t("auth.forgot")) }

        Spacer(Modifier.height(14.dp))
        CrystalCard(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(t("auth.demo"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    t("auth.demoHint"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        state.launch {
                            app.graph.auth.enterDemo()
                            app.refreshEverything()
                            nav.home()
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) { Text(t("auth.demo")) }
            }
        }

        Spacer(Modifier.height(4.dp))
        TextButton(
            onClick = { nav.open(Routes.SETTINGS) },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                "${t("settings.server")}: ${app.baseUrl}",
                style = MaterialTheme.typography.labelMedium
            )
        }

        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(t("auth.noAccount"), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { nav.open(Routes.REGISTER) }) { Text(t("action.register")) }
        }
        Text(
            t("auth.verifyNote"),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun RegisterScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val passwordHint = t("auth.passwordHint")

    AuthShell(t("auth.registerTitle"), t("auth.registerSubtitle")) {
        (state.error ?: localError)?.let { ErrorNotice(it) }
        AppTextField(name, { name = it }, t("auth.name"))
        Spacer(Modifier.height(10.dp))
        AppTextField(email, { email = it }, t("auth.email"), keyboardType = KeyboardType.Email)
        Spacer(Modifier.height(10.dp))
        AppTextField(
            value = password,
            onValueChange = { password = it },
            label = t("auth.password"),
            supporting = passwordHint,
            password = true
        )
        Spacer(Modifier.height(16.dp))
        PrimaryButton(
            label = t("action.register"),
            loading = state.busy,
            onClick = {
                localError = when {
                    name.trim().length < 2 -> "Enter your full name."
                    !email.contains("@") -> "Enter a valid email address."
                    password.length < 10 -> passwordHint
                    else -> null
                }
                if (localError == null) {
                    state.launch {
                        val result = app.auth.signUp(name, email, password)
                        app.pendingVerificationUrl = result.verificationUrl
                        app.refreshEverything()
                        nav.open(Routes.VERIFY_PENDING)
                    }
                }
            }
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            Text(t("auth.haveAccount"), style = MaterialTheme.typography.bodyMedium)
            TextButton(onClick = { nav.open(Routes.LOGIN) }) { Text(t("action.signIn")) }
        }
    }
}

@Composable
fun ForgotPasswordScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    var email by remember { mutableStateOf(app.graph.session.lastEmail) }
    var resetUrl by remember { mutableStateOf<String?>(null) }
    var sent by remember { mutableStateOf(false) }

    AuthShell(t("auth.forgot"), t("auth.signInSubtitle")) {
        state.error?.let { ErrorNotice(it) }
        if (sent) {
            SuccessNote("If that email has an account, a reset link has been created.")
            Spacer(Modifier.height(10.dp))
            resetUrl?.let { url ->
                InfoNote("Development link:")
                Spacer(Modifier.height(6.dp))
                Text(url, style = MaterialTheme.typography.bodySmall)
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = {
                    val token = Uri.parse(url).getQueryParameter("token")
                    if (token != null) nav.open(Routes.reset(token))
                }) { Text("Open reset link") }
            }
        } else {
            AppTextField(email, { email = it }, t("auth.email"), keyboardType = KeyboardType.Email)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(t("action.send"), loading = state.busy, onClick = {
                state.launch {
                    val result = app.graph.auth.forgotPassword(email)
                    resetUrl = result.resetUrl
                    sent = true
                }
            })
        }
        Spacer(Modifier.height(12.dp))
        TextButton(onClick = { nav.open(Routes.LOGIN) }) { Text(t("action.signIn")) }
    }
}

@Composable
fun ResetPasswordScreen(nav: Navigator, token: String) {
    val app = LocalApp.current
    val state = rememberScreenState()
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }
    val passwordHint = t("auth.passwordHint")

    AuthShell(t("auth.password"), t("auth.registerSubtitle")) {
        (state.error ?: localError)?.let { ErrorNotice(it) }
        if (state.done) {
            SuccessNote("Password updated. Sign in with the new password.")
            Spacer(Modifier.height(14.dp))
            PrimaryButton(t("action.signIn"), onClick = { nav.open(Routes.LOGIN) })
        } else {
            AppTextField(password, { password = it }, t("auth.password"), password = true)
            Spacer(Modifier.height(10.dp))
            AppTextField(confirm, { confirm = it }, t("auth.password"), password = true)
            Spacer(Modifier.height(16.dp))
            PrimaryButton(t("action.save"), loading = state.busy, onClick = {
                localError = when {
                    password.length < 10 -> passwordHint
                    password != confirm -> "The two passwords do not match."
                    else -> null
                }
                if (localError == null) {
                    state.launch { app.graph.auth.resetPassword(token, password) }
                }
            })
        }
    }
}

@Composable
fun VerifyPendingScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    var cooldown by remember { mutableIntStateOf(0) }
    var devUrl by remember { mutableStateOf(app.pendingVerificationUrl) }

    LaunchedEffect(cooldown) {
        if (cooldown > 0) {
            delay(1000)
            cooldown -= 1
        }
    }

    AuthShell(t("profile.emailUnverified"), t("auth.verifyNote")) {
        state.error?.let { ErrorNotice(it) }
        if (state.done) SuccessNote("A fresh verification link was created.")
        Spacer(Modifier.height(8.dp))
        Text(
            text = app.user?.email.orEmpty(),
            style = MaterialTheme.typography.titleMedium
        )
        Spacer(Modifier.height(14.dp))
        PrimaryButton(
            label = if (cooldown > 0) "Resend in ${cooldown}s" else "Resend verification email",
            enabled = cooldown == 0,
            loading = state.busy,
            onClick = {
                state.launch {
                    val result = app.graph.auth.resendVerification()
                    cooldown = result.retryAfterSeconds.coerceAtLeast(30)
                    result.verificationUrl?.let { devUrl = it }
                }
            }
        )
        devUrl?.let { url ->
            Spacer(Modifier.height(14.dp))
            Pill("development", PillTone.Gold)
            Spacer(Modifier.height(6.dp))
            Text(url, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = {
                val token = Uri.parse(url).getQueryParameter("token")
                if (token != null) nav.open(Routes.verifyEmail(token))
            }) { Text("Open verification link") }
        }
        Spacer(Modifier.height(16.dp))
        TextButton(onClick = { nav.home() }) { Text(t("nav.home")) }
    }
}

@Composable
fun VerifyEmailScreen(nav: Navigator, token: String) {
    val app = LocalApp.current
    val state = rememberScreenState()

    LaunchedEffect(token) {
        state.launch {
            app.graph.auth.verifyEmail(token)
            app.refreshEverything()
        }
    }

    AuthShell(t("profile.verifyEmail"), t("auth.verifyNote")) {
        when {
            state.busy -> Text(t("common.loading"))
            state.error != null -> {
                ErrorNotice(state.error.orEmpty())
                Spacer(Modifier.height(12.dp))
                PrimaryButton(t("action.retry"), onClick = { state.reset(); state.launch {
                    app.graph.auth.verifyEmail(token)
                    app.refreshEverything()
                } })
            }
            else -> {
                SuccessNote("Your email is verified.")
                Spacer(Modifier.height(14.dp))
                PrimaryButton(t("action.done"), onClick = { nav.home() })
            }
        }
    }
}
