// AuthViewModel

package com.uaa.misgastosapp.ui.viewmodel

import android.app.Application
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.uaa.misgastosapp.data.AppDatabase
import com.uaa.misgastosapp.data.repository.AuthRepository
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.network.model.ErrorResponse
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import kotlinx.coroutines.delay

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SecureSessionManager(application)
    private val authRepository: AuthRepository

    init {
        val db = AppDatabase.getInstance(application)
        authRepository = AuthRepository(
            userDao = db.userDao(),
            sessionManager = this.sessionManager
        )
    }

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _isOnlineMode = MutableStateFlow(true)
    val isOnlineMode: StateFlow<Boolean> = _isOnlineMode.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (email.isBlank() || password.isBlank()) {
                    onError("Por favor completa todos los campos")
                    return@launch
                }

                if (_isOnlineMode.value) {
                    try {
                        val loginResponse = authRepository.loginApi(email, password)
                        val token = loginResponse.accessToken
                        sessionManager.saveToken(token)
                        NetworkModule.updateApiClient()
                        delay(200)
                        val profileResponse = authRepository.getProfileApi()
                        authRepository.saveUserFromProfile(profileResponse, password, token)
                        _isLoggedIn.value = true
                        _isOnlineMode.value = true
                        onSuccess()

                    } catch (e: UnknownHostException) {
                        Log.e("AuthVM", "Sin conexión, intentando login offline.", e)
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: SocketTimeoutException) {
                        Log.e("AuthVM", "Timeout, intentando login offline.", e)
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: Exception) {
                        Log.e("AuthVM", "Error en login online: ${e.message}", e)
                        onError(parseApiErrorMessage(e.message ?: "Ocurrió un error inesperado"))
                    }
                } else {
                    performOfflineLogin(email, password, onSuccess, onError)
                }
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun performOfflineLogin(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        try {
            authRepository.loginOffline(email, password)
            _isLoggedIn.value = true
            onSuccess()
        } catch (e: Exception) {
            Log.e("AuthVM", "Error en login offline: ${e.message}", e)
            onError(e.message ?: "Error desconocido en modo offline")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun register(name: String, email: String, username: String, password: String, confirmPassword: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when {
                    name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank() ->
                        onError("Por favor completa todos los campos")
                    password != confirmPassword ->
                        onError("Las contraseñas no coinciden")
                    !isPasswordValid(password) ->
                        onError("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
                    !isValidEmail(email) ->
                        onError("Email inválido")
                    else -> {
                        authRepository.register(name, email, username, password)
                        onSuccess()
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Error en registro: ${e.message}", e)
                onError(parseApiErrorMessage(e.message ?: "Ocurrió un error inesperado"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                authRepository.logout()
                _isLoggedIn.value = false
                _isOnlineMode.value = true
                delay(100)
                NetworkModule.clearAuthentication()

            } catch (e: Exception) {
                Log.e("AuthVM", "Error durante logout: ${e.message}", e)

                _isLoggedIn.value = false
                _isOnlineMode.value = true
                sessionManager.logout()
                NetworkModule.clearAuthentication()
            }
        }
    }

    private fun parseApiErrorMessage(rawMessage: String): String {
        return try {
            if (rawMessage.contains("{") && rawMessage.contains("}")) {
                val jsonStart = rawMessage.indexOf("{")
                val jsonEnd = rawMessage.lastIndexOf("}") + 1
                val jsonString = rawMessage.substring(jsonStart, jsonEnd)
                val error = Gson().fromJson(jsonString, ErrorResponse::class.java)
                error.msg ?: error.error ?: "Error del servidor"
            } else {
                when {
                    rawMessage.contains("401") -> "Credenciales inválidas"
                    rawMessage.contains("409") -> "El email o usuario ya existe"
                    rawMessage.contains("500") -> "Error del servidor"
                    rawMessage.contains("404") -> "Servicio no disponible"
                    else -> "Ocurrió un error inesperado"
                }
            }
        } catch (e: Exception) {
            Log.e("AuthVM", "Error parsing error message: ${e.message}")
            "Ocurrió un error inesperado"
        }
    }

    fun getCurrentUserName(): String? = sessionManager.getUserName()
    fun getCurrentUserId(): Int = sessionManager.getUserId()

    private fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isPasswordValid(password: String): Boolean = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex().matches(password)
}