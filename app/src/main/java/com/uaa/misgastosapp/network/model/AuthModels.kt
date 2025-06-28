// AuthModels

package com.uaa.misgastosapp.network.model

import com.google.gson.annotations.SerializedName

data class RegisterRequest(
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val username: String,
    val password: String
)

data class LoginRequest(
    val identifier: String,
    val password: String
)

data class RegisterResponse(
    val msg: String
)

data class LoginResponse(
    @SerializedName("access_token") val accessToken: String
)

data class ProfileResponse(
    val id: Int,
    @SerializedName("full_name") val fullName: String,
    val email: String,
    val username: String,
    @SerializedName("created_at") val createdAt: String,
    @SerializedName("is_confirmed") val isConfirmed: Boolean
)

data class LogoutResponse(
    val msg: String
)

data class ErrorResponse(
    val msg: String?,
    val error: String?
)