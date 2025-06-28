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
import com.uaa.misgastosapp.data.UserEntity
import com.uaa.misgastosapp.network.NetworkModule
import com.uaa.misgastosapp.network.model.*
import com.uaa.misgastosapp.utils.SecureSessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.Response
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getInstance(application).userDao()
    private val sessionManager = SecureSessionManager(application)
    private val apiService = NetworkModule.apiService

    init {
        NetworkModule.initialize(sessionManager)
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
                        Log.d("AuthVM", "Attempting login with: $email")
                        val response = apiService.login(LoginRequest(identifier = email, password = password))

                        if (response.isSuccessful && response.body() != null) {
                            val token = response.body()!!.accessToken
                            Log.d("AuthVM", "Login successful, token received: ${token.take(20)}...")
                            sessionManager.saveToken(token)
                            NetworkModule.updateApiClient()

                            try {
                                val profileResponse = apiService.getProfile()

                                if (profileResponse.isSuccessful && profileResponse.body() != null) {
                                    val profile = profileResponse.body()!!
                                    Log.d("AuthVM", "Profile obtained successfully")

                                    sessionManager.saveUserSession(
                                        userId = profile.id,
                                        email = profile.email,
                                        name = profile.fullName,
                                        username = profile.username,
                                        accessToken = token
                                    )

                                    val localUser = UserEntity(
                                        id = profile.id,
                                        email = profile.email,
                                        password = hashPassword(password),
                                        name = profile.fullName,
                                        createdAt = profile.createdAt
                                    )
                                    userDao.insert(localUser)

                                    _isLoggedIn.value = true
                                    onSuccess()
                                } else {
                                    Log.e("AuthVM", "Profile request failed: ${profileResponse.code()}")
                                    handleApiError(profileResponse, onError)
                                }
                            } catch (e: Exception) {
                                Log.e("AuthVM", "Error getting profile: ${e.message}")

                                sessionManager.saveUserSession(
                                    userId = 0,
                                    email = email,
                                    name = email.substringBefore("@"),
                                    username = email.substringBefore("@"),
                                    accessToken = token
                                )
                                _isLoggedIn.value = true
                                onSuccess()
                            }
                        } else {
                            Log.e("AuthVM", "Login failed: ${response.code()}")
                            handleApiError(response, onError)
                        }
                    } catch (e: UnknownHostException) {
                        Log.e("AuthVM", "No internet connection")
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: SocketTimeoutException) {
                        Log.e("AuthVM", "Connection timeout")
                        _isOnlineMode.value = false
                        performOfflineLogin(email, password, onSuccess, onError)
                    } catch (e: Exception) {
                        Log.e("AuthVM", "Unexpected error during login: ${e.message}", e)
                        onError("Error inesperado: ${e.message}")
                    }
                } else {
                    performOfflineLogin(email, password, onSuccess, onError)
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Login error: ${e.message}", e)
                onError("Error al iniciar sesión: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    private suspend fun performOfflineLogin(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val hashedPassword = hashPassword(password)
        val user = userDao.login(email.lowercase(), hashedPassword)

        if (user != null) {

            sessionManager.saveUserSession(
                userId = user.id,
                email = user.email,
                name = user.name,
                username = email.substringBefore("@"),
                accessToken = "offline_mode"
            )
            _isLoggedIn.value = true
            onSuccess()
        } else {
            onError("Email o contraseña incorrectos (modo offline)")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun register(name: String, email: String, username: String, password: String, confirmPassword: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {

                when {
                    name.isBlank() || email.isBlank() || username.isBlank() || password.isBlank() -> {
                        onError("Por favor completa todos los campos")
                    }
                    password != confirmPassword -> {
                        onError("Las contraseñas no coinciden")
                    }
                    !isPasswordValid(password) -> {
                        onError("La contraseña debe tener mínimo 8 caracteres, una mayúscula, una minúscula y un número")
                    }
                    !isValidEmail(email) -> {
                        onError("Email inválido")
                    }
                    else -> {
                        if (_isOnlineMode.value) {
                            try {
                                Log.d("AuthVM", "Attempting registration for: $email")
                                val response = apiService.register(
                                    RegisterRequest(
                                        fullName = name,
                                        email = email,
                                        username = username,
                                        password = password
                                    )
                                )

                                if (response.isSuccessful) {
                                    Log.d("AuthVM", "Registration successful")

                                    val hashedPassword = hashPassword(password)
                                    val newUser = UserEntity(
                                        email = email.lowercase(),
                                        password = hashedPassword,
                                        name = name,
                                        createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                                    )
                                    userDao.insert(newUser)

                                    onSuccess()
                                } else {
                                    handleApiError(response, onError)
                                }
                            } catch (e: UnknownHostException) {
                                _isOnlineMode.value = false
                                onError("Sin conexión a internet. Registro no disponible en modo offline.")
                            } catch (e: SocketTimeoutException) {
                                _isOnlineMode.value = false
                                onError("Tiempo de conexión agotado. Intenta nuevamente.")
                            } catch (e: Exception) {
                                Log.e("AuthVM", "Registration error: ${e.message}", e)
                                onError("Error durante el registro: ${e.message}")
                            }
                        } else {
                            onError("El registro requiere conexión a internet")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Register error: ${e.message}", e)
                onError("Error al registrar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            try {
                if (_isOnlineMode.value && sessionManager.getAccessToken() != "offline_mode") {
                    Log.d("AuthVM", "Attempting logout on server")
                    apiService.logout()
                }
            } catch (e: Exception) {
                Log.e("AuthVM", "Logout error: ${e.message}")
            } finally {
                sessionManager.logout()
                _isLoggedIn.value = false
            }
        }
    }

    private fun <T> handleApiError(response: Response<T>, onError: (String) -> Unit) {
        val errorBody = response.errorBody()?.string()
        Log.e("AuthVM", "API Error - Code: ${response.code()}, Body: $errorBody")

        val errorMessage = try {
            val error = Gson().fromJson(errorBody, ErrorResponse::class.java)
            error.msg ?: error.error ?: "Error desconocido"
        } catch (e: Exception) {
            when (response.code()) {
                401 -> "Credenciales inválidas"
                409 -> "El email o usuario ya existe"
                429 -> "Demasiados intentos. Espera un momento"
                else -> "Error del servidor (${response.code()})"
            }
        }
        onError(errorMessage)
    }

    fun getCurrentUserName(): String? {
        return sessionManager.getUserName()
    }

    fun getCurrentUserId(): Int {
        return sessionManager.getUserId()
    }

    fun isInOnlineMode(): Boolean {
        return _isOnlineMode.value
    }

    private fun hashPassword(password: String): String {
        val bytes = java.security.MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }

    private fun isPasswordValid(password: String): Boolean {
        val passwordPattern = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$"
        return password.matches(passwordPattern.toRegex())
    }
}