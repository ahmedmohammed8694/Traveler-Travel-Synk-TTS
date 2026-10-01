package com.ridesync.ui.profile

import android.app.DatePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ElectricBike
import androidx.compose.material.icons.filled.ElectricCar
import androidx.compose.material.icons.filled.LocalGasStation
import androidx.compose.material.icons.filled.RadioButtonUnchecked
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ridesync.data.model.FuelType
import com.ridesync.data.model.Vehicle
import com.ridesync.data.model.VehicleType
import com.ridesync.ui.theme.HudColors
import java.util.Calendar
import java.util.UUID

/**
 * Interactive Date Picker Button / Field that triggers a Calendar Dialog.
 */
@Composable
fun DateOfBirthCalendarField(
    value: String,
    onDateSelected: (String) -> Unit,
    label: String = "Date of Birth",
    enabled: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Parse existing date if valid YYYY-MM-DD
    if (value.isNotBlank() && value.contains("-")) {
        val parts = value.split("-")
        if (parts.size == 3) {
            val y = parts[0].toIntOrNull()
            val m = parts[1].toIntOrNull()?.minus(1)
            val d = parts[2].toIntOrNull()
            if (y != null && m != null && d != null) {
                calendar.set(Calendar.YEAR, y)
                calendar.set(Calendar.MONTH, m)
                calendar.set(Calendar.DAY_OF_MONTH, d)
            }
        }
    }

    val datePickerDialog = DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    )

    // Limit date of birth to past dates
    datePickerDialog.datePicker.maxDate = System.currentTimeMillis()

    OutlinedTextField(
        value = value,
        onValueChange = {},
        readOnly = true,
        enabled = enabled,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.CalendarToday,
                contentDescription = "Select Date of Birth from Calendar",
                tint = HudColors.CyanPrimary
            )
        },
        trailingIcon = {
            IconButton(
                onClick = { if (enabled) datePickerDialog.show() },
                enabled = enabled
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarToday,
                    contentDescription = "Open Calendar",
                    tint = HudColors.CyanPrimary
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(enabled = enabled) { datePickerDialog.show() },
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = HudColors.ObsidianSurface,
            unfocusedContainerColor = HudColors.ObsidianSurface,
            disabledContainerColor = HudColors.ObsidianSurface,
            focusedBorderColor = HudColors.CyanPrimary,
            unfocusedBorderColor = HudColors.ObsidianBorder,
            disabledBorderColor = HudColors.ObsidianBorder,
            focusedLabelColor = HudColors.CyanPrimary,
            unfocusedLabelColor = HudColors.TextCoolSilver,
            disabledLabelColor = HudColors.TextCoolSilver,
            focusedTextColor = HudColors.TextCrispWhite,
            unfocusedTextColor = HudColors.TextCrispWhite,
            disabledTextColor = HudColors.TextCrispWhite
        )
    )
}

