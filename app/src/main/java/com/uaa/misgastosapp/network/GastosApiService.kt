// GastosApiService

package com.uaa.gastos.network

import com.uaa.gastos.network.model.*
import retrofit2.Response
import retrofit2.http.*

interface GastosApiService {
    @POST("register")
    suspend fun register(@Body request: RegisterRequest): Response<RegisterResponse>

    @POST("login")
    suspend fun login(@Body request: LoginRequest): Response<LoginResponse>

    @GET("profile")
    suspend fun getProfile(): Response<ProfileResponse>

    @POST("logout")
    suspend fun logout(): Response<LogoutResponse>
}