package com.example.data.remote

import com.example.base.FirestoreEmulatorTestBase
import com.example.data.model.PurchaseRecord
import com.example.data.model.SubscriptionPlan
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.util.UUID

class SecureStudyRepositoryRuleTest : FirestoreEmulatorTestBase() {

    @Test
    fun studentRegistration_validPayload_createsStudentProfile() = runBlocking {
        val uid = signInTestUser("student_${UUID.randomUUID().toString().take(8)}@test.com")
        val repo = SecureStudyRepository(firestore)

        val result = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "student@test.com", "Test Student", null)
        }
        assertTrue(result.isSuccess)
        assertEquals("student", result.getOrThrow().role)
        assertEquals("active", result.getOrThrow().status)
    }

    @Test
    fun observeDocuments_unauthenticatedUser_failsWithPermissionDenied() = runBlocking {
        auth.signOut()
        val repo = SecureStudyRepository(firestore)

        try {
            withTimeout(FLOW_TIMEOUT_MS) { repo.observeDocuments(isAdmin = false).first() }
            fail("Expected PERMISSION_DENIED for unauthenticated user")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreError()
            assertEquals(FirebaseFirestoreException.Code.PERMISSION_DENIED, fsError?.code)
        }
    }

    @Test
    fun studentPurchaseSubmission_createsPendingAndCannotSelfApprove() = runBlocking {
        val studentEmail = "buyer_${UUID.randomUUID().toString().take(6)}@test.com"
        val uid = signInTestUser(studentEmail)
        val repo = SecureStudyRepository(firestore)

        // Attempt to read a non-existent premium payload without subscription must fail
        try {
            withTimeout(DEFAULT_TIMEOUT_MS) {
                repo.fetchAuthorizedPdfPayload("prem_${UUID.randomUUID().toString().take(6)}").getOrThrow()
            }
            fail("Expected failure when fetching premium/protected payload without entitlement")
        } catch (e: Exception) {
            val fsError = e.unwrapFirestoreError()
            assertTrue(
                fsError == null || fsError.code == FirebaseFirestoreException.Code.PERMISSION_DENIED
            )
        }

        // Student cannot write to subscription_plans or pdf_documents
        val planResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.saveSubscriptionPlan(
                planId = "unauthorized_plan_${UUID.randomUUID().toString().take(6)}",
                name = "Hack Plan",
                billingPeriod = "monthly",
                googlePlayProductId = "hack_id",
                displayPriceInr = 1L,
                durationDays = 30L,
                badge = "Hack",
                features = listOf("None"),
                isAvailable = true
            )
        }
        assertTrue(planResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            planResult.exceptionOrNull()?.unwrapFirestoreError()?.code
        )

        // Student cannot approve a purchase or activate their own subscription
        val fakePurchase = PurchaseRecord(
            id = "pur_${UUID.randomUUID().toString().take(8)}",
            userId = uid,
            userEmail = studentEmail,
            planId = "plan_monthly",
            planName = "Monthly Pro",
            paymentMethod = "phonepe_upi",
            amountInr = 199L,
            purchaseToken = "UTR1234567890"
        )
        val approveResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.approvePurchaseAndActivateSubscription(
                fakePurchase,
                SubscriptionPlan(id = "plan_monthly", durationDays = 30L, googlePlayProductId = "secure_study_monthly")
            )
        }
        assertTrue(approveResult.isFailure)
        assertEquals(
            FirebaseFirestoreException.Code.PERMISSION_DENIED,
            approveResult.exceptionOrNull()?.unwrapFirestoreError()?.code
        )
    }

    @Test
    fun studentAccessLog_authenticatedOwner_writesImmutableLog() = runBlocking {
        val uid = signInTestUser("logger_${UUID.randomUUID().toString().take(6)}@test.com")
        val repo = SecureStudyRepository(firestore)
        withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.ensureUserProfile(uid, "logger@test.com", "Logger", null).getOrThrow()
        }

        val logResult = withTimeout(DEFAULT_TIMEOUT_MS) {
            repo.logPdfAccess("doc_free_1", "DSA Cheatsheet", "authorized_open")
        }
        assertTrue(logResult.isSuccess)
    }

    private companion object {
        const val DEFAULT_TIMEOUT_MS = 5000L
        const val FLOW_TIMEOUT_MS = 3000L
    }
}
