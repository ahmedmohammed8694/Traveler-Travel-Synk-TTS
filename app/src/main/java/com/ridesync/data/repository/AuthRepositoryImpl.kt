package com.ridesync.data.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import com.ridesync.RideSyncApplication
import com.ridesync.data.model.AuthRepository
import com.ridesync.data.model.AuthUser
import com.ridesync.data.model.PrivacySettings
import com.ridesync.data.model.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import org.json.JSONArray
import org.json.JSONObject
import com.ridesync.data.model.Vehicle
import com.ridesync.data.model.VehicleType
import com.ridesync.data.model.FuelType
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

class AuthRepositoryImpl : AuthRepository {

    private val cloudflareEdgeUrl = "https://ahmedmohammed8694-riders-ride-sync.mdahmed08061994.workers.dev"

    private val prefs: SharedPreferences? by lazy {
        try {
            RideSyncApplication.appContext.getSharedPreferences("ridesync_auth_prefs", Context.MODE_PRIVATE)
        } catch (e: Exception) {
            null
        }
    }

    private var inMemoryUser: AuthUser? = null

    init {
        loadSavedUser()
    }

    override val currentUser: AuthUser?
        get() = inMemoryUser

    private fun loadSavedUser() {
        prefs?.let { p ->
            val uid = p.getString("saved_uid", null)
            val email = p.getString("saved_email", null)
            val name = p.getString("saved_display_name", null)
            val photo = p.getString("saved_photo_url", "")
            if (!uid.isNullOrBlank() && !email.isNullOrBlank()) {
                inMemoryUser = AuthUser(
                    uid = uid,
                    email = email,
                    displayName = name ?: email.split("@")[0],
                    photoUrl = photo ?: ""
                )
            }
        }
    }

    private fun saveUserToPrefs(user: AuthUser) {
        val existingPhoto = prefs?.getString("saved_photo_url", "") ?: ""
        val finalPhoto = user.photoUrl.ifBlank { existingPhoto }
        inMemoryUser = user.copy(photoUrl = finalPhoto)
        prefs?.edit()?.apply {
            putString("saved_uid", user.uid)
            putString("saved_email", user.email)
            putString("saved_display_name", user.displayName)
            putString("saved_photo_url", finalPhoto)
            apply()
        }
    }

    private fun vehicleToJson(v: Vehicle): JSONObject {
        return JSONObject().apply {
            put("id", v.id)
            put("type", v.type)
            put("fuelType", v.fuelType)
            put("brandName", v.brandName)
            put("model", v.model)
            put("registrationNumber", v.registrationNumber)
            put("fuelTankCapacity", v.fuelTankCapacity)
            put("mileage", v.mileage)
            put("currentFuelAvailable", v.currentFuelAvailable)
            put("isActive", v.isActive)
        }
    }

    private fun parseVehicleJson(j: JSONObject): Vehicle {
        return Vehicle(
            id = j.optString("id", java.util.UUID.randomUUID().toString()),
            type = j.optString("type", VehicleType.BIKE.name),
            fuelType = j.optString("fuelType", FuelType.PETROL.name),
            brandName = j.optString("brandName", ""),
            model = j.optString("model", ""),
            registrationNumber = j.optString("registrationNumber", j.optString("regNo", "")),
            fuelTankCapacity = j.optDouble("fuelTankCapacity", 15.0),
            mileage = j.optDouble("mileage", 35.0),
            currentFuelAvailable = j.optDouble("currentFuelAvailable", 10.0),
            isActive = j.optBoolean("isActive", false)
        )
    }

