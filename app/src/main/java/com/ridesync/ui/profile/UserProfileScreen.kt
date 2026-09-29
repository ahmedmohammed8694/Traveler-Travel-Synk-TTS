package com.ridesync.ui.profile

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.R
import com.ridesync.data.model.FuelType
import com.ridesync.data.model.UserProfile
import com.ridesync.data.model.Vehicle
import com.ridesync.data.model.VehicleType
import com.ridesync.ui.theme.HudColors
import com.ridesync.util.rememberRiderAvatarBitmap
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(
    userProfile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isEditing by remember { mutableStateOf(false) }

    var displayName by remember(userProfile) { mutableStateOf(userProfile.displayName) }
    var dateOfBirth by remember(userProfile) { mutableStateOf(userProfile.dateOfBirth) }
    var emergencyContactPhone by remember(userProfile) { mutableStateOf(userProfile.privacySettings.emergencyContactPhone) }
    var email by remember(userProfile) { mutableStateOf(userProfile.email) }
    var mobileNumber by remember(userProfile) { mutableStateOf(userProfile.mobileNumber) }
    var photoUrl by remember(userProfile) { mutableStateOf(userProfile.photoUrl) }

    // Vehicles List State
    val vehiclesList = remember(userProfile) {
        mutableStateListOf<Vehicle>().apply {
            if (userProfile.vehicles.isNotEmpty()) {
                addAll(userProfile.vehicles)
            } else if (userProfile.vehicleModel.isNotBlank()) {
                // Legacy fallback vehicle creation
                add(
                    Vehicle(
                        id = UUID.randomUUID().toString(),
                        type = VehicleType.BIKE.name,
                        fuelType = FuelType.PETROL.name,
                        brandName = userProfile.vehicleModel.split(" ").firstOrNull() ?: "",
                        model = userProfile.vehicleModel.split(" ").drop(1).joinToString(" ").ifBlank { userProfile.vehicleModel },
                        fuelTankCapacity = userProfile.tankCapacityLiters,
                        mileage = 35.0,
                        currentFuelAvailable = 10.0,
                        isActive = true
                    )
                )
            }
        }
    }

    var activeVehicleId by remember(userProfile, vehiclesList.size) {
        mutableStateOf(
            userProfile.activeVehicleId.ifBlank {
                vehiclesList.firstOrNull { it.isActive }?.id ?: vehiclesList.firstOrNull()?.id ?: ""
            }
        )
    }

    // Active vehicle computation
    val activeVehicle = remember(vehiclesList, activeVehicleId) {
        vehiclesList.firstOrNull { it.id == activeVehicleId } ?: vehiclesList.firstOrNull()
    }

    // Add / Edit vehicle dialog state
    var showVehicleDialog by remember { mutableStateOf(false) }
    var vehicleToEdit by remember { mutableStateOf<Vehicle?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            photoUrl = uri.toString()
        }
    }

    val backgroundColor = HudColors.ObsidianCanvas
    val cardColor = HudColors.ObsidianSurface
    val accentColor = HudColors.CyanPrimary

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "RRS User Profile",
                        fontWeight = FontWeight.Bold,
                        color = HudColors.TextCrispWhite
                    )
                },
                actions = {
                    IconButton(onClick = { isEditing = !isEditing }) {
                        Icon(
                            imageVector = if (isEditing) Icons.Default.Save else Icons.Default.Edit,
                            contentDescription = if (isEditing) "Save Profile" else "Edit Profile",
                            tint = accentColor
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = backgroundColor,
                    titleContentColor = HudColors.TextCrispWhite
                )
            )
        },
        containerColor = backgroundColor
    ) { paddingValues ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // High-Contrast Rally Instrument Graphic Background Pattern
            com.ridesync.ui.theme.RallyGridGraphicBackground()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp)
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(12.dp))

                // User Profile & Avatar
                val avatarBitmap by rememberRiderAvatarBitmap(photoUrl)

                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(116.dp)
                        .clip(CircleShape)
                        .border(3.dp, accentColor, CircleShape)
                        .background(cardColor)
                        .then(
                            if (isEditing) Modifier.clickable { imagePickerLauncher.launch("image/*") }
                            else Modifier
                        )
                ) {
                    if (avatarBitmap != null) {
                        Image(
                            bitmap = avatarBitmap!!.asImageBitmap(),
                            contentDescription = "Rider Profile Photo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Image(
                            painter = painterResource(id = R.drawable.ic_app_logo_badge),
                            contentDescription = "RRS Profile Picture",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(76.dp)
                        )
                    }

                    if (isEditing) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.45f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = "Change Profile Photo",
                                    tint = Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                                Text(
                                    text = "CHANGE",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }

                if (isEditing) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { imagePickerLauncher.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                            border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                        ) {
                            Icon(imageVector = Icons.Default.CameraAlt, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (photoUrl.isNotBlank()) "Change Profile Photo" else "Upload Profile Photo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        if (photoUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.width(8.dp))
                            IconButton(onClick = { photoUrl = "" }) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Remove Photo",
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = displayName.ifBlank { "Rider" },
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = HudColors.TextCrispWhite
                )

                Text(
                    text = activeVehicle?.fullDisplayName?.let { "Default Ride: $it" } ?: "Riders Ride Sync (RRS) Member",
                    fontSize = 14.sp,
                    color = HudColors.TextCoolSilver,
                    modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
                )

                // ACTIVE VEHICLE HUD RANGE CARD (RECIRCULATING REMINDER)
                if (activeVehicle != null) {
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(18.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, accentColor),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 20.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = activeVehicle.vehicleTypeEnum.iconEmoji, fontSize = 22.sp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "★ DEFAULT VEHICLE FOR TRIPS",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = accentColor
                                    )
                                }
                                Text(
                                    text = activeVehicle.fullDisplayName,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudColors.TextCrispWhite
                                )
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            Divider(color = HudColors.ObsidianBorder)
                            Spacer(modifier = Modifier.height(10.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(text = "Current Fuel Level", fontSize = 11.sp, color = HudColors.TextCoolSilver)
                                    Text(
                                        text = "${activeVehicle.currentFuelAvailable} / ${activeVehicle.fuelTankCapacity} ${activeVehicle.fuelTypeEnum.fuelUnit}",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = HudColors.TextCrispWhite
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(text = "Available Range", fontSize = 11.sp, color = HudColors.TextCoolSilver)
                                    Text(
                                        text = "${activeVehicle.estimatedRangeKm} km",
                                        fontSize = 20.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = if (activeVehicle.isLowFuelAlert) Color(0xFFEF4444) else accentColor
                                    )
                                }
                            }

                            // Refuel Reminder Banner
                            if (activeVehicle.isLowFuelAlert) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Surface(
                                    color = Color(0xFF3B1212),
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier.padding(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Warning,
                                            contentDescription = null,
                                            tint = Color(0xFFEF4444),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Refuel Reminder: Fuel low (${activeVehicle.estimatedRangeKm} km remaining)! Please fill up your vehicle before taking on long convoy trips.",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFCA5A5)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Profile Detail Cards / Editable Fields
                if (isEditing) {
                    // Editable Mode
                    OutlinedProfileField(
                        value = displayName,
                        onValueChange = { displayName = it },
                        label = "Full Name",
                        icon = Icons.Default.Person,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    // Date of Birth Calendar Picker Field
                    DateOfBirthCalendarField(
                        value = dateOfBirth,
                        onDateSelected = { dateOfBirth = it },
                        label = "Date of Birth (Select via Calendar)",
                        enabled = true,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    OutlinedProfileField(
                        value = mobileNumber,
                        onValueChange = { mobileNumber = it },
                        label = "Register Mobile Number",
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = emergencyContactPhone,
                        onValueChange = { emergencyContactPhone = it },
                        label = "Emergency Contact Number",
                        icon = Icons.Default.Phone,
                        keyboardType = KeyboardType.Phone,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                    OutlinedProfileField(
                        value = email,
                        onValueChange = { email = it },
                        label = "Email ID",
                        icon = Icons.Default.Email,
                        keyboardType = KeyboardType.Email,
                        accentColor = accentColor,
                        cardColor = cardColor
                    )

                } else {
                    // Read-Only Detail View
                    ProfileDetailItem(label = "Full Name", value = displayName, icon = Icons.Default.Person, cardColor = cardColor)
                    
                    // Date of Birth Read-Only Calendar Field
                    DateOfBirthCalendarField(
                        value = dateOfBirth,
                        onDateSelected = { dateOfBirth = it },
                        label = "Date of Birth",
                        enabled = false,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    ProfileDetailItem(label = "Register Mobile Number", value = mobileNumber.ifBlank { "Not set" }, icon = Icons.Default.Phone, cardColor = cardColor)
                    ProfileDetailItem(label = "Emergency Contact Number", value = emergencyContactPhone.ifBlank { "Not set" }, icon = Icons.Default.Phone, cardColor = cardColor)
                    ProfileDetailItem(label = "Email ID", value = email.ifBlank { "Not set" }, icon = Icons.Default.Email, cardColor = cardColor)
                }

                Spacer(modifier = Modifier.height(16.dp))

                // MY VEHICLES / GARAGE SECTION
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "My Vehicles / Garage",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudColors.TextCrispWhite
                        )
                        Text(
                            text = "Add multiple vehicles (Car, Bike, Jeep, EV) & set default vehicle for trips",
                            fontSize = 11.sp,
                            color = HudColors.TextCoolSilver
                        )
                    }

                    OutlinedButton(
                        onClick = {
                            vehicleToEdit = null
                            showVehicleDialog = true
                        },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Vehicle", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                if (vehiclesList.isEmpty()) {
                    Surface(
                        color = cardColor,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(text = "🏍️ 🚗 🚙", fontSize = 32.sp)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "No Vehicles Added Yet",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = HudColors.TextCrispWhite
                            )
                            Text(
                                text = "Tap '+ Add Vehicle' above to add your Bike, Car, Jeep, or EV with fuel tank capacity & mileage range calculation.",
                                fontSize = 12.sp,
                                color = HudColors.TextCoolSilver,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                } else {
                    vehiclesList.forEach { vehicle ->
                        val isThisActive = vehicle.id == activeVehicleId
                        VehicleItemCard(
                            vehicle = vehicle,
                            isActive = isThisActive,
                            onSelectActive = {
                                activeVehicleId = vehicle.id
                                // Update active flags in list
                                for (i in vehiclesList.indices) {
                                    vehiclesList[i] = vehiclesList[i].copy(isActive = vehiclesList[i].id == vehicle.id)
                                }
                            },
                            onEdit = {
                                vehicleToEdit = vehicle
                                showVehicleDialog = true
                            },
                            onDelete = {
                                vehiclesList.remove(vehicle)
                                if (activeVehicleId == vehicle.id) {
                                    activeVehicleId = vehiclesList.firstOrNull()?.id ?: ""
                                }
                            },
                            modifier = Modifier.padding(bottom = 12.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Save Changes CTA (if editing)
                if (isEditing) {
                    Button(
                        onClick = {
                            val updatedVehicles = vehiclesList.map {
                                it.copy(isActive = it.id == activeVehicleId)
                            }
                            val updatedActiveVehicle = updatedVehicles.firstOrNull { it.id == activeVehicleId }
                                ?: updatedVehicles.firstOrNull()

                            val updated = userProfile.copy(
                                displayName = displayName.trim(),
                                dateOfBirth = dateOfBirth.trim(),
                                mobileNumber = mobileNumber.trim(),
                                email = email.trim(),
                                vehicleModel = updatedActiveVehicle?.fullDisplayName ?: userProfile.vehicleModel,
                                tankCapacityLiters = updatedActiveVehicle?.fuelTankCapacity ?: userProfile.tankCapacityLiters,
                                vehicles = updatedVehicles,
                                activeVehicleId = activeVehicleId,
                                photoUrl = photoUrl.trim(),
                                privacySettings = userProfile.privacySettings.copy(
                                    emergencyContactPhone = emergencyContactPhone.trim()
                                )
                            )
                            onSaveProfile(updated)
                            isEditing = false
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = Color.White
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Save, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile & Vehicle Changes", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { isEditing = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = cardColor,
                            contentColor = HudColors.TextCrispWhite
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Edit, contentDescription = null, tint = accentColor)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Profile Details", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Sign Out Button
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFDC2626)
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFECACA))
                    )
                ) {
                    Text("Sign Out of RRS", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // Vehicle Dialog
    if (showVehicleDialog) {
        AddEditVehicleDialog(
            vehicle = vehicleToEdit,
            onDismiss = { showVehicleDialog = false },
            onSaveVehicle = { savedVehicle ->
                val existingIndex = vehiclesList.indexOfFirst { it.id == savedVehicle.id }
                if (existingIndex >= 0) {
                    vehiclesList[existingIndex] = savedVehicle
                } else {
                    vehiclesList.add(savedVehicle)
                }

                if (savedVehicle.isActive || vehiclesList.size == 1) {
                    activeVehicleId = savedVehicle.id
                    for (i in vehiclesList.indices) {
                        vehiclesList[i] = vehiclesList[i].copy(isActive = vehiclesList[i].id == savedVehicle.id)
                    }
                }

                // Immediately trigger profile update if not in edit mode
                val updatedActive = vehiclesList.firstOrNull { it.id == activeVehicleId } ?: savedVehicle
                val updatedProfile = userProfile.copy(
                    vehicles = vehiclesList.toList(),
                    activeVehicleId = activeVehicleId,
                    vehicleModel = updatedActive.fullDisplayName,
                    tankCapacityLiters = updatedActive.fuelTankCapacity
                )
                onSaveProfile(updatedProfile)

                showVehicleDialog = false
            }
        )
    }
}

@Composable
private fun ProfileDetailItem(
    label: String,
    value: String,
    icon: ImageVector,
    cardColor: Color
) {
    Surface(
        color = cardColor,
        shape = RoundedCornerShape(14.dp),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .border(1.dp, HudColors.ObsidianBorder, RoundedCornerShape(14.dp))
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = HudColors.CyanPrimary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    color = HudColors.TextCoolSilver
                )
                Text(
                    text = value,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HudColors.TextCrispWhite,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun OutlinedProfileField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    keyboardType: KeyboardType = KeyboardType.Text,
    accentColor: Color,
    cardColor: Color
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor)
        },
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        singleLine = true,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = cardColor,
            unfocusedContainerColor = cardColor,
            focusedBorderColor = accentColor,
            unfocusedBorderColor = HudColors.ObsidianBorder,
            focusedLabelColor = accentColor,
            unfocusedLabelColor = HudColors.TextCoolSilver,
            focusedTextColor = HudColors.TextCrispWhite,
            unfocusedTextColor = HudColors.TextCrispWhite
        )
    )
}
