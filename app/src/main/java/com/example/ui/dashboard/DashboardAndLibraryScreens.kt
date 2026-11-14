package com.example.ui.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material.icons.rounded.WorkspacePremium
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.PdfDocumentMeta
import com.example.data.model.StudyCategory
import com.example.data.model.UiState
import com.example.data.model.UserSubscription
import com.example.ui.theme.CharcoalBorder
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ErrorRecoveryBanner
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.LiquidActionButton
import com.example.ui.theme.LiquidCard
import com.example.ui.theme.LiquidProgressIndicator
import com.example.ui.theme.LiquidSecondaryButton
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.ObsidianSurface
import com.example.ui.theme.PureWhite
import com.example.ui.theme.StatusPill
import java.text.SimpleDateFormat
import java.util.Locale

@Composable
fun HomeDashboardScreen(
    displayName: String,
    isAdmin: Boolean,
    isOnline: Boolean,
    subscription: UserSubscription?,
    hasEntitlement: Boolean,
    categoriesState: UiState<List<StudyCategory>>,
    documentsState: UiState<List<PdfDocumentMeta>>,
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    onNavigateToCategories: () -> Unit,
    onNavigateToLibrary: (String?) -> Unit,
    onNavigateToPlans: () -> Unit,
    onOpenDocument: (PdfDocumentMeta) -> Unit,
    onSeedCatalog: () -> Unit,
    onOpenAdmin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val documents = (documentsState as? UiState.Success)?.data.orEmpty()
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val filteredDocs = remember(documents, searchQuery) {
        if (searchQuery.isBlank()) documents
        else documents.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                it.categoryName.contains(searchQuery, ignoreCase = true) ||
                it.description.contains(searchQuery, ignoreCase = true)
        }
    }
    val featuredDocs = filteredDocs.filter { it.isFeatured }
    val premiumDocs = filteredDocs.filter { it.accessTier == "premium" }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "WELCOME BACK",
                        style = MaterialTheme.typography.labelMedium,
                        color = LiquidYellow
                    )
                    Text(
                        text = displayName.ifBlank { "Student" },
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (isAdmin) {
                    LiquidSecondaryButton(
                        text = "Admin",
                        icon = Icons.Rounded.AdminPanelSettings,
                        onClick = onOpenAdmin,
                        testTag = "dashboard_admin_button"
                    )
                }
            }
        }

        if (!isOnline) {
            item {
                ErrorRecoveryBanner(
                    title = "Offline Mode",
                    message = "You are offline. Live catalog updates and server entitlement checks require internet.",
                    isNetworkError = true
                )
            }
        }

        // Subscription Status Card
        item {
            SubscriptionStatusHeroCard(
                subscription = subscription,
                hasEntitlement = hasEntitlement,
                isAdmin = isAdmin,
                onUpgradeClick = onNavigateToPlans
            )
        }

        // Search Bar
        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Search study PDFs, subjects, topics...", color = MutedZinc) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = "Search", tint = LiquidYellow) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LiquidYellow,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dashboard_search_input")
            )
        }

        if (documentsState is UiState.Error) {
            item {
                ErrorRecoveryBanner(
                    title = "Unable to Load Library",
                    message = documentsState.message,
                    isNetworkError = !isOnline
                )
            }
        } else if (documentsState is UiState.Loading) {
            item {
                LiquidCard(modifier = Modifier.fillMaxWidth()) {
                    Text("Decrypting Study Catalog...", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                    Spacer(modifier = Modifier.height(10.dp))
                    LiquidProgressIndicator()
                }
            }
        } else if (documents.isEmpty()) {
            item {
                LiquidCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                    Text("Vault Catalog Empty", style = MaterialTheme.typography.titleLarge, color = PureWhite)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (isAdmin) {
                            "Initialize default subject categories, Monthly/Quarterly/Yearly plans, and sample encrypted PDFs."
                        } else {
                            "No published study PDFs are available yet. Please check back once your administrator publishes materials."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedZinc
                    )
                    if (isAdmin) {
                        Spacer(modifier = Modifier.height(14.dp))
                        LiquidActionButton(
                            text = "Initialize Default Catalog & Plans",
                            icon = Icons.Rounded.AutoAwesome,
                            onClick = onSeedCatalog,
                            testTag = "seed_catalog_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }

        // Categories strip
        if (categories.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Subject Categories",
                    actionLabel = "All",
                    onAction = onNavigateToCategories
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(categories, key = { it.id }) { cat ->
                        CategoryPillCard(
                            category = cat,
                            onClick = { onNavigateToLibrary(cat.id) }
                        )
                    }
                }
            }
        }

        // Featured Materials
        if (featuredDocs.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Featured Study Materials",
                    actionLabel = "Browse",
                    onAction = { onNavigateToLibrary(null) }
                )
                Spacer(modifier = Modifier.height(10.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(featuredDocs, key = { it.id }) { doc ->
                        FeaturedPdfCard(
                            doc = doc,
                            hasEntitlement = hasEntitlement,
                            onClick = { onOpenDocument(doc) }
                        )
                    }
                }
            }
        }

        // Recently Added PDFs
        if (filteredDocs.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Recently Added PDFs",
                    actionLabel = "Library",
                    onAction = { onNavigateToLibrary(null) }
                )
            }
            items(filteredDocs.take(4), key = { "recent_${it.id}" }) { doc ->
                PdfDocumentListCard(
                    doc = doc,
                    hasEntitlement = hasEntitlement,
                    onClick = { onOpenDocument(doc) }
                )
            }
        }

        // Premium Materials
        if (premiumDocs.isNotEmpty()) {
            item {
                SectionHeaderRow(
                    title = "Premium Vault Exclusives",
                    actionLabel = if (hasEntitlement) "Unlocked" else "Unlock",
                    onAction = if (hasEntitlement) {
                        { onNavigateToLibrary(null) }
                    } else onNavigateToPlans
                )
            }
            items(premiumDocs.take(3), key = { "prem_${it.id}" }) { doc ->
                PdfDocumentListCard(
                    doc = doc,
                    hasEntitlement = hasEntitlement,
                    onClick = { onOpenDocument(doc) }
                )
            }
        }
    }
}

