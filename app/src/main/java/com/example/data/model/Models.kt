package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val displayName: String = "",
    val photoUrl: String? = null,
    val role: String = "student",
    val status: String = "active",
    val biometricLockEnabled: Boolean = false,
    val inactivityTimeoutSec: Long = 60L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AdminRole(
    @DocumentId val id: String = "",
    val userId: String = "",
    val email: String = "",
    val grantedBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class StudyCategory(
    @DocumentId val id: String = "",
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    val iconName: String = "menu_book",
    val sortOrder: Long = 0L,
    val isActive: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PdfDocumentMeta(
    @DocumentId val id: String = "",
    val title: String = "",
    val description: String = "",
    val categoryId: String = "",
    val categoryName: String = "",
    val accessTier: String = "free",
    val isPublished: Boolean = true,
    val isFeatured: Boolean = false,
    val pageCount: Long = 1L,
    val fileSizeLabel: String = "120 KB",
    val thumbnailColorHex: String = "#FFD600",
    val createdBy: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PdfPayload(
    @DocumentId val id: String = "",
    val documentId: String = "",
    val accessTier: String = "free",
    val pdfBase64: String = "",
    val pageTexts: List<String> = emptyList(),
    val watermarkRequired: Boolean = true,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class SubscriptionPlan(
    @DocumentId val id: String = "",
    val name: String = "",
    val billingPeriod: String = "monthly",
    val googlePlayProductId: String = "",
    val displayPriceInr: Long = 199L,
    val durationDays: Long = 30L,
    val badge: String = "",
    val features: List<String> = emptyList(),
    val isAvailable: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class UserSubscription(
    @DocumentId val id: String = "",
    val userId: String = "",
    val planId: String = "",
    val planName: String = "",
    val productId: String = "",
    val status: String = "expired",
    val source: String = "google_play",
    val purchaseToken: String = "",
    val autoRenewing: Boolean = false,
    val startedAt: Timestamp? = null,
    val expiresAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun isCurrentlyActive(nowMillis: Long = System.currentTimeMillis()): Boolean {
        val exp = expiresAt?.toDate()?.time ?: 0L
        return status == "active" && exp > nowMillis
    }
}

@IgnoreExtraProperties
data class PurchaseRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val planId: String = "",
    val planName: String = "",
    val paymentMethod: String = "google_play",
    val amountInr: Long = 0L,
    val purchaseToken: String = "",
    val status: String = "pending_verification",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class PdfAccessLog(
    @DocumentId val id: String = "",
    val userId: String = "",
    val userEmail: String = "",
    val documentId: String = "",
    val documentTitle: String = "",
    val action: String = "authorized_open",
    @ServerTimestamp val createdAt: Timestamp? = null
)

@IgnoreExtraProperties
data class AuditLog(
    @DocumentId val id: String = "",
    val actorId: String = "",
    val actorEmail: String = "",
    val eventType: String = "",
    val targetId: String = "",
    val details: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String, val isNetworkError: Boolean = false) : UiState<Nothing>
}
