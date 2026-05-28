package com.example.webqrclient.network

import com.example.webqrclient.BuildConfig
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val DEFAULT_URL = BuildConfig.API_DEFAULT_URL
    private const val BACKUP_URL = BuildConfig.API_BACKUP_URL

    val availableUrls: List<Pair<String, String>> = listOf(
        "默认" to DEFAULT_URL,
        "备用" to BACKUP_URL
    )

    private var currentBaseUrl: String = DEFAULT_URL
    private var retrofit: Retrofit? = null

    val instance: ApiService
        get() {
            if (retrofit == null || retrofit?.baseUrl().toString() != currentBaseUrl) {
                retrofit = Retrofit.Builder()
                    .baseUrl(currentBaseUrl)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!.create(ApiService::class.java)
        }

    fun setBaseUrl(url: String) {
        if (availableUrls.any { it.second == url }) {
            currentBaseUrl = url
            retrofit = null
        }
    }

    fun getCurrentBaseUrl(): String = currentBaseUrl

    fun getCurrentBaseUrlLabel(): String {
        val label = availableUrls.find { it.second == currentBaseUrl }?.first ?: ""
        return "$label: $currentBaseUrl"
    }

    fun getSelectedIndex(): Int =
        availableUrls.indexOfFirst { it.second == currentBaseUrl }
}
