package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface LeaderboardDao {
    @Query("SELECT * FROM leaderboard ORDER BY score DESC LIMIT 50")
    fun getAllHighScores(): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard WHERE isCoop = :isCoop ORDER BY score DESC LIMIT 50")
    fun getScoresByMode(isCoop: Boolean): Flow<List<LeaderboardEntry>>

    @Query("SELECT * FROM leaderboard WHERE isSynced = 0")
    suspend fun getUnsyncedScores(): List<LeaderboardEntry>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScore(entry: LeaderboardEntry): Long

    @Query("UPDATE leaderboard SET isSynced = 1 WHERE id IN (:ids)")
    suspend fun markScoresSynced(ids: List<Long>)

    @Query("DELETE FROM leaderboard")
    suspend fun clearAll()
}
