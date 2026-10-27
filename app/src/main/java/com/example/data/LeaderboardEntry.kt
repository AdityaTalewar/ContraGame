package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "leaderboard")
data class LeaderboardEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val playerName: String,
    val score: Int,
    val stageReached: Int,
    val isCoop: Boolean,
    val difficultyTier: String, // CASUAL, BALANCED, VETERAN, INSANE
    val kills: Int,
    val accuracyPercent: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
