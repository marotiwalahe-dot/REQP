package com.example.ui.theme

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun LiquidMorphBackground(
    modifier: Modifier = Modifier,
    intensity: Float = 1f,
    content: @Composable () -> Unit
) {
    val transition = rememberInfiniteTransition(label = "liquid_morph")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 9000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "liquid_phase"
    )
    val pulse by transition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 4200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "liquid_pulse"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DeepBlack)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            val topBlobCenter = Offset(
                x = w * (0.78f + 0.08f * cos(phase)),
                y = h * (0.14f + 0.04f * sin(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        LiquidYellow.copy(alpha = 0.16f * intensity),
                        GoldenYellow.copy(alpha = 0.05f * intensity),
                        Color.Transparent
                    ),
                    center = topBlobCenter,
                    radius = w * 0.62f * pulse
                ),
                radius = w * 0.62f * pulse,
                center = topBlobCenter
            )

            val bottomBlobCenter = Offset(
                x = w * (0.18f + 0.07f * sin(phase)),
                y = h * (0.86f + 0.04f * cos(phase))
            )
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        GoldenYellow.copy(alpha = 0.12f * intensity),
                        Color.Transparent
                    ),
                    center = bottomBlobCenter,
                    radius = w * 0.55f
                ),
                radius = w * 0.55f,
                center = bottomBlobCenter
            )

            // Subtle liquid wave ribbon near top
            val wavePath = Path().apply {
                val baseY = h * 0.11f
                moveTo(0f, baseY)
                var x = 0f
                val step = w / 16f
                while (x <= w + step) {
                    val norm = x / w
                    val yOffset = sin(norm * 2f * PI + phase).toFloat() * 14f
                    lineTo(x, baseY + yOffset)
                    x += step
                }
            }
            drawPath(
                path = wavePath,
                brush = Brush.horizontalGradient(
                    colors = listOf(
                        Color.Transparent,
                        LiquidYellow.copy(alpha = 0.14f * intensity),
                        GoldenYellow.copy(alpha = 0.08f * intensity),
                        Color.Transparent
                    )
                ),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
        }
        content()
    }
}

@Composable
fun LiquidProgressIndicator(
    progress: Float? = null,
    modifier: Modifier = Modifier
) {
    val transition = rememberInfiniteTransition(label = "liquid_progress")
    val waveShift by transition.animateFloat(
        initialValue = 0f,
        targetValue = (2f * PI).toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1600, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "wave_shift"
    )
    val indeterminateSweep by transition.animateFloat(
        initialValue = 0.18f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "indeterminate_sweep"
    )
    val fillFraction = (progress ?: indeterminateSweep).coerceIn(0.05f, 1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(10.dp)
            .clip(RoundedCornerShape(50))
            .background(CharcoalElevated)
    ) {
        val fillWidth = size.width * fillFraction
        val path = Path().apply {
            moveTo(0f, size.height)
            lineTo(0f, size.height * 0.2f)
            val segments = 12
            for (i in 0..segments) {
                val x = (fillWidth / segments) * i
                val waveY = size.height * 0.18f + sin(i * 0.8f + waveShift) * (size.height * 0.16f)
                lineTo(x, waveY)
            }
            lineTo(fillWidth, size.height)
            close()
        }
        drawPath(
            path = path,
            brush = Brush.horizontalGradient(listOf(GoldenYellow, LiquidYellow, AmberGlow))
        )
    }
}

@Composable
fun LiquidActionButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 400f),
        label = "btn_scale"
    )

    Button(
        onClick = onClick,
        enabled = enabled,
        interactionSource = interactionSource,
        shape = RoundedCornerShape(18.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = LiquidYellow,
            contentColor = DeepBlack,
            disabledContainerColor = CharcoalElevated,
            disabledContentColor = MutedZinc
        ),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .then(
                if (enabled) {
                    Modifier.shadow(
                        elevation = 14.dp,
                        shape = RoundedCornerShape(18.dp),
                        ambientColor = LiquidYellow.copy(alpha = 0.45f),
                        spotColor = LiquidYellow.copy(alpha = 0.45f)
                    )
                } else Modifier
            )
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiquidSecondaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    testTag: String = "",
    icon: ImageVector? = null,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.2.dp, if (enabled) LiquidYellow.copy(alpha = 0.6f) else CharcoalBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = LiquidYellow,
            disabledContentColor = MutedZinc
        ),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 12.dp),
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    ) {
        if (icon != null) {
            Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun LiquidCard(
    modifier: Modifier = Modifier,
    highlighted: Boolean = false,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val borderColor = if (highlighted) LiquidYellow.copy(alpha = 0.55f) else CharcoalBorder
    val bgColor = if (highlighted) WarmGoldSurface.copy(alpha = 0.65f) else ObsidianSurface
    val cardModifier = modifier
        .then(
            if (highlighted) {
                Modifier.shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(22.dp),
                    ambientColor = LiquidYellow.copy(alpha = 0.25f),
                    spotColor = LiquidYellow.copy(alpha = 0.25f)
                )
            } else Modifier
        )
        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)

    Card(
        modifier = cardModifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = bgColor),
        border = BorderStroke(1.dp, borderColor)
    ) {
        Column(modifier = Modifier.padding(16.dp), content = content)
    }
}

@Composable
fun StatusPill(
    label: String,
    containerColor: Color,
    contentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = containerColor,
        contentColor = contentColor,
        shape = RoundedCornerShape(50)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            maxLines = 1
        )
    }
}

@Composable
fun ErrorRecoveryBanner(
    title: String,
    message: String,
    isNetworkError: Boolean = false,
    onRetry: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    LiquidCard(
        modifier = modifier.fillMaxWidth(),
        highlighted = false
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(CrimsonError.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isNetworkError) Icons.Rounded.WifiOff else Icons.Rounded.WarningAmber,
                    contentDescription = title,
                    tint = CrimsonError
                )
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = PureWhite
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MutedZinc
                )
            }
        }
        if (onRetry != null) {
            Spacer(modifier = Modifier.height(12.dp))
            LiquidSecondaryButton(
                text = "Retry Connection",
                onClick = onRetry,
                icon = Icons.Rounded.Refresh,
                testTag = "error_retry_button",
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
