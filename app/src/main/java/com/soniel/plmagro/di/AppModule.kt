package com.soniel.plmagro.di

import android.content.Context
import com.soniel.plmagro.api.WialonRepository
import com.soniel.plmagro.api.WialonSessionManager
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
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

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
        @ApplicationContext context: Context
    ): PlmRepository {
        return PlmRepository(plmDao, context)
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
        diagnosticRepository: DiagnosticRepository
    ): WialonRepository {
        return WialonRepository(sessionManager, userPreferencesManager, diagnosticRepository)
    }

    @Provides
    @Singleton
    fun provideOutboxManager(
        plmDao: PlmDao,
        wialonRepository: WialonRepository
    ): OutboxManager {
        return OutboxManager(plmDao, wialonRepository)
    }
}
