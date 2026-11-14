package com.example

import com.example.data.model.UserSubscription
import com.google.firebase.Timestamp
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date

class ExampleUnitTest {

    @Test
    fun subscriptionEntitlement_checksStatusAndExpiry() {
        val now = System.currentTimeMillis()
        val active = UserSubscription(
            userId = "u1",
            status = "active",
            expiresAt = Timestamp(Date(now + 86_400_000L))
        )
        val expired = UserSubscription(
            userId = "u1",
            status = "active",
            expiresAt = Timestamp(Date(now - 86_400_000L))
        )
        val revoked = UserSubscription(
            userId = "u1",
            status = "revoked",
            expiresAt = Timestamp(Date(now + 86_400_000L))
        )

        assertTrue(active.isCurrentlyActive(now))
        assertFalse(expired.isCurrentlyActive(now))
        assertFalse(revoked.isCurrentlyActive(now))
    }
}
