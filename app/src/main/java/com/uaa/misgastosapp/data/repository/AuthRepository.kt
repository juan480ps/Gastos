// AuthRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.network.GastosApiService
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.network.model.LoginRequest
import com.uaa.misgastosapp.network.model.LoginResponse
import com.uaa.misgastosapp.network.model.ProfileResponse
import com.uaa.misgastosapp.network.model.RegisterRequest
import com.uaa.misgastosapp.utils.SecureSessionManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

class AuthRepository(
    private val userDao: UserDao,
    private val sessionManager: SecureSessionManager
) {

    private val apiService: GastosApiService
        get() = NetworkModule.apiService

    suspend fun loginApi(email: String, password: String): LoginResponse {

        sessionManager.clearToken()
        NetworkModule.clearAuthentication()
        delay(100)

        val response = apiService.login(LoginRequest(identifier = email, password = password))
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("API Login fallido: ${response.code()} - ${response.errorBody()?.string()}")
        }
        return response.body()!!
    }

    suspend fun getProfileApi(): ProfileResponse {
        val response = apiService.getProfile()
        if (!response.isSuccessful || response.body() == null) {
            throw Exception("API GetProfile fallido: ${response.code()} - ${response.errorBody()?.string()}")
        }
        return response.body()!!
    }

    suspend fun saveUserFromProfile(profile: ProfileResponse, password: String, token: String) {
        val localUser = UserEntity(
            id = profile.id,
            email = profile.email,
            password = hashPassword(password),
            name = profile.fullName,
            createdAt = profile.createdAt
        )

        try {
            val existingUser = userDao.getUserById(profile.id)
            if (existingUser != null) {
                userDao.update(localUser)
            } else {
                userDao.insert(localUser)
            }
        } catch (e: Exception) {
            Log.e("AuthRepository", "Error saving user, trying insert: ${e.message}")
            try {
                userDao.insert(localUser)
            } catch (insertError: Exception) {
                Log.e("AuthRepository", "Insert also failed: ${insertError.message}")
            }
        }

        sessionManager.saveUserSession(
            userId = profile.id,
            email = profile.email,
            name = profile.fullName,
            username = profile.username,
            accessToken = token
        )
    }

    suspend fun loginOffline(email: String, password: String): UserEntity {
        val hashedPassword = hashPassword(password)
        val user = userDao.login(email.lowercase(), hashedPassword)
            ?: throw Exception("Credenciales inválidas (modo offline)")

        sessionManager.saveUserSession(
            userId = user.id,
            email = user.email,
            name = user.name,
            username = email.substringBefore("@"),
            accessToken = "offline_mode"
        )
        return user
    }

    @RequiresApi(Build.VERSION_CODES.O)
    suspend fun register(name: String, email: String, username: String, password: String) {
        val response = apiService.register(
            RegisterRequest(
                fullName = name,
                email = email,
                username = username,
                password = password
            )
        )

        if (!response.isSuccessful) {
            throw Exception("API Register fallido: ${response.code()} - ${response.errorBody()?.string()}")
        }

        val hashedPassword = hashPassword(password)
        val newUser = UserEntity(
            email = email.lowercase(),
            password = hashedPassword,
            name = name,
            createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        )
        userDao.insert(newUser)
    }

    suspend fun logout() {
        try {
            if (sessionManager.getAccessToken() != "offline_mode") {
                try {
                    apiService.logout()
                } catch (e: Exception) {
                    Log.e("AuthRepository", "API logout failed, continuing with local logout", e)
                }
            }
        } finally {

            sessionManager.logout()
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}