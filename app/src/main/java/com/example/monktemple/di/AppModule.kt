package com.example.monktemple.di

import android.content.Context
import androidx.room.Room
import com.example.monktemple.RoomUser.UserDao
import com.example.monktemple.RoomUser.UserDatabase
import com.example.monktemple.UserManager
import com.example.monktemple.Utlis.SessionManager
import com.example.monktemple.ai.AIInsightsManager
import com.example.monktemple.community.FocusCommunityManager
import com.example.monktemple.customization.CustomizationManager
import com.example.monktemple.data.remote.FirebaseRemoteDataSource
import com.example.monktemple.data.sync.DataSyncManager
import com.example.monktemple.data.sync.EnhancedSyncManager
import com.example.monktemple.data.sync.UserDataManager
import com.example.monktemple.security.SecurityManager
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
    fun provideUserDao(database: UserDatabase): UserDao {
        return database.userDao()
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
        userDao: UserDao,
        remoteDataSource: FirebaseRemoteDataSource
    ): UserDataManager {
        return UserDataManager(context, userDao, remoteDataSource)
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

    @Provides
    @Singleton
    fun provideAIInsightsManager(
        @ApplicationContext context: Context,
        userDao: UserDao
    ): AIInsightsManager {
        return AIInsightsManager(context, userDao)
    }

    @Provides
    @Singleton
    fun provideFocusCommunityManager(
        @ApplicationContext context: Context,
        remoteDataSource: FirebaseRemoteDataSource
    ): FocusCommunityManager {
        return FocusCommunityManager(context, remoteDataSource)
    }


    @Provides
    @Singleton
    fun provideCustomizationManager(
        @ApplicationContext context: Context
    ): CustomizationManager {
        return CustomizationManager(context)
    }

    @Provides
    @Singleton
    fun provideSecurityManager(
        @ApplicationContext context: Context
    ): SecurityManager {
        return SecurityManager(context)
    }

    @Provides
    @Singleton
    fun provideEnhancedSyncManager(
        @ApplicationContext context: Context,
        userDao: UserDao,
        remoteDataSource: FirebaseRemoteDataSource
    ): EnhancedSyncManager {
        return EnhancedSyncManager(context, userDao, remoteDataSource)
    }
}
