package com.example.myapplication.di

import com.example.myapplication.data.repository.AuthRepository
import com.example.myapplication.data.repository.AuthRepositoryImpl
import com.example.myapplication.data.repository.LogsRepository
import com.example.myapplication.data.repository.LogsRepositoryImpl
import com.example.myapplication.data.repository.NodeHealthRepository
import com.example.myapplication.data.repository.NodeHealthRepositoryImpl
import com.example.myapplication.data.repository.QueryRepository
import com.example.myapplication.data.repository.QueryRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository

    @Binds
    @Singleton
    abstract fun bindQueryRepository(
        queryRepositoryImpl: QueryRepositoryImpl
    ): QueryRepository

    @Binds
    @Singleton
    abstract fun bindNodeHealthRepository(
        nodeHealthRepositoryImpl: NodeHealthRepositoryImpl
    ): NodeHealthRepository

    @Binds
    @Singleton
    abstract fun bindLogsRepository(
        logsRepositoryImpl: LogsRepositoryImpl
    ): LogsRepository
}
