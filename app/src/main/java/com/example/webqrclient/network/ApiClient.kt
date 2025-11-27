package com.example.webqrclient.network

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

import android.content.Context
import android.content.SharedPreferences
import com.example.webqrclient.BuildConfig

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

    fun initialize(context: Context) {
        // Always default to the default URL and do not remember previous selections.
        currentBaseUrl = DEFAULT_URL
    }

    fun setBaseUrl(url: String) {
        if (availableUrls.any { it.second == url }) {
            currentBaseUrl = url
            // Do not save to SharedPreferences, so the selection is not remembered.
            // Invalidate retrofit instance
            retrofit = null
        }
    }

    fun getCurrentBaseUrl(): String {
        val label = availableUrls.find { it.second == currentBaseUrl }?.first ?: ""
        return "$label: $currentBaseUrl"
    }

    fun getSelectedIndex(): Int {
        return availableUrls.indexOfFirst { it.second == currentBaseUrl }
    }

}
