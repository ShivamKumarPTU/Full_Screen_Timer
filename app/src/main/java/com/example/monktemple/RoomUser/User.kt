package com.example.monktemple.RoomUser

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "users",
    indices = [Index(value = ["firebaseUid"], unique = true)]
)
data class User(
    @PrimaryKey
    val userId: String, // Changed from String? to String (non-nullable)
    val firebaseUid: String?, // This links to Firebase authentication
    val displayName: String?,
    val email: String?,
    val photoUrl: String?,
    val createdAt: Long = System.currentTimeMillis(),
    val lastLogin: Long = System.currentTimeMillis() // Track last login
)