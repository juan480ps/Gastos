package com.uaa.gastos.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uaa.gastos.data.AppDatabase
import com.uaa.gastos.data.UserEntity
import com.uaa.gastos.utils.SessionManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val userDao = AppDatabase.getInstance(application).userDao()
    private val sessionManager = SessionManager(application)

    private val _isLoggedIn = MutableStateFlow(sessionManager.isLoggedIn())
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    fun login(email: String, password: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                if (email.isBlank() || password.isBlank()) {
                    onError("Por favor completa todos los campos")
                    _isLoading.value = false
                    return@launch
                }

                val hashedPassword = hashPassword(password)
                val user = userDao.login(email.lowercase(), hashedPassword)

                if (user != null) {
                    sessionManager.saveUserSession(user.id, user.email, user.name)
                    _isLoggedIn.value = true
                    onSuccess()
                } else {
                    onError("Email o contraseña incorrectos")
                }
            } catch (e: Exception) {
                onError("Error al iniciar sesión: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun register(name: String, email: String, password: String, confirmPassword: String,
                 onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                when {
                    name.isBlank() || email.isBlank() || password.isBlank() -> {
                        onError("Por favor completa todos los campos")
                    }
                    password != confirmPassword -> {
                        onError("Las contraseñas no coinciden")
                    }
                    password.length < 6 -> {
                        onError("La contraseña debe tener al menos 6 caracteres")
                    }
                    !isValidEmail(email) -> {
                        onError("Email inválido")
                    }
                    else -> {
                        val existingUser = userDao.getUserByEmail(email.lowercase())
                        if (existingUser != null) {
                            onError("Este email ya está registrado")
                        } else {
                            val hashedPassword = hashPassword(password)
                            val newUser = UserEntity(
                                email = email.lowercase(),
                                password = hashedPassword,
                                name = name,
                                createdAt = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                            )
                            val userId = userDao.insert(newUser)
                            sessionManager.saveUserSession(userId.toInt(), email.lowercase(), name)
                            _isLoggedIn.value = true
                            onSuccess()
                        }
                    }
                }
            } catch (e: Exception) {
                onError("Error al registrar: ${e.message}")
            } finally {
                _isLoading.value = false
            }
        }
    }

    fun logout() {
        sessionManager.logout()
        _isLoggedIn.value = false
    }

    fun getCurrentUserName(): String? {
        return sessionManager.getUserName()
    }

    fun getCurrentUserId(): Int {
        return sessionManager.getUserId()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.fold("") { str, it -> str + "%02x".format(it) }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}