package com.example.myapplication.di

import android.content.Context
import androidx.room.Room
import com.example.myapplication.data.local.AppDatabase
import com.example.myapplication.data.local.dao.NodeHealthDao
import com.example.myapplication.data.local.dao.QueryResultDao
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
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "hospital_federation_db"
        ).fallbackToDestructiveMigration().build()
    }

    @Provides
    @Singleton
    fun provideQueryResultDao(appDatabase: AppDatabase): QueryResultDao {
        return appDatabase.queryResultDao()
    }

    @Provides
    @Singleton
    fun provideNodeHealthDao(appDatabase: AppDatabase): NodeHealthDao {
        return appDatabase.nodeHealthDao()
    }
}
