package com.example.ui.profile

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.HelpOutline
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.ReceiptLong
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import com.example.data.model.UserSubscription
import com.example.security.BiometricAuthManager
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.LiquidActionButton
import com.example.ui.theme.LiquidCard
import com.example.ui.theme.LiquidSecondaryButton
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusPill

@Composable
fun UserProfileScreen(
    displayName: String,
    email: String,
    isEmailVerified: Boolean,
    isAdmin: Boolean,
    subscription: UserSubscription?,
    biometricEnabled: Boolean,
    onNavigateToSubscriptionManagement: () -> Unit,
    onNavigateToPurchaseHistory: () -> Unit,
    onNavigateToSecuritySettings: () -> Unit,
    onNavigateToHelpSupport: () -> Unit,
    onNavigateToAdmin: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Account & Vault", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
        }

        item {
            LiquidCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(58.dp)
                            .clip(CircleShape)
                            .background(LiquidYellow),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = displayName.firstOrNull()?.uppercase() ?: "S",
                            style = MaterialTheme.typography.headlineMedium,
                            color = DeepBlack
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(displayName.ifBlank { "Student" }, style = MaterialTheme.typography.titleLarge, color = PureWhite)
                        Text(email, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatusPill(
                                label = if (isAdmin) "ADMIN" else "STUDENT",
                                containerColor = if (isAdmin) LiquidYellow else CharcoalElevated,
                                contentColor = if (isAdmin) DeepBlack else PureWhite
                            )
                            StatusPill(
                                label = if (isEmailVerified) "EMAIL VERIFIED" else "UNVERIFIED",
                                containerColor = if (isEmailVerified) EmeraldSuccess.copy(alpha = 0.2f) else CrimsonError.copy(alpha = 0.2f),
                                contentColor = if (isEmailVerified) EmeraldSuccess else CrimsonError
                            )
                        }
                    }
                }
            }
        }

        if (isAdmin) {
            item {
                ProfileMenuRow(
                    icon = Icons.Rounded.AdminPanelSettings,
                    title = "Admin Control Console",
                    subtitle = "Upload PDFs, verify purchases, manage plans & audit logs",
                    highlighted = true,
                    testTag = "profile_admin_button",
                    onClick = onNavigateToAdmin
                )
            }
        }

        item {
            ProfileMenuRow(
                icon = Icons.Rounded.WorkspacePremium,
                title = "Subscription Management",
                subtitle = if (subscription?.isCurrentlyActive() == true) {
                    "Active: ${subscription.planName}"
                } else "Free tier • Manage or restore purchases",
                testTag = "profile_subscription_button",
                onClick = onNavigateToSubscriptionManagement
            )
        }

        item {
            ProfileMenuRow(
                icon = Icons.Rounded.ReceiptLong,
                title = "Purchase History",
                subtitle = "View Google Play & PhonePe UPI verification records",
                testTag = "profile_history_button",
                onClick = onNavigateToPurchaseHistory
            )
        }

        item {
            ProfileMenuRow(
                icon = Icons.Rounded.Fingerprint,
                title = "App Security Settings",
                subtitle = if (biometricEnabled) "Biometric App Lock Enabled" else "Configure BiometricPrompt & auto-lock",
                testTag = "profile_security_button",
                onClick = onNavigateToSecuritySettings
            )
        }

        item {
            ProfileMenuRow(
                icon = Icons.AutoMirrored.Rounded.HelpOutline,
                title = "Help & Support",
                subtitle = "Security architecture FAQ, billing help & disclosures",
                testTag = "profile_help_button",
                onClick = onNavigateToHelpSupport
            )
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            LiquidSecondaryButton(
                text = "Sign Out",
                icon = Icons.AutoMirrored.Rounded.Logout,
                onClick = onSignOut,
                testTag = "profile_sign_out_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    highlighted: Boolean = false,
    testTag: String,
    onClick: () -> Unit
) {
    LiquidCard(
        highlighted = highlighted,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().testTag(testTag)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(LiquidYellow.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(imageVector = icon, contentDescription = null, tint = LiquidYellow)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
            }
            Icon(Icons.AutoMirrored.Rounded.KeyboardArrowRight, contentDescription = null, tint = MutedZinc)
        }
    }
}