/**
 * Dialog for adding or editing a Vehicle (Bike, Car, Jeep, Others).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditVehicleDialog(
    vehicle: Vehicle? = null,
    onDismiss: () -> Unit,
    onSaveVehicle: (Vehicle) -> Unit
) {
    val isEdit = vehicle != null

    var selectedType by remember { mutableStateOf(vehicle?.type ?: VehicleType.BIKE.name) }
    var selectedFuelType by remember { mutableStateOf(vehicle?.fuelType ?: FuelType.PETROL.name) }
    var brandName by remember { mutableStateOf(vehicle?.brandName ?: "") }
    var modelName by remember { mutableStateOf(vehicle?.model ?: "") }
    var registrationNumber by remember { mutableStateOf(vehicle?.registrationNumber ?: "") }
    var capacityText by remember { mutableStateOf(vehicle?.fuelTankCapacity?.toString() ?: "15.0") }
    var mileageText by remember { mutableStateOf(vehicle?.mileage?.toString() ?: "35.0") }
    var currentFuelText by remember { mutableStateOf(vehicle?.currentFuelAvailable?.toString() ?: "10.0") }
    var setAsActive by remember { mutableStateOf(vehicle?.isActive ?: true) }

    val currentFuelVal = currentFuelText.toDoubleOrNull() ?: 0.0
    val mileageVal = mileageText.toDoubleOrNull() ?: 0.0
    val capacityVal = capacityText.toDoubleOrNull() ?: 0.0
    val estimatedRange = (currentFuelVal * mileageVal * 10.0).let { Math.round(it) / 10.0 }
    val isLowFuel = estimatedRange < 50.0 || (capacityVal > 0 && (currentFuelVal / capacityVal) < 0.2)

    val currentFuelTypeEnum = try { FuelType.valueOf(selectedFuelType) } catch (_: Exception) { FuelType.PETROL }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = HudColors.ObsidianCanvas,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HudColors.CyanPrimary),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEdit) "Edit Vehicle Details" else "Add New Vehicle",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = HudColors.TextCrispWhite
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HudColors.TextCoolSilver
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 1. Vehicle Type Selection (Car, Bike, Jeep, Others)
                Text(
                    text = "Vehicle Category",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HudColors.TextCoolSilver,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    VehicleType.entries.forEach { type ->
                        val isSelected = selectedType == type.name
                        Surface(
                            onClick = { selectedType = type.name },
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) HudColors.CyanPrimary.copy(alpha = 0.25f) else HudColors.ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.5.dp,
                                if (isSelected) HudColors.CyanPrimary else HudColors.ObsidianBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 10.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(text = type.iconEmoji, fontSize = 20.sp)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = type.displayName,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) HudColors.CyanPrimary else HudColors.TextCrispWhite,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 2. Fuel Type Selection (CNG, Petrol, Diesel, EV)
                Text(
                    text = "Fuel Type",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = HudColors.TextCoolSilver,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FuelType.entries.forEach { fuel ->
                        val isSelected = selectedFuelType == fuel.name
                        Surface(
                            onClick = { selectedFuelType = fuel.name },
                            shape = RoundedCornerShape(10.dp),
                            color = if (isSelected) HudColors.CyanPrimary else HudColors.ObsidianSurface,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) HudColors.CyanPrimary else HudColors.ObsidianBorder
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Row(
                                modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = fuel.iconEmoji, fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = fuel.displayName.split(" ").first(),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.Black else HudColors.TextCrispWhite
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 3. Vehicle Brand Name
                OutlinedTextField(
                    value = brandName,
                    onValueChange = { brandName = it },
                    label = { Text("Vehicle Brand Name (e.g. Honda, Royal Enfield, Tesla)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HudColors.ObsidianSurface,
                        unfocusedContainerColor = HudColors.ObsidianSurface,
                        focusedBorderColor = HudColors.CyanPrimary,
                        unfocusedBorderColor = HudColors.ObsidianBorder,
                        focusedLabelColor = HudColors.CyanPrimary,
                        unfocusedLabelColor = HudColors.TextCoolSilver,
                        focusedTextColor = HudColors.TextCrispWhite,
                        unfocusedTextColor = HudColors.TextCrispWhite
                    )
                )

                // 4. Vehicle Model Name
                OutlinedTextField(
                    value = modelName,
                    onValueChange = { modelName = it },
                    label = { Text("Vehicle Model (e.g. Classic 350, Thar, Model 3)") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HudColors.ObsidianSurface,
                        unfocusedContainerColor = HudColors.ObsidianSurface,
                        focusedBorderColor = HudColors.CyanPrimary,
                        unfocusedBorderColor = HudColors.ObsidianBorder,
                        focusedLabelColor = HudColors.CyanPrimary,
                        unfocusedLabelColor = HudColors.TextCoolSilver,
                        focusedTextColor = HudColors.TextCrispWhite,
                        unfocusedTextColor = HudColors.TextCrispWhite
                    )
                )

                // 4b. Vehicle Registration Number (e.g. TS 09 AB 1234)
                OutlinedTextField(
                    value = registrationNumber,
                    onValueChange = { registrationNumber = it.uppercase() },
                    label = { Text("Vehicle Registration No. (e.g. TS 09 AB 1234)") },
                    placeholder = { Text("Enter registration plate number") },
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, tint = HudColors.CyanPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HudColors.ObsidianSurface,
                        unfocusedContainerColor = HudColors.ObsidianSurface,
                        focusedBorderColor = HudColors.CyanPrimary,
                        unfocusedBorderColor = HudColors.ObsidianBorder,
                        focusedLabelColor = HudColors.CyanPrimary,
                        unfocusedLabelColor = HudColors.TextCoolSilver,
                        focusedTextColor = HudColors.TextCrispWhite,
                        unfocusedTextColor = HudColors.TextCrispWhite
                    )
                )

                // 5. Fuel Tank Capacity
                OutlinedTextField(
                    value = capacityText,
                    onValueChange = { capacityText = it },
                    label = { Text("Fuel Tank Capacity (${currentFuelTypeEnum.capacityUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocalGasStation, contentDescription = null, tint = HudColors.CyanPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HudColors.ObsidianSurface,
                        unfocusedContainerColor = HudColors.ObsidianSurface,
                        focusedBorderColor = HudColors.CyanPrimary,
                        unfocusedBorderColor = HudColors.ObsidianBorder,
                        focusedLabelColor = HudColors.CyanPrimary,
                        unfocusedLabelColor = HudColors.TextCoolSilver,
                        focusedTextColor = HudColors.TextCrispWhite,
                        unfocusedTextColor = HudColors.TextCrispWhite
                    )
                )

                // 6. Mileage / Efficiency
                OutlinedTextField(
                    value = mileageText,
                    onValueChange = { mileageText = it },
                    label = { Text("Mileage / Efficiency (${currentFuelTypeEnum.mileageUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Speed, contentDescription = null, tint = HudColors.CyanPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HudColors.ObsidianSurface,
                        unfocusedContainerColor = HudColors.ObsidianSurface,
                        focusedBorderColor = HudColors.CyanPrimary,
                        unfocusedBorderColor = HudColors.ObsidianBorder,
                        focusedLabelColor = HudColors.CyanPrimary,
                        unfocusedLabelColor = HudColors.TextCoolSilver,
                        focusedTextColor = HudColors.TextCrispWhite,
                        unfocusedTextColor = HudColors.TextCrispWhite
                    )
                )

                // 7. Current Fuel Available
                OutlinedTextField(
                    value = currentFuelText,
                    onValueChange = { currentFuelText = it },
                    label = { Text("Current Fuel Available (${currentFuelTypeEnum.fuelUnit})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.LocalGasStation, contentDescription = null, tint = HudColors.CyanPrimary)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = HudColors.ObsidianSurface,
                        unfocusedContainerColor = HudColors.ObsidianSurface,
                        focusedBorderColor = HudColors.CyanPrimary,
                        unfocusedBorderColor = HudColors.ObsidianBorder,
                        focusedLabelColor = HudColors.CyanPrimary,
                        unfocusedLabelColor = HudColors.TextCoolSilver,
                        focusedTextColor = HudColors.TextCrispWhite,
                        unfocusedTextColor = HudColors.TextCrispWhite
                    )
                )

                // 8. Range Reminder Indicator Card
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isLowFuel) Color(0xFF451A1A) else Color(0xFF0F2D2E),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (isLowFuel) Color(0xFFEF4444) else HudColors.CyanPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Estimated Vehicle Range:",
                                fontSize = 13.sp,
                                color = HudColors.TextCoolSilver
                            )
                            Text(
                                text = "$estimatedRange km",
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (isLowFuel) Color(0xFFEF4444) else HudColors.CyanPrimary
                            )
                        }

                        if (isLowFuel) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = Color(0xFFEF4444),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Refuel Reminder: Fuel low! Range is under 50 km. Fill up soon.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFCA5A5),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        } else {
                            Text(
                                text = "Based on $currentFuelVal ${currentFuelTypeEnum.fuelUnit} fuel × $mileageVal ${currentFuelTypeEnum.mileageUnit} mileage",
                                fontSize = 11.sp,
                                color = HudColors.TextCoolSilver,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                }

                // 9. Default Vehicle Toggle
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Set as Default Vehicle for Trips",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudColors.TextCrispWhite
                        )
                        Text(
                            text = "Sets this vehicle as default for all new trips",
                            fontSize = 11.sp,
                            color = HudColors.TextCoolSilver
                        )
                    }
                    Switch(
                        checked = setAsActive,
                        onCheckedChange = { setAsActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = HudColors.CyanPrimary
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Action Button
                Button(
                    onClick = {
                        val newVehicle = Vehicle(
                            id = vehicle?.id ?: UUID.randomUUID().toString(),
                            type = selectedType,
                            fuelType = selectedFuelType,
                            brandName = brandName.trim(),
                            model = modelName.trim(),
                            registrationNumber = registrationNumber.trim(),
                            fuelTankCapacity = capacityVal,
                            mileage = mileageVal,
                            currentFuelAvailable = currentFuelVal,
                            isActive = setAsActive
                        )
                        onSaveVehicle(newVehicle)
                    },
                    enabled = brandName.isNotBlank() || modelName.isNotBlank(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HudColors.CyanPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text(
                        text = if (isEdit) "Save Vehicle Details" else "Add Vehicle to Garage",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

/**
 * Dialog displaying complete details of a vehicle.
 */
