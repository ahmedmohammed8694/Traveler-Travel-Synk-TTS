package com.ridesync.ui.theme

import androidx.compose.ui.graphics.Color

// Apex Telemetry HUD Color System - Matched to Official Design Board
object HudColors {
    // Neutral Void Canvas & Carbon Surface Tones
    val ObsidianCanvas = Color(0xFF080C16)    // Neutral Dark Charcoal Canvas (#080C16)
    val ObsidianSurface = Color(0xFF0F131D)   // Deep Carbon Surface (#0F131D)
    val ObsidianElevated = Color(0xFF171B26)  // Machined Titanium Container (#171B26)
    val ObsidianBorder = Color(0x3300F3FF)    // Translucent Electric Cyan Border
    val ObsidianModal = Color(0xF20F131D)     // Translucent Carbon Glass Modal

    // Primary, Secondary & Tertiary Accents (From Official Design Board)
    val CyanPrimary = Color(0xFF00F3FF)      // Primary Electric Cyan (#00F3FF)
    val CyanLight = Color(0xFF7DF4FF)        // Luminous Cyan Fixed Accent (#7DF4FF)
    val CyanGlow = Color(0x3300F3FF)         // Primary Cyan Photon Glow
    val CobaltBlue = Color(0xFF0066FF)       // Ultramarine Route Ribbon

    val HazardSecondary = Color(0xFFFF5500)  // Secondary Hazard Orange (#FF5500)
    val SecondaryGlow = Color(0x33FF5500)

    val TelemetryTertiary = Color(0xFF00FF66)// Tertiary Active Green (#00FF66)
    val TertiaryGlow = Color(0x3300FF66)

    // Status Palette & Halos
    val StatusRiding = Color(0xFF00FF66)     // Active Telemetry Green (#00FF66)
    val StatusRidingGlow = Color(0x3300FF66)

    val StatusStopped = Color(0xFFFF5500)    // Secondary Hazard Orange (#FF5500)
    val StatusStoppedGlow = Color(0x33FF5500)

    val StatusDelayed = Color(0xFFFF3366)    // Warning Crimson
    val StatusDelayedGlow = Color(0x33FF3366)

    val StatusSos = Color(0xFFFF0055)        // Emergency SOS Beacon Red
    val StatusSosGlow = Color(0x40FF0055)

    // Text Hierarchy (From Design Board Specifications)
    val TextCrispWhite = Color(0xFFDFE2F1)   // High-Contrast Cyber White (#DFE2F1)
    val TextCoolSilver = Color(0xFFB9CACB)   // Cool Slate Variant (#B9CACB)
    val TextMuted = Color(0xFF849495)        // Muted Outline Slate (#849495)

    // 3D Bevel Rim Highlights & Frosted Glass Borders
    val RimHighlight = Color(0x4000F3FF)     // Electric Cyan Rim Highlight
    val RimHighlightCyan = Color(0x400066FF)
    val FrostedOverlay = Color(0xBF0F131D)   // Translucent Carbon Glass
    val FrostedBorder = Color(0x3300F3FF)
}

