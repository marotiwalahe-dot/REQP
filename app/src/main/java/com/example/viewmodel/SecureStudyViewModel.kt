package com.example.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.billing.BillingEvent
import com.example.billing.PlayBillingManager
import com.example.data.model.PdfDocumentMeta
import com.example.data.model.PdfPayload
import com.example.data.model.PurchaseRecord
import com.example.data.model.StudyCategory
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.example.data.remote.CatalogSeeder
import com.example.data.remote.SecureStudyRepository
import com.example.data.remote.unwrapFirestoreException
import com.example.security.ConnectivityObserver
import com.example.security.SecurityPreferences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class SecureStudyViewModel(
    private val repository: SecureStudyRepository,
    private val securityPreferences: SecurityPreferences,
    private val connectivityObserver: ConnectivityObserver,
    val billingManager: PlayBillingManager,
    private val currentUserId: String,
    private val userEmail: String,
    private val displayName: String,
    private val photoUrl: String?,
    private val isEmailVerified: Boolean
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = connectivityObserver.observeIsOnline()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), true)

    val isAdmin: StateFlow<Boolean> = repository.observeIsAdmin(currentUserId, userEmail, isEmailVerified)
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000L),
            userEmail == SecureStudyRepository.BOOTSTRAP_ADMIN_EMAIL && isEmailVerified
        )

    val userProfile: StateFlow<UiState<UserProfile?>> = repository.observeUserProfile(currentUserId)
        .map<UserProfile?, UiState<UserProfile?>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Profile observation failed", root)
            emit(UiState.Error(root.message ?: "Failed to load profile"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val categories: StateFlow<UiState<List<StudyCategory>>> = isAdmin
        .flatMapLatest { admin -> repository.observeCategories(admin) }
        .map<List<StudyCategory>, UiState<List<StudyCategory>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Categories observation failed", root)
            emit(UiState.Error(root.message ?: "Failed to load categories"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val documents: StateFlow<UiState<List<PdfDocumentMeta>>> = isAdmin
        .flatMapLatest { admin -> repository.observeDocuments(admin) }
        .map<List<PdfDocumentMeta>, UiState<List<PdfDocumentMeta>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Documents observation failed", root)
            emit(UiState.Error(root.message ?: "Failed to load study materials"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val plans: StateFlow<UiState<List<SubscriptionPlan>>> = isAdmin
        .flatMapLatest { admin -> repository.observeSubscriptionPlans(admin) }
        .map<List<SubscriptionPlan>, UiState<List<SubscriptionPlan>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Plans observation failed", root)
            emit(UiState.Error(root.message ?: "Failed to load subscription plans"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val userSubscription: StateFlow<UserSubscription?> = repository.observeUserSubscription(currentUserId)
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            Log.w(TAG, "Subscription observation error", fsError ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val userPurchases: StateFlow<UiState<List<PurchaseRecord>>> = repository.observeUserPurchases(currentUserId)
        .map<List<PurchaseRecord>, UiState<List<PurchaseRecord>>> { UiState.Success(it) }
        .catch { error ->
            val fsError = error.unwrapFirestoreException()
            if (fsError == null && error is CancellationException) throw error
            val root = fsError ?: error
            Log.w(TAG, "Purchases observation failed", root)
            emit(UiState.Error(root.message ?: "Failed to load purchase history"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val biometricLockEnabled: StateFlow<Boolean> = securityPreferences.biometricLockEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val inactivityTimeoutSec: StateFlow<Long> = securityPreferences.inactivityTimeoutSeconds
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), 60L)

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategoryId = MutableStateFlow<String?>(null)
    val selectedCategoryId: StateFlow<String?> = _selectedCategoryId.asStateFlow()

    private val _isVaultLocked = MutableStateFlow(false)
    val isVaultLocked: StateFlow<Boolean> = _isVaultLocked.asStateFlow()

    private val _paymentStatusState = MutableStateFlow<PaymentStatusUi>(PaymentStatusUi.Idle)
    val paymentStatusState: StateFlow<PaymentStatusUi> = _paymentStatusState.asStateFlow()

    private val _readerPayloadState = MutableStateFlow<UiState<PdfPayload>>(UiState.Loading)
    val readerPayloadState: StateFlow<UiState<PdfPayload>> = _readerPayloadState.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private var backgroundTimestampMs: Long = 0L

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(currentUserId, userEmail, displayName, photoUrl)
        }
        billingManager.startConnection()
        observeBillingEvents()
    }

    private fun observeBillingEvents() {
        viewModelScope.launch {
            billingManager.billingEvent.collect { event ->
                when (event) {
                    is BillingEvent.PurchaseSubmittedForVerification -> {
                        val currentPlans = (plans.value as? UiState.Success)?.data.orEmpty()
                        val matchedPlan = currentPlans.firstOrNull {
                            it.googlePlayProductId == event.productId
                        } ?: currentPlans.firstOrNull()
                        if (matchedPlan != null) {
                            submitPurchaseRecord(
                                plan = matchedPlan,
                                paymentMethod = "google_play",
                                tokenOrUtr = event.purchaseToken
                            )
                        } else {
                            _paymentStatusState.value = PaymentStatusUi.Failed(
                                "Purchase received for ${event.productId}, waiting for plan catalog sync."
                            )
                        }
                        billingManager.clearEvent()
                    }
                    is BillingEvent.Cancelled -> {
                        _paymentStatusState.value = PaymentStatusUi.Cancelled(event.message)
                        billingManager.clearEvent()
                    }
                    is BillingEvent.Error -> {
                        _paymentStatusState.value = PaymentStatusUi.Failed(event.message)
                        billingManager.clearEvent()
                    }
                    else -> Unit
                }
            }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(categoryId: String?) {
        _selectedCategoryId.value = categoryId
    }

    fun hasEntitlement(): Boolean {
        val sub = userSubscription.value
        val activeUser = (userProfile.value as? UiState.Success)?.data?.status != "suspended"
        return activeUser && (isAdmin.value || (sub?.isCurrentlyActive() == true))
    }

    fun openDocumentPayload(doc: PdfDocumentMeta) {
        _readerPayloadState.value = UiState.Loading
        viewModelScope.launch {
            val res = repository.fetchAuthorizedPdfPayload(doc.id)
            res.onSuccess { payload ->
                repository.logPdfAccess(doc.id, doc.title, "authorized_open")
                _readerPayloadState.value = UiState.Success(payload)
            }.onFailure { err ->
                repository.logPdfAccess(doc.id, doc.title, "denied_no_subscription")
                _readerPayloadState.value = UiState.Error(
                    err.localizedMessage ?: "Subscription required or access denied by server policy.",
                    isNetworkError = !isOnline.value
                )
            }
        }
    }

    fun logReaderEvent(doc: PdfDocumentMeta, action: String) {
        viewModelScope.launch {
            repository.logPdfAccess(doc.id, doc.title, action)
        }
    }

    fun submitPurchaseRecord(
        plan: SubscriptionPlan,
        paymentMethod: String,
        tokenOrUtr: String
    ) {
        if (tokenOrUtr.trim().length < 4) {
            _paymentStatusState.value = PaymentStatusUi.Failed("Please enter a valid transaction reference / token.")
            return
        }
        _paymentStatusState.value = PaymentStatusUi.Verifying
        viewModelScope.launch {
            val res = repository.submitPendingPurchase(plan, paymentMethod, tokenOrUtr)
            res.onSuccess { purchaseId ->
                _paymentStatusState.value = PaymentStatusUi.PendingVerification(
                    purchaseId = purchaseId,
                    planName = plan.name,
                    paymentMethod = paymentMethod
                )
            }.onFailure { err ->
                _paymentStatusState.value = PaymentStatusUi.Failed(
                    err.localizedMessage ?: "Failed to submit purchase for verification."
                )
            }
        }
    }

    fun clearPaymentStatus() {
        _paymentStatusState.value = PaymentStatusUi.Idle
    }

    fun setBiometricLock(enabled: Boolean) {
        viewModelScope.launch {
            securityPreferences.setBiometricLockEnabled(enabled)
            repository.updateSecurityPreferences(currentUserId, enabled, inactivityTimeoutSec.value)
        }
    }

    fun setInactivityTimeout(seconds: Long) {
        viewModelScope.launch {
            securityPreferences.setInactivityTimeoutSeconds(seconds)
            repository.updateSecurityPreferences(currentUserId, biometricLockEnabled.value, seconds)
        }
    }

    fun lockVaultNow() {
        _isVaultLocked.value = true
    }

    fun unlockVault() {
        _isVaultLocked.value = false
    }

    fun onAppBackgrounded() {
        backgroundTimestampMs = System.currentTimeMillis()
        if (biometricLockEnabled.value && inactivityTimeoutSec.value == 0L) {
            _isVaultLocked.value = true
        }
    }

    fun onAppForegrounded() {
        if (!biometricLockEnabled.value) return
        if (backgroundTimestampMs == 0L) return
        val elapsedSec = (System.currentTimeMillis() - backgroundTimestampMs) / 1000L
        if (elapsedSec >= inactivityTimeoutSec.value) {
            _isVaultLocked.value = true
        }
    }

    fun observeReadingProgress(documentId: String) = securityPreferences.observeReadingProgress(documentId)

    fun saveReadingProgress(documentId: String, pageIndex: Int) {
        viewModelScope.launch {
            securityPreferences.saveReadingProgress(documentId, pageIndex)
        }
    }

    fun initializeCatalogAsAdmin() {
        if (!isAdmin.value) return
        viewModelScope.launch {
            val cats = (categories.value as? UiState.Success)?.data?.size ?: 0
            val pl = (plans.value as? UiState.Success)?.data?.size ?: 0
            val docs = (documents.value as? UiState.Success)?.data?.size ?: 0
            val result = CatalogSeeder.seedDefaultCatalogIfEmpty(repository, cats, pl, docs)
            _statusBanner.value = result.getOrElse { it.localizedMessage ?: "Seeding failed" }
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    override fun onCleared() {
        super.onCleared()
        billingManager.endConnection()
    }

    private companion object {
        const val TAG = "SecureStudyVM"
    }
}

sealed interface PaymentStatusUi {
    data object Idle : PaymentStatusUi
    data object Verifying : PaymentStatusUi
    data class PendingVerification(
        val purchaseId: String,
        val planName: String,
        val paymentMethod: String
    ) : PaymentStatusUi
    data class Cancelled(val message: String) : PaymentStatusUi
    data class Failed(val message: String) : PaymentStatusUi
}
