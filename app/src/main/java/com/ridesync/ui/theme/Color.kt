package com.ridesync.ui.theme

import androidx.compose.ui.graphics.Color

// 3D Glassy Dark Obsidian & Azure Theme System
object HudColors {
    // Dark Obsidian Canvas & Surface Tones (High Contrast Dark Mode)
    val ObsidianCanvas = Color(0xFF0F172A)    // Dark Obsidian Canvas (#0F172A)
    val ObsidianSurface = Color(0xFF1E293B)   // Dark Slate Surface (#1E293B)
    val ObsidianElevated = Color(0xFF334155)  // Elevated Slate Container (#334155)
    val ObsidianBorder = Color(0xFF334155)    // Fine Slate Border (#334155)
    val ObsidianModal = Color(0xFF1E293B)     // Dark Slate Glass Modal (#1E293B)

    // Primary Azure & Lead Accents
    val CyanPrimary = Color(0xFF38BDF8)      // Bright Azure (#38BDF8)
    val CyanLight = Color(0xFF60A5FA)        // Light Azure Accent (#60A5FA)
    val CyanGlow = Color(0x3338BDF8)         // Azure Photon Glow
    val CobaltBlue = Color(0xFF2563EB)       // Cobalt Blue Route Ribbon

    val HazardSecondary = Color(0xFFF59E0B)  // Sunburst Amber Gold (#F59E0B)
    val SecondaryGlow = Color(0x33F59E0B)

    val TelemetryTertiary = Color(0xFF10B981)// Fresh Emerald Green (#10B981)
    val TertiaryGlow = Color(0x3310B981)

    // Status Palette & Halos (High Visibility HUD)
    val StatusRiding = Color(0xFF10B981)     // Fresh Emerald Green (#10B981)
    val StatusRidingGlow = Color(0x3310B981)

    val StatusStopped = Color(0xFFF59E0B)    // Sunburst Amber Gold (#F59E0B)
    val StatusStoppedGlow = Color(0x33F59E0B)

    val StatusDelayed = Color(0xFFDC2626)    // Warning Crimson (#DC2626)
    val StatusDelayedGlow = Color(0x33DC2626)

    val StatusSos = Color(0xFFB91C1C)        // Emergency SOS Beacon Red (#B91C1C)
    val StatusSosGlow = Color(0x40B91C1C)

    // Text Hierarchy (Crisp White & Cool Silver on Dark Slate - 100% WCAG AAA Legibility)
    val TextCrispWhite = Color(0xFFFFFFFF)   // Crisp White for titles (#FFFFFF)
    val TextCoolSilver = Color(0xFFCBD5E1)   // Cool Slate Silver for subtitles & labels (#CBD5E1)
    val TextMuted = Color(0xFF94A3B8)        // Muted Slate (#94A3B8)

    // 3D Bevel Rim Highlights & Frosted Glass Borders
    val RimHighlight = Color(0x4038BDF8)     // Azure Rim Highlight
    val RimHighlightCyan = Color(0x4060A5FA)
    val FrostedOverlay = Color(0xF21E293B)   // Translucent Dark Glass
    val FrostedBorder = Color(0xFF334155)
}

