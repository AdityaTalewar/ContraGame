package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSaveDao {
    @Query("SELECT * FROM game_saves WHERE slotId = :slotId LIMIT 1")
    suspend fun getSave(slotId: Int): GameSaveEntity?

    @Query("SELECT * FROM game_saves WHERE slotId = :slotId LIMIT 1")
    fun getSaveFlow(slotId: Int): Flow<GameSaveEntity?>

    @Query("SELECT * FROM game_saves ORDER BY timestamp DESC")
    fun getAllSaves(): Flow<List<GameSaveEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveGame(save: GameSaveEntity)

    @Query("DELETE FROM game_saves WHERE slotId = :slotId")
    suspend fun deleteSave(slotId: Int)

    @Query("UPDATE game_saves SET isSynced = 1 WHERE slotId = :slotId")
    suspend fun markSaveSynced(slotId: Int)
}
