package com.example.monktemple.RoomUser

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "user_statistics",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])] // Added index for foreign key
)
data class NewStatistics(
    @PrimaryKey(autoGenerate = true)
    val statId: Long = 0,

    val userId: String,
    val firebaseUid: String?, // Store Firebase UID for migration

    // Time period identifier
    val periodType: String, // "day" or "week"
    val periodStart: Long, // Start timestamp of the period
    val periodEnd: Long,   // End timestamp of the period

    // Statistics data
    val noOfSessions: Int,
    val focusTime: Long, // in milliseconds
    val averageSessionTime: Long, // in milliseconds
    val longestSession: Long, // in milliseconds
    val completionRate: Float, // percentage
    val mostProductiveDay: String,

    // Metadata
    val lastUpdated: Long = System.currentTimeMillis()
)