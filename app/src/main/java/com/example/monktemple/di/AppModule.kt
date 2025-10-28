package com.example.monktemple.di

import android.content.Context
import androidx.room.Room
import com.example.monktemple.RoomUser.UserDatabase
import com.example.monktemple.UserManager
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.sync.DataSyncManager
import com.example.monktemple.data.sync.UserDataManager
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
    fun provideUserDatabase(@ApplicationContext context: Context): UserDatabase {
        return Room.databaseBuilder(
            context.applicationContext,
            UserDatabase::class.java,
            "user_database"
        ).fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideFirebaseRemoteDataSource(): FirebaseRemoteDataSource {
        return FirebaseRemoteDataSource()
    }

    @Provides
    @Singleton
    fun provideUserDataManager(
        @ApplicationContext context: Context,
        userDatabase: UserDatabase,
        remoteDataSource: FirebaseRemoteDataSource
    ): UserDataManager {
        return UserDataManager(context, userDatabase.userDao(), remoteDataSource)
    }

    @Provides
    @Singleton
    fun provideDataSyncManager(
        @ApplicationContext context: Context,
        userDataManager: UserDataManager,
        remoteDataSource: FirebaseRemoteDataSource
    ): DataSyncManager {
        return DataSyncManager(context, userDataManager, remoteDataSource)
    }

    // Add SessionManager provider
    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    // Add UserManager provider
    @Provides
    @Singleton
    fun provideUserManager(sessionManager: SessionManager): UserManager {
        return UserManager(sessionManager)
    }
}