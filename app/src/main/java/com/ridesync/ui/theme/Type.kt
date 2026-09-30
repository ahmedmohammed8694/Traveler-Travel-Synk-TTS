package com.ridesync.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp

/**
 * High-Contrast Cockpit HUD Typography System Tokens for RIDERsYNK (RideSync).
 * Grounded in rapid glanceability on motorcycle handlebar vibration mounts.
 */
object HudTypographyTokens {
    // Primary Font Family: System Sans-serif (Chivo / Roboto fallback)
    val PrimaryFontFamily = FontFamily.SansSerif

    // Numbers / Telemetry Font: Tabular Monospace (Chivo Mono, Roboto Mono, Monospace)
    val TelemetryFontFamily = FontFamily.Monospace

    // 1. App Title (RIDERSYNK)
    val AppTitle = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05.em,
        color = HudColors.PrimaryText
    )

    // 2. Section Eyebrow Tags ([ACTIVE.SESSION_LIVE])
    val SectionEyebrow = TextStyle(
        fontFamily = TelemetryFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.08.em,
        color = HudColors.SecondaryText
    )

    val SectionEyebrowCyan = SectionEyebrow.copy(
        color = HudColors.CyanPrimary
    )

    // 3. Trip Headings (Western Ghats Alpine Rally)
    val TripHeading = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 18.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.em,
        color = HudColors.PrimaryText
    )

    // 4. Metric Figures (266 KM, 420 KM, 84 KPH)
    val MetricFigure = TextStyle(
        fontFamily = TelemetryFontFamily,
        fontSize = 22.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 0.em,
        color = HudColors.PrimaryText
    )

    val MetricFigureCyan = MetricFigure.copy(
        color = HudColors.CyanPrimary
    )

    val MetricFigureAmber = MetricFigure.copy(
        color = HudColors.WarningAmber
    )

    // 5. Convoy Roster Names (Marcus Vance, Alex Rivera)
    val ConvoyRosterName = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 14.sp,
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.em,
        color = HudColors.PrimaryText
    )

    // 6. Subtitles / Bike Specs (Yamaha Ténéré 700, BMW R1250GS)
    val SubtitleBikeSpec = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 12.sp,
        fontWeight = FontWeight.Normal,
        letterSpacing = 0.em,
        color = HudColors.SecondaryText
    )

    // 7. Badges / Roles (LEAD, SWEEPER, SLOT #2)
    val BadgeRole = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.05.em,
        color = HudColors.PrimaryText
    )

    // 8. Bottom Navigation Tabs
    val BottomNavTabActive = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.04.em,
        color = HudColors.CyanPrimary
    )

    val BottomNavTabInactive = TextStyle(
        fontFamily = PrimaryFontFamily,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.04.em,
        color = HudColors.MutedText
    )
}

/**
 * Standard Jetpack Compose Material3 Typography integration.
 */
val HudTypography = Typography(
    titleLarge = HudTypographyTokens.AppTitle,
    titleMedium = HudTypographyTokens.TripHeading,
    bodyLarge = HudTypographyTokens.ConvoyRosterName,
    bodyMedium = HudTypographyTokens.SubtitleBikeSpec,
    labelSmall = HudTypographyTokens.SectionEyebrow,
    labelMedium = HudTypographyTokens.BadgeRole
)

/**
 * Tactical Microcopy Brackets Formatter.
 * Formats technical section headers with tactical brackets e.g. [SYS.CONVOY.ACCESS]
 */
fun formatTacticalHeader(headerText: String): String {
    val clean = headerText.trim()
    return if (clean.startsWith("[") && clean.endsWith("]")) {
        clean.uppercase()
    } else {
        "[${clean.uppercase()}]"
    }
}

/**
 * Uppercase 7-Character Trip Code Formatter (e.g. RSS1041).
 */
fun formatTripCode(code: String): String {
    val cleaned = code.uppercase().replace(Regex("[^A-Z0-9]"), "")
    return if (cleaned.length >= 7) cleaned.take(7) else cleaned
}
