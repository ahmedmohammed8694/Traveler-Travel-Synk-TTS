package com.ridesync.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private val DarkHudColorScheme = darkColorScheme(
    primary = HudColors.CyanPrimary,
    onPrimary = Color.White,
    primaryContainer = HudColors.CyanGlow,
    onPrimaryContainer = HudColors.CyanLight,
    secondary = HudColors.StatusRiding,
    onSecondary = Color.White,
    background = HudColors.ObsidianCanvas,
    onBackground = HudColors.TextCrispWhite,
    surface = HudColors.ObsidianSurface,
    onSurface = HudColors.TextCrispWhite,
    surfaceVariant = HudColors.ObsidianElevated,
    onSurfaceVariant = HudColors.TextCoolSilver,
    outline = HudColors.ObsidianBorder,
    error = HudColors.StatusSos,
    onError = Color.White
)

@Composable
fun RideSyncTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkHudColorScheme,
        content = content
    )
}

/**
 * Custom 3D Elevated Card Modifier with multi-stop linear gradient, top rim highlight, and drop shadow.
 */
fun Modifier.hud3dCard(
    shape: Shape = RoundedCornerShape(20.dp),
    elevation: Dp = 10.dp,
    startColor: Color = Color(0xFFFFFFFF),
    endColor: Color = Color(0xFFF1F5F9),
    rimColor: Color = HudColors.RimHighlight,
    borderWidth: Dp = 1.dp
): Modifier = this
    .shadow(elevation = elevation, shape = shape, clip = false)
    .clip(shape)
    .background(
        brush = Brush.verticalGradient(
            colors = listOf(startColor, endColor)
        )
    )
    .border(
        width = borderWidth,
        brush = Brush.verticalGradient(
            colors = listOf(rimColor, HudColors.ObsidianBorder)
        ),
        shape = shape
    )

/**
 * Custom Frosted Glass HUD Overlay Modifier with translucent backdrop & fine rim.
 */
fun Modifier.frostedGlassHud(
    shape: Shape = RoundedCornerShape(16.dp),
    backgroundColor: Color = HudColors.FrostedOverlay,
    borderColor: Color = HudColors.FrostedBorder,
    elevation: Dp = 8.dp
): Modifier = this
    .shadow(elevation = elevation, shape = shape, clip = false)
    .clip(shape)
    .background(backgroundColor)
    .border(
        width = 1.dp,
        color = borderColor,
        shape = shape
    )

/**
 * High-Contrast Dakar Rally Graphic Background Pattern.
 * Draws subtle instrument grid lines, rally crosshairs, and topographic contour accents.
 */
@Composable
fun RallyGridGraphicBackground(
    modifier: Modifier = Modifier,
    gridSpacingDp: Dp = 48.dp,
    lineColor: Color = HudColors.CyanPrimary.copy(alpha = 0.05f),
    accentColor: Color = HudColors.CobaltBlue.copy(alpha = 0.05f)
) {
    androidx.compose.foundation.Canvas(modifier = modifier.fillMaxSize()) {
        val spacingPx = gridSpacingDp.toPx()
        val width = size.width
        val height = size.height

        // Draw Vertical Rally Grid Lines
        var x = 0f
        while (x < width) {
            drawLine(
                color = lineColor,
                start = androidx.compose.ui.geometry.Offset(x, 0f),
                end = androidx.compose.ui.geometry.Offset(x, height),
                strokeWidth = 1f
            )
            x += spacingPx
        }

        // Draw Horizontal Rally Grid Lines
        var y = 0f
        while (y < height) {
            drawLine(
                color = lineColor,
                start = androidx.compose.ui.geometry.Offset(0f, y),
                end = androidx.compose.ui.geometry.Offset(width, y),
                strokeWidth = 1f
            )
            y += spacingPx
        }

        // Draw Rally Crosshair (+ Ticks) at Intersections
        x = spacingPx
        while (x < width) {
            y = spacingPx
            while (y < height) {
                // Horizontal tick
                drawLine(
                    color = accentColor,
                    start = androidx.compose.ui.geometry.Offset(x - 6f, y),
                    end = androidx.compose.ui.geometry.Offset(x + 6f, y),
                    strokeWidth = 2f
                )
                // Vertical tick
                drawLine(
                    color = accentColor,
                    start = androidx.compose.ui.geometry.Offset(x, y - 6f),
                    end = androidx.compose.ui.geometry.Offset(x, y + 6f),
                    strokeWidth = 2f
                )
                y += spacingPx * 2
            }
            x += spacingPx * 2
        }

        // Draw Topographic Contour Accent Paths
        val path = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.15f)
            cubicTo(
                width * 0.35f, height * 0.10f,
                width * 0.65f, height * 0.25f,
                width, height * 0.18f
            )
        }
        drawPath(
            path = path,
            color = HudColors.CyanPrimary.copy(alpha = 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2.5f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(12f, 8f))
            )
        )

        val path2 = androidx.compose.ui.graphics.Path().apply {
            moveTo(0f, height * 0.85f)
            cubicTo(
                width * 0.40f, height * 0.92f,
                width * 0.70f, height * 0.78f,
                width, height * 0.88f
            )
        }
        drawPath(
            path = path2,
            color = HudColors.CobaltBlue.copy(alpha = 0.08f),
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                width = 2f,
                pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(16f, 10f))
            )
        )
    }
}


