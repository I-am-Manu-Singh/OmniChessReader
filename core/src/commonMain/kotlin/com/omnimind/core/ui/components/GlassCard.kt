package com.creativeblisslabs.omnimind.core.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.*
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.creativeblisslabs.omnimind.core.ui.theme.*

@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 14.dp,
    borderAlpha: Float = 0.18f,
    onClick: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    val isLight = scheme.background.luminance() > 0.5f
    
    val baseModifier = if (onClick != null) {
        modifier.clip(RoundedCornerShape(cornerRadius)).clickable(onClick = onClick)
    } else {
        modifier.clip(RoundedCornerShape(cornerRadius))
    }

    Box(
        modifier = baseModifier
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        scheme.surface.copy(alpha = if (isLight) 1f else 0.98f),
                        scheme.surfaceVariant.copy(alpha = if (isLight) 1f else 0.94f)
                    )
                )
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        scheme.outline.copy(alpha = if (isLight) 0.35f else borderAlpha),
                        AmberWarm.copy(alpha = if (isLight) 0.12f else borderAlpha * 0.45f),
                        NeonCyan.copy(alpha = if (isLight) 0.10f else borderAlpha * 0.25f)
                    )
                ),
                shape = RoundedCornerShape(cornerRadius)
            ),
        content = content
    )
}

@Composable
fun GradientBackground(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit
) {
    val scheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier.background(
            Brush.radialGradient(
                colors = listOf(
                    AmberWarm.copy(alpha = if (scheme.isLight) 0.06f else 0.14f),
                    scheme.background,
                    scheme.background
                ),
                radius = 1200f
            )
        ),
        content = content
    )
}

private val androidx.compose.material3.ColorScheme.isLight: Boolean
    get() = background.luminance() > 0.5f

@Composable
fun AnimatedGradientText(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition()
    val offset by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing), RepeatMode.Reverse)
    )
    content()
}
