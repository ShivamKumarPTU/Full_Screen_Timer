package com.example.monktemple.RoomUser
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "focus_sessions",
    foreignKeys = [
        ForeignKey(
            entity = User::class,
            parentColumns = ["userId"],
            childColumns = ["sessionOwnerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sessionOwnerId"])] // Added index for foreign key
)
data class UserClass(
    @PrimaryKey(autoGenerate = true)
    val sessionId: Int = 0,

    val sessionOwnerId: String, // This column now has an index

    val completionTimestamp: Long,
    val workDuration: Long,
    val status: String
)