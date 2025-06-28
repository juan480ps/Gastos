// SessionManager

package com.uaa.gastos.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SessionManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("user_session", Context.MODE_PRIVATE)

    private val _userIdFlow = MutableStateFlow(getUserId())
    val userIdFlow: StateFlow<Int> = _userIdFlow.asStateFlow()

    companion object {
        const val USER_ID = "user_id"
        const val USER_EMAIL = "user_email"
        const val USER_NAME = "user_name"
        const val IS_LOGGED_IN = "is_logged_in"
    }

    init {
        prefs.registerOnSharedPreferenceChangeListener { _, key ->
            if (key == USER_ID) {
                _userIdFlow.value = getUserId()
            }
        }
    }

    fun saveUserSession(userId: Int, email: String, name: String) {
        prefs.edit {
            putInt(USER_ID, userId)
            putString(USER_EMAIL, email)
            putString(USER_NAME, name)
            putBoolean(IS_LOGGED_IN, true)
        }
        _userIdFlow.value = userId
    }

    fun isLoggedIn(): Boolean {
        return prefs.getBoolean(IS_LOGGED_IN, false)
    }

    fun getUserId(): Int {
        return prefs.getInt(USER_ID, 0)
    }

    fun getUserEmail(): String? {
        return prefs.getString(USER_EMAIL, null)
    }

    fun getUserName(): String? {
        return prefs.getString(USER_NAME, null)
    }

    fun logout() {
        prefs.edit {
            clear()
        }
        _userIdFlow.value = 0
    }
}