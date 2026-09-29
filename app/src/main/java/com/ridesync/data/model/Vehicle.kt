package com.ridesync.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import java.util.UUID

enum class VehicleType(val displayName: String, val iconEmoji: String) {
    BIKE("Bike", "🏍️"),
    CAR("Car", "🚗"),
    JEEP("Jeep", "🚙"),
    OTHER("Others", "🚘")
}

enum class FuelType(
    val displayName: String,
    val capacityUnit: String,
    val mileageUnit: String,
    val fuelUnit: String,
    val iconEmoji: String
) {
    PETROL("Petrol", "Liters", "km/L", "Liters", "⛽"),
    DIESEL("Diesel", "Liters", "km/L", "Liters", "⛽"),
    CNG("CNG", "kg", "km/kg", "kg", "🟢"),
    EV("EV (Electric)", "kWh", "km/kWh", "kWh", "⚡")
}

@IgnoreExtraProperties
data class Vehicle(
    val id: String = UUID.randomUUID().toString(),
    val type: String = VehicleType.BIKE.name,
    val fuelType: String = FuelType.PETROL.name,
    val brandName: String = "",
    val model: String = "",
    val fuelTankCapacity: Double = 15.0,     // Liters or kWh
    val mileage: Double = 35.0,               // km/L or km/kWh
    val currentFuelAvailable: Double = 10.0,  // Current fuel/charge in Liters or kWh
    val isActive: Boolean = false
) {
    val vehicleTypeEnum: VehicleType
        get() = try { VehicleType.valueOf(type) } catch (_: Exception) { VehicleType.BIKE }

    val fuelTypeEnum: FuelType
        get() = try { FuelType.valueOf(fuelType) } catch (_: Exception) { FuelType.PETROL }

    // Range in kilometers based on current fuel available: currentFuelAvailable * mileage
    val estimatedRangeKm: Double
        get() = ((currentFuelAvailable * mileage) * 10.0).let { Math.round(it) / 10.0 }

    // Maximum range with full tank: fuelTankCapacity * mileage
    val maxRangeKm: Double
        get() = ((fuelTankCapacity * mileage) * 10.0).let { Math.round(it) / 10.0 }

    // Fuel percentage: (currentFuelAvailable / fuelTankCapacity) * 100
    val fuelPercentage: Int
        get() = if (fuelTankCapacity > 0) {
            ((currentFuelAvailable / fuelTankCapacity) * 100).toInt().coerceIn(0, 100)
        } else 0

    // Full vehicle display name (e.g. "Honda CBR 250R" or "Tesla Model 3")
    val fullDisplayName: String
        get() {
            val combined = listOf(brandName.trim(), model.trim()).filter { it.isNotBlank() }.joinToString(" ")
            return combined.ifBlank { "Vehicle" }
        }

    // Low fuel warning threshold (true if range < 50 km or fuel percentage < 20%)
    val isLowFuelAlert: Boolean
        get() = estimatedRangeKm < 50.0 || fuelPercentage < 20
}