@Composable
fun VehicleDetailsViewDialog(
    vehicle: Vehicle,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss
    ) {
        Surface(
            shape = RoundedCornerShape(22.dp),
            color = HudColors.ObsidianCanvas,
            border = androidx.compose.foundation.BorderStroke(1.5.dp, HudColors.CyanPrimary),
            shadowElevation = 14.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(22.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(text = vehicle.vehicleTypeEnum.iconEmoji, fontSize = 32.sp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = vehicle.fullDisplayName,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Black,
                                color = HudColors.TextCrispWhite
                            )
                            Text(
                                text = "${vehicle.vehicleTypeEnum.displayName} • ${vehicle.fuelTypeEnum.displayName}",
                                fontSize = 13.sp,
                                color = HudColors.TextCoolSilver
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = HudColors.TextCoolSilver
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Registration Number Plate Highlight Card
                if (vehicle.registrationNumber.isNotBlank()) {
                    Surface(
                        color = Color(0xFF1E293B),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.5.dp, Color(0xFFF59E0B)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "REGISTRATION NUMBER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = HudColors.TextCoolSilver
                                )
                                Text(
                                    text = "🇮🇳 ${vehicle.registrationNumber}",
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF59E0B)
                                )
                            }
                            Surface(
                                color = Color(0xFFFEF3C7),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(
                                    text = "VERIFIED REG",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFD97706),
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                    }
                }

                // Detailed Specs List
                Surface(
                    color = HudColors.ObsidianSurface,
                    shape = RoundedCornerShape(14.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, HudColors.ObsidianBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Technical Specifications",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudColors.CyanPrimary,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        DetailRowItem(label = "Vehicle Type / Category", value = "${vehicle.vehicleTypeEnum.iconEmoji} ${vehicle.vehicleTypeEnum.displayName}")
                        DetailRowItem(label = "Fuel / Engine Type", value = "${vehicle.fuelTypeEnum.iconEmoji} ${vehicle.fuelTypeEnum.displayName}")
                        DetailRowItem(label = "Fuel Tank Capacity", value = "${vehicle.fuelTankCapacity} ${vehicle.fuelTypeEnum.capacityUnit}")
                        DetailRowItem(label = "Fuel Mileage / Efficiency", value = "${vehicle.mileage} ${vehicle.fuelTypeEnum.mileageUnit}")
                        DetailRowItem(label = "Current Fuel Level", value = "${vehicle.currentFuelAvailable} ${vehicle.fuelTypeEnum.fuelUnit} (${vehicle.fuelPercentage}%)")
                        DetailRowItem(label = "Calculated Trip Range", value = "${vehicle.estimatedRangeKm} km")
                        DetailRowItem(label = "Full Tank Max Range", value = "${vehicle.maxRangeKm} km")
                        DetailRowItem(label = "Default Vehicle Status", value = if (vehicle.isActive) "★ Default Trip Vehicle" else "Secondary Garage Vehicle")
                    }
                }

                // Refuel Warning Banner
                if (vehicle.isLowFuelAlert) {
                    Spacer(modifier = Modifier.height(14.dp))
                    Surface(
                        color = Color(0xFF3B1212),
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = Color(0xFFEF4444),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Refuel Alert: Fuel level low (${vehicle.estimatedRangeKm} km remaining). Refuel before long trips.",
                                fontSize = 12.sp,
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HudColors.CyanPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Text("Close Vehicle Details", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }
            }
        }
    }
}

