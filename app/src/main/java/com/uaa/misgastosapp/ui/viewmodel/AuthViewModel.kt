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

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val sessionManager = SecureSessionManager(application)
    private val authRepository: AuthRepository

    init {
        // Inicializar NetworkModule primero para asegurar que apiService esté listo
        NetworkModule.initialize(sessionManager)
        val db = AppDatabase.getInstance(application)
        authRepository = AuthRepository(
            apiService = NetworkModule.apiService,
            userDao = db.userDao(),
            sessionManager = sessionManager
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
                    _isLoading.value = false
                    return@launch
                }

                if (_isOnlineMode.value) {
                    try {
                        authRepository.login(email, password)
                        _isLoggedIn.value = true
                        onSuccess()
                    } catch (e: UnknownHostException) {
                        Log.e("AuthVM", "Sin conexión a internet, intentando login offline.", e)
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: SocketTimeoutException) {
                        Log.e("AuthVM", "Timeout, intentando login offline.", e)
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: Exception) {
                        Log.e("AuthVM", "Error en login online: ${e.message}", e)
                        onError(parseApiErrorMessage(e.message ?: "Error desconocido"))
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
                onError(parseApiErrorMessage(e.message ?: "Error desconocido"))
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            authRepository.logout()
            _isLoggedIn.value = false
        }
    }

    private fun parseApiErrorMessage(rawMessage: String): String {
        return try {
            val errorBody = rawMessage.substringAfter("-").trim()
            val error = Gson().fromJson(errorBody, ErrorResponse::class.java)
            error.msg ?: error.error ?: "Error del servidor"
        } catch (e: Exception) {
            if (rawMessage.contains("401")) "Credenciales inválidas"
            else if (rawMessage.contains("409")) "El email o usuario ya existe"
            else "Ocurrió un error inesperado"
        }
    }

    fun getCurrentUserName(): String? = sessionManager.getUserName()
    fun getCurrentUserId(): Int = sessionManager.getUserId()

    private fun isValidEmail(email: String): Boolean = android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    private fun isPasswordValid(password: String): Boolean = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$".toRegex().matches(password)
}