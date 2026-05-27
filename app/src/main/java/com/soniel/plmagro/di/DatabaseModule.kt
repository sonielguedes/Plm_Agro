package com.soniel.plmagro.di

import android.content.Context
import com.soniel.plmagro.model.PlmDao
import com.soniel.plmagro.model.PlmDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): PlmDatabase {
        return PlmDatabase.getDatabase(context)
    }

    @Provides
    fun providePlmDao(database: PlmDatabase): PlmDao {
        return database.plmDao()
    }
}
