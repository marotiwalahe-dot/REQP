package com.example.data.remote

import android.content.Context
import com.example.R
import com.example.data.model.AdminRole
import com.example.data.model.AuditLog
import com.example.data.model.PdfAccessLog
import com.example.data.model.PdfDocumentMeta
import com.example.data.model.PdfPayload
import com.example.data.model.PurchaseRecord
import com.example.data.model.StudyCategory
import com.example.data.model.SubscriptionPlan
import com.example.data.model.UserProfile
import com.example.data.model.UserSubscription
import com.google.firebase.Firebase
import com.google.firebase.Timestamp
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot.ServerTimestampBehavior
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.Date
import java.util.UUID

class SecureStudyRepository(private val db: FirebaseFirestore) {

    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private val auth = Firebase.auth

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    private fun requireUserEmail(): String =
        auth.currentUser?.email ?: "unknown@securestudy.app"

    suspend fun ensureUserProfile(
        userId: String,
        email: String,
        displayName: String,
        photoUrl: String?
    ): Result<UserProfile> = runCatching {
        val docRef = db.collection("users").document(userId)
        val snap = docRef.get().await()
        if (!snap.exists()) {
            val payload = mutableMapOf<String, Any>(
                "userId" to userId,
                "email" to email.ifBlank { "$userId@securestudy.app" },
                "displayName" to displayName.ifBlank { "Secure Study Student" },
                "role" to "student",
                "status" to "active",
                "biometricLockEnabled" to false,
                "inactivityTimeoutSec" to 60L,
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            if (!photoUrl.isNullOrBlank()) {
                payload["photoUrl"] = photoUrl
            }
            try {
                docRef.set(payload).await()
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                handleFirestoreError(e, OperationType.CREATE, docRef.path)
                throw e
            }
            docRef.get().await().toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = userId, userId = userId, email = email, displayName = displayName)
        } else {
            snap.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: UserProfile(id = userId, userId = userId, email = email, displayName = displayName)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> =
        db.collection("users").document(userId)
            .snapshots()
            .map { it.toObject(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "users/$userId", userId)
                throw error
            }

    fun observeIsAdmin(userId: String, email: String?, isEmailVerified: Boolean): Flow<Boolean> =
        db.collection("admins").document(userId)
            .snapshots()
            .map { snap ->
                snap.exists() || (email == BOOTSTRAP_ADMIN_EMAIL && isEmailVerified)
            }
            .catch { error ->
                if (email == BOOTSTRAP_ADMIN_EMAIL && isEmailVerified) {
                    emit(true)
                } else {
                    logStreamErrorIfActive(error, OperationType.GET, "admins/$userId", userId)
                    emit(false)
                }
            }

    suspend fun updateSecurityPreferences(
        userId: String,
        biometricEnabled: Boolean,
        inactivityTimeoutSec: Long
    ): Result<Unit> = runCatching {
        val docRef = db.collection("users").document(userId)
        try {
            docRef.update(
                mapOf(
                    "biometricLockEnabled" to biometricEnabled,
                    "inactivityTimeoutSec" to inactivityTimeoutSec,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, docRef.path)
            throw e
        }
        Unit
    }

    fun observeCategories(isAdmin: Boolean): Flow<List<StudyCategory>> {
        val query = if (isAdmin) {
            db.collection("categories")
        } else {
            db.collection("categories").whereEqualTo("isActive", true)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(StudyCategory::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.sortOrder }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "categories", auth.currentUser?.uid)
                throw error
            }
    }

    fun observeDocuments(isAdmin: Boolean): Flow<List<PdfDocumentMeta>> {
        val query = if (isAdmin) {
            db.collection("pdf_documents")
        } else {
            db.collection("pdf_documents").whereEqualTo("isPublished", true)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(PdfDocumentMeta::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "pdf_documents", auth.currentUser?.uid)
                throw error
            }
    }

    fun observeSubscriptionPlans(isAdmin: Boolean): Flow<List<SubscriptionPlan>> {
        val query = if (isAdmin) {
            db.collection("subscription_plans")
        } else {
            db.collection("subscription_plans").whereEqualTo("isAvailable", true)
        }
        return query.snapshots()
            .map { snap ->
                snap.toObjects(SubscriptionPlan::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedBy { it.durationDays }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "subscription_plans", auth.currentUser?.uid)
                throw error
            }
    }

    fun observeUserSubscription(userId: String): Flow<UserSubscription?> =
        db.collection("subscriptions").document(userId)
            .snapshots()
            .map { it.toObject(UserSubscription::class.java, ServerTimestampBehavior.ESTIMATE) }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.GET, "subscriptions/$userId", userId)
                throw error
            }

