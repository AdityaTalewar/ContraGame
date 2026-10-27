package com.example.game

enum class DifficultyTier(
    val title: String,
    val scoreMultiplier: Float,
    val bulletSpeedMultiplier: Float,
    val enemyFireRateMultiplier: Float,
    val bonusDropRate: Float
) {
    CASUAL("RECRUIT", 1.0f, 0.8f, 0.75f, 1.5f),
    BALANCED("BALANCED", 1.5f, 1.0f, 1.0f, 1.0f),
    VETERAN("VETERAN", 2.5f, 1.25f, 1.3f, 0.8f),
    INSANE("INSANE", 4.0f, 1.5f, 1.6f, 0.5f)
}

class DynamicDifficultyManager {
    var currentTier: DifficultyTier = DifficultyTier.BALANCED
        private set

    private var recentDeaths = 0
    private var deathTimestamps = mutableListOf<Long>()
    private var consecutiveHitsWithoutDamage = 0
    private var recentKills = 0

    // Callback when difficulty tier adjusts
    var onTierChanged: ((DifficultyTier, String) -> Unit)? = null

    fun recordPlayerDeath() {
        val now = System.currentTimeMillis()
        deathTimestamps.add(now)
        consecutiveHitsWithoutDamage = 0
        pruneOldDeaths(now)

        // If struggling (2+ deaths in 40s), decrease difficulty
        if (deathTimestamps.size >= 2) {
            when (currentTier) {
                DifficultyTier.INSANE -> setTier(DifficultyTier.VETERAN, "Support Drop Deployed")
                DifficultyTier.VETERAN -> setTier(DifficultyTier.BALANCED, "Enemy Pace Calibrated")
                DifficultyTier.BALANCED -> setTier(DifficultyTier.CASUAL, "Emergency Armor Assist")
                DifficultyTier.CASUAL -> {}
            }
            deathTimestamps.clear()
        }
    }

    fun recordEnemyKilled() {
        recentKills++
        consecutiveHitsWithoutDamage++

        // If dominating (12+ consecutive kills without dying), ramp up challenge and rewards!
        if (consecutiveHitsWithoutDamage >= 12) {
            when (currentTier) {
                DifficultyTier.CASUAL -> setTier(DifficultyTier.BALANCED, "Difficulty Raised: Standard")
                DifficultyTier.BALANCED -> setTier(DifficultyTier.VETERAN, "High Threat Level: 2.5x Score!")
                DifficultyTier.VETERAN -> setTier(DifficultyTier.INSANE, "MAXIMUM INTENSITY: 4x Score!")
                DifficultyTier.INSANE -> {}
            }
            consecutiveHitsWithoutDamage = 0
        }
    }

    fun recordPlayerDamaged() {
        consecutiveHitsWithoutDamage = maxOf(0, consecutiveHitsWithoutDamage - 4)
    }

    private fun setTier(newTier: DifficultyTier, reason: String) {
        if (currentTier != newTier) {
            currentTier = newTier
            onTierChanged?.invoke(newTier, reason)
        }
    }

    private fun pruneOldDeaths(now: Long) {
        deathTimestamps.removeAll { now - it > 40_000L }
    }

    fun reset() {
        currentTier = DifficultyTier.BALANCED
        deathTimestamps.clear()
        consecutiveHitsWithoutDamage = 0
        recentKills = 0
    }

    fun setDirectTier(tier: DifficultyTier) {
        currentTier = tier
    }
}
