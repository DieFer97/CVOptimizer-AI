package com.example.cvoptimizer

import okhttp3.MultipartBody
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part

interface ApiService {
    @Multipart
    @POST("analyze/file")
    suspend fun uploadCV(
        @Part file: MultipartBody.Part
    ): ApiResponse
}

