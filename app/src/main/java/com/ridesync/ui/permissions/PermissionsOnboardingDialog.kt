package com.ridesync.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.theme.frostedGlassHud
import com.ridesync.ui.theme.hud3dCard

/**
 * Google Play Store Policy Compliant Permissions Onboarding Dialog.
 * Discloses the prominent rationale for GPS, Background Location, Floating Overlay HUD,
 * Camera, Emergency Calling, and Battery Optimization before requesting user authorization.
 */
@Composable
fun PermissionsOnboardingDialog(
    onAllGranted: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var refreshTrigger by remember { mutableIntStateOf(0) }

    fun checkPermission(permission: String): Boolean {
        return ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun checkOverlayPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }

    fun checkBatteryIgnored(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
            pm?.isIgnoringBatteryOptimizations(context.packageName) == true
        } else true
    }

    // Permission states
    val isLocationGranted = remember(refreshTrigger) { checkPermission(Manifest.permission.ACCESS_FINE_LOCATION) }
    val isBgLocationGranted = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            checkPermission(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        } else true
    }
    val isCameraGranted = remember(refreshTrigger) { checkPermission(Manifest.permission.CAMERA) }
    val isCallGranted = remember(refreshTrigger) { checkPermission(Manifest.permission.CALL_PHONE) }
    val isOverlayGranted = remember(refreshTrigger) { checkOverlayPermission() }
    val isBatteryOptimizedIgnored = remember(refreshTrigger) { checkBatteryIgnored() }

    val multiplePermLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) {
        refreshTrigger++
    }

    val overlayLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTrigger++
    }

    val batteryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) {
        refreshTrigger++
    }

    val allCoreGranted = isLocationGranted && isCameraGranted

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = HudColors.ObsidianModal,
        shape = RoundedCornerShape(24.dp),
        title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(HudColors.CyanPrimary.copy(alpha = 0.15f), CircleShape)
                        .border(1.dp, HudColors.CyanPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = null,
                        tint = HudColors.CyanPrimary,
                        modifier = Modifier.size(30.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "RideSync Safety & Device Setup",
                    color = HudColors.TextCrispWhite,
                    fontWeight = FontWeight.Black,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "Authorized per Google Play Safety & Touring Policies",
                    color = HudColors.CyanPrimary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "To keep your convoy connected and provide real-time crash SOS, RideSync requires the following permissions:",
                    color = HudColors.TextCoolSilver,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )

                // 1. Live GPS Location
                PermissionItemCard(
                    title = "Live GPS Location & Convoy Radar",
                    description = "Required to calculate your position on the map, speed telemetry, and relative distances from fellow riders.",
                    icon = Icons.Default.MyLocation,
                    isGranted = isLocationGranted,
                    onRequestClick = {
                        val perms = mutableListOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        multiplePermLauncher.launch(perms.toTypedArray())
                    }
                )

                // 2. Background Continuous Tracking
                PermissionItemCard(
                    title = "Screen-Off Background Tracking",
                    description = "Keeps live GPS convoy sharing active when your phone screen turns off or when navigating with Google Maps.",
                    icon = Icons.Default.Sync,
                    isGranted = isBgLocationGranted,
                    onRequestClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            multiplePermLauncher.launch(arrayOf(Manifest.permission.ACCESS_BACKGROUND_LOCATION))
                        }
                    }
                )

                // 3. Floating Screen Overlay (HUD over Google Maps)
                PermissionItemCard(
                    title = "Overlay on Phone Screen (HUD)",
                    description = "Displays the mini floating Convoy Radar speedometer and SOS button on top of Google Maps or other apps.",
                    icon = Icons.Default.Layers,
                    isGranted = isOverlayGranted,
                    onRequestClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            val intent = Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                            overlayLauncher.launch(intent)
                        }
                    }
                )

                // 4. Camera (Lobby QR Scan)
                PermissionItemCard(
                    title = "Camera (Lobby QR Joining)",
                    description = "Enables instant 1-tap joining of tour groups by scanning the Lead Captain's QR Code.",
                    icon = Icons.Default.CameraAlt,
                    isGranted = isCameraGranted,
                    onRequestClick = {
                        multiplePermLauncher.launch(arrayOf(Manifest.permission.CAMERA))
                    }
                )

                // 5. Emergency Calling
                PermissionItemCard(
                    title = "One-Tap Convoy Quick Calling",
                    description = "Directly dials Lead Captain or emergency contacts with glove-friendly large buttons during incidents.",
                    icon = Icons.Default.Phone,
                    isGranted = isCallGranted,
                    onRequestClick = {
                        multiplePermLauncher.launch(arrayOf(Manifest.permission.CALL_PHONE))
                    }
                )

                // 6. Battery Optimization Exemption
                PermissionItemCard(
                    title = "Battery Unrestricted Highway Mode",
                    description = "Prevents Android OS Doze mode from cutting off live GPS telemetry during 6+ hour cross-country tours.",
                    icon = Icons.Default.BatteryChargingFull,
                    isGranted = isBatteryOptimizedIgnored,
                    onRequestClick = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                batteryLauncher.launch(intent)
                            } catch (_: Exception) {
                                val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                batteryLauncher.launch(intent)
                            }
                        }
                    }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (allCoreGranted) {
                        onAllGranted()
                    } else {
                        // Request core bundle
                        val perms = mutableListOf(
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.CAMERA,
                            Manifest.permission.CALL_PHONE
                        )
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            perms.add(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        multiplePermLauncher.launch(perms.toTypedArray())
                    }
                },
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (allCoreGranted) HudColors.StatusRiding else HudColors.CyanPrimary,
                    contentColor = Color.Black
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(if (allCoreGranted) Icons.Default.CheckCircle else Icons.Default.LockOpen, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (allCoreGranted) "Ready! Enter RideSync Cockpit" else "Grant Permissions",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Configure Later",
                    color = HudColors.TextCoolSilver,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    )
}

@Composable
private fun PermissionItemCard(
    title: String,
    description: String,
    icon: ImageVector,
    isGranted: Boolean,
    onRequestClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .hud3dCard(
                shape = RoundedCornerShape(14.dp),
                startColor = if (isGranted) Color(0x2210B981) else HudColors.ObsidianElevated,
                endColor = HudColors.ObsidianSurface,
                rimColor = if (isGranted) HudColors.StatusRiding else HudColors.RimHighlight
            )
            .clickable(onClick = onRequestClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .background(
                        if (isGranted) HudColors.StatusRiding.copy(alpha = 0.2f) else HudColors.FrostedOverlay,
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) HudColors.StatusRiding else HudColors.CyanPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = HudColors.TextCrispWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = description,
                    color = HudColors.TextCoolSilver,
                    fontSize = 10.sp,
                    lineHeight = 14.sp
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Box(
                modifier = Modifier
                    .frostedGlassHud(
                        shape = RoundedCornerShape(10.dp),
                        backgroundColor = if (isGranted) Color(0x3310B981) else Color(0x3306B6D4),
                        borderColor = if (isGranted) Color(0xFF10B981) else Color(0xFF06B6D4)
                    )
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Text(
                    text = if (isGranted) "GRANTED ✓" else "ALLOW",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Black,
                    color = if (isGranted) Color(0xFF10B981) else Color(0xFF38BDF8)
                )
            }
        }
    }
}