    private fun profileToJson(profile: UserProfile): JSONObject {
        return JSONObject().apply {
            put("userId", profile.userId)
            put("displayName", profile.displayName)
            put("email", profile.email)
            put("mobileNumber", profile.mobileNumber)
            put("dateOfBirth", profile.dateOfBirth)
            put("photoUrl", profile.photoUrl)
            put("vehicleModel", profile.displayVehicleModel)
            put("tankCapacityLiters", profile.displayTankCapacity)
            put("fuelTankCapacityLiters", profile.displayTankCapacity)
            put("activeVehicleId", profile.activeVehicleId)
            put("createdAt", profile.createdAt)

            put("friends", JSONArray(profile.friends))
            put("friendRequestsSent", JSONArray(profile.friendRequestsSent))
            put("friendRequestsReceived", JSONArray(profile.friendRequestsReceived))
            put("following", JSONArray(profile.following))
            put("followers", JSONArray(profile.followers))
            put("blockedUsers", JSONArray(profile.blockedUsers))

            val vehArray = JSONArray()
            profile.vehicles.forEach { v ->
                vehArray.put(vehicleToJson(v))
            }
            put("vehicles", vehArray)

            put("privacySettings", JSONObject().apply {
                put("shareLocationWithGroup", profile.privacySettings.shareLocationWithGroup)
                put("emergencyContactPhone", profile.privacySettings.emergencyContactPhone)
            })
            put("shareRealtimeLocation", profile.privacySettings.shareLocationWithGroup)
            put("emergencyContactNumber", profile.privacySettings.emergencyContactPhone)
        }
    }

