package com.soniel.plmagro.di

import android.content.Context
import com.soniel.plmagro.api.WialonRepository
import com.soniel.plmagro.api.WialonSessionManager
import com.google.gson.Gson
import com.soniel.plmagro.core.outbox.OutboxManager
import com.soniel.plmagro.core.utils.AlertManager
import com.soniel.plmagro.model.DiagnosticRepository
import com.soniel.plmagro.model.PlmDao
import com.soniel.plmagro.model.PlmRepository
import com.soniel.plmagro.model.UserPreferencesManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideGson(): Gson {
        return Gson()
    }



    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): WialonSessionManager {
        return WialonSessionManager(context)
    }

    @Provides
    @Singleton
    fun provideUserPreferencesManager(@ApplicationContext context: Context): UserPreferencesManager {
        return UserPreferencesManager(context)
    }

    @Provides
    @Singleton
    fun providePlmRepository(
        plmDao: PlmDao,
        canBusManager: com.soniel.plmagro.core.hardware.CanBusManager,
        @ApplicationContext context: Context
    ): PlmRepository {
        return PlmRepository(plmDao, canBusManager, context)
    }

    @Provides
    @Singleton
    fun provideAlertManager(@ApplicationContext context: Context): AlertManager {
        return AlertManager(context)
    }

    @Provides
    @Singleton
    fun provideDiagnosticRepository(plmDao: PlmDao): DiagnosticRepository {
        return DiagnosticRepository(plmDao)
    }

    @Provides
    @Singleton
    fun provideWialonRepository(
        sessionManager: WialonSessionManager,
        userPreferencesManager: UserPreferencesManager,
        diagnosticRepository: DiagnosticRepository,
        plmDao: PlmDao
    ): WialonRepository {
        return WialonRepository(sessionManager, userPreferencesManager, diagnosticRepository, plmDao)
    }

    @Provides
    @Singleton
    fun provideOutboxManager(
        plmDao: PlmDao,
        wialonRepository: WialonRepository,
        erpRepository: com.soniel.plmagro.api.ErpRepository,
        userPreferencesManager: UserPreferencesManager,
        @ApplicationContext context: Context
    ): OutboxManager {
        return OutboxManager(plmDao, wialonRepository, erpRepository, userPreferencesManager, context)
    }
}
