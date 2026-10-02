package com.ridesync.data.model

import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.PropertyName

@IgnoreExtraProperties
data class UserProfile(
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val mobileNumber: String = "",
    val dateOfBirth: String = "",
    val photoUrl: String = "",
    val profileCode: String = "",
    val bio: String = "Passionate Traveler & Adventure Cyclist",
    val vehicleModel: String = "",
    val tankCapacityLiters: Double = 15.0,
    val vehicles: List<Vehicle> = emptyList(),
    val activeVehicleId: String = "",
    val friends: List<String> = emptyList(),
    val friendRequestsSent: List<String> = emptyList(),
    val friendRequestsReceived: List<String> = emptyList(),
    val following: List<String> = emptyList(),
    val followers: List<String> = emptyList(),
    val blockedUsers: List<String> = emptyList(),
    val privacySettings: PrivacySettings = PrivacySettings(),
    val createdAt: Long = System.currentTimeMillis()
) {
    val safeProfileCode: String
        get() = if (profileCode.isNotBlank()) profileCode else "TTS-${(userId.hashCode() % 9000 + 1000).let { if (it < 0) -it else it }}"

    val activeVehicle: Vehicle?
        get() {
            if (vehicles.isEmpty()) return null
            return vehicles.firstOrNull { it.id == activeVehicleId }
                ?: vehicles.firstOrNull { it.isActive }
                ?: vehicles.firstOrNull()
        }

    val displayVehicleModel: String
        get() = activeVehicle?.fullDisplayName?.takeIf { it.isNotBlank() } ?: vehicleModel

    val displayTankCapacity: Double
        get() = activeVehicle?.fuelTankCapacity ?: tankCapacityLiters
}

@IgnoreExtraProperties
data class PrivacySettings(
    @get:PropertyName("shareLocationWithGroup")
    @set:PropertyName("shareLocationWithGroup")
    var shareLocationWithGroup: Boolean = true,

    @get:PropertyName("emergencyContactPhone")
    @set:PropertyName("emergencyContactPhone")
    var emergencyContactPhone: String = ""
)

