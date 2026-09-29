package com.ridesync.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.R
import com.ridesync.data.model.FuelType
import com.ridesync.data.model.Vehicle
import com.ridesync.data.model.VehicleType
import com.ridesync.ui.profile.DateOfBirthCalendarField

@Composable
fun ProfileSetupScreen(
    initialDisplayName: String,
    onSaveProfile: (vehicleModel: String, tankCapacity: Double, shareLocation: Boolean, emergencyPhone: String) -> Unit,
    onSaveFullProfile: ((dateOfBirth: String, initialVehicle: Vehicle, shareLocation: Boolean, emergencyPhone: String) -> Unit)? = null,
    isLoading: Boolean
) {
    var dateOfBirth by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(VehicleType.BIKE.name) }
    var selectedFuelType by remember { mutableStateOf(FuelType.PETROL.name) }
    var brandName by remember { mutableStateOf("") }
    var modelName by remember { mutableStateOf("") }
    var capacityText by remember { mutableStateOf("15.0") }
    var mileageText by remember { mutableStateOf("35.0") }
    var currentFuelText by remember { mutableStateOf("10.0") }
    var emergencyPhone by remember { mutableStateOf("") }
    var shareLocation by remember { mutableStateOf(true) }

    val currentFuelVal = currentFuelText.toDoubleOrNull() ?: 0.0
    val mileageVal = mileageText.toDoubleOrNull() ?: 0.0
    val capacityVal = capacityText.toDoubleOrNull() ?: 0.0
    val calculatedRange = (currentFuelVal * mileageVal * 10.0).let { Math.round(it) / 10.0 }
    val currentFuelTypeEnum = try { FuelType.valueOf(selectedFuelType) } catch (_: Exception) { FuelType.PETROL }

    val backgroundColor = com.ridesync.ui.theme.HudColors.ObsidianCanvas
    val cardColor = com.ridesync.ui.theme.HudColors.ObsidianSurface
    val accentColor = com.ridesync.ui.theme.HudColors.CyanPrimary

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(backgroundColor)
            .padding(24.dp)
    ) {
        // High-Contrast Rally Instrument Graphic Background Pattern
        com.ridesync.ui.theme.RallyGridGraphicBackground()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Spacer(modifier = Modifier.height(16.dp))

                Image(
                    painter = painterResource(id = R.drawable.ic_app_logo_badge),
                    contentDescription = "RRS Logo",
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.height(56.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Welcome, $initialDisplayName!",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite
                )

                Text(
                    text = "Complete your profile, date of birth & vehicle specifications to calculate live trip range & refuel reminders.",
                    fontSize = 14.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    modifier = Modifier.padding(top = 6.dp, bottom = 24.dp)
                )

                // Date of Birth Calendar Field
                DateOfBirthCalendarField(
                    value = dateOfBirth,
                    onDateSelected = { dateOfBirth = it },
                    label = "Date of Birth (Select via Calendar)",
                    modifier = Modifier.padding(bottom = 20.dp)
                )

                Text(
                    text = "Primary Vehicle Details",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                    modifier = Modifier.padding(bottom = 12.dp)
                )

                // 1. Vehicle Type Selection
                Text(
                    text = "Vehicle Type",
                    fontSize = 13.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VehicleType.entries.forEach { type ->
                        val isSelected = selectedType == type.name
                        Surface(
                            onClick = { selectedType = type.name },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) accentColor.copy(alpha = 0.25f) else cardColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) accentColor else com.ridesync.ui.theme.HudColors.ObsidianBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = type.iconEmoji, fontSize = 18.sp)
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = type.displayName,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) accentColor else com.ridesync.ui.theme.HudColors.TextCrispWhite
                                )
                            }
                        }
                    }
                }

                // 2. Fuel Type Selection
                Text(
                    text = "Fuel Type",
                    fontSize = 13.sp,
                    color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FuelType.entries.forEach { fuel ->
                        val isSelected = selectedFuelType == fuel.name
                        Surface(
                            onClick = { selectedFuelType = fuel.name },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) accentColor else cardColor,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) accentColor else com.ridesync.ui.theme.HudColors.ObsidianBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = fuel.iconEmoji, fontSize = 13.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fuel.displayName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else com.ridesync.ui.theme.HudColors.TextCrispWhite
                                )
                            }
                        }
                    }
                }

                // Brand Name
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Vehicle Brand (e.g. Honda, Royal Enfield, Tesla)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                        focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                        unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                    )
                )

                // Model Name
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Vehicle Model (e.g. Classic 350, Thar, Model 3)") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.TwoWheeler, contentDescription = null, tint = accentColor)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                        focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                        unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                    )
                )

                // Capacity & Mileage & Current Fuel Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Capacity
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it },
                        label = { Text("Tank (${currentFuelTypeEnum.capacityUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardColor,
                            unfocusedContainerColor = cardColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                            focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                            unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                        )
                    )

                    // Mileage
                    OutlinedTextField(
                        value = mileageText,
                        onValueChange = { mileageText = it },
                        label = { Text("Mileage (${currentFuelTypeEnum.mileageUnit})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = cardColor,
                            unfocusedContainerColor = cardColor,
                            focusedBorderColor = accentColor,
                            unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                            focusedLabelColor = accentColor,
                            unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                            focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                            unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                        )
                    )
                }

                // Current Fuel Field
                OutlinedTextField(
                    value = currentFuelText,
                    onValueChange = { currentFuelText = it },
                    label = { Text("Current Fuel Available in Vehicle (${currentFuelTypeEnum.fuelUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocalGasStation, contentDescription = null, tint = accentColor)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                        focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                        unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                    )
                )

                // Range Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF0F2D2E),
                    border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(text = "Estimated Vehicle Range", fontSize = 12.sp, color = com.ridesync.ui.theme.HudColors.TextCoolSilver)
                            Text(text = "Current fuel × Mileage efficiency", fontSize = 11.sp, color = com.ridesync.ui.theme.HudColors.TextCoolSilver)
                        }
                        Text(
                            text = "$calculatedRange km",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = accentColor
                        )
                    }
                }

                // Emergency Contact Input
                OutlinedTextField(
                    value = emergencyPhone,
                    onValueChange = { emergencyPhone = it },
                    label = { Text("Emergency Contact Phone (Optional)") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Phone,
                            contentDescription = null,
                            tint = accentColor
                        )
                    },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 20.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = cardColor,
                        unfocusedContainerColor = cardColor,
                        focusedBorderColor = accentColor,
                        unfocusedBorderColor = com.ridesync.ui.theme.HudColors.ObsidianBorder,
                        focusedLabelColor = accentColor,
                        unfocusedLabelColor = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                        focusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                        unfocusedTextColor = com.ridesync.ui.theme.HudColors.TextCrispWhite
                    )
                )

                // Privacy Switch
                Surface(
                    color = cardColor,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Share Live Location with Convoy",
                                color = com.ridesync.ui.theme.HudColors.TextCrispWhite,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp
                            )
                            Text(
                                text = "Allows convoy members to track your position during active rides",
                                color = com.ridesync.ui.theme.HudColors.TextCoolSilver,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = shareLocation,
                            onCheckedChange = { shareLocation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = accentColor
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Save Button
            Button(
                onClick = {
                    val fullModel = listOf(brandName.trim(), modelName.trim()).filter { it.isNotBlank() }.joinToString(" ")
                    val initVehicle = Vehicle(
                        type = selectedType,
                        fuelType = selectedFuelType,
                        brandName = brandName.trim(),
                        model = modelName.trim(),
                        fuelTankCapacity = capacityVal,
                        mileage = mileageVal,
                        currentFuelAvailable = currentFuelVal,
                        isActive = true
                    )

                    if (onSaveFullProfile != null) {
                        onSaveFullProfile(dateOfBirth.trim(), initVehicle, shareLocation, emergencyPhone)
                    } else {
                        onSaveProfile(fullModel, capacityVal, shareLocation, emergencyPhone)
                    }
                },
                enabled = !isLoading && (brandName.isNotBlank() || modelName.isNotBlank()),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = Color.White
                )
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.5.dp
                    )
                } else {
                    Text(
                        text = "Complete Profile & Start",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
