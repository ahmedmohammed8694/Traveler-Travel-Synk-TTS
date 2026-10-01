package com.ridesync.ui.hud

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ridesync.ui.theme.HudColors

/**
 * Glove-Friendly Tactile 3D Physical Hardware Button Component.
 * Features state-driven gradients, 3D top rim bevels, physical press scaling (scale=0.96f),
 * haptic feedback, and a guaranteed minimum touch target height of 64dp for heavy gloves.
 */
@Composable
fun TactileGloveButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    containerGradient: List<Color> = listOf(Color(0xFF171B26), Color(0xFF0F131D)),
    accentGlow: Color? = null,
    minHeight: Dp = 64.dp,
    shape: RoundedCornerShape = RoundedCornerShape(20.dp),
    content: @Composable RowScope.() -> Unit
) {
    val haptics = LocalHapticFeedback.current
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1.0f,
        animationSpec = tween(durationMillis = 100),
        label = "button_press_scale"
    )

    LaunchedEffect(isPressed) {
        if (isPressed) {
            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
        }
    }

    val glowBorderColor = accentGlow?.copy(alpha = if (isPressed) 0.8f else 0.4f)
        ?: HudColors.RimHighlight

    val effectiveElevation = if (isPressed) 2.dp else 10.dp

    Box(
        modifier = modifier
            .scale(scale)
            .heightIn(min = minHeight)
            .shadow(
                elevation = effectiveElevation,
                shape = shape,
                clip = false
            )
            .clip(shape)
            .background(
                brush = Brush.verticalGradient(colors = containerGradient)
            )
            .border(
                width = 1.5.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        glowBorderColor,
                        Color.Transparent
                    )
                ),
                shape = shape
            )
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                enabled = enabled,
                onClick = onClick
            )
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            content()
        }
    }
}
