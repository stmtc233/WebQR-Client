package com.example.webqrclient.network

import com.example.webqrclient.data.QrData
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.POST

interface ApiService {
    @POST("upload_qr")
    suspend fun uploadQrCode(@Body qrData: QrData): Response<Void>
}