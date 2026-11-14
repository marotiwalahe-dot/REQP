package com.example.ui.admin

import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.util.Base64
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.SecondaryIndicator
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.AuditLog
import com.example.data.model.PdfAccessLog
import com.example.data.model.PdfDocumentMeta
import com.example.data.model.PurchaseRecord
import com.example.data.model.StudyCategory
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.security.PdfGenerator
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
import java.util.UUID

@Composable
fun AdminDashboardScreen(
    isAdmin: Boolean,
    categoriesState: UiState<List<StudyCategory>>,
    documentsState: UiState<List<PdfDocumentMeta>>,
    plansState: UiState<List<SubscriptionPlan>>,
    usersState: UiState<List<UserProfile>>,
    subscriptionsState: UiState<List<UserSubscription>>,
    purchasesState: UiState<List<PurchaseRecord>>,
    accessLogsState: UiState<List<PdfAccessLog>>,
    auditLogsState: UiState<List<AuditLog>>,
    actionMessage: String?,
    isWorking: Boolean,
    onApprovePurchase: (PurchaseRecord, List<SubscriptionPlan>) -> Unit,
    onUpdatePurchaseStatus: (PurchaseRecord, String, String?) -> Unit,
    onToggleUserSuspension: (UserProfile) -> Unit,
    onToggleAdminRole: (UserProfile) -> Unit,
    onUploadPdf: (
        docId: String,
        title: String,
        description: String,
        category: StudyCategory,
        accessTier: String,
        isFeatured: Boolean,
        pageCount: Long,
        fileSizeLabel: String,
        pdfBase64: String,
        pageTexts: List<String>
    ) -> Unit,
    onTogglePdfPublish: (PdfDocumentMeta) -> Unit,
    onTogglePdfTier: (PdfDocumentMeta) -> Unit,
    onSavePlan: (SubscriptionPlan) -> Unit,
    onSeedCatalog: (Int, Int, Int) -> Unit,
    onClearMessage: () -> Unit,
    onBack: () -> Unit
) {
    if (!isAdmin) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PureWhite)
            }
            ErrorRecoveryBanner(
                title = "Admin Permission Error",
                message = "Your account does not hold verified administrator privileges."
            )
        }
        return
    }

    var selectedTab by remember { mutableIntStateOf(0) }
    var showUploadDialog by remember { mutableStateOf(false) }
    var editingPlan by remember { mutableStateOf<SubscriptionPlan?>(null) }

    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val docs = (documentsState as? UiState.Success)?.data.orEmpty()
    val plans = (plansState as? UiState.Success)?.data.orEmpty()
    val users = (usersState as? UiState.Success)?.data.orEmpty()
    val subs = (subscriptionsState as? UiState.Success)?.data.orEmpty()
    val purchases = (purchasesState as? UiState.Success)?.data.orEmpty()
    val accessLogs = (accessLogsState as? UiState.Success)?.data.orEmpty()
    val auditLogs = (auditLogsState as? UiState.Success)?.data.orEmpty()

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = PureWhite)
                }
                Text("Admin Console", style = MaterialTheme.typography.titleLarge, color = PureWhite)
            }
            StatusPill(label = "ADMIN VERIFIED", containerColor = LiquidYellow, contentColor = DeepBlack)
        }

        if (isWorking) {
            LiquidProgressIndicator(modifier = Modifier.padding(horizontal = 16.dp))
        }

        if (!actionMessage.isNullOrBlank()) {
            LiquidCard(
                highlighted = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp),
                onClick = onClearMessage
            ) {
                Text(actionMessage, style = MaterialTheme.typography.bodySmall, color = LiquidYellow)
            }
        }

        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = ObsidianSurface,
            contentColor = LiquidYellow,
            indicator = { tabPositions ->
                SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                    color = LiquidYellow
                )
            }
        ) {
            listOf("Catalog", "Payments", "Users & Logs").forEachIndexed { idx, label ->
                Tab(
                    selected = selectedTab == idx,
                    onClick = { selectedTab = idx },
                    text = { Text(label, color = if (selectedTab == idx) LiquidYellow else MutedZinc) }
                )
            }
        }

        when (selectedTab) {
            0 -> AdminCatalogTab(
                categories = categories,
                docs = docs,
                usersCount = users.size,
                activeSubsCount = subs.count { it.isCurrentlyActive() },
                expiredSubsCount = subs.count { !it.isCurrentlyActive() },
                pendingPaymentsCount = purchases.count { it.status == "pending_verification" },
                onSeedCatalog = { onSeedCatalog(categories.size, plans.size, docs.size) },
                onOpenUploadDialog = { showUploadDialog = true },
                onTogglePublish = onTogglePdfPublish,
                onToggleTier = onTogglePdfTier
            )
            1 -> AdminPaymentsAndPlansTab(
                purchases = purchases,
                plans = plans,
                onApprovePurchase = { onApprovePurchase(it, plans) },
                onRejectPurchase = { onUpdatePurchaseStatus(it, "rejected", null) },
                onRefundPurchase = { onUpdatePurchaseStatus(it, "refunded", "revoked") },
                onEditPlan = { editingPlan = it },
                onTogglePlanAvailability = { plan ->
                    onSavePlan(plan.copy(isAvailable = !plan.isAvailable))
                }
            )
            2 -> AdminUsersAndLogsTab(
                users = users,
                subs = subs,
                accessLogs = accessLogs,
                auditLogs = auditLogs,
                onToggleSuspension = onToggleUserSuspension,
                onToggleAdmin = onToggleAdminRole
            )
        }
    }

    if (showUploadDialog) {
        AdminUploadPdfDialog(
            categories = categories,
            onDismiss = { showUploadDialog = false },
            onSubmit = { title, desc, cat, tier, featured, pageCount, sizeLabel, base64, pageTexts ->
                val docId = "doc_${UUID.randomUUID().toString().replace("-", "").take(12)}"
                onUploadPdf(docId, title, desc, cat, tier, featured, pageCount, sizeLabel, base64, pageTexts)
                showUploadDialog = false
            }
        )
    }

    if (editingPlan != null) {
        val plan = editingPlan!!
        var priceText by remember(plan) { mutableStateOf(plan.displayPriceInr.toString()) }
        var productIdText by remember(plan) { mutableStateOf(plan.googlePlayProductId) }
        AlertDialog(
            onDismissRequest = { editingPlan = null },
            containerColor = ObsidianSurface,
            titleContentColor = PureWhite,
            title = { Text("Configure ${plan.name}") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = priceText,
                        onValueChange = { priceText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Display Price (INR)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LiquidYellow,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )
                    OutlinedTextField(
                        value = productIdText,
                        onValueChange = { productIdText = it },
                        label = { Text("Google Play Product ID") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = LiquidYellow,
                            unfocusedBorderColor = CharcoalBorder,
                            focusedTextColor = PureWhite,
                            unfocusedTextColor = PureWhite
                        )
                    )
                }
            },
            confirmButton = {
                LiquidActionButton(
                    text = "Save Plan",
                    onClick = {
                        val price = priceText.toLongOrNull() ?: plan.displayPriceInr
                        onSavePlan(
                            plan.copy(
                                displayPriceInr = price,
                                googlePlayProductId = productIdText.trim().ifBlank { plan.googlePlayProductId }
                            )
                        )
                        editingPlan = null
                    }
                )
            },
            dismissButton = {
                TextButton(onClick = { editingPlan = null }) { Text("Cancel", color = MutedZinc) }
            }
        )
    }
}

