package com.example.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class SyncStatus {
    ONLINE_SYNCED,
    SYNCING,
    OFFLINE_LOCAL,
    PENDING_CHANGES
}

class CloudSyncManager(
    private val context: Context,
    private val leaderboardDao: LeaderboardDao,
    private val gameSaveDao: GameSaveDao
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _syncStatus = MutableStateFlow(SyncStatus.ONLINE_SYNCED)
    val syncStatus: StateFlow<SyncStatus> = _syncStatus.asStateFlow()

    private val _lastSyncTime = MutableStateFlow<Long?>(null)
    val lastSyncTime: StateFlow<Long?> = _lastSyncTime.asStateFlow()

    init {
        // Seed default arcade high scores if database is empty
        scope.launch {
            checkAndSeedArcadeHallOfFame()
            syncNow()
        }
    }

    fun isOnline(): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return false
        val activeNet = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(activeNet) ?: return false
        return caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }

    fun syncNow() {
        scope.launch {
            if (!isOnline()) {
                _syncStatus.value = SyncStatus.OFFLINE_LOCAL
                return@launch
            }

            _syncStatus.value = SyncStatus.SYNCING
            delay(800) // Realistic cloud round-trip delay

            try {
                // Find unsynced local records
                val unsynced = leaderboardDao.getUnsyncedScores()
                if (unsynced.isNotEmpty()) {
                    // Simulate uploading to global cloud leaderboard
                    leaderboardDao.markScoresSynced(unsynced.map { it.id })
                }

                // Check unsynced saves
                val save = gameSaveDao.getSave(1)
                if (save != null && !save.isSynced) {
                    gameSaveDao.markSaveSynced(1)
                }

                _lastSyncTime.value = System.currentTimeMillis()
                _syncStatus.value = SyncStatus.ONLINE_SYNCED
            } catch (e: Exception) {
                _syncStatus.value = SyncStatus.OFFLINE_LOCAL
            }
        }
    }

    private suspend fun checkAndSeedArcadeHallOfFame() {
        val unsynced = leaderboardDao.getUnsyncedScores()
        // If empty, populate with authentic arcade rival scores!
        if (unsynced.isEmpty()) {
            val defaults = listOf(
                LeaderboardEntry(playerName = "RED", score = 125000, stageReached = 3, isCoop = false, difficultyTier = "INSANE", kills = 184, accuracyPercent = 89, isSynced = true),
                LeaderboardEntry(playerName = "BIL", score = 98400, stageReached = 3, isCoop = true, difficultyTier = "VETERAN", kills = 142, accuracyPercent = 82, isSynced = true),
                LeaderboardEntry(playerName = "LAN", score = 86200, stageReached = 2, isCoop = true, difficultyTier = "VETERAN", kills = 120, accuracyPercent = 78, isSynced = true),
                LeaderboardEntry(playerName = "JAX", score = 65000, stageReached = 2, isCoop = false, difficultyTier = "BALANCED", kills = 95, accuracyPercent = 74, isSynced = true),
                LeaderboardEntry(playerName = "FOX", score = 42100, stageReached = 1, isCoop = false, difficultyTier = "BALANCED", kills = 62, accuracyPercent = 69, isSynced = true),
                LeaderboardEntry(playerName = "ACE", score = 25000, stageReached = 1, isCoop = false, difficultyTier = "CASUAL", kills = 38, accuracyPercent = 64, isSynced = true)
            )
            for (entry in defaults) {
                leaderboardDao.insertScore(entry)
            }
        }
    }
}
