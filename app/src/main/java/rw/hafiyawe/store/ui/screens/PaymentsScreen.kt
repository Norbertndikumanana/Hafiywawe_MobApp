package rw.hafiyawe.store.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.UploadFile
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil.compose.AsyncImage
import rw.hafiyawe.store.core.Iso
import rw.hafiyawe.store.core.daysLeft
import rw.hafiyawe.store.core.rwf
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
import rw.hafiyawe.store.ui.components.RemoteImage
import rw.hafiyawe.store.ui.components.StatusPill
import rw.hafiyawe.store.ui.i18n.t
import rw.hafiyawe.store.ui.navigation.Navigator
import rw.hafiyawe.store.ui.rememberScreenState

@Composable
fun PaymentsScreen(nav: Navigator) {
    val app = LocalApp.current
    val state = rememberScreenState()
    val user = app.user
    val config = app.config
    val payments by app.payments.payments.collectAsStateWithLifecycle(emptyList())
    val clipboard = LocalClipboardManager.current

    if (user == null) {
        SignInPrompt(nav)
        return
    }

    var kind by remember { mutableStateOf("single") }
    var reference by remember { mutableStateOf("") }
    var proof by remember { mutableStateOf<Uri?>(null) }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        proof = uri
    }

    LaunchedEffect(Unit) { runCatching { app.payments.refresh() } }

    val paymentsOpen = config.paymentsConfigured || app.demoMode
    val copiedLabel = t("action.copied")
    val planDays = daysLeft(user.planExpiresAt)

    Column(modifier = Modifier.fillMaxSize()) {
        BackBar(title = t("payments.title"), onBack = { nav.back() })

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            state.error?.let { ErrorNotice(it) }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                CrystalCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(t("payments.single"), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(rwf(config.singlePostPrice), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text(t("common.perListing"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                CrystalCard(modifier = Modifier.weight(1f)) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(t("payments.monthly"), style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(4.dp))
                        Text(rwf(config.monthlyPlanPrice), style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        Text(t("common.forDays"), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            CrystalCard(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(t("sell.quota"), style = MaterialTheme.typography.titleMedium)
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Pill("${user.freePostsRemaining} free", PillTone.Neutral)
                        Pill("${user.postCredits} credits", PillTone.Gold)
                        if (planDays > 0) Pill("plan · $planDays d", PillTone.Positive)
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            if (!paymentsOpen) {
                InfoNote(t("payments.notConfigured"))
            } else {
                if (app.demoMode) {
                    InfoNote(t("payments.demoNote"))
                    Spacer(Modifier.height(8.dp))
                }
                Text(t("payments.plans"), style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    PlanOption(
                        label = t("payments.single"),
                        price = rwf(config.singlePostPrice),
                        selected = kind == "single",
                        modifier = Modifier.weight(1f)
                    ) { kind = "single" }
                    PlanOption(
                        label = t("payments.monthly"),
                        price = rwf(config.monthlyPlanPrice),
                        selected = kind == "monthly",
                        modifier = Modifier.weight(1f)
                    ) { kind = "monthly" }
                }

                if (config.paymentInstructions.isNotBlank() ||
                    config.merchantCode.isNotBlank() ||
                    config.merchantName.isNotBlank()
                ) {
                    Spacer(Modifier.height(12.dp))
                    CrystalCard(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(t("payments.instructions"), style = MaterialTheme.typography.titleMedium)
                            if (config.paymentInstructions.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text(config.paymentInstructions, style = MaterialTheme.typography.bodyMedium)
                            }
                            if (config.merchantName.isNotBlank()) {
                                Spacer(Modifier.height(6.dp))
                                Text("${t("payments.merchant")}: ${config.merchantName}", style = MaterialTheme.typography.bodyMedium)
                            }
                            if (config.merchantCode.isNotBlank()) {
                                Spacer(Modifier.height(8.dp))
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            clipboard.setText(AnnotatedString(config.merchantCode))
                                            app.toast(copiedLabel)
                                        }
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        config.merchantCode,
                                        style = MaterialTheme.typography.headlineSmall,
                                        color = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(Icons.Filled.ContentCopy, contentDescription = t("action.copy"))
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))
                AppTextField(
                    value = reference,
                    onValueChange = { reference = it.uppercase().take(100) },
                    label = t("payments.reference"),
                    supporting = "6–100 letters, digits, - or _"
                )

                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { picker.launch("image/*") }
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(14.dp)
                        )
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (proof != null) {
                        AsyncImage(
                            model = proof,
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .padding(end = 10.dp)
                        )
                    } else {
                        Icon(
                            Icons.Filled.UploadFile,
                            contentDescription = null,
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Text(
                        text = if (proof == null) t("payments.receipt") else "Receipt attached",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(onClick = { picker.launch("image/*") }) { Text(t("action.upload")) }
                }

                Spacer(Modifier.height(14.dp))
                PrimaryButton(
                    label = t("payments.submit"),
                    loading = state.busy,
                    enabled = reference.length >= 6 && proof != null,
                    onClick = {
                        state.launch {
                            app.payments.submit(kind, reference, proof)
                            reference = ""
                            proof = null
                            app.toast("Payment submitted for review")
                            app.payments.refresh()
                        }
                    }
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    t("payments.pendingNote"),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(Modifier.height(22.dp))
            Text(t("payments.history"), style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(8.dp))

            if (payments.isEmpty()) {
                EmptyState(icon = Icons.Filled.UploadFile, title = t("payments.empty"))
            } else {
                payments.forEach { payment ->
                    CrystalCard(modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)) {
                        Row(modifier = Modifier.padding(12.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    if (payment.kind == "monthly") t("payments.monthly") else t("payments.single"),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    "${rwf(payment.amount)} · ${payment.reference}",
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
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                StatusPill(payment.status)
                                payment.proofUrl?.let { url ->
                                    Spacer(Modifier.height(6.dp))
                                    RemoteImage(
                                        path = url,
                                        contentDescription = t("payments.receipt"),
                                        modifier = Modifier
                                            .size(44.dp)
                                            .padding(2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(30.dp))
        }
    }
}

@Composable
private fun PlanOption(
    label: String,
    price: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier.clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer
        else MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(
            width = if (selected) 2.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.secondary
            else MaterialTheme.colorScheme.outline
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(2.dp))
            Text(price, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
    }
}

/** Kept for admin receipt previews. */
@Composable
fun ReceiptPreview(path: String?, modifier: Modifier = Modifier) {
    Box(modifier = modifier) {
        RemoteImage(path = path, contentDescription = null, modifier = Modifier.fillMaxSize())
    }
}