@Composable
private fun AdminCatalogTab(
    categories: List<StudyCategory>,
    docs: List<PdfDocumentMeta>,
    usersCount: Int,
    activeSubsCount: Int,
    expiredSubsCount: Int,
    pendingPaymentsCount: Int,
    onSeedCatalog: () -> Unit,
    onOpenUploadDialog: () -> Unit,
    onTogglePublish: (PdfDocumentMeta) -> Unit,
    onToggleTier: (PdfDocumentMeta) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            LiquidCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                Text("Platform Analytics", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                Spacer(modifier = Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    MetricBlock("Users", usersCount.toString())
                    MetricBlock("Active Subs", activeSubsCount.toString())
                    MetricBlock("Expired", expiredSubsCount.toString())
                    MetricBlock("Pending Pay", pendingPaymentsCount.toString())
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                LiquidActionButton(
                    text = "Upload / Create PDF",
                    icon = Icons.Rounded.Add,
                    onClick = onOpenUploadDialog,
                    enabled = categories.isNotEmpty(),
                    testTag = "admin_upload_pdf_button",
                    modifier = Modifier.weight(1f)
                )
                LiquidSecondaryButton(
                    text = "Seed Defaults",
                    icon = Icons.Rounded.AutoAwesome,
                    onClick = onSeedCatalog,
                    testTag = "admin_seed_button"
                )
            }
        }

        items(docs, key = { it.id }) { doc ->
            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(doc.title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("${doc.categoryName} • ${doc.pageCount}p • ${doc.fileSizeLabel}", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                    }
                    StatusPill(
                        label = doc.accessTier.uppercase(),
                        containerColor = if (doc.accessTier == "premium") LiquidYellow else CharcoalElevated,
                        contentColor = if (doc.accessTier == "premium") DeepBlack else PureWhite
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidSecondaryButton(
                        text = if (doc.isPublished) "Unpublish" else "Publish",
                        onClick = { onTogglePublish(doc) },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidSecondaryButton(
                        text = if (doc.accessTier == "premium") "Make Free" else "Make Premium",
                        onClick = { onToggleTier(doc) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun MetricBlock(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = LiquidYellow)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MutedZinc)
    }
}

@Composable
private fun AdminPaymentsAndPlansTab(
    purchases: List<PurchaseRecord>,
    plans: List<SubscriptionPlan>,
    onApprovePurchase: (PurchaseRecord) -> Unit,
    onRejectPurchase: (PurchaseRecord) -> Unit,
    onRefundPurchase: (PurchaseRecord) -> Unit,
    onEditPlan: (SubscriptionPlan) -> Unit,
    onTogglePlanAvailability: (SubscriptionPlan) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Subscription Plans Configuration", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        items(plans, key = { it.id }) { plan ->
            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${plan.name} (₹${plan.displayPriceInr})", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text("Play ID: ${plan.googlePlayProductId} • ${plan.durationDays}d", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                    }
                    StatusPill(
                        label = if (plan.isAvailable) "LIVE" else "HIDDEN",
                        containerColor = if (plan.isAvailable) EmeraldSuccess.copy(alpha = 0.2f) else CharcoalElevated,
                        contentColor = if (plan.isAvailable) EmeraldSuccess else MutedZinc
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidSecondaryButton(text = "Edit Price/ID", onClick = { onEditPlan(plan) }, modifier = Modifier.weight(1f))
                    LiquidSecondaryButton(
                        text = if (plan.isAvailable) "Disable" else "Enable",
                        onClick = { onTogglePlanAvailability(plan) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Payment Verification Queue", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }

        if (purchases.isEmpty()) {
            item {
                LiquidCard(modifier = Modifier.fillMaxWidth()) {
                    Text("No purchase transactions recorded yet.", color = MutedZinc)
                }
            }
        } else {
            items(purchases, key = { it.id }) { purchase ->
                LiquidCard(
                    highlighted = purchase.status == "pending_verification",
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(purchase.userEmail, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Text("${purchase.planName} • ₹${purchase.amountInr} • ${purchase.paymentMethod}", style = MaterialTheme.typography.bodySmall, color = LiquidYellow)
                            Text("Token/UTR: ${purchase.purchaseToken}", style = MaterialTheme.typography.labelSmall, color = MutedZinc)
                        }
                        StatusPill(
                            label = purchase.status.uppercase(),
                            containerColor = CharcoalElevated,
                            contentColor = if (purchase.status == "verified") EmeraldSuccess else LiquidYellow
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if (purchase.status == "pending_verification") {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            LiquidActionButton(
                                text = "Approve & Activate",
                                icon = Icons.Rounded.Check,
                                onClick = { onApprovePurchase(purchase) },
                                testTag = "admin_approve_${purchase.id}",
                                modifier = Modifier.weight(1f)
                            )
                            LiquidSecondaryButton(
                                text = "Reject",
                                icon = Icons.Rounded.Close,
                                onClick = { onRejectPurchase(purchase) }
                            )
                        }
                    } else if (purchase.status == "verified") {
                        LiquidSecondaryButton(
                            text = "Mark Refunded & Revoke Access",
                            onClick = { onRefundPurchase(purchase) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AdminUsersAndLogsTab(
    users: List<UserProfile>,
    subs: List<UserSubscription>,
    accessLogs: List<PdfAccessLog>,
    auditLogs: List<AuditLog>,
    onToggleSuspension: (UserProfile) -> Unit,
    onToggleAdmin: (UserProfile) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Registered Users (${users.size})", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        items(users, key = { it.userId }) { user ->
            val sub = subs.firstOrNull { it.userId == user.userId }
            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user.displayName, style = MaterialTheme.typography.titleMedium, color = PureWhite)
                        Text(user.email, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                        Text(
                            text = "Role: ${user.role} • Status: ${user.status} • Sub: ${sub?.status ?: "none"}",
                            style = MaterialTheme.typography.labelSmall,
                            color = LiquidYellow
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    LiquidSecondaryButton(
                        text = if (user.status == "suspended") "Reactivate" else "Suspend",
                        onClick = { onToggleSuspension(user) },
                        modifier = Modifier.weight(1f)
                    )
                    LiquidSecondaryButton(
                        text = if (user.role == "admin") "Revoke Admin" else "Make Admin",
                        onClick = { onToggleAdmin(user) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Recent PDF Access Logs (${accessLogs.size})", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        items(accessLogs.take(15), key = { it.id }) { log ->
            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                Text("${log.action.uppercase()} — ${log.documentTitle}", style = MaterialTheme.typography.labelMedium, color = if (log.action.contains("denied")) CrimsonError else LiquidYellow)
                Text(log.userEmail, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
            }
        }

        item {
            Spacer(modifier = Modifier.height(6.dp))
            Text("Admin Audit Trail (${auditLogs.size})", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }
        items(auditLogs.take(15), key = { it.id }) { audit ->
            LiquidCard(modifier = Modifier.fillMaxWidth()) {
                Text(audit.eventType, style = MaterialTheme.typography.labelMedium, color = EmeraldSuccess)
                Text(audit.details, style = MaterialTheme.typography.bodySmall, color = PureWhite)
                Text("Actor: ${audit.actorEmail}", style = MaterialTheme.typography.labelSmall, color = MutedZinc)
            }
        }
    }
}

@Composable
private fun AdminUploadPdfDialog(
    categories: List<StudyCategory>,
    onDismiss: () -> Unit,
    onSubmit: (
        title: String,
        description: String,
        category: StudyCategory,
        accessTier: String,
        isFeatured: Boolean,
        pageCount: Long,
        fileSizeLabel: String,
        pdfBase64: String,
        pageTexts: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedCat by remember { mutableStateOf(categories.first()) }
    var accessTier by remember { mutableStateOf("premium") }
    var isFeatured by remember { mutableStateOf(true) }
    var contentText by remember { mutableStateOf("") }
    var pickedPdfBase64 by remember { mutableStateOf<String?>(null) }
    var pickedPageCount by remember { mutableIntStateOf(1) }
    var pickedSizeLabel by remember { mutableStateOf("") }
    var pickerError by remember { mutableStateOf<String?>(null) }

    val pdfPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            try {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                if (bytes == null || bytes.isEmpty()) {
                    pickerError = "Selected PDF file is empty."
                    return@rememberLauncherForActivityResult
                }
                if (bytes.size > 600_000) {
                    pickerError = "PDF exceeds 600 KB Firestore payload limit. Use a smaller PDF or compose study text below."
                    return@rememberLauncherForActivityResult
                }
                val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                val renderer = pfd?.let { PdfRenderer(it) }
                val count = renderer?.pageCount ?: 1
                renderer?.close()
                pfd?.close()

                pickedPdfBase64 = Base64.encodeToString(bytes, Base64.NO_WRAP)
                pickedPageCount = count.coerceAtLeast(1)
                pickedSizeLabel = "${(bytes.size / 1024).coerceAtLeast(1)} KB"
                pickerError = null
            } catch (e: Exception) {
                pickerError = e.localizedMessage ?: "Could not read selected PDF."
            }
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ObsidianSurface,
        titleContentColor = PureWhite,
        title = { Text("Publish Study PDF") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("PDF Title") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LiquidYellow, unfocusedBorderColor = CharcoalBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description") },
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LiquidYellow, unfocusedBorderColor = CharcoalBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                )

                Text("Subject Category", style = MaterialTheme.typography.labelMedium, color = LiquidYellow)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    categories.take(3).forEach { cat ->
                        FilterChip(
                            selected = selectedCat.id == cat.id,
                            onClick = { selectedCat = cat },
                            label = { Text(cat.name) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LiquidYellow, selectedLabelColor = DeepBlack)
                        )
                    }
                }

                Text("Access Tier", style = MaterialTheme.typography.labelMedium, color = LiquidYellow)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("free", "premium").forEach { tier ->
                        FilterChip(
                            selected = accessTier == tier,
                            onClick = { accessTier = tier },
                            label = { Text(tier.uppercase()) },
                            colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LiquidYellow, selectedLabelColor = DeepBlack)
                        )
                    }
                    FilterChip(
                        selected = isFeatured,
                        onClick = { isFeatured = !isFeatured },
                        label = { Text("FEATURED") },
                        colors = FilterChipDefaults.filterChipColors(selectedContainerColor = LiquidYellow, selectedLabelColor = DeepBlack)
                    )
                }

                LiquidSecondaryButton(
                    text = if (pickedPdfBase64 != null) "PDF Attached ($pickedPageCount p • $pickedSizeLabel)" else "Pick PDF File from Device",
                    icon = Icons.Rounded.UploadFile,
                    onClick = { pdfPicker.launch(arrayOf("application/pdf")) },
                    modifier = Modifier.fillMaxWidth()
                )

                if (!pickerError.isNullOrBlank()) {
                    Text(pickerError!!, style = MaterialTheme.typography.bodySmall, color = CrimsonError)
                }

                if (pickedPdfBase64 == null) {
                    OutlinedTextField(
                        value = contentText,
                        onValueChange = { contentText = it },
                        label = { Text("Or Compose Study Content (Generates Multi-Page PDF)") },
                        minLines = 3,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = LiquidYellow, unfocusedBorderColor = CharcoalBorder, focusedTextColor = PureWhite, unfocusedTextColor = PureWhite)
                    )
                }
            }
        },
        confirmButton = {
            LiquidActionButton(
                text = "Publish PDF",
                enabled = title.isNotBlank() && (pickedPdfBase64 != null || contentText.isNotBlank()),
                onClick = {
                    if (pickedPdfBase64 != null) {
                        onSubmit(
                            title.trim(),
                            description.trim().ifBlank { "Protected ${selectedCat.name} study document." },
                            selectedCat,
                            accessTier,
                            isFeatured,
                            pickedPageCount.toLong(),
                            pickedSizeLabel,
                            pickedPdfBase64!!,
                            listOf(title.trim(), description.trim())
                        )
                    } else {
                        val generated = PdfGenerator.createStudyPdf(
                            title = title.trim(),
                            subject = selectedCat.name,
                            sections = listOf(title.trim() to contentText.trim())
                        )
                        onSubmit(
                            title.trim(),
                            description.trim().ifBlank { "Protected ${selectedCat.name} study document." },
                            selectedCat,
                            accessTier,
                            isFeatured,
                            generated.pageCount.toLong(),
                            generated.fileSizeLabel,
                            generated.pdfBase64,
                            generated.pageTexts
                        )
                    }
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel", color = MutedZinc) }
        }
    )
}