@Composable
private fun DetailRowItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, fontSize = 12.sp, color = HudColors.TextCoolSilver)
        Text(text = value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = HudColors.TextCrispWhite)
    }
}

/**
 * Vehicle Card displaying Vehicle details with Active Mode selection & Details View CTA.
 */
@Composable
fun VehicleItemCard(
    vehicle: Vehicle,
    isActive: Boolean,
    onSelectActive: () -> Unit,
    onViewDetails: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hudCardColor = HudColors.ObsidianSurface
    val accentColor = HudColors.CyanPrimary

    Surface(
        color = hudCardColor,
        shape = RoundedCornerShape(16.dp),
        shadowElevation = 4.dp,
        modifier = modifier
            .fillMaxWidth()
            .border(
                1.5.dp,
                if (isActive) accentColor else HudColors.ObsidianBorder,
                RoundedCornerShape(16.dp)
            )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header Row: Vehicle Icon, Name, Registration Plate, Active Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = vehicle.vehicleTypeEnum.iconEmoji,
                        fontSize = 26.sp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = vehicle.fullDisplayName,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = HudColors.TextCrispWhite
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${vehicle.vehicleTypeEnum.displayName} • ${vehicle.fuelTypeEnum.displayName}",
                                fontSize = 12.sp,
                                color = HudColors.TextCoolSilver
                            )
                        }
                        if (vehicle.registrationNumber.isNotBlank()) {
                            Surface(
                                color = Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFF59E0B)),
                                modifier = Modifier.padding(top = 4.dp)
                            ) {
                                Text(
                                    text = "🇮🇳 ${vehicle.registrationNumber}",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFF59E0B),
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }

                if (isActive) {
                    Surface(
                        color = accentColor.copy(alpha = 0.2f),
                        shape = RoundedCornerShape(8.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "★ DEFAULT",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = accentColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Divider(color = HudColors.ObsidianBorder)
            Spacer(modifier = Modifier.height(14.dp))

            // Spec Grid: Fuel Capacity, Mileage, Current Fuel, Range
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Capacity", fontSize = 11.sp, color = HudColors.TextCoolSilver)
                    Text(
                        text = "${vehicle.fuelTankCapacity} ${vehicle.fuelTypeEnum.capacityUnit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HudColors.TextCrispWhite
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Mileage", fontSize = 11.sp, color = HudColors.TextCoolSilver)
                    Text(
                        text = "${vehicle.mileage} ${vehicle.fuelTypeEnum.mileageUnit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HudColors.TextCrispWhite
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Current Fuel", fontSize = 11.sp, color = HudColors.TextCoolSilver)
                    Text(
                        text = "${vehicle.currentFuelAvailable} ${vehicle.fuelTypeEnum.fuelUnit}",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = HudColors.TextCrispWhite
                    )
                }

                Column(modifier = Modifier.weight(1.2f), horizontalAlignment = Alignment.End) {
                    Text(text = "Trip Range", fontSize = 11.sp, color = HudColors.TextCoolSilver)
                    Text(
                        text = "${vehicle.estimatedRangeKm} km",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (vehicle.isLowFuelAlert) Color(0xFFEF4444) else accentColor
                    )
                }
            }

            // Low Fuel Reminder Bar
            if (vehicle.isLowFuelAlert) {
                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    color = Color(0xFF3B1212),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Refuel Reminder: Fuel low (${vehicle.estimatedRangeKm} km left). Fill up vehicle!",
                            fontSize = 11.sp,
                            color = Color(0xFFFCA5A5),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Action CTAs: View Details | Select Default Vehicle | Edit | Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, accentColor),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = accentColor),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Icon(imageVector = Icons.Default.DirectionsCar, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("View Details", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    if (!isActive) {
                        OutlinedButton(
                            onClick = onSelectActive,
                            shape = RoundedCornerShape(10.dp),
                            border = androidx.compose.foundation.BorderStroke(1.dp, HudColors.ObsidianBorder),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = HudColors.TextCoolSilver),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Set Default", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Row {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Vehicle",
                            tint = HudColors.TextCoolSilver,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Delete Vehicle",
                            tint = Color(0xFFEF4444),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}
