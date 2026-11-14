package com.example.ui.onboarding

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MenuBook
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.ui.theme.CharcoalElevated
import com.example.ui.theme.CrimsonError
import com.example.ui.theme.DeepBlack
import com.example.ui.theme.GoldenYellow
import com.example.ui.theme.LiquidActionButton
import com.example.ui.theme.LiquidCard
import com.example.ui.theme.LiquidMorphBackground
import com.example.ui.theme.LiquidProgressIndicator
import com.example.ui.theme.LiquidSecondaryButton
import com.example.ui.theme.LiquidYellow
import com.example.ui.theme.MutedZinc
import com.example.ui.theme.PureWhite
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun SplashScreen(
    onFinished: () -> Unit
) {
    LaunchedEffect(Unit) {
        delay(1250L)
        onFinished()
    }

    val pulseTransition = rememberInfiniteTransition(label = "splash_pulse")
    val scale by pulseTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.06f,
        animationSpec = infiniteRepeatable(
            animation = tween(1100, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "logo_pulse"
    )

    LiquidMorphBackground(intensity = 1.25f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(28.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.widthIn(max = 380.dp)
            ) {
                Box(
                    modifier = Modifier.size((110 * scale).dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    LiquidYellow.copy(alpha = 0.35f),
                                    GoldenYellow.copy(alpha = 0.08f),
                                    Color.Transparent
                                )
                            )
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(84.dp)
                            .clip(RoundedCornerShape(26.dp))
                            .background(Brush.linearGradient(listOf(LiquidYellow, GoldenYellow))),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shield,
                            contentDescription = "Secure Study Shield",
                            tint = DeepBlack,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "SECURE STUDY",
                    style = MaterialTheme.typography.displaySmall,
                    color = PureWhite,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "ENCRYPTED ACADEMIC VAULT",
                    style = MaterialTheme.typography.labelMedium,
                    color = LiquidYellow
                )
                Spacer(modifier = Modifier.height(36.dp))
                LiquidProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .testTag("splash_liquid_progress")
                )
            }
        }
    }
}

private data class OnboardingPage(
    val title: String,
    val subtitle: String,
    val badge: String,
    val icon: ImageVector
)

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit
) {
    val pages = listOf(
        OnboardingPage(
            title = "Zero-Trust Protected Study PDFs",
            subtitle = "Every premium handbook stays encrypted inside the vault, protected by FLAG_SECURE, dynamic watermarks, and short-lived access.",
            badge = "DOCUMENT SHIELD",
            icon = Icons.Rounded.Security
        ),
        OnboardingPage(
            title = "Device-Native Biometric Vault",
            subtitle = "Lock your library with Android Fingerprint, Face, or Device PIN. Biometric credentials never leave your hardware.",
            badge = "BIOMETRIC LOCK",
            icon = Icons.Rounded.Fingerprint
        ),
        OnboardingPage(
            title = "Verified Pro Subscriptions",
            subtitle = "Unlock all study categories seamlessly with Google Play Billing or verified UPI plans synced across your account.",
            badge = "PRO ACCESS",
            icon = Icons.Rounded.MenuBook
        )
    )

    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()

    LiquidMorphBackground {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "SECURE STUDY",
                    style = MaterialTheme.typography.labelMedium,
                    color = LiquidYellow
                )
                TextButton(
                    onClick = onComplete,
                    modifier = Modifier.testTag("onboarding_skip_button")
                ) {
                    Text("Skip", color = MutedZinc)
                }
            }

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .widthIn(max = 520.dp)
            ) { pageIndex ->
                val page = pages[pageIndex]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(160.dp)
                            .clip(RoundedCornerShape(42.dp))
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        LiquidYellow.copy(alpha = 0.24f),
                                        CharcoalElevated
                                    ),
                                    center = Offset(120f, 120f)
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = page.icon,
                            contentDescription = page.title,
                            tint = LiquidYellow,
                            modifier = Modifier.size(72.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(
                        text = page.badge,
                        style = MaterialTheme.typography.labelMedium,
                        color = LiquidYellow
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = page.title,
                        style = MaterialTheme.typography.displaySmall,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = page.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MutedZinc,
                        textAlign = TextAlign.Center
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(pages.size) { index ->
                    val active = pagerState.currentPage == index
                    Box(
                        modifier = Modifier
                            .height(8.dp)
                            .width(if (active) 28.dp else 8.dp)
                            .clip(CircleShape)
                            .background(if (active) LiquidYellow else CharcoalElevated)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            val isLast = pagerState.currentPage == pages.lastIndex
            LiquidActionButton(
                text = if (isLast) "Enter Secure Study" else "Next",
                icon = Icons.AutoMirrored.Rounded.ArrowForward,
                testTag = "onboarding_next_button",
                modifier = Modifier
                    .widthIn(max = 460.dp)
                    .fillMaxWidth(),
                onClick = {
                    if (isLast) {
                        onComplete()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
fun BiometricLockScreen(
    errorMessage: String?,
    onUnlockRequested: () -> Unit,
    onSignOutRequested: () -> Unit
) {
    LiquidMorphBackground(intensity = 1.15f) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            LiquidCard(
                highlighted = true,
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
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(LiquidYellow.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Lock,
                            contentDescription = "Vault Locked",
                            tint = LiquidYellow,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(18.dp))
                    Text(
                        text = "Study Vault Locked",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PureWhite,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Authenticate with your Android biometric sensor or device screen lock to resume protected study.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MutedZinc,
                        textAlign = TextAlign.Center
                    )
                    if (!errorMessage.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = CrimsonError,
                            textAlign = TextAlign.Center
                        )
                    }
                    Spacer(modifier = Modifier.height(22.dp))
                    LiquidActionButton(
                        text = "Unlock with Biometrics",
                        icon = Icons.Rounded.Fingerprint,
                        onClick = onUnlockRequested,
                        testTag = "biometric_unlock_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    LiquidSecondaryButton(
                        text = "Sign Out Account",
                        onClick = onSignOutRequested,
                        testTag = "biometric_sign_out_button",
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}
