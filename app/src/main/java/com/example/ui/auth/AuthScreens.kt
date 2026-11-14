package com.example.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.EmeraldSuccess
import com.example.ui.theme.ErrorRecoveryBanner
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.LiquidActionButton
import com.example.ui.theme.LiquidCard
import com.example.ui.theme.LiquidMorphBackground
import com.example.ui.theme.LiquidSecondaryButton
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.PureWhite
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@Composable
fun LoginScreen(
    isOnline: Boolean,
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }
    var authError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    LiquidMorphBackground(intensity = 1.15f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.widthIn(max = 460.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(24.dp))
                        .background(Brush.linearGradient(listOf(LiquidYellow, GoldenYellow))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Shield,
                        contentDescription = "Secure Study",
                        tint = DeepBlack,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(22.dp))
                Text(
                    text = "Welcome to Secure Study",
                    style = MaterialTheme.typography.displaySmall,
                    color = PureWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Sign in with your verified Google account to sync subscription entitlements and access protected study PDFs.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedZinc,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(26.dp))

                LiquidCard(highlighted = true, modifier = Modifier.fillMaxWidth()) {
                    SecurityFeatureRow(
                        icon = Icons.Rounded.VerifiedUser,
                        title = "Server-Verified Subscriptions",
                        subtitle = "Zero-trust entitlement enforcement via Cloud Firestore & Google Play Billing."
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    SecurityFeatureRow(
                        icon = Icons.Rounded.Lock,
                        title = "Protected In-App PDF Reader",
                        subtitle = "FLAG_SECURE screen shield, dynamic user watermarks, and AES-256 temp cache."
                    )
                }

                if (!isOnline) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorRecoveryBanner(
                        title = "No Internet Connection",
                        message = "Connect to Wi-Fi or mobile data to sign in and verify your study entitlements.",
                        isNetworkError = true
                    )
                }

                if (!authError.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(16.dp))
                    ErrorRecoveryBanner(
                        title = "Sign-In Notice",
                        message = authError!!,
                        isNetworkError = !isOnline
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                GoogleSignInButton(
                    onAuthSuccess = {
                        authError = null
                        onAuthSuccess()
                    },
                    onAuthError = { err ->
                        authError = err
                    }
                )

                Spacer(modifier = Modifier.height(14.dp))
                Text(
                    text = "Your biometric credentials stay exclusively on your Android hardware.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedZinc,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun SecurityFeatureRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String
) {
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(LiquidYellow.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = LiquidYellow, modifier = Modifier.size(20.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.titleMedium, color = PureWhite)
            Spacer(modifier = Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MutedZinc)
        }
    }
}

@Composable
fun EmailVerificationScreen(
    email: String,
    isEmailVerified: Boolean,
    onContinueVerified: () -> Unit,
    onSignOut: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var statusMessage by remember { mutableStateOf<String?>(null) }
    var checking by remember { mutableStateOf(false) }

    LiquidMorphBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidCard(
                highlighted = isEmailVerified,
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth()
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(
                                if (isEmailVerified) EmeraldSuccess.copy(alpha = 0.18f)
                                else LiquidYellow.copy(alpha = 0.16f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isEmailVerified) Icons.Rounded.CheckCircle else Icons.Rounded.MarkEmailRead,
                            contentDescription = "Email Verification",
                            tint = if (isEmailVerified) EmeraldSuccess else LiquidYellow,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isEmailVerified) "Identity Verified" else "Verify Your Email",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = email,
                        style = MaterialTheme.typography.labelMedium,
                        color = LiquidYellow
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = if (isEmailVerified) {
                            "Your Google account email is cryptographically verified and ready for Secure Study access."
                        } else {
                            "Please verify your email address before accessing protected study PDFs, then refresh your verification status."
                        },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedZinc,
                        textAlign = TextAlign.Center
                    )
                    if (!statusMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = statusMessage!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = LiquidYellow,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    if (isEmailVerified) {
                        LiquidActionButton(
                            text = "Proceed to Dashboard",
                            onClick = onContinueVerified,
                            testTag = "email_verified_continue_button",
                            modifier = Modifier.fillMaxWidth()
                        )
                    } else {
                        LiquidActionButton(
                            text = if (checking) "Checking Status..." else "Refresh Verification Status",
                            icon = Icons.Rounded.Refresh,
                            enabled = !checking,
                            testTag = "refresh_verification_button",
                            modifier = Modifier.fillMaxWidth(),
                            onClick = {
                                checking = true
                                scope.launch {
                                    try {
                                        Firebase.auth.currentUser?.reload()?.await()
                                        val verified = Firebase.auth.currentUser?.isEmailVerified == true
                                        if (verified) {
                                            onContinueVerified()
                                        } else {
                                            Firebase.auth.currentUser?.sendEmailVerification()?.await()
                                            statusMessage = "Verification email sent. Check your inbox and tap Refresh again."
                                        }
                                    } catch (e: Exception) {
                                        statusMessage = e.localizedMessage ?: "Could not refresh verification status."
                                    } finally {
                                        checking = false
                                    }
                                }
                            }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    LiquidSecondaryButton(
                        text = "Switch Google Account",
                        onClick = onSignOut,
                        testTag = "verification_sign_out_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
fun SuspendedAccountScreen(
    email: String,
    onSignOut: () -> Unit
) {
    LiquidMorphBackground {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidCard(modifier = Modifier.widthIn(max = 460.dp).fillMaxWidth()) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .size(68.dp)
                            .clip(CircleShape)
                            .background(CrimsonError.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Block,
                            contentDescription = "Account Suspended",
                            tint = CrimsonError,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Account Suspended",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Access for $email has been suspended by an administrator. Protected PDFs and subscriptions are locked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedZinc,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(22.dp))
                    LiquidSecondaryButton(
                        text = "Sign Out",
                        onClick = onSignOut,
                        testTag = "suspended_sign_out_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
