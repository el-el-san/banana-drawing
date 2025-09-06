package com.example.bdrowclient.data.api

import com.example.bdrowclient.data.models.*
import retrofit2.http.*

interface GeminiService {
    @POST("v1beta/models/{model}:generateContent")
    suspend fun generateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): GeminiResponse

    @Streaming
    @POST("v1beta/models/{model}:streamGenerateContent") 
    suspend fun streamGenerateContent(
        @Path("model") model: String,
        @Query("key") apiKey: String,
        @Body request: GeminiRequest
    ): retrofit2.Response<okhttp3.ResponseBody>
}

object GeminiApiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/"
    
    fun create(): GeminiService {
        val loggingInterceptor = okhttp3.logging.HttpLoggingInterceptor().apply {
            // Quick fix: disable HTTP logging to avoid BuildConfig dependency
            level = okhttp3.logging.HttpLoggingInterceptor.Level.NONE
        }

        val okHttpClient = okhttp3.OkHttpClient.Builder()
            // タイムアウトを60秒に設定（画像生成には時間がかかるため）
            .connectTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(60, java.util.concurrent.TimeUnit.SECONDS)
            .addInterceptor(loggingInterceptor)
            .build()
        
        val retrofit = retrofit2.Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(retrofit2.converter.gson.GsonConverterFactory.create())
            .build()
        
        return retrofit.create(GeminiService::class.java)
    }
}
