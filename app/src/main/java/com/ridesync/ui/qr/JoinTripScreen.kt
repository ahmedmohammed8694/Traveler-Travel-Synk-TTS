package com.ridesync.ui.qr

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.LatLng
import com.ridesync.data.model.ConvoyRole
import com.ridesync.data.model.JoinedRiderProfile
import com.ridesync.data.model.SavedTrip
import com.ridesync.data.model.TripCategory
import com.ridesync.data.model.UserProfile
import com.ridesync.data.repository.TripRepository
import com.ridesync.ui.theme.HudColors
import com.ridesync.ui.trip.TripFullDetailsDialog

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JoinTripScreen(
    userProfile: UserProfile,
    onTripJoined: (SavedTrip) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var activeTab by remember { mutableIntStateOf(0) } // 0: Scan QR, 1: Enter Code / Link

    var tripCodeInput by remember { mutableStateOf("") }
    var selectedPreviewTrip by remember { mutableStateOf<SavedTrip?>(null) }

    val allExistingTrips by TripRepository.tripsFlow.collectAsState()

    // Helper to resolve trip from input string (code, full URL, or ID)
    fun resolveTripFromCodeOrLink(input: String): SavedTrip {
        val clean = input.trim()
        val extractedCode = if (clean.contains("/join/")) {
            clean.substringAfter("/join/").takeWhile { it != '?' && it != '/' }
        } else if (clean.contains("code=")) {
            clean.substringAfter("code=").takeWhile { it != '&' }
        } else {
            clean
        }.uppercase()

        // 1. Search existing saved trips by lobbyCode or tripId
        val existing = allExistingTrips.firstOrNull {
            it.lobbyCode.equals(extractedCode, ignoreCase = true) ||
            it.tripId.equals(extractedCode, ignoreCase = true)
        }
        if (existing != null) return existing

        // 2. Fallback: Create dynamic clean trip preview for code
        val activeVehicle = userProfile.vehicles.firstOrNull { it.id == userProfile.activeVehicleId }
            ?: userProfile.vehicles.firstOrNull()
        
        return SavedTrip(
            tripId = "TRIP-JOINED-${System.currentTimeMillis()}",
            plannerId = "user_host_discovered",
            title = "Convoy Ride ($extractedCode)",
            originName = "Current Location",
            destinationName = "Destination",
            startLatLng = LatLng(17.3753, 78.4344),
            destLatLng = LatLng(17.4401, 78.3489),
            waypoints = emptyList(),
            waypointLatLngs = emptyList(),
            distanceKm = 0.0,
            durationMinutes = 0,
            role = ConvoyRole.MEMBER,
            category = TripCategory.UPCOMING,
            lobbyCode = extractedCode,
            scheduledDate = "Upcoming Ride",
            joinedRiders = emptyList()
        )
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(HudColors.ObsidianCanvas)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header Top Bar
            Surface(
                color = HudColors.ObsidianSurface,
                border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp)
                ) {
                    Text(
                        text = "Join Trip",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = HudColors.TextCrispWhite
                    )
                    Text(
                        text = "Scan QR code, enter trip invite code, or paste join link to inspect and join",
                        fontSize = 12.sp,
                        color = HudColors.TextCoolSilver,
                        modifier = Modifier.padding(top = 2.dp)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    // Tab Segmented Switcher (Scan QR Code vs Enter Code)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(HudColors.ObsidianCanvas, RoundedCornerShape(12.dp))
                            .border(1.dp, HudColors.ObsidianBorder, RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Surface(
                            onClick = { activeTab = 0 },
                            color = if (activeTab == 0) HudColors.CyanPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.QrCodeScanner,
                                    contentDescription = null,
                                    tint = if (activeTab == 0) Color.Black else HudColors.TextCoolSilver,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Scan QR Code",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 0) Color.Black else HudColors.TextCoolSilver
                                )
                            }
                        }

                        Surface(
                            onClick = { activeTab = 1 },
                            color = if (activeTab == 1) HudColors.CyanPrimary else Color.Transparent,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 10.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Key,
                                    contentDescription = null,
                                    tint = if (activeTab == 1) Color.Black else HudColors.TextCoolSilver,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Code / Link",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeTab == 1) Color.Black else HudColors.TextCoolSilver
                                )
                            }
                        }
                    }
                }
            }

            // Tab Content
            when (activeTab) {
                0 -> {
                    // QR Code Camera Scanner Mode
                    Box(modifier = Modifier.fillMaxSize()) {
                        QrCodeScannerScreen(
                            onQrCodeScanned = { scannedCode ->
                                val targetTrip = resolveTripFromCodeOrLink(scannedCode)
                                selectedPreviewTrip = targetTrip
                            },
                            onCancel = onCancel
                        )
                    }
                }

                1 -> {
                    // Manual Code or Join Link Mode
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Surface(
                            color = HudColors.ObsidianSurface,
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Enter Trip Code or Join Link",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudColors.TextCrispWhite
                                )
                                Text(
                                    text = "Example: RRS-9921 or https://ridesync.app/join/RRS-9921",
                                    fontSize = 12.sp,
                                    color = HudColors.TextCoolSilver,
                                    modifier = Modifier.padding(top = 2.dp, bottom = 12.dp)
                                )

                                OutlinedTextField(
                                    value = tripCodeInput,
                                    onValueChange = { tripCodeInput = it },
                                    placeholder = { Text("e.g. RRS-9921 or paste trip link", color = HudColors.TextCoolSilver) },
                                    singleLine = true,
                                    leadingIcon = {
                                        Icon(Icons.Default.Key, contentDescription = null, tint = HudColors.CyanPrimary)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = HudColors.ObsidianCanvas,
                                        unfocusedContainerColor = HudColors.ObsidianCanvas,
                                        focusedBorderColor = HudColors.CyanPrimary,
                                        unfocusedBorderColor = HudColors.ObsidianBorder,
                                        focusedTextColor = HudColors.TextCrispWhite,
                                        unfocusedTextColor = HudColors.TextCrispWhite
                                    )
                                )

                                Spacer(modifier = Modifier.height(14.dp))

                                Button(
                                    onClick = {
                                        if (tripCodeInput.isNotBlank()) {
                                            val targetTrip = resolveTripFromCodeOrLink(tripCodeInput)
                                            selectedPreviewTrip = targetTrip
                                        } else {
                                            Toast.makeText(context, "Please enter a valid trip code or join link", Toast.LENGTH_SHORT).show()
                                        }
                                    },
                                    enabled = tripCodeInput.isNotBlank(),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = HudColors.CyanPrimary,
                                        contentColor = Color.Black
                                    )
                                ) {
                                    Icon(Icons.Default.Search, contentDescription = null)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Inspect & View Trip Details", fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }
                        }

                        // Sample / Available Public Trips to Join
                        Text(
                            text = "🌐 Available Trips to Join",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudColors.TextCrispWhite
                        )

                        val availablePublicTrips = remember(allExistingTrips) {
                            allExistingTrips.take(4)
                        }

                        if (availablePublicTrips.isEmpty()) {
                            Text(
                                text = "No public trips currently available. Enter a trip code or scan a QR code above to join.",
                                color = HudColors.TextCoolSilver,
                                fontSize = 13.sp,
                                modifier = Modifier.padding(vertical = 8.dp)
                            )
                        }

                        availablePublicTrips.forEach { publicTrip ->
                            Surface(
                                color = HudColors.ObsidianSurface,
                                shape = RoundedCornerShape(16.dp),
                                border = BorderStroke(1.dp, HudColors.ObsidianBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        selectedPreviewTrip = publicTrip
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Surface(
                                                color = HudColors.CyanPrimary.copy(alpha = 0.2f),
                                                shape = RoundedCornerShape(6.dp)
                                            ) {
                                                Text(
                                                    text = publicTrip.lobbyCode.ifBlank { "RRS-1001" },
                                                    color = HudColors.CyanPrimary,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Black,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = publicTrip.title,
                                                color = HudColors.TextCrispWhite,
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.Bold,
                                                maxLines = 1
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "📍 ${publicTrip.originName} ➔ ${publicTrip.destinationName}",
                                            color = HudColors.TextCoolSilver,
                                            fontSize = 12.sp,
                                            maxLines = 1
                                        )
                                        Text(
                                            text = "👥 ${publicTrip.joinedRiders.size} Riders Joined • ${publicTrip.distanceKm.toInt()} KM",
                                            color = Color(0xFF10B981),
                                            fontSize = 11.sp
                                        )
                                    }

                                    Button(
                                        onClick = { selectedPreviewTrip = publicTrip },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B), contentColor = HudColors.CyanPrimary),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Preview Full Details Dialog
        val previewTrip = selectedPreviewTrip
        if (previewTrip != null) {
            TripFullDetailsDialog(
                trip = previewTrip,
                onDismiss = { selectedPreviewTrip = null },
                onLoadInPlanner = {},
                onLaunchTrip = {},
                onEditTrip = {},
                onDeleteTrip = {},
                onExitTrip = { selectedPreviewTrip = null },
                onViewItinerary = {},
                onJoinTrip = { tripToJoin ->
                    // Add current user to trip's joined riders roster
                    val activeVehicle = userProfile.vehicles.firstOrNull { it.id == userProfile.activeVehicleId }
                        ?: userProfile.vehicles.firstOrNull()

                    val riderProfile = JoinedRiderProfile(
                        riderId = userProfile.userId.ifBlank { "user_me" },
                        displayName = userProfile.displayName.ifBlank { "Ahmed (You)" },
                        bikeModel = activeVehicle?.fullDisplayName ?: userProfile.vehicleModel.ifBlank { "Royal Enfield Meteor 350" },
                        role = ConvoyRole.MEMBER,
                        status = "Joined & Confirmed",
                        experienceBadge = "Convoy Rider",
                        emergencyContact = userProfile.privacySettings.emergencyContactPhone.ifBlank { "+91 86868 71994" }
                    )

                    val updatedRiders = if (tripToJoin.joinedRiders.none { it.riderId == riderProfile.riderId || it.displayName == riderProfile.displayName }) {
                        tripToJoin.joinedRiders + riderProfile
                    } else {
                        tripToJoin.joinedRiders
                    }

                    val updatedTrip = tripToJoin.copy(
                        joinedRiders = updatedRiders,
                        activeRidersCount = updatedRiders.size,
                        category = if (tripToJoin.category == TripCategory.COMPLETED) TripCategory.UPCOMING else tripToJoin.category
                    )

                    // Persist joined trip to TripRepository
                    TripRepository.saveTrip(updatedTrip)

                    Toast.makeText(context, "🎉 You have joined '${updatedTrip.title}'!", Toast.LENGTH_LONG).show()
                    selectedPreviewTrip = null
                    onTripJoined(updatedTrip)
                }
            )
        }
    }
}
