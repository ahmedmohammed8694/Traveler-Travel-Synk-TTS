package com.ridesync.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VehicleProfileTest {

    @Test
    fun `test vehicle range and low fuel calculation`() {
        val bike = Vehicle(
            brandName = "Honda",
            model = "CBR 250R",
            type = VehicleType.BIKE.name,
            fuelType = FuelType.PETROL.name,
            fuelTankCapacity = 13.0,
            mileage = 30.0,
            currentFuelAvailable = 5.0,
            isActive = true
        )

        // Estimated range: 5.0 * 30.0 = 150.0 km
        assertEquals(150.0, bike.estimatedRangeKm, 0.01)

        // Max range: 13.0 * 30.0 = 390.0 km
        assertEquals(390.0, bike.maxRangeKm, 0.01)

        // Fuel percentage: (5 / 13) * 100 = 38%
        assertEquals(38, bike.fuelPercentage)

        // Not low fuel since range is 150 km and percentage is 38%
        assertFalse(bike.isLowFuelAlert)
    }

    @Test
    fun `test low fuel alert triggering when range is under 50km`() {
        val lowFuelBike = Vehicle(
            brandName = "Royal Enfield",
            model = "Classic 350",
            fuelTankCapacity = 13.5,
            mileage = 35.0,
            currentFuelAvailable = 1.2, // 1.2 * 35 = 42 km
            isActive = true
        )

        assertEquals(42.0, lowFuelBike.estimatedRangeKm, 0.01)
        assertTrue(lowFuelBike.isLowFuelAlert)
    }

    @Test
    fun `test EV vehicle capacity and range`() {
        val evCar = Vehicle(
            brandName = "Tesla",
            model = "Model 3",
            type = VehicleType.CAR.name,
            fuelType = FuelType.EV.name,
            fuelTankCapacity = 60.0, // 60 kWh
            mileage = 6.5,          // 6.5 km per kWh
            currentFuelAvailable = 40.0 // 40 kWh
        )

        // 40 kWh * 6.5 km/kWh = 260 km
        assertEquals(260.0, evCar.estimatedRangeKm, 0.01)
        assertEquals("Tesla Model 3", evCar.fullDisplayName)
        assertEquals("EV (Electric)", evCar.fuelTypeEnum.displayName)
        assertEquals("kWh", evCar.fuelTypeEnum.capacityUnit)
    }

    @Test
    fun `test user profile active vehicle selection`() {
        val car = Vehicle(id = "v1", brandName = "Mahindra", model = "Thar", type = VehicleType.JEEP.name, isActive = false)
        val bike = Vehicle(id = "v2", brandName = "Yamaha", model = "R15", type = VehicleType.BIKE.name, isActive = true)

        val profile = UserProfile(
            userId = "user_1",
            displayName = "Ahmed",
            dateOfBirth = "1998-08-20",
            vehicles = listOf(car, bike),
            activeVehicleId = "v2"
        )

        assertNotNull(profile.activeVehicle)
        assertEquals("v2", profile.activeVehicle?.id)
        assertEquals("Yamaha R15", profile.displayVehicleModel)
        assertEquals("1998-08-20", profile.dateOfBirth)
    }
}
