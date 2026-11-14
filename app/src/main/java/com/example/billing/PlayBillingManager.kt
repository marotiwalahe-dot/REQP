package com.example.billing

import android.app.Activity
import android.content.Context
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface BillingEvent {
    data object Idle : BillingEvent
    data class ProductCatalogLoaded(val products: Map<String, ProductDetails>) : BillingEvent
    data class PurchaseSubmittedForVerification(
        val productId: String,
        val purchaseToken: String,
        val isPending: Boolean
    ) : BillingEvent
    data class Cancelled(val message: String) : BillingEvent
    data class Error(val message: String) : BillingEvent
}

class PlayBillingManager(context: Context) : PurchasesUpdatedListener {

    private val _billingReady = MutableStateFlow(false)
    val billingReady: StateFlow<Boolean> = _billingReady.asStateFlow()

    private val _productDetailsMap = MutableStateFlow<Map<String, ProductDetails>>(emptyMap())
    val productDetailsMap: StateFlow<Map<String, ProductDetails>> = _productDetailsMap.asStateFlow()

    private val _billingEvent = MutableStateFlow<BillingEvent>(BillingEvent.Idle)
    val billingEvent: StateFlow<BillingEvent> = _billingEvent.asStateFlow()

    private val billingClient: BillingClient = BillingClient.newBuilder(context.applicationContext)
        .setListener(this)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .enablePrepaidPlans()
                .build()
        )
        .build()

    fun startConnection(productIds: List<String> = DEFAULT_SUBSCRIPTION_PRODUCT_IDS) {
        if (billingClient.isReady) {
            _billingReady.value = true
            querySubscriptionProducts(productIds)
            return
        }
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(billingResult: BillingResult) {
                if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                    _billingReady.value = true
                    querySubscriptionProducts(productIds)
                    queryExistingPurchases()
                } else {
                    _billingReady.value = false
                }
            }

            override fun onBillingServiceDisconnected() {
                _billingReady.value = false
            }
        })
    }

    fun querySubscriptionProducts(productIds: List<String> = DEFAULT_SUBSCRIPTION_PRODUCT_IDS) {
        if (!billingClient.isReady || productIds.isEmpty()) return
        val productList = productIds.distinct().map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(BillingClient.ProductType.SUBS)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { billingResult, productDetailsList ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                val map = productDetailsList.associateBy { it.productId }
                _productDetailsMap.value = map
                _billingEvent.value = BillingEvent.ProductCatalogLoaded(map)
            }
        }
    }

    fun launchSubscriptionPurchase(activity: Activity, productId: String): Boolean {
        val details = _productDetailsMap.value[productId]
        if (!billingClient.isReady || details == null) {
            _billingEvent.value = BillingEvent.Error(
                "Google Play product '$productId' is unavailable on this device/console track. Use PhonePe UPI QR or configure Play Console subscription products."
            )
            return false
        }

        val offerToken = details.subscriptionOfferDetails?.firstOrNull()?.offerToken
        if (offerToken == null) {
            _billingEvent.value = BillingEvent.Error("No active subscription offer found for $productId.")
            return false
        }

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .setOfferToken(offerToken)
            .build()

        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            _billingEvent.value = BillingEvent.Error(
                "Unable to launch Google Play Billing (${result.debugMessage})"
            )
            return false
        }
        return true
    }

    fun queryExistingPurchases() {
        if (!billingClient.isReady) return
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(BillingClient.ProductType.SUBS)
            .build()

        billingClient.queryPurchasesAsync(params) { billingResult, purchases ->
            if (billingResult.responseCode == BillingClient.BillingResponseCode.OK) {
                purchases.forEach { handlePurchase(it) }
            }
        }
    }

    override fun onPurchasesUpdated(
        billingResult: BillingResult,
        purchases: MutableList<Purchase>?
    ) {
        when (billingResult.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                purchases?.forEach { handlePurchase(it) }
            }
            BillingClient.BillingResponseCode.USER_CANCELED -> {
                _billingEvent.value = BillingEvent.Cancelled("Google Play purchase was cancelled.")
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                queryExistingPurchases()
            }
            else -> {
                _billingEvent.value = BillingEvent.Error(
                    "Google Play Billing failed (${billingResult.responseCode}): ${billingResult.debugMessage}"
                )
            }
        }
    }

    private fun handlePurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        when (purchase.purchaseState) {
            Purchase.PurchaseState.PURCHASED -> {
                if (!purchase.isAcknowledged) {
                    val ackParams = AcknowledgePurchaseParams.newBuilder()
                        .setPurchaseToken(purchase.purchaseToken)
                        .build()
                    billingClient.acknowledgePurchase(ackParams) { /* handled server-side too */ }
                }
                _billingEvent.value = BillingEvent.PurchaseSubmittedForVerification(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    isPending = false
                )
            }
            Purchase.PurchaseState.PENDING -> {
                _billingEvent.value = BillingEvent.PurchaseSubmittedForVerification(
                    productId = productId,
                    purchaseToken = purchase.purchaseToken,
                    isPending = true
                )
            }
            else -> Unit
        }
    }

    fun clearEvent() {
        _billingEvent.value = BillingEvent.Idle
    }

    fun endConnection() {
        if (billingClient.isReady) {
            billingClient.endConnection()
        }
    }

    companion object {
        val DEFAULT_SUBSCRIPTION_PRODUCT_IDS = listOf(
            "secure_study_monthly",
            "secure_study_quarterly",
            "secure_study_yearly"
        )
    }
}
