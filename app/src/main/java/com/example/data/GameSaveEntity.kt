package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_saves")
data class GameSaveEntity(
    @PrimaryKey
    val slotId: Int = 1, // 1 for quick-save, 2/3 for manual slots
    val stageIndex: Int = 0,
    val stageProgressX: Float = 0f,
    val isCoop: Boolean = false,
    val p1Weapon: String = "RIFLE",
    val p1Lives: Int = 3,
    val p1Score: Int = 0,
    val p1Health: Int = 100,
    val p2Weapon: String = "RIFLE",
    val p2Lives: Int = 3,
    val p2Score: Int = 0,
    val p2Health: Int = 100,
    val difficultyTier: String = "BALANCED",
    val timestamp: Long = System.currentTimeMillis(),
    val isSynced: Boolean = false
)
