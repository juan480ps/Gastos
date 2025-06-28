// GastosApiService

package com.uaa.misgastosapp.network

import com.uaa.misgastosapp.network.model.*
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