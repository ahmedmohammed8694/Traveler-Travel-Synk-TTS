package com.ridesync.ui.hud

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.StopReason
import kotlin.math.roundToInt

/**
 * Draggable, Minimizable Tactile HUD Action Pad.
 * Features:
 * 1. Movable anywhere on screen by dragging the handle / body.
 * 2. Minimizable into a tiny floating quick-action pill to maximize map view.
 * 3. Supports quick "I'm Stopping" & "SOS" trigger with haptic feedback.
 */
@Composable
fun GloveFriendlyActionPad(
    onStopReported: (StopReason) -> Unit,
    onSosReported: () -> Unit,
    modifier: Modifier = Modifier
) {
    val haptics = LocalHapticFeedback.current
    var showStopPickerModal by remember { mutableStateOf(false) }

    // Position offset for dragging anywhere on screen
    var offsetX by remember { mutableFloatStateOf(0f) }
    var offsetY by remember { mutableFloatStateOf(0f) }

    // Minimizable / Compact State
    var isMinimized by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .offset { IntOffset(offsetX.roundToInt(), offsetY.roundToInt()) }
            .pointerInput(Unit) {
                detectDragGestures { change, dragAmount ->
                    change.consume()
                    offsetX += dragAmount.x
                    offsetY += dragAmount.y
                }
            }
    ) {
        if (isMinimized) {
            // MINIMIZED SMALL FLOATING PILL
            Surface(
                color = Color(0xF00F172A),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0xFF38BDF8)),
                shadowElevation = 10.dp,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to Move",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Compact Round Stop Circle
                    Surface(
                        color = Color(0xFFD97706),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showStopPickerModal = true
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.WarningAmber, contentDescription = "Stop", tint = Color.Black, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // Compact Round SOS Circle
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = CircleShape,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSosReported()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Warning, contentDescription = "SOS", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isMinimized = false
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Expand", tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                    }
                }
            }
        } else {
            // SLEEK SMALL ROUND SHAPE FLOATING ACTION OPTIONS PAD
            Surface(
                color = Color(0xEE0F172A),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, Color(0xFF334155)),
                shadowElevation = 12.dp,
                modifier = Modifier.padding(4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag Handle",
                        tint = Color(0xFF94A3B8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))

                    // SMALL ROUND SHAPE OPTION: "I'M STOPPING"
                    Surface(
                        color = Color(0xFFD97706),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFBBF24)),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                showStopPickerModal = true
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.WarningAmber,
                                contentDescription = null,
                                tint = Color.Black,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Stopping",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.Black
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    // SMALL ROUND SHAPE OPTION: "SOS"
                    Surface(
                        color = Color(0xFFDC2626),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                        modifier = Modifier
                            .height(44.dp)
                            .clickable {
                                haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                                onSosReported()
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "SOS",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    // MINIMIZE BUTTON
                    IconButton(
                        onClick = {
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            isMinimized = true
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Remove,
                            contentDescription = "Minimize",
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }

    if (showStopPickerModal) {
        AlertDialog(
            onDismissRequest = { showStopPickerModal = false },
            containerColor = com.ridesync.ui.theme.HudColors.ObsidianSurface,
            title = {
                Text(
                    text = "Select Stop Reason",
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    StopReasonOptionCard(
                        title = "Fuel Stop ⛽",
                        icon = Icons.Default.LocalGasStation,
                        color = com.ridesync.ui.theme.HudColors.CyanPrimary,
                        onClick = {
                            onStopReported(StopReason.FUEL)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Food / Rest 🍔",
                        icon = Icons.Default.Restaurant,
                        color = com.ridesync.ui.theme.HudColors.StatusRiding,
                        onClick = {
                            onStopReported(StopReason.FOOD)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Breakdown 🛠️",
                        icon = Icons.Default.Build,
                        color = Color(0xFFEA580C),
                        onClick = {
                            onStopReported(StopReason.BREAKDOWN)
                            showStopPickerModal = false
                        }
                    )
                    StopReasonOptionCard(
                        title = "Short Rest ☕",
                        icon = Icons.Default.Coffee,
                        color = com.ridesync.ui.theme.HudColors.CobaltBlue,
                        onClick = {
                            onStopReported(StopReason.REST)
                            showStopPickerModal = false
                        }
                    )
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showStopPickerModal = false }) {
                    Text("Cancel", color = com.ridesync.ui.theme.HudColors.TextCoolSilver, fontSize = 15.sp)
                }
            }
        )
    }
}

@Composable
private fun StopReasonOptionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    onClick: () -> Unit
) {
    TactileGloveButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        minHeight = 56.dp,
        containerGradient = listOf(Color(0xFF0F172A), Color(0xFF020617)),
        accentGlow = color
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(26.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = title,
                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}
