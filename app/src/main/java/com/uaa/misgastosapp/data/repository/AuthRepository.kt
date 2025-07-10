// AuthRepository.kt

package com.uaa.misgastosapp.data.repository

import android.os.Build
import androidx.annotation.RequiresApi
import com.uaa.misgastosapp.data.UserDao
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.network.GastosApiService
import com.uaa.misgastosapp.network.model.LoginRequest
import com.uaa.misgastosapp.network.model.RegisterRequest
import com.uaa.misgastosapp.utils.SecureSessionManager
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthRepository(
    private val apiService: GastosApiService,
    private val userDao: UserDao,
    private val sessionManager: SecureSessionManager
) {

    suspend fun login(email: String, password: String): UserEntity {

        val response = apiService.login(LoginRequest(identifier = email, password = password))
        if (!response.isSuccessful || response.body() == null) {
            val errorBody = response.errorBody()?.string() ?: "Error desconocido"
            throw Exception("API Login fallido: ${response.code()} - $errorBody")
        }

        val token = response.body()!!.accessToken
        sessionManager.saveToken(token)


        val profileResponse = apiService.getProfile()
        if (!profileResponse.isSuccessful || profileResponse.body() == null) {
            throw Exception("API GetProfile fallido tras login: ${profileResponse.code()}")
        }

        val profile = profileResponse.body()!!
        val localUser = UserEntity(
            id = profile.id,
            email = profile.email,
            password = hashPassword(password),
            name = profile.fullName,
            createdAt = profile.createdAt
        )

        userDao.insert(localUser)

        sessionManager.saveUserSession(
            userId = profile.id,
            email = profile.email,
            name = profile.fullName,
            username = profile.username,
            accessToken = token
        )

        return localUser
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
            val errorBody = response.errorBody()?.string() ?: "Error desconocido"
            throw Exception("API Register fallido: ${response.code()} - $errorBody")
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
        if (sessionManager.getAccessToken() != "offline_mode") {
            try {
                apiService.logout()
            } catch (e: Exception) {

            }
        }
        sessionManager.logout()
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }
}