@Composable
fun AppSecuritySettingsScreen(
    biometricEnabled: Boolean,
    inactivityTimeoutSec: Long,
    onToggleBiometric: (Boolean) -> Unit,
    onTimeoutChange: (Long) -> Unit,
    onLockVaultNow: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val capability = remember(context) { BiometricAuthManager.checkCapability(context) }
    var feedbackMsg by remember { mutableStateOf<String?>(null) }

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
            Text("App Security Settings", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        Spacer(modifier = Modifier.height(12.dp))

        LiquidCard(highlighted = biometricEnabled, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = LiquidYellow, modifier = Modifier.size(30.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Device Biometric Capability", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text(capability.statusMessage, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                StatusPill(
                    label = if (capability.hasFingerprintHardware) "FINGERPRINT HW" else "NO FINGERPRINT HW",
                    containerColor = CharcoalElevated,
                    contentColor = if (capability.hasFingerprintHardware) LiquidYellow else MutedZinc
                )
                StatusPill(
                    label = if (capability.hasFaceHardware) "FACE HW" else "NO FACE HW",
                    containerColor = CharcoalElevated,
                    contentColor = if (capability.hasFaceHardware) LiquidYellow else MutedZinc
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LiquidCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Biometric App Lock", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text(
                        "Require BiometricPrompt when returning from background or inactivity.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MutedZinc
                    )
                }
                Switch(
                    checked = biometricEnabled,
                    onCheckedChange = { wantEnable ->
                        if (!wantEnable) {
                            onToggleBiometric(false)
                            feedbackMsg = "App Lock disabled."
                        } else if (activity != null && (capability.isEnrolled || capability.supportsDeviceCredential)) {
                            BiometricAuthManager.authenticate(
                                activity = activity,
                                title = "Enable Secure Study App Lock",
                                subtitle = "Confirm your device biometric or screen lock",
                                onSuccess = {
                                    onToggleBiometric(true)
                                    feedbackMsg = "Biometric App Lock activated."
                                },
                                onError = { err -> feedbackMsg = "Biometric error: $err" },
                                onCancelled = { feedbackMsg = "Biometric confirmation cancelled." }
                            )
                        } else {
                            feedbackMsg = capability.statusMessage
                        }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DeepBlack,
                        checkedTrackColor = LiquidYellow
                    ),
                    modifier = Modifier.testTag("biometric_lock_switch")
                )
            }

            if (!feedbackMsg.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(feedbackMsg!!, style = MaterialTheme.typography.bodySmall, color = LiquidYellow)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        LiquidCard(modifier = Modifier.fillMaxWidth()) {
            Text("Inactivity Auto-Lock Delay", style = MaterialTheme.typography.titleMedium, color = PureWhite)
            Spacer(modifier = Modifier.height(6.dp))
            Text("Choose how quickly the vault locks after leaving the app.", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
            Spacer(modifier = Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(0L to "Immediate", 30L to "30s", 60L to "1 min", 300L to "5 min").forEach { (sec, label) ->
                    FilterChip(
                        selected = inactivityTimeoutSec == sec,
                        onClick = { onTimeoutChange(sec) },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiquidYellow,
                            selectedLabelColor = DeepBlack
                        )
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))
        LiquidCard(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.VerifiedUser, contentDescription = null, tint = EmeraldSuccess)
                Text(
                    text = "Hardware Privacy Guarantee: Biometric templates are processed solely inside Android's local hardware-backed keystore/TEE and are never uploaded or stored in Cloud Firestore.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedZinc
                )
            }
        }

        if (biometricEnabled) {
            Spacer(modifier = Modifier.height(18.dp))
            LiquidActionButton(
                text = "Lock Vault Now",
                icon = Icons.Rounded.Lock,
                onClick = onLockVaultNow,
                testTag = "lock_vault_now_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun HelpAndSupportScreen(
    onBack: () -> Unit
) {
    val faqs = listOf(
        "Why can't I take screenshots or share PDFs?" to
            "Secure Study enables Android FLAG_SECURE inside the PDF reader to block standard screenshots, screen recording, and recent-app previews, and removes share/print/export actions. Note that no software can prevent external physical cameras from photographing a screen, so dynamic account watermarks are overlaid on every page.",
        "How does subscription verification work?" to
            "Premium PDFs are protected by server-side Cloud Firestore Security Rules. Client devices cannot self-unlock subscriptions. Entitlements activate only after verified Google Play Billing or admin-verified PhonePe UPI transactions.",
        "How do I restore my Google Play subscription?" to
            "Open Subscription Management or Subscription Plans and tap 'Restore'. Google Play Billing queries your active Play Store subscriptions and submits the purchase token for verification.",
        "Where is my biometric data stored?" to
            "Nowhere outside your phone. Secure Study uses AndroidX BiometricPrompt, which delegates directly to your device's Trusted Execution Environment (TEE) / Secure Element."
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .widthIn(max = 640.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            Text("Help & Support", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        Spacer(modifier = Modifier.height(12.dp))

        LiquidCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.Security, contentDescription = null, tint = LiquidYellow)
                Column {
                    Text("Secure Study Support Desk", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Text("support@securestudy.app • Response within 24 hours", style = MaterialTheme.typography.bodySmall, color = LiquidYellow)
                }
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        faqs.forEach { (question, answer) ->
            var expanded by remember { mutableStateOf(false) }
            LiquidCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp),
                onClick = { expanded = !expanded }
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = question,
                        style = MaterialTheme.typography.titleMedium,
                        color = PureWhite,
                        modifier = Modifier.weight(1f)
                    )
                    Icon(
                        imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                        contentDescription = null,
                        tint = LiquidYellow
                    )
                }
                AnimatedVisibility(visible = expanded) {
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(answer, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                    }
                }
            }
        }
    }
}
