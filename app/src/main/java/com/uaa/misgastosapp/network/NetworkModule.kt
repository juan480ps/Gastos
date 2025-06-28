// Network Module

package com.uaa.misgastosapp.network

import android.util.Log
import com.uaa.misgastosapp.utils.SecureSessionManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val BASE_URL = "http://10.0.2.2:5000/api/"

    private lateinit var sessionManager: SecureSessionManager
    private lateinit var retrofit: Retrofit
    lateinit var apiService: GastosApiService

    fun initialize(sessionManager: SecureSessionManager) {
        this.sessionManager = sessionManager
        createRetrofit()
        Log.d("NetworkModule", "Initialized with base URL: $BASE_URL")
    }

    private fun createRetrofit() {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val authInterceptor = Interceptor { chain ->
            val original = chain.request()
            val requestBuilder = original.newBuilder()
                .header("Content-Type", "application/json")

            val path = original.url.encodedPath
            if (!path.contains("login") && !path.contains("register")) {
                val token = sessionManager.getAccessToken()
                if (token != null && token != "offline_mode") {
                    Log.d("NetworkModule", "Adding token to request: Bearer $token")
                    requestBuilder.header("Authorization", "Bearer $token")
                } else {
                    Log.d("NetworkModule", "No token available for: $path")
                }
            }

            val request = requestBuilder.build()
            Log.d("NetworkModule", "Request URL: ${request.url}")
            Log.d("NetworkModule", "Headers: ${request.headers}")

            chain.proceed(request)
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        apiService = retrofit.create(GastosApiService::class.java)
    }

    fun updateApiClient() {
        createRetrofit()
    }
}