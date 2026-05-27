package com.soniel.plmagro.core.network

import com.soniel.plmagro.core.utils.IndustrialLogger
import okhttp3.ConnectionPool
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkModule {
    private const val TAG = "NetworkModule"

    private val connectionPool = ConnectionPool(5, 5, TimeUnit.MINUTES)

    private val okHttpClient: OkHttpClient by lazy {
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            IndustrialLogger.d("OkHttp", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        OkHttpClient.Builder()
            .connectionPool(connectionPool)
            .connectTimeout(10, TimeUnit.SECONDS) // Reduzido de 15 para 10
            .readTimeout(15, TimeUnit.SECONDS)    // Reduzido de 20 para 15
            .writeTimeout(15, TimeUnit.SECONDS)   // Reduzido de 20 para 15
            .retryOnConnectionFailure(true)
            .addInterceptor(loggingInterceptor)
            .build()
    }

    private var retrofit: Retrofit? = null
    private var currentBaseUrl: String? = null

    fun getRetrofit(baseUrl: String): Retrofit {
        synchronized(this) {
            if (retrofit == null || currentBaseUrl != baseUrl) {
                IndustrialLogger.i(TAG, "Creating Retrofit instance for $baseUrl")
                currentBaseUrl = baseUrl
                retrofit = Retrofit.Builder()
                    .baseUrl(baseUrl)
                    .client(okHttpClient)
                    .addConverterFactory(GsonConverterFactory.create())
                    .build()
            }
            return retrofit!!
        }
    }
}