@Composable
private fun SubscriptionStatusHeroCard(
    subscription: UserSubscription?,
    hasEntitlement: Boolean,
    isAdmin: Boolean,
    onUpgradeClick: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM yyyy", Locale.getDefault()) }
    val expiryText = subscription?.expiresAt?.toDate()?.let { dateFormat.format(it) }

    LiquidCard(highlighted = hasEntitlement, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                StatusPill(
                    label = when {
                        isAdmin -> "ADMIN FULL ACCESS"
                        hasEntitlement -> "PRO ACTIVE"
                        subscription?.status == "expired" -> "SUBSCRIPTION EXPIRED"
                        else -> "FREE TIER"
                    },
                    containerColor = if (hasEntitlement) LiquidYellow else CharcoalElevated,
                    contentColor = if (hasEntitlement) DeepBlack else MutedZinc
                )
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = when {
                        isAdmin -> "Administrator Vault Access"
                        hasEntitlement -> subscription?.planName ?: "Active Pro Membership"
                        else -> "Unlock All Protected PDFs"
                    },
                    style = MaterialTheme.typography.titleLarge,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = when {
                        isAdmin -> "Full catalog management & protected reader access enabled."
                        hasEntitlement && expiryText != null -> "Valid until $expiryText • Server verified"
                        else -> "Upgrade to Monthly, Quarterly, or Yearly Pro to read premium materials."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedZinc
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Box(
                modifier = Modifier
                    .size(52.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(if (hasEntitlement) LiquidYellow.copy(alpha = 0.2f) else CharcoalElevated),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (hasEntitlement) Icons.Rounded.Verified else Icons.Rounded.WorkspacePremium,
                    contentDescription = "Subscription Badge",
                    tint = LiquidYellow,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
        if (!hasEntitlement) {
            Spacer(modifier = Modifier.height(14.dp))
            LiquidActionButton(
                text = "View Subscription Plans",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                onClick = onUpgradeClick,
                testTag = "hero_upgrade_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun SectionHeaderRow(
    title: String,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, color = PureWhite)
        TextButton(onClick = onAction) {
            Text(text = actionLabel, color = LiquidYellow, style = MaterialTheme.typography.labelLarge)
        }
    }
}

@Composable
private fun CategoryPillCard(
    category: StudyCategory,
    onClick: () -> Unit
) {
    LiquidCard(
        modifier = Modifier.width(156.dp),
        onClick = onClick
    ) {
        Icon(
            imageVector = categoryIcon(category.iconName),
            contentDescription = category.name,
            tint = LiquidYellow,
            modifier = Modifier.size(26.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = category.name,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = category.description,
            style = MaterialTheme.typography.bodySmall,
            color = MutedZinc,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun FeaturedPdfCard(
    doc: PdfDocumentMeta,
    hasEntitlement: Boolean,
    onClick: () -> Unit
) {
    val unlocked = doc.accessTier == "free" || hasEntitlement
    LiquidCard(
        highlighted = doc.accessTier == "premium",
        modifier = Modifier.width(248.dp),
        onClick = onClick
    ) {
        PdfThumbnailBanner(doc = doc, unlocked = unlocked, heightDp = 88)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = doc.title,
            style = MaterialTheme.typography.titleMedium,
            color = PureWhite,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${doc.categoryName} • ${doc.pageCount} pages",
            style = MaterialTheme.typography.labelSmall,
            color = LiquidYellow
        )
    }
}

@Composable
fun PdfDocumentListCard(
    doc: PdfDocumentMeta,
    hasEntitlement: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val unlocked = doc.accessTier == "free" || hasEntitlement
    LiquidCard(
        modifier = modifier
            .fillMaxWidth()
            .testTag("pdf_card_${doc.id}"),
        highlighted = doc.accessTier == "premium" && unlocked,
        onClick = onClick
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                if (doc.accessTier == "premium") LiquidYellow.copy(alpha = 0.24f) else CharcoalElevated,
                                ObsidianSurface
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Description,
                    contentDescription = doc.title,
                    tint = LiquidYellow,
                    modifier = Modifier.size(30.dp)
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill(
                        label = doc.accessTier.uppercase(),
                        containerColor = if (doc.accessTier == "premium") LiquidYellow else CharcoalElevated,
                        contentColor = if (doc.accessTier == "premium") DeepBlack else PureWhite
                    )
                    StatusPill(
                        label = if (unlocked) "UNLOCKED" else "PRO REQUIRED",
                        containerColor = if (unlocked) EmeraldSuccess.copy(alpha = 0.18f) else CharcoalElevated,
                        contentColor = if (unlocked) EmeraldSuccess else MutedZinc
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = doc.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = doc.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = LiquidYellow
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = doc.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedZinc,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                contentDescription = if (unlocked) "Unlocked" else "Locked",
                tint = if (unlocked) LiquidYellow else MutedZinc
            )
        }
    }
}

@Composable
private fun PdfThumbnailBanner(
    doc: PdfDocumentMeta,
    unlocked: Boolean,
    heightDp: Int
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(heightDp.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        LiquidYellow.copy(alpha = 0.25f),
                        GoldenYellow.copy(alpha = 0.08f),
                        CharcoalElevated
                    )
                )
            )
            .padding(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            StatusPill(
                label = doc.accessTier.uppercase(),
                containerColor = if (doc.accessTier == "premium") LiquidYellow else DeepBlack,
                contentColor = if (doc.accessTier == "premium") DeepBlack else PureWhite
            )
            Icon(
                imageVector = if (unlocked) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                contentDescription = null,
                tint = if (unlocked) LiquidYellow else PureWhite
            )
        }
    }
}

@Composable
fun PdfCategoriesScreen(
    categoriesState: UiState<List<StudyCategory>>,
    documentsState: UiState<List<PdfDocumentMeta>>,
    onCategorySelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val documents = (documentsState as? UiState.Success)?.data.orEmpty()

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Subject Categories", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
            Spacer(modifier = Modifier.height(4.dp))
            Text("Choose a discipline to explore curated study PDFs.", style = MaterialTheme.typography.bodyMedium, color = MutedZinc)
        }

        when (categoriesState) {
            is UiState.Loading -> item { LiquidProgressIndicator() }
            is UiState.Error -> item {
                ErrorRecoveryBanner(title = "Failed to load categories", message = categoriesState.message)
            }
            is UiState.Success -> {
                if (categories.isEmpty()) {
                    item {
                        LiquidCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No subject categories published yet.", color = MutedZinc)
                        }
                    }
                } else {
                    items(categories, key = { it.id }) { cat ->
                        val docCount = documents.count { it.categoryId == cat.id }
                        LiquidCard(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { onCategorySelected(cat.id) }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(14.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .background(LiquidYellow.copy(alpha = 0.16f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = categoryIcon(cat.iconName),
                                        contentDescription = cat.name,
                                        tint = LiquidYellow
                                    )
                                }
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(cat.name, style = MaterialTheme.typography.titleLarge, color = PureWhite)
                                    Text(cat.description, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                                }
                                StatusPill(
                                    label = "$docCount PDFs",
                                    containerColor = CharcoalElevated,
                                    contentColor = LiquidYellow
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
fun StudyLibraryScreen(
    categoriesState: UiState<List<StudyCategory>>,
    documentsState: UiState<List<PdfDocumentMeta>>,
    hasEntitlement: Boolean,
    searchQuery: String,
    selectedCategoryId: String?,
    onSearchChange: (String) -> Unit,
    onCategoryFilterChange: (String?) -> Unit,
    onDocumentClick: (PdfDocumentMeta) -> Unit,
    modifier: Modifier = Modifier
) {
    var tierFilter by remember { mutableStateOf("all") }
    val categories = (categoriesState as? UiState.Success)?.data.orEmpty()
    val documents = (documentsState as? UiState.Success)?.data.orEmpty()

    val filtered = remember(documents, searchQuery, selectedCategoryId, tierFilter) {
        documents.filter { doc ->
            val matchesQuery = searchQuery.isBlank() ||
                doc.title.contains(searchQuery, ignoreCase = true) ||
                doc.categoryName.contains(searchQuery, ignoreCase = true) ||
                doc.description.contains(searchQuery, ignoreCase = true)
            val matchesCategory = selectedCategoryId == null || doc.categoryId == selectedCategoryId
            val matchesTier = tierFilter == "all" || doc.accessTier == tierFilter
            matchesQuery && matchesCategory && matchesTier
        }
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Study Material Library", style = MaterialTheme.typography.headlineMedium, color = PureWhite)
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchChange,
                placeholder = { Text("Filter PDFs by title or subject...", color = MutedZinc) },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = LiquidYellow) },
                singleLine = true,
                shape = RoundedCornerShape(18.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LiquidYellow,
                    unfocusedBorderColor = CharcoalBorder,
                    focusedContainerColor = ObsidianSurface,
                    unfocusedContainerColor = ObsidianSurface,
                    focusedTextColor = PureWhite,
                    unfocusedTextColor = PureWhite
                ),
                modifier = Modifier.fillMaxWidth().testTag("library_search_input")
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("all" to "All", "free" to "Free", "premium" to "Premium").forEach { (key, label) ->
                    FilterChip(
                        selected = tierFilter == key,
                        onClick = { tierFilter = key },
                        label = { Text(label) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiquidYellow,
                            selectedLabelColor = DeepBlack
                        )
                    )
                }
                if (selectedCategoryId != null) {
                    FilterChip(
                        selected = true,
                        onClick = { onCategoryFilterChange(null) },
                        label = { Text("Clear Subject ×") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = CharcoalElevated,
                            selectedLabelColor = LiquidYellow
                        )
                    )
                }
                categories.forEach { cat ->
                    FilterChip(
                        selected = selectedCategoryId == cat.id,
                        onClick = {
                            onCategoryFilterChange(if (selectedCategoryId == cat.id) null else cat.id)
                        },
                        label = { Text(cat.name) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LiquidYellow,
                            selectedLabelColor = DeepBlack
                        )
                    )
                }
            }
        }

        when (documentsState) {
            is UiState.Loading -> item { LiquidProgressIndicator() }
            is UiState.Error -> item {
                ErrorRecoveryBanner(title = "Library Unavailable", message = documentsState.message)
            }
            is UiState.Success -> {
                if (filtered.isEmpty()) {
                    item {
                        LiquidCard(modifier = Modifier.fillMaxWidth()) {
                            Text("No matching PDFs found.", style = MaterialTheme.typography.titleMedium, color = PureWhite)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Adjust your search query or category filter.", style = MaterialTheme.typography.bodySmall, color = MutedZinc)
                        }
                    }
                } else {
                    items(filtered, key = { it.id }) { doc ->
                        PdfDocumentListCard(
                            doc = doc,
                            hasEntitlement = hasEntitlement,
                            onClick = { onDocumentClick(doc) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PdfDetailsScreen(
    document: PdfDocumentMeta?,
    hasEntitlement: Boolean,
    lastReadPage: Int,
    onBack: () -> Unit,
    onOpenReader: (PdfDocumentMeta) -> Unit,
    onUnlockSubscription: () -> Unit
) {
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
            Text("Document Details", style = MaterialTheme.typography.titleLarge, color = PureWhite)
        }

        if (document == null) {
            Spacer(modifier = Modifier.height(16.dp))
            ErrorRecoveryBanner(
                title = "Missing Document",
                message = "This PDF document is no longer available or has been unpublished."
            )
            return
        }

        val unlocked = document.accessTier == "free" || hasEntitlement

        Spacer(modifier = Modifier.height(12.dp))
        LiquidCard(highlighted = unlocked, modifier = Modifier.fillMaxWidth()) {
            PdfThumbnailBanner(doc = document, unlocked = unlocked, heightDp = 120)
            Spacer(modifier = Modifier.height(16.dp))
            Text(text = document.title, style = MaterialTheme.typography.headlineMedium, color = PureWhite)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "${document.categoryName} • ${document.pageCount} pages • ${document.fileSizeLabel}",
                style = MaterialTheme.typography.labelMedium,
                color = LiquidYellow
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = document.description, style = MaterialTheme.typography.bodyLarge, color = MutedZinc)
            if (lastReadPage > 0) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Saved reading progress: Page ${lastReadPage + 1} of ${document.pageCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = EmeraldSuccess
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        LiquidCard(modifier = Modifier.fillMaxWidth()) {
            Text("Vault Protection Policy", style = MaterialTheme.typography.titleMedium, color = PureWhite)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Native in-app rendering only (no external browser, download, share, or print)\n" +
                    "• FLAG_SECURE blocks normal screenshots, recording & recent-apps thumbnails\n" +
                    "• AES-256 encrypted temp descriptor wiped on lock or background\n" +
                    "• Dynamic watermark bound to your account identifier",
                style = MaterialTheme.typography.bodySmall,
                color = MutedZinc
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        if (unlocked) {
            LiquidActionButton(
                text = if (lastReadPage > 0) "Resume Secure Reader" else "Open in Secure Reader",
                icon = Icons.Rounded.LockOpen,
                onClick = { onOpenReader(document) },
                testTag = "open_secure_reader_button",
                modifier = Modifier.fillMaxWidth()
            )
        } else {
            LiquidActionButton(
                text = "Unlock with Pro Subscription",
                icon = Icons.Rounded.WorkspacePremium,
                onClick = onUnlockSubscription,
                testTag = "unlock_subscription_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

fun categoryIcon(name: String): ImageVector = when (name) {
    "code" -> Icons.Rounded.Code
    "hub" -> Icons.Rounded.Hub
    "shield" -> Icons.Rounded.Shield
    "smartphone" -> Icons.Rounded.Smartphone
    "school" -> Icons.Rounded.School
    "security" -> Icons.Rounded.Security
    else -> Icons.Rounded.MenuBook
}
