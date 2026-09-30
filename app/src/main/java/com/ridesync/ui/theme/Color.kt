package com.ridesync.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * High-Contrast Cockpit HUD Color System Tokens for RIDERsYNK (RideSync).
 * Grounded in ultra-glanceable high-contrast dark cockpit specifications for motorcycle handlebar mounts.
 */
object HudColors {
    // 1. Color Palette Tokens
    val CyanPrimary = Color(0xFF00F0FF)      // Primary Accent (Cyan): Active cockpit rider tag (YOU), interactive buttons (ENTER LIVE HUD, INSPECT & VIEW), focus rings, cursors, waypoint links
    val SuccessGreen = Color(0xFF10B981)     // Success / Connected: GPS synced dot, connected status, verified route badges, online mesh health
    val WarningAmber = Color(0xFFF59E0B)     // Warning / Caution: Sweeper role badges (SWEEPER), gap alerts, telemetry pace highlight
    val SosRed = Color(0xFFEF4444)           // SOS / Hazard Alert: Header emergency SOS trigger button, crash beacons, dropout warnings

    // Base Surfaces & Container Tokens
    val BaseDark = Color(0xFF0A0F1D)         // Base Surface (Deep Dark): Global canvas, outer app frame, top app bar & bottom navigation bar background
    val ElevatedContainer = Color(0xFF0F172A)// Elevated Container: Cards (Active Session, Saved Trips), search input box, roster sheets
    val SubtleContainer = Color(0xFF161B2A)  // Subtle Container / High: Inner cards, table rows, convoy roll call list items, input fields
    val StructuralBorder = Color(0xFF1E293B) // Structural Border: Card outlines, tab dividers, input field borders

    // Text Hierarchy Tokens
    val PrimaryText = Color(0xFFFFFFFF)      // Primary Text: Main trip titles, rider names, live speed & distance figures, primary button labels
    val SecondaryText = Color(0xFF94A3B8)    // Secondary Text: Field labels, bike models, host names, telemetry metadata headers
    val MutedText = Color(0xFF64748B)        // Muted / Placeholder Text: Search input placeholder (RSS1041), inactive bottom nav tabs, timestamps

    // Semantic Legacy Aliases for seamless component integration
    val ObsidianCanvas = BaseDark
    val ObsidianSurface = ElevatedContainer
    val ObsidianElevated = SubtleContainer
    val ObsidianBorder = StructuralBorder
    val ObsidianModal = ElevatedContainer

    val CyanLight = Color(0xFF38BDF8)
    val CyanGlow = Color(0x3300F0FF)
    val CobaltBlue = Color(0xFF0284C7)

    val StatusRiding = SuccessGreen
    val StatusRidingGlow = Color(0xFF22C55E)

    val StatusStopped = WarningAmber
    val StatusStoppedGlow = Color(0xFFFBBF24)

    val StatusDelayed = WarningAmber
    val StatusDelayedGlow = Color(0xFFFBBF24)

    val StatusSos = SosRed
    val StatusSosGlow = Color(0xFFF87171)

    val TextCrispWhite = PrimaryText
    val TextCoolSilver = SecondaryText
    val TextMuted = MutedText

    val RimHighlight = Color(0x4000F0FF)
    val RimHighlightCyan = Color(0x400284C7)
    val FrostedOverlay = Color(0xF20F172A)
    val FrostedBorder = StructuralBorder
}

