package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AuditLog
import com.example.data.model.PdfAccessLog
import com.example.data.model.PdfDocumentMeta
import com.example.data.model.PurchaseRecord
import com.example.data.model.StudyCategory
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.data.remote.CatalogSeeder
import com.example.data.remote.SecureStudyRepository
import com.example.data.remote.unwrapFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AdminViewModel(
    private val repository: SecureStudyRepository
) : ViewModel() {

    val users: StateFlow<UiState<List<UserProfile>>> = repository.observeAllUsers()
        .map<List<UserProfile>, UiState<List<UserProfile>>> { UiState.Success(it) }
        .catch { e -> handleStreamError(e, "users") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val subscriptions: StateFlow<UiState<List<UserSubscription>>> = repository.observeAllSubscriptions()
        .map<List<UserSubscription>, UiState<List<UserSubscription>>> { UiState.Success(it) }
        .catch { e -> handleStreamError(e, "subscriptions") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val purchases: StateFlow<UiState<List<PurchaseRecord>>> = repository.observeAllPurchases()
        .map<List<PurchaseRecord>, UiState<List<PurchaseRecord>>> { UiState.Success(it) }
        .catch { e -> handleStreamError(e, "purchases") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val accessLogs: StateFlow<UiState<List<PdfAccessLog>>> = repository.observeAllAccessLogs()
        .map<List<PdfAccessLog>, UiState<List<PdfAccessLog>>> { UiState.Success(it) }
        .catch { e -> handleStreamError(e, "pdf_access_logs") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val auditLogs: StateFlow<UiState<List<AuditLog>>> = repository.observeAuditLogs()
        .map<List<AuditLog>, UiState<List<AuditLog>>> { UiState.Success(it) }
        .catch { e -> handleStreamError(e, "audit_logs") { emit(UiState.Error(it)) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _actionMessage = MutableStateFlow<String?>(null)
    val actionMessage: StateFlow<String?> = _actionMessage.asStateFlow()

    private val _isWorking = MutableStateFlow(false)
    val isWorking: StateFlow<Boolean> = _isWorking.asStateFlow()

    fun approvePurchase(purchase: PurchaseRecord, plans: List<SubscriptionPlan>) {
        runAdminTask("Subscription activated for ${purchase.userEmail}") {
            val matchedPlan = plans.firstOrNull { it.id == purchase.planId }
            repository.approvePurchaseAndActivateSubscription(purchase, matchedPlan).getOrThrow()
        }
    }

    fun updatePurchaseStatus(purchase: PurchaseRecord, newPurchaseStatus: String, newSubStatus: String?) {
        runAdminTask("Updated transaction ${purchase.id} -> $newPurchaseStatus") {
            repository.updatePurchaseAndSubscriptionStatus(purchase, newPurchaseStatus, newSubStatus)
                .getOrThrow()
        }
    }

    fun toggleUserSuspension(user: UserProfile) {
        val nextSuspend = user.status != "suspended"
        runAdminTask("${user.email} status -> ${if (nextSuspend) "suspended" else "active"}") {
            repository.toggleUserSuspension(user, nextSuspend).getOrThrow()
        }
    }

    fun toggleAdminRole(user: UserProfile) {
        val makeAdmin = user.role != "admin"
        runAdminTask("${user.email} role -> ${if (makeAdmin) "admin" else "student"}") {
            repository.grantOrRevokeAdmin(user, makeAdmin).getOrThrow()
        }
    }

    fun uploadPdf(
        documentId: String,
        title: String,
        description: String,
        category: StudyCategory,
        accessTier: String,
        isFeatured: Boolean,
        pageCount: Long,
        fileSizeLabel: String,
        pdfBase64: String,
        pageTexts: List<String>
    ) {
        runAdminTask("Published '$title' ($accessTier)") {
            repository.uploadPdfDocumentAndPayload(
                documentId = documentId,
                title = title,
                description = description,
                categoryId = category.id,
                categoryName = category.name,
                accessTier = accessTier,
                isPublished = true,
                isFeatured = isFeatured,
                pageCount = pageCount,
                fileSizeLabel = fileSizeLabel,
                thumbnailColorHex = if (accessTier == "premium") "#FFD600" else "#FFC400",
                pdfBase64 = pdfBase64,
                pageTexts = pageTexts
            ).getOrThrow()
        }
    }

    fun togglePdfPublish(doc: PdfDocumentMeta) {
        runAdminTask("Updated '${doc.title}' visibility") {
            repository.updatePdfPublication(
                documentId = doc.id,
                isPublished = !doc.isPublished,
                accessTier = doc.accessTier,
                isFeatured = doc.isFeatured
            ).getOrThrow()
        }
    }

    fun togglePdfTier(doc: PdfDocumentMeta) {
        val nextTier = if (doc.accessTier == "premium") "free" else "premium"
        runAdminTask("Changed '${doc.title}' tier to $nextTier") {
            repository.updatePdfPublication(
                documentId = doc.id,
                isPublished = doc.isPublished,
                accessTier = nextTier,
                isFeatured = doc.isFeatured
            ).getOrThrow()
        }
    }

    fun savePlan(plan: SubscriptionPlan) {
        runAdminTask("Saved plan '${plan.name}'") {
            repository.saveSubscriptionPlan(
                planId = plan.id,
                name = plan.name,
                billingPeriod = plan.billingPeriod,
                googlePlayProductId = plan.googlePlayProductId,
                displayPriceInr = plan.displayPriceInr,
                durationDays = plan.durationDays,
                badge = plan.badge,
                features = plan.features,
                isAvailable = plan.isAvailable
            ).getOrThrow()
        }
    }

    fun seedCatalog(catCount: Int, planCount: Int, docCount: Int) {
        runAdminTask("Catalog initialization complete") {
            val msg = CatalogSeeder.seedDefaultCatalogIfEmpty(repository, catCount, planCount, docCount)
                .getOrThrow()
            _actionMessage.value = msg
        }
    }

    fun clearMessage() {
        _actionMessage.value = null
    }

    private fun runAdminTask(successMsg: String, block: suspend () -> Unit) {
        viewModelScope.launch {
            _isWorking.value = true
            try {
                block()
                if (_actionMessage.value == null) {
                    _actionMessage.value = successMsg
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _actionMessage.value = e.localizedMessage ?: "Admin operation failed."
            } finally {
                _isWorking.value = false
            }
        }
    }

    private suspend fun handleStreamError(
        error: Throwable,
        tag: String,
        onEmit: suspend (String) -> Unit
    ) {
        val fs = error.unwrapFirestoreException()
        if (fs == null && error is CancellationException) throw error
        val root = fs ?: error
        Log.w("AdminVM", "Error observing $tag", root)
        onEmit(root.message ?: "Admin access denied or connection error")
    }
}