    private fun parseUserProfileJson(j: JSONObject): UserProfile? {
        return try {
            val uId = j.optString("userId", "")
            val vehModel = j.optString("vehicleModel", "")
            val dName = j.optString("displayName", "Rider")
            val uEmail = j.optString("email", "")
            val mobile = j.optString("mobileNumber", "")
            val dob = j.optString("dateOfBirth", "")
            val photo = j.optString("photoUrl", "")
            val tank = j.optDouble("tankCapacityLiters", j.optDouble("fuelTankCapacityLiters", 15.0))
            val activeVehId = j.optString("activeVehicleId", "")
            val created = j.optLong("createdAt", System.currentTimeMillis())

            fun parseJsonList(key: String): List<String> {
                if (!j.has(key) || j.isNull(key)) return emptyList()
                val arr = j.getJSONArray(key)
                val list = mutableListOf<String>()
                for (i in 0 until arr.length()) {
                    val s = arr.optString(i, "")
                    if (s.isNotBlank()) list.add(s)
                }
                return list
            }

            val friendsList = parseJsonList("friends")
            val reqSentList = parseJsonList("friendRequestsSent")
            val reqRecvList = parseJsonList("friendRequestsReceived")
            val followingList = parseJsonList("following")
            val followersList = parseJsonList("followers")
            val blockedList = parseJsonList("blockedUsers")

            val vehiclesList = mutableListOf<Vehicle>()
            if (j.has("vehicles") && !j.isNull("vehicles")) {
                val arr = j.getJSONArray("vehicles")
                for (i in 0 until arr.length()) {
                    val vObj = arr.getJSONObject(i)
                    vehiclesList.add(parseVehicleJson(vObj))
                }
            }

            val privObj = j.optJSONObject("privacySettings")
            val shareLoc = privObj?.optBoolean("shareLocationWithGroup", true)
                ?: j.optBoolean("shareRealtimeLocation", true)
            val emergency = privObj?.optString("emergencyContactPhone", "")
                ?: j.optString("emergencyContactNumber", "")

            UserProfile(
                userId = uId,
                displayName = dName,
                email = uEmail,
                mobileNumber = mobile,
                dateOfBirth = dob,
                photoUrl = photo,
                vehicleModel = vehModel,
                tankCapacityLiters = tank,
                vehicles = vehiclesList,
                activeVehicleId = activeVehId,
                friends = friendsList,
                friendRequestsSent = reqSentList,
                friendRequestsReceived = reqRecvList,
                following = followingList,
                followers = followersList,
                blockedUsers = blockedList,
                privacySettings = PrivacySettings(
                    shareLocationWithGroup = shareLoc,
                    emergencyContactPhone = emergency
                ),
                createdAt = created
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun saveUserProfileToPrefs(profile: UserProfile) {
        prefs?.edit()?.apply {
            putString("saved_uid", profile.userId)
            putString("saved_email", profile.email)
            putString("saved_display_name", profile.displayName)
            putString("saved_photo_url", profile.photoUrl)
            putString("saved_mobile_number", profile.mobileNumber)
            putString("saved_dob", profile.dateOfBirth)
            putString("saved_vehicle_model", profile.displayVehicleModel)
            putFloat("saved_tank_capacity", profile.displayTankCapacity.toFloat())
            putBoolean("saved_share_location", profile.privacySettings.shareLocationWithGroup)
            putString("saved_emergency_phone", profile.privacySettings.emergencyContactPhone)
            putBoolean("saved_profile_completed", profile.displayVehicleModel.isNotBlank())

            val json = profileToJson(profile).toString()

            if (profile.userId.isNotBlank()) {
                putString("profile_json_${profile.userId}", json)
            }
            if (profile.email.isNotBlank()) {
                putString("profile_json_${profile.email.trim().lowercase()}", json)
            }
            apply()
        }
        syncUserToSupabaseAsync(profile)
    }

    private fun loadUserProfileFromPrefs(userId: String, email: String? = null): UserProfile? {
        val p = prefs ?: return null
        val cleanEmail = email?.trim()?.lowercase()

        // 1. Try direct JSON string by userId or email
        val jsonStr = (if (userId.isNotBlank()) p.getString("profile_json_$userId", null) else null)
            ?: (if (!cleanEmail.isNullOrBlank()) p.getString("profile_json_$cleanEmail", null) else null)

        if (!jsonStr.isNullOrBlank()) {
            try {
                val j = JSONObject(jsonStr)
                val parsed = parseUserProfileJson(j)
                if (parsed != null && (parsed.displayVehicleModel.isNotBlank() || parsed.displayName.isNotBlank())) {
                    return parsed
                }
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Failed parsing cached profile JSON", e)
            }
        }

        // 2. Fallback to individual keys
        val vehicleModel = p.getString("saved_vehicle_model", "") ?: ""
        if (vehicleModel.isNotBlank()) {
            val savedUid = p.getString("saved_uid", userId) ?: userId
            val savedEmail = p.getString("saved_email", email ?: "") ?: (email ?: "")
            val savedName = p.getString("saved_display_name", "Rider") ?: "Rider"
            val savedMobile = p.getString("saved_mobile_number", "") ?: ""
            val savedDob = p.getString("saved_dob", "") ?: ""
            val savedPhoto = p.getString("saved_photo_url", "") ?: ""
            val savedTank = p.getFloat("saved_tank_capacity", 15f).toDouble()
            val savedShareLoc = p.getBoolean("saved_share_location", true)
            val savedEmergency = p.getString("saved_emergency_phone", "") ?: ""

            return UserProfile(
                userId = savedUid,
                displayName = savedName,
                email = savedEmail,
                mobileNumber = savedMobile,
                dateOfBirth = savedDob,
                photoUrl = savedPhoto,
                vehicleModel = vehicleModel,
                tankCapacityLiters = savedTank,
                privacySettings = PrivacySettings(
                    shareLocationWithGroup = savedShareLoc,
                    emergencyContactPhone = savedEmergency
                )
            )
        }

        return null
    }

    private fun clearUserFromPrefs() {
        inMemoryUser = null
        prefs?.edit()?.apply {
            remove("saved_uid")
            remove("saved_email")
            remove("saved_display_name")
            remove("saved_photo_url")
            remove("saved_vehicle_model")
            remove("saved_tank_capacity")
            remove("saved_share_location")
            remove("saved_emergency_phone")
            remove("saved_profile_completed")
            apply()
        }
    }

    override fun signInWithGoogleIdToken(idToken: String): Flow<Result<AuthUser>> = flow {
        try {
            // 1. Decode Google ID Token (JWT) payload on client
            val authUser = parseGoogleIdToken(idToken)
                ?: throw Exception("Invalid Google ID Token received")

            // 2. Try to sync / authenticate with Cloudflare Edge
            try {
                syncGoogleUserWithCloudflare(idToken, authUser)
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Cloudflare edge sync warning: ${e.message}")
            }

            // 3. Optional background Firebase session sync (ignored if blocked by Google policy)
            try {
                val auth = FirebaseAuth.getInstance()
                val credential = GoogleAuthProvider.getCredential(idToken, null)
                auth.signInWithCredential(credential).await()
            } catch (e: Exception) {
                Log.i("AuthRepositoryImpl", "Firebase Identity Toolkit bypassed (using Cloudflare Edge Auth): ${e.message}")
            }

            // 4. Persist user session locally
            saveUserToPrefs(authUser)
            emit(Result.success(authUser))
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Google Sign-In error", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun signUpWithEmail(
        email: String,
        password: String,
        displayName: String
    ): Flow<Result<AuthUser>> = flow {
        try {
            // 1. Register with Cloudflare Edge API
            val cfResult = performCloudflareSignUp(email, password, displayName)
            if (cfResult.isFailure) {
                throw cfResult.exceptionOrNull() ?: Exception("Sign-up failed on Cloudflare Edge")
            }

            val user = cfResult.getOrThrow()
            saveUserToPrefs(user)

            // 2. Try Firebase Auth in background if enabled
            try {
                FirebaseAuth.getInstance().createUserWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                Log.i("AuthRepositoryImpl", "Firebase signup bypassed (using Cloudflare Edge): ${e.message}")
            }

            emit(Result.success(user))
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Sign up error", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun signInWithEmail(
        email: String,
        password: String
    ): Flow<Result<AuthUser>> = flow {
        try {
            // 1. Authenticate with Cloudflare Edge API
            val cfResult = performCloudflareSignIn(email, password)
            if (cfResult.isFailure) {
                throw cfResult.exceptionOrNull() ?: Exception("Invalid email or password")
            }

            val user = cfResult.getOrThrow()
            saveUserToPrefs(user)

            // 2. Try Firebase Auth in background if enabled
            try {
                FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password).await()
            } catch (e: Exception) {
                Log.i("AuthRepositoryImpl", "Firebase sign-in bypassed (using Cloudflare Edge): ${e.message}")
            }

            emit(Result.success(user))
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Sign in error", e)
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun sendPasswordResetEmail(email: String): Flow<Result<Unit>> = flow {
        try {
            try {
                FirebaseAuth.getInstance().sendPasswordResetEmail(email).await()
            } catch (e: Exception) {
                Log.i("AuthRepositoryImpl", "Firebase password reset skipped: ${e.message}")
            }
            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun fetchUserProfile(userId: String): Flow<Result<UserProfile?>> = flow {
        try {
            // 1. Check local persistent cache first
            val cachedProfile = loadUserProfileFromPrefs(userId, inMemoryUser?.email)
            if (cachedProfile != null && cachedProfile.vehicleModel.isNotBlank()) {
                emit(Result.success(cachedProfile))
            }

            // 2. Fetch from Cloudflare Edge Database (passing both userId and email)
            val cfProfile = fetchCloudflareProfile(userId, inMemoryUser?.email)
            if (cfProfile != null && cfProfile.vehicleModel.isNotBlank()) {
                val mergedProfile = cfProfile.copy(
                    photoUrl = cfProfile.photoUrl.ifBlank { cachedProfile?.photoUrl ?: "" },
                    vehicles = if (cfProfile.vehicles.isNotEmpty()) cfProfile.vehicles else cachedProfile?.vehicles ?: emptyList(),
                    activeVehicleId = cfProfile.activeVehicleId.ifBlank { cachedProfile?.activeVehicleId ?: "" }
                )
                saveUserProfileToPrefs(mergedProfile)
                emit(Result.success(mergedProfile))
                return@flow
            }

            // 3. Fallback to Firestore if available
            try {
                val snapshot = FirebaseFirestore.getInstance().collection("users")
                    .document(userId)
                    .get()
                    .await()
                if (snapshot.exists()) {
                    val profile = snapshot.toObject(UserProfile::class.java)
                    if (profile != null) {
                        saveUserProfileToPrefs(profile)
                        emit(Result.success(profile))
                        return@flow
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Firestore fetch skipped: ${e.message}")
            }

            // 4. Return cached profile if available, else null
            emit(Result.success(cachedProfile))
        } catch (e: Exception) {
            val cached = loadUserProfileFromPrefs(userId, inMemoryUser?.email)
            if (cached != null) {
                emit(Result.success(cached))
            } else {
                emit(Result.failure(e))
            }
        }
    }.flowOn(Dispatchers.IO)

    override fun saveUserProfile(userProfile: UserProfile): Flow<Result<Unit>> = flow {
        try {
            // 1. Always save immediately to local persistent storage
            saveUserProfileToPrefs(userProfile)

            // Update in-memory user display name if set
            inMemoryUser?.let { current ->
                if (userProfile.displayName.isNotBlank()) {
                    inMemoryUser = current.copy(displayName = userProfile.displayName)
                }
            }

            // 2. Save to Cloudflare Edge Database
            val savedCf = saveCloudflareProfile(userProfile)
            
            // 3. Also try Firestore in background
            try {
                FirebaseFirestore.getInstance().collection("users")
                    .document(userProfile.userId)
                    .set(userProfile)
                    .await()
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Firestore profile save skipped: ${e.message}")
            }

            emit(Result.success(Unit))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }.flowOn(Dispatchers.IO)

    override fun signOut() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (e: Exception) {
            // Ignore
        }
        clearUserFromPrefs()
    }

    // --- Helpers & Cloudflare Edge Network Calls ---

    private fun parseGoogleIdToken(idToken: String): AuthUser? {
        return try {
            val parts = idToken.split(".")
            if (parts.size >= 2) {
                val payloadJson = String(Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP))
                val json = JSONObject(payloadJson)
                val sub = json.optString("sub", "")
                val email = json.optString("email", "")
                val name = json.optString("name", email.split("@").firstOrNull() ?: "Rider")
                val picture = json.optString("picture", "")
                val uid = if (sub.isNotBlank()) "google_$sub" else "cf_usr_${Base64.encodeToString(email.toByteArray(), Base64.NO_WRAP).replace("=", "")}"
                AuthUser(
                    uid = uid,
                    email = email,
                    displayName = name,
                    photoUrl = picture
                )
            } else null
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Failed to parse Google ID token payload", e)
            null
        }
    }

    private fun syncGoogleUserWithCloudflare(idToken: String, user: AuthUser) {
        try {
            val url = URL("$cloudflareEdgeUrl/api/auth/google")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 5000
            conn.readTimeout = 5000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("idToken", idToken)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }
            val code = conn.responseCode
            Log.d("AuthRepositoryImpl", "Cloudflare Google Auth response: $code")
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Cloudflare /api/auth/google call note: ${e.message}")
        }
    }

    private fun performCloudflareSignUp(email: String, pass: String, name: String): Result<AuthUser> {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/signup")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
                put("displayName", name.trim())
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val userObj = json.optJSONObject("user")
                val uid = userObj?.optString("uid") ?: "cf_usr_${Base64.encodeToString(email.toByteArray(), Base64.NO_WRAP).replace("=", "")}"
                val user = AuthUser(
                    uid = uid,
                    email = email.trim(),
                    displayName = name.trim()
                )
                Result.success(user)
            } else {
                val errorBody = runCatching {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Sign-up failed"
                }.getOrDefault("Sign-up failed")
                Log.e("AuthRepositoryImpl", "Cloudflare Sign-Up failed ($responseCode): $errorBody")
                Result.failure(Exception("Sign-up failed (Code: $responseCode)"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Cloudflare Sign-Up Network Error", e)
            Result.failure(Exception("Network error during sign-up. Please check your connection."))
        }
    }

    private fun performCloudflareSignIn(email: String, pass: String): Result<AuthUser> {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/signin")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true

            val payload = JSONObject().apply {
                put("email", email.trim())
                put("password", pass)
            }.toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }

            val responseCode = conn.responseCode
            if (responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()
                val json = JSONObject(response)
                val userObj = json.optJSONObject("user")
                val uid = userObj?.optString("uid") ?: "cf_usr_${Base64.encodeToString(email.toByteArray(), Base64.NO_WRAP).replace("=", "")}"
                val displayName = userObj?.optString("displayName") ?: email.split("@").firstOrNull() ?: "Rider"
                val user = AuthUser(
                    uid = uid,
                    email = email.trim(),
                    displayName = displayName
                )
                Result.success(user)
            } else if (responseCode == 401 || responseCode == 403) {
                Result.failure(Exception("Invalid email or password. Please check your credentials."))
            } else {
                val errorBody = runCatching {
                    conn.errorStream?.bufferedReader()?.readText() ?: "Sign-in failed"
                }.getOrDefault("Sign-in failed")
                Log.e("AuthRepositoryImpl", "Cloudflare Sign-In failed ($responseCode): $errorBody")
                Result.failure(Exception("Sign-in failed. Please try again. (Code: $responseCode)"))
            }
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Cloudflare Sign-In Network Error", e)
            Result.failure(Exception("Network error during sign-in. Please check your connection."))
        }
    }

    private fun saveCloudflareProfile(profile: UserProfile): Boolean {
        return try {
            val url = URL("$cloudflareEdgeUrl/api/auth/profile")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000
            conn.doOutput = true

            val payload = profileToJson(profile).toString()

            OutputStreamWriter(conn.outputStream).use { it.write(payload) }
            conn.responseCode in 200..299
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Failed to save profile to Cloudflare Edge", e)
            false
        }
    }

    private fun fetchCloudflareProfile(userId: String, email: String? = null): UserProfile? {
        return try {
            val emailParam = if (!email.isNullOrBlank()) "&email=${email.trim().lowercase()}" else ""
            val url = URL("$cloudflareEdgeUrl/api/auth/profile?userId=$userId$emailParam")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.setRequestProperty("Content-Type", "application/json")
            conn.connectTimeout = 6000
            conn.readTimeout = 6000

            if (conn.responseCode in 200..299) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val response = reader.readText()
                reader.close()

                val json = JSONObject(response)
                if (json.has("profile") && !json.isNull("profile")) {
                    val pObj = json.getJSONObject("profile")
                    parseUserProfileJson(pObj)
                } else null
            } else null
        } catch (e: Exception) {
            null
        }
    }

    private fun syncUserToSupabaseAsync(profile: UserProfile) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL("https://oktfyxdrvscmifomtlkp.supabase.co/rest/v1/users")
                val conn = url.openConnection() as HttpURLConnection
                conn.requestMethod = "POST"
                conn.setRequestProperty("Content-Type", "application/json")
                conn.setRequestProperty("apikey", "sb_publishable_dF8gDIF6Ahw4wWPRfYOH7Q_AaJrBrO8")
                conn.setRequestProperty("Authorization", "Bearer sb_publishable_dF8gDIF6Ahw4wWPRfYOH7Q_AaJrBrO8")
                conn.setRequestProperty("Prefer", "resolution=merge-duplicates")
                conn.connectTimeout = 6000
                conn.readTimeout = 6000
                conn.doOutput = true

                val payload = JSONObject().apply {
                    put("uid", profile.userId)
                    put("email", profile.email.ifBlank { "${profile.userId}@ridesync.app" })
                    put("display_name", profile.displayName.ifBlank { "Rider" })
                    put("photo_url", profile.photoUrl)
                    put("auth_provider", "google")
                    put("vehicle_model", profile.displayVehicleModel)
                    put("active_vehicle_id", profile.activeVehicleId)
                    put("emergency_contact", profile.privacySettings.emergencyContactPhone)
                    put("created_at", if (profile.createdAt > 0) profile.createdAt else System.currentTimeMillis())
                }.toString()

                OutputStreamWriter(conn.outputStream).use { it.write(payload) }
                Log.d("AuthRepositoryImpl", "Supabase User Sync Response: ${conn.responseCode}")
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Supabase User Sync Note: ${e.message}")
            }
        }
    }
}