    fun observeUserPurchases(userId: String): Flow<List<PurchaseRecord>> =
        db.collection("purchases")
            .whereEqualTo("userId", userId)
            .snapshots()
            .map { snap ->
                snap.toObjects(PurchaseRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "purchases", userId)
                throw error
            }

    suspend fun fetchAuthorizedPdfPayload(documentId: String): Result<PdfPayload> = runCatching {
        val docRef = db.collection("pdf_payloads").document(documentId)
        try {
            val snap = docRef.get().await()
            if (!snap.exists()) {
                throw NoSuchElementException("Protected PDF payload not found.")
            }
            snap.toObject(PdfPayload::class.java, ServerTimestampBehavior.ESTIMATE)
                ?: throw IllegalStateException("Malformed PDF payload.")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, docRef.path)
            throw e
        }
    }

    suspend fun submitPendingPurchase(
        plan: SubscriptionPlan,
        paymentMethod: String,
        purchaseTokenOrUtr: String
    ): Result<String> = runCatching {
        val uid = requireUserId()
        val email = requireUserEmail()
        val purchaseId = "pur_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("purchases").document(purchaseId)
        val payload = mapOf(
            "userId" to uid,
            "userEmail" to email,
            "planId" to plan.id,
            "planName" to plan.name,
            "paymentMethod" to paymentMethod,
            "amountInr" to plan.displayPriceInr,
            "purchaseToken" to purchaseTokenOrUtr.trim(),
            "status" to "pending_verification",
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        purchaseId
    }

    suspend fun logPdfAccess(
        documentId: String,
        documentTitle: String,
        action: String
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val email = requireUserEmail()
        val logId = "log_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("pdf_access_logs").document(logId)
        val payload = mapOf(
            "userId" to uid,
            "userEmail" to email,
            "documentId" to documentId,
            "documentTitle" to documentTitle.take(140),
            "action" to action,
            "createdAt" to FieldValue.serverTimestamp()
        )
        try {
            docRef.set(payload).await()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, docRef.path)
            throw e
        }
        Unit
    }

    // --- Admin Operations (Protected by Firestore Security Rules) ---

    fun observeAllUsers(): Flow<List<UserProfile>> =
        db.collection("users").snapshots()
            .map { snap ->
                snap.toObjects(UserProfile::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "users", auth.currentUser?.uid)
                throw error
            }

    fun observeAllSubscriptions(): Flow<List<UserSubscription>> =
        db.collection("subscriptions").snapshots()
            .map { snap ->
                snap.toObjects(UserSubscription::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.updatedAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "subscriptions", auth.currentUser?.uid)
                throw error
            }

    fun observeAllPurchases(): Flow<List<PurchaseRecord>> =
        db.collection("purchases").snapshots()
            .map { snap ->
                snap.toObjects(PurchaseRecord::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "purchases", auth.currentUser?.uid)
                throw error
            }

    fun observeAllAccessLogs(): Flow<List<PdfAccessLog>> =
        db.collection("pdf_access_logs").snapshots()
            .map { snap ->
                snap.toObjects(PdfAccessLog::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "pdf_access_logs", auth.currentUser?.uid)
                throw error
            }

    fun observeAuditLogs(): Flow<List<AuditLog>> =
        db.collection("audit_logs").snapshots()
            .map { snap ->
                snap.toObjects(AuditLog::class.java, ServerTimestampBehavior.ESTIMATE)
                    .sortedByDescending { it.createdAt ?: Timestamp(0, 0) }
            }
            .catch { error ->
                logStreamErrorIfActive(error, OperationType.LIST, "audit_logs", auth.currentUser?.uid)
                throw error
            }

    suspend fun saveCategory(
        categoryId: String,
        name: String,
        description: String,
        iconName: String,
        sortOrder: Long,
        isActive: Boolean
    ): Result<Unit> = runCatching {
        val docRef = db.collection("categories").document(categoryId)
        val exists = docRef.get().await().exists()
        val slug = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "cat" }
        try {
            if (!exists) {
                docRef.set(
                    mapOf(
                        "name" to name,
                        "slug" to slug,
                        "description" to description,
                        "iconName" to iconName,
                        "sortOrder" to sortOrder,
                        "isActive" to isActive,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name,
                        "slug" to slug,
                        "description" to description,
                        "iconName" to iconName,
                        "sortOrder" to sortOrder,
                        "isActive" to isActive,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("CATEGORY_SAVED", categoryId, "Saved category $name")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun uploadPdfDocumentAndPayload(
        documentId: String,
        title: String,
        description: String,
        categoryId: String,
        categoryName: String,
        accessTier: String,
        isPublished: Boolean,
        isFeatured: Boolean,
        pageCount: Long,
        fileSizeLabel: String,
        thumbnailColorHex: String,
        pdfBase64: String,
        pageTexts: List<String>
    ): Result<Unit> = runCatching {
        val uid = requireUserId()
        val metaRef = db.collection("pdf_documents").document(documentId)
        val payloadRef = db.collection("pdf_payloads").document(documentId)
        try {
            val batch = db.batch()
            batch.set(
                metaRef,
                mapOf(
                    "title" to title,
                    "description" to description,
                    "categoryId" to categoryId,
                    "categoryName" to categoryName,
                    "accessTier" to accessTier,
                    "isPublished" to isPublished,
                    "isFeatured" to isFeatured,
                    "pageCount" to pageCount,
                    "fileSizeLabel" to fileSizeLabel,
                    "thumbnailColorHex" to thumbnailColorHex,
                    "createdBy" to uid,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                payloadRef,
                mapOf(
                    "documentId" to documentId,
                    "accessTier" to accessTier,
                    "pdfBase64" to pdfBase64,
                    "pageTexts" to pageTexts.take(50),
                    "watermarkRequired" to true,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            writeAuditLog("PDF_UPLOADED", documentId, "Uploaded $accessTier PDF: $title")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, metaRef.path)
            throw e
        }
        Unit
    }

    suspend fun updatePdfPublication(
        documentId: String,
        isPublished: Boolean,
        accessTier: String,
        isFeatured: Boolean
    ): Result<Unit> = runCatching {
        val metaRef = db.collection("pdf_documents").document(documentId)
        val payloadRef = db.collection("pdf_payloads").document(documentId)
        try {
            val batch = db.batch()
            batch.update(
                metaRef,
                mapOf(
                    "isPublished" to isPublished,
                    "accessTier" to accessTier,
                    "isFeatured" to isFeatured,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.update(
                payloadRef,
                mapOf(
                    "accessTier" to accessTier,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            writeAuditLog("PDF_UPDATED", documentId, "Published=$isPublished, tier=$accessTier")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, metaRef.path)
            throw e
        }
        Unit
    }

    suspend fun saveSubscriptionPlan(
        planId: String,
        name: String,
        billingPeriod: String,
        googlePlayProductId: String,
        displayPriceInr: Long,
        durationDays: Long,
        badge: String,
        features: List<String>,
        isAvailable: Boolean
    ): Result<Unit> = runCatching {
        val docRef = db.collection("subscription_plans").document(planId)
        val exists = docRef.get().await().exists()
        try {
            if (!exists) {
                docRef.set(
                    mapOf(
                        "name" to name,
                        "billingPeriod" to billingPeriod,
                        "googlePlayProductId" to googlePlayProductId,
                        "displayPriceInr" to displayPriceInr,
                        "durationDays" to durationDays,
                        "badge" to badge,
                        "features" to features.take(10),
                        "isAvailable" to isAvailable,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            } else {
                docRef.update(
                    mapOf(
                        "name" to name,
                        "billingPeriod" to billingPeriod,
                        "googlePlayProductId" to googlePlayProductId,
                        "displayPriceInr" to displayPriceInr,
                        "durationDays" to durationDays,
                        "badge" to badge,
                        "features" to features.take(10),
                        "isAvailable" to isAvailable,
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                ).await()
            }
            writeAuditLog("PLAN_SAVED", planId, "Plan $name ($billingPeriod) available=$isAvailable")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docRef.path)
            throw e
        }
        Unit
    }

    suspend fun approvePurchaseAndActivateSubscription(
        purchase: PurchaseRecord,
        plan: SubscriptionPlan?
    ): Result<Unit> = runCatching {
        val purchaseRef = db.collection("purchases").document(purchase.id)
        val subRef = db.collection("subscriptions").document(purchase.userId)
        val durationDays = plan?.durationDays ?: when {
            purchase.planName.contains("Year", ignoreCase = true) -> 365L
            purchase.planName.contains("Quarter", ignoreCase = true) -> 90L
            else -> 30L
        }
        val nowMillis = System.currentTimeMillis()
        val expiresMillis = nowMillis + durationDays * 24L * 60L * 60L * 1000L

        try {
            val batch = db.batch()
            batch.update(
                purchaseRef,
                mapOf(
                    "status" to "verified",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.set(
                subRef,
                mapOf(
                    "userId" to purchase.userId,
                    "planId" to purchase.planId,
                    "planName" to purchase.planName,
                    "productId" to (plan?.googlePlayProductId ?: "secure_study_monthly"),
                    "status" to "active",
                    "source" to purchase.paymentMethod,
                    "purchaseToken" to purchase.purchaseToken,
                    "autoRenewing" to (purchase.paymentMethod == "google_play"),
                    "startedAt" to Timestamp(Date(nowMillis)),
                    "expiresAt" to Timestamp(Date(expiresMillis)),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            batch.commit().await()
            writeAuditLog(
                "PURCHASE_VERIFIED",
                purchase.id,
                "Verified ${purchase.paymentMethod} for ${purchase.userEmail} (${purchase.planName})"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, subRef.path)
            throw e
        }
        Unit
    }

    suspend fun updatePurchaseAndSubscriptionStatus(
        purchase: PurchaseRecord,
        newPurchaseStatus: String,
        newSubscriptionStatus: String?
    ): Result<Unit> = runCatching {
        val purchaseRef = db.collection("purchases").document(purchase.id)
        val subRef = db.collection("subscriptions").document(purchase.userId)
        try {
            val batch = db.batch()
            batch.update(
                purchaseRef,
                mapOf(
                    "status" to newPurchaseStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            )
            if (newSubscriptionStatus != null) {
                val subSnap = subRef.get().await()
                if (subSnap.exists()) {
                    batch.update(
                        subRef,
                        mapOf(
                            "status" to newSubscriptionStatus,
                            "autoRenewing" to false,
                            "updatedAt" to FieldValue.serverTimestamp()
                        )
                    )
                }
            }
            batch.commit().await()
            writeAuditLog(
                "PURCHASE_STATUS_CHANGED",
                purchase.id,
                "Purchase -> $newPurchaseStatus, Subscription -> ${newSubscriptionStatus ?: "unchanged"}"
            )
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, purchaseRef.path)
            throw e
        }
        Unit
    }

    suspend fun toggleUserSuspension(user: UserProfile, suspend: Boolean): Result<Unit> = runCatching {
        val newStatus = if (suspend) "suspended" else "active"
        val userRef = db.collection("users").document(user.userId)
        try {
            userRef.update(
                mapOf(
                    "status" to newStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            writeAuditLog("USER_STATUS_CHANGED", user.userId, "${user.email} -> $newStatus")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, userRef.path)
            throw e
        }
        Unit
    }

    suspend fun grantOrRevokeAdmin(user: UserProfile, makeAdmin: Boolean): Result<Unit> = runCatching {
        val actorId = requireUserId()
        val adminRef = db.collection("admins").document(user.userId)
        val userRef = db.collection("users").document(user.userId)
        try {
            val batch = db.batch()
            if (makeAdmin) {
                batch.set(
                    adminRef,
                    mapOf(
                        "userId" to user.userId,
                        "email" to user.email,
                        "grantedBy" to actorId,
                        "createdAt" to FieldValue.serverTimestamp()
                    )
                )
                batch.update(userRef, mapOf("role" to "admin", "updatedAt" to FieldValue.serverTimestamp()))
            } else {
                batch.delete(adminRef)
                batch.update(userRef, mapOf("role" to "student", "updatedAt" to FieldValue.serverTimestamp()))
            }
            batch.commit().await()
            writeAuditLog("ADMIN_ROLE_CHANGED", user.userId, "${user.email} admin=$makeAdmin")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, adminRef.path)
            throw e
        }
        Unit
    }

    private suspend fun writeAuditLog(eventType: String, targetId: String, details: String) {
        val uid = auth.currentUser?.uid ?: return
        val email = auth.currentUser?.email ?: "admin@securestudy.app"
        val logId = "aud_${UUID.randomUUID().toString().replace("-", "").take(16)}"
        val docRef = db.collection("audit_logs").document(logId)
        docRef.set(
            mapOf(
                "actorId" to uid,
                "actorEmail" to email,
                "eventType" to eventType,
                "targetId" to targetId,
                "details" to details.take(500),
                "createdAt" to FieldValue.serverTimestamp()
            )
        ).await()
    }

    private fun logStreamErrorIfActive(
        error: Throwable,
        operationType: OperationType,
        path: String,
        expectedUid: String?
    ) {
        val firestoreError = error.unwrapFirestoreException()
        if (firestoreError == null && error is CancellationException) return
        val toLog = firestoreError ?: error
        if (expectedUid != null && auth.currentUser?.uid == expectedUid) {
            handleFirestoreError(toLog, operationType, path)
        }
    }

    companion object {
        const val BOOTSTRAP_ADMIN_EMAIL = "marotiwalahe@gmail.com"
    }
}
