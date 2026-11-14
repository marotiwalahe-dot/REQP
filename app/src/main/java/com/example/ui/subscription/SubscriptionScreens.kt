package com.example.ui.subscription

import android.app.Activity
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CreditCard
import androidx.compose.material.icons.rounded.HourglassTop
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.ShoppingBag
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.billing.PlayBillingManager
import com.example.data.model.PurchaseRecord
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserSubscription
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ErrorRecoveryBanner
import com.example.ui.theme.LiquidActionButton
import com.example.ui.theme.LiquidCard
import com.example.ui.theme.LiquidProgressIndicator
import com.example.ui.theme.LiquidSecondaryButton
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusPill
import com.example.viewmodel.PaymentStatusUi
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun SubscriptionPlansScreen(
    plansState: UiState<List<SubscriptionPlan>>,
    billingManager: PlayBillingManager,
    userSubscription: UserSubscription?,
    onBack: () -> Unit,
    onSubmitPhonePeUtr: (SubscriptionPlan, String) -> Unit,
    onNavigateToPaymentStatus: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val productMap by billingManager.productDetailsMap.collectAsState()
    var phonePeSelectedPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }
    var utrInput by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Text("Pro Subscription Plans", style = MaterialTheme.typography.titleLarge, color = PureWhite)
            }
            TextButton(
                onClick = onRestorePurchases,
                modifier = Modifier.testTag("restore_purchases_button")
            ) {
                Icon(Icons.Rounded.Restore, contentDescription = null, tint = LiquidYellow, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Restore", color = LiquidYellow)
            }
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            if (userSubscription?.isCurrentlyActive() == true) {
                item {
                    LiquidCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Icon(Icons.Rounded.Verified, contentDescription = null, tint = LiquidYellow)
                            Column {
                                Text("Active Plan: ${userSubscription.planName}", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                Text("Your premium study entitlement is active and verified.", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                            }
                        }
                    }
                }
            }

            when (plansState) {
                is UiState.Loading -> item { LiquidProgressIndicator() }
                is UiState.Error -> item {
                    ErrorRecoveryBanner(title = "Could not load plans", message = plansState.message)
                }
                is UiState.Success -> {
                    val plans = plansState.data
                    if (plans.isEmpty()) {
                        item {
                            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                                Text("No subscription plans are currently available.", color = MutedZinc)
                            }
                        }
                    } else {
                        items(plans, key = { it.id }) { plan ->
                            val playDetails = productMap[plan.googlePlayProductId]
                            val localizedPrice = playDetails
                                ?.subscriptionOfferDetails
                                ?.firstOrNull()
                                ?.pricingPhases
                                ?.pricingPhaseList
                                ?.firstOrNull()
                                ?.formattedPrice
                                ?: "₹${plan.displayPriceInr}"

                            LiquidCard(
                                highlighted = plan.billingPeriod == "quarterly" || plan.billingPeriod == "yearly",
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        if (plan.badge.isNotBlank()) {
                                            StatusPill(
                                                label = plan.badge,
                                                containerColor = LiquidYellow,
                                                contentColor = DeepBlack
                                            )
                                            Spacer(modifier = Modifier.height(8.dp))
                                        }
                                        Text(plan.name, style = MaterialTheme.typography.headlineSmall, color = PureWhite)
                                        Text(
                                            text = "${plan.durationDays} days • ${plan.billingPeriod.replaceFirstChar { it.uppercase() }}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MutedZinc
                                        )
                                    }
                                    Text(
                                        text = localizedPrice,
                                        style = MaterialTheme.typography.displaySmall,
                                        color = LiquidYellow,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Spacer(modifier = Modifier.height(12.dp))
                                plan.features.forEach { feat ->
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                                        modifier = Modifier.padding(vertical = 3.dp)
                                    ) {
                                        Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = LiquidYellow, modifier = Modifier.size(16.dp))
                                        Text(feat, style = MaterialTheme.typography.bodySmall, color = PureWhite)
                                    }
                                }

                                Spacer(modifier = Modifier.height(14.dp))
                                LiquidActionButton(
                                    text = "Subscribe with Google Play",
                                    icon = Icons.Rounded.ShoppingBag,
                                    testTag = "play_subscribe_${plan.billingPeriod}",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        if (activity != null) {
                                            val launched = billingManager.launchSubscriptionPurchase(activity, plan.googlePlayProductId)
                                            if (!launched) {
                                                onNavigateToPaymentStatus()
                                            }
                                        }
                                    }
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                LiquidSecondaryButton(
                                    text = "Pay via PhonePe UPI QR",
                                    icon = Icons.Rounded.QrCode2,
                                    testTag = "phonepe_subscribe_${plan.billingPeriod}",
                                    modifier = Modifier.fillMaxWidth(),
                                    onClick = {
                                        utrInput = ""
                                        phonePeSelectedPlan = plan
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (phonePeSelectedPlan != null) {
        val selected = phonePeSelectedPlan!!
        AlertDialog(
            onDismissRequest = { phonePeSelectedPlan = null },
            containerColor = ObsidianSurface,
            titleContentColor = PureWhite,
            title = {
                Text("Pay ₹${selected.displayPriceInr} via PhonePe UPI", style = MaterialTheme.typography.titleLarge)
            },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.phonepe_upi_qr),
                        contentDescription = "PhonePe UPI QR Code for Kunal Pramod Thakur",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(PureWhite)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Scan & pay ₹${selected.displayPriceInr} using PhonePe / UPI, then submit your 12-digit UTR / Reference ID below. Entitlement activates only after server/admin verification.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedZinc,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = utrInput,
                        onValueChange = { utrInput = it },
                        label = { Text("UPI UTR / Transaction Ref ID") },
                        placeholder = { Text("e.g. 431892014567") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LiquidYellow,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("upi_utr_input")
                    )
                }
            },
            confirmButton = {
                LiquidActionButton(
                    text = "Submit for Verification",
                    enabled = utrInput.trim().length >= 6,
                    testTag = "submit_upi_button",
                    onClick = {
                        onSubmitPhonePeUtr(selected, utrInput.trim())
                        phonePeSelectedPlan = null
                        onNavigateToPaymentStatus()
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { phonePeSelectedPlan = null }) {
                    Text("Cancel", color = MutedZinc)
                }
            }
        )
    }
}

@Composable
fun PaymentStatusScreen(
    paymentStatus: PaymentStatusUi,
    userSubscription: UserSubscription?,
    onBackToPlans: () -> Unit,
    onDone: () -> Unit
) {
    val isActive = userSubscription?.isCurrentlyActive() == true

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        LiquidCard(
            highlighted = isActive || paymentStatus is PaymentStatusUi.PendingVerification,
            modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                when {
                    isActive -> {
                        Icon(Icons.Rounded.CheckCircle, contentDescription = "Active", tint = EmeraldSuccess, modifier = Modifier.size(60.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Subscription Activated", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Your Pro plan (${userSubscription?.planName}) is verified and unlocked.", style = MaterialTheme.typography.bodyMedium, color = MutedZinc, textAlign = TextAlign.Center)
                    }
                    paymentStatus is PaymentStatusUi.Verifying -> {
                        LiquidProgressIndicator()
                        Spacer(modifier = Modifier.height(18.dp))
                        Text("Submitting Purchase...", style = MaterialTheme.typography.headlineSmall, color = PureWhite)
                        Text("Recording transaction for server-side verification.", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                    }
                    paymentStatus is PaymentStatusUi.PendingVerification -> {
                        Icon(Icons.Rounded.HourglassTop, contentDescription = "Pending Verification", tint = LiquidYellow, modifier = Modifier.size(56.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Pending Verification", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Transaction ${paymentStatus.purchaseId} for ${paymentStatus.planName} (${paymentStatus.paymentMethod}) has been securely logged.\n\nPremium access unlocks automatically as soon as server/admin verification completes.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MutedZinc,
                            textAlign = TextAlign.Center
                        )
                    }
                    paymentStatus is PaymentStatusUi.Cancelled -> {
                        Icon(Icons.Rounded.CreditCard, contentDescription = "Cancelled", tint = LiquidYellow, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Checkout Cancelled", style = MaterialTheme.typography.headlineSmall, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(paymentStatus.message, style = MaterialTheme.typography.bodyMedium, color = MutedZinc, textAlign = TextAlign.Center)
                    }
                    paymentStatus is PaymentStatusUi.Failed -> {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Payment Error", tint = CrimsonError, modifier = Modifier.size(52.dp))
                        Spacer(modifier = Modifier.height(14.dp))
                        Text("Verification Notice", style = MaterialTheme.typography.headlineSmall, color = PureWhite)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(paymentStatus.message, style = MaterialTheme.typography.bodyMedium, color = MutedZinc, textAlign = TextAlign.Center)
                    }
                    else -> {
                        Text("No Active Checkout", style = MaterialTheme.typography.titleLarge, color = PureWhite)
                        Text("Select a plan to subscribe or restore purchases.", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                    }
                }

                Spacer(modifier = Modifier.height(22.dp))
                LiquidActionButton(
                    text = if (isActive) "Open Library" else "Back to Dashboard",
                    onClick = onDone,
                    testTag = "payment_status_done_button",
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                LiquidSecondaryButton(
                    text = "View Plans",
                    onClick = onBackToPlans,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun SubscriptionManagementScreen(
    subscription: UserSubscription?,
    isAdmin: Boolean,
    onBack: () -> Unit,
    onUpgrade: () -> Unit,
    onRestorePurchases: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()) }
    val active = subscription?.isCurrentlyActive() == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 600.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text("Subscription Management", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        Spacer(modifier = Modifier.height(14.dp))

        LiquidCard(highlighted = active || isAdmin, modifier = Modifier.fillMaxWidth()) {
            StatusPill(
                label = when {
                    isAdmin -> "ADMIN"
                    active -> "ACTIVE"
                    subscription != null -> subscription.status.uppercase()
                    else -> "NO ACTIVE PLAN"
                },
                containerColor = if (active || isAdmin) LiquidYellow else CharcoalElevated,
                contentColor = if (active || isAdmin) DeepBlack else MutedZinc
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = subscription?.planName ?: if (isAdmin) "Admin Entitlement" else "Free Plan",
                style = MaterialTheme.typography.headlineMedium,
                color = PureWhite
            )
            Spacer(modifier = Modifier.height(6.dp))
            if (subscription != null) {
                val started = subscription.startedAt?.toDate()?.let { dateFormat.format(it) } ?: "-"
                val expires = subscription.expiresAt?.toDate()?.let { dateFormat.format(it) } ?: "-"
                Text("Source: ${subscription.source}", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                Text("Started: $started", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                Text("Expires: $expires", style = MaterialTheme.typography.bodySmall, color = LiquidYellow)
                Text("Auto-renewing: ${if (subscription.autoRenewing) "Yes" else "No"}", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
            } else {
                Text(
                    "No active paid subscription is linked to your account. Subscribe to unlock premium study materials.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedZinc
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LiquidActionButton(
            text = if (active) "Change or Extend Plan" else "Browse Pro Plans",
            onClick = onUpgrade,
            testTag = "manage_plans_button",
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))
        LiquidSecondaryButton(
            text = "Restore Google Play Purchases",
            icon = Icons.Rounded.Restore,
            onClick = onRestorePurchases,
            testTag = "manage_restore_button",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PurchaseHistoryScreen(
    purchasesState: UiState<List<PurchaseRecord>>,
    onBack: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text("Purchase History", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (purchasesState) {
                is UiState.Loading -> item { LiquidProgressIndicator() }
                is UiState.Error -> item {
                    ErrorRecoveryBanner(title = "Could not load purchase history", message = purchasesState.message)
                }
                is UiState.Success -> {
                    val itemsList = purchasesState.data
                    if (itemsList.isEmpty()) {
                        item {
                            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Box(
                                        modifier = Modifier.size(44.dp).clip(CircleShape).background(CharcoalElevated),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Rounded.ReceiptLong, contentDescription = null, tint = LiquidYellow)
                                    }
                                    Column {
                                        Text("No Transactions Yet", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                        Text("Your Google Play and UPI payment records will appear here.", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                                    }
                                }
                            }
                        }
                    } else {
                        items(itemsList, key = { it.id }) { purchase ->
                            val statusColor = when (purchase.status) {
                                "verified" -> EmeraldSuccess
                                "pending_verification" -> LiquidYellow
                                else -> CrimsonError
                            }
                            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(purchase.planName, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                                    StatusPill(
                                        label = purchase.status.replace('_', ' ').uppercase(),
                                        containerColor = statusColor.copy(alpha = 0.18f),
                                        contentColor = statusColor
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "₹${purchase.amountInr} • ${purchase.paymentMethod}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = LiquidYellow
                                )
                                Text(
                                    text = "Ref: ${purchase.purchaseToken.take(28)}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MutedZinc
                                )
                                val ts = purchase.createdAt?.toDate()?.let { dateFormat.format(it) } ?: "Pending timestamp"
                                Text(text = ts, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                            }
                        }
                    }
                }
            }
        }
    }
}
