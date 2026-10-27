package com.example.game

data class EnemySpawnTrigger(
    val triggerX: Float,
    val type: EnemyType,
    val spawnX: Float,
    val spawnY: Float,
    val facingRight: Boolean = false
)

data class PowerUpCapsuleTrigger(
    val triggerX: Float,
    val weaponType: WeaponType,
    val startX: Float,
    val startY: Float
)

data class StageDef(
    val stageIndex: Int,
    val name: String,
    val subtitle: String,
    val totalLength: Float,
    val bossArenaX: Float,
    val bossType: BossType,
    val groundY: Float = 420f,
    val platforms: List<Platform>,
    val enemySpawns: List<EnemySpawnTrigger>,
    val capsuleSpawns: List<PowerUpCapsuleTrigger>
)

object StageCatalog {

    fun getStage(stageIndex: Int): StageDef {
        return when (stageIndex) {
            0 -> createStage1Jungle()
            1 -> createStage2Waterfall()
            2 -> createStage3AlienCore()
            else -> createStage1Jungle()
        }
    }

    private fun createStage1Jungle(): StageDef {
        val platforms = mutableListOf<Platform>()
        // Ground segments with bridges and water gaps
        platforms.add(Platform(0f, 420f, 600f, 180f, isOneWay = false))
        // River section with water hazard
        platforms.add(Platform(600f, 440f, 240f, 160f, isOneWay = false, isWater = true))
        // Suspended wooden bridge over water
        platforms.add(Platform(620f, 380f, 200f, 20f, isOneWay = true))

        platforms.add(Platform(840f, 420f, 650f, 180f, isOneWay = false))
        // Mid-air tree platforms & rock ledges
        platforms.add(Platform(220f, 340f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(340f, 270f, 140f, 18f, isOneWay = true))
        platforms.add(Platform(440f, 200f, 120f, 18f, isOneWay = true))

        platforms.add(Platform(950f, 330f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(1150f, 260f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(1350f, 340f, 180f, 18f, isOneWay = true))

        // Second river gap
        platforms.add(Platform(1490f, 440f, 220f, 160f, isOneWay = false, isWater = true))
        platforms.add(Platform(1510f, 370f, 180f, 20f, isOneWay = true))

        // Outpost approach
        platforms.add(Platform(1710f, 420f, 700f, 180f, isOneWay = false))
        platforms.add(Platform(1850f, 320f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(2100f, 250f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(2280f, 330f, 160f, 18f, isOneWay = true))

        // Boss arena ground
        platforms.add(Platform(2410f, 420f, 790f, 180f, isOneWay = false))
        platforms.add(Platform(2500f, 340f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(2680f, 270f, 160f, 18f, isOneWay = true))

        // Enemy triggers
        val enemies = listOf(
            EnemySpawnTrigger(150f, EnemyType.RUNNER, 550f, 400f),
            EnemySpawnTrigger(250f, EnemyType.PALM_SNIPER, 470f, 180f),
            EnemySpawnTrigger(350f, EnemyType.RUNNER, 700f, 400f),
            EnemySpawnTrigger(500f, EnemyType.SNIPER, 900f, 400f),
            EnemySpawnTrigger(750f, EnemyType.RUNNER, 1100f, 400f),
            EnemySpawnTrigger(880f, EnemyType.WALL_TURRET, 1200f, 240f),
            EnemySpawnTrigger(1020f, EnemyType.SNIPER, 1380f, 320f),
            EnemySpawnTrigger(1250f, EnemyType.RUNNER, 1600f, 400f),
            EnemySpawnTrigger(1400f, EnemyType.JETPACK_SOLDIER, 1800f, 200f),
            EnemySpawnTrigger(1650f, EnemyType.WALL_TURRET, 2150f, 230f),
            EnemySpawnTrigger(1800f, EnemyType.RUNNER, 2200f, 400f),
            EnemySpawnTrigger(1950f, EnemyType.SNIPER, 2320f, 310f),
            EnemySpawnTrigger(2150f, EnemyType.JETPACK_SOLDIER, 2500f, 180f),
            EnemySpawnTrigger(2300f, EnemyType.RUNNER, 2600f, 400f)
        )

        // Flying falcon capsule triggers
        val capsules = listOf(
            PowerUpCapsuleTrigger(200f, WeaponType.SPREAD, 100f, 160f),
            PowerUpCapsuleTrigger(700f, WeaponType.MACHINE_GUN, 600f, 180f),
            PowerUpCapsuleTrigger(1300f, WeaponType.LASER, 1200f, 150f),
            PowerUpCapsuleTrigger(1750f, WeaponType.BARRIER, 1650f, 170f),
            PowerUpCapsuleTrigger(2200f, WeaponType.SPREAD, 2100f, 160f)
        )

        return StageDef(
            stageIndex = 0,
            name = "STAGE 1",
            subtitle = "Jungle Outpost & Gate",
            totalLength = 3200f,
            bossArenaX = 2600f,
            bossType = BossType.GORGON_WALL,
            platforms = platforms,
            enemySpawns = enemies,
            capsuleSpawns = capsules
        )
    }

    private fun createStage2Waterfall(): StageDef {
        val platforms = mutableListOf<Platform>()
        platforms.add(Platform(0f, 420f, 400f, 180f, isOneWay = false))
        platforms.add(Platform(150f, 340f, 150f, 18f, isOneWay = true))
        platforms.add(Platform(280f, 260f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(420f, 330f, 180f, 18f, isOneWay = true))

        platforms.add(Platform(550f, 420f, 500f, 180f, isOneWay = false))
        platforms.add(Platform(700f, 280f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(900f, 200f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(1100f, 280f, 180f, 18f, isOneWay = true))

        platforms.add(Platform(1200f, 420f, 600f, 180f, isOneWay = false))
        platforms.add(Platform(1350f, 320f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(1520f, 240f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(1680f, 330f, 160f, 18f, isOneWay = true))

        platforms.add(Platform(1900f, 420f, 600f, 180f, isOneWay = false))
        platforms.add(Platform(2050f, 320f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(2200f, 240f, 180f, 18f, isOneWay = true))

        // Boss arena
        platforms.add(Platform(2600f, 420f, 900f, 180f, isOneWay = false))
        platforms.add(Platform(2750f, 320f, 200f, 18f, isOneWay = true))
        platforms.add(Platform(3000f, 250f, 200f, 18f, isOneWay = true))

        val enemies = listOf(
            EnemySpawnTrigger(100f, EnemyType.RUNNER, 450f, 400f),
            EnemySpawnTrigger(220f, EnemyType.CEILING_POD, 350f, 120f),
            EnemySpawnTrigger(400f, EnemyType.JETPACK_SOLDIER, 650f, 180f),
            EnemySpawnTrigger(600f, EnemyType.WALL_TURRET, 800f, 260f),
            EnemySpawnTrigger(800f, EnemyType.RUNNER, 1050f, 400f),
            EnemySpawnTrigger(1000f, EnemyType.CEILING_POD, 1200f, 110f),
            EnemySpawnTrigger(1250f, EnemyType.WALL_TURRET, 1450f, 220f),
            EnemySpawnTrigger(1400f, EnemyType.JETPACK_SOLDIER, 1650f, 180f),
            EnemySpawnTrigger(1600f, EnemyType.RUNNER, 1850f, 400f),
            EnemySpawnTrigger(1850f, EnemyType.CEILING_POD, 2000f, 120f),
            EnemySpawnTrigger(2100f, EnemyType.JETPACK_SOLDIER, 2350f, 170f)
        )

        val capsules = listOf(
            PowerUpCapsuleTrigger(200f, WeaponType.FIREBALL, 100f, 150f),
            PowerUpCapsuleTrigger(750f, WeaponType.LASER, 650f, 170f),
            PowerUpCapsuleTrigger(1350f, WeaponType.SPREAD, 1250f, 150f),
            PowerUpCapsuleTrigger(1850f, WeaponType.BARRIER, 1750f, 160f)
        )

        return StageDef(
            stageIndex = 1,
            name = "STAGE 2",
            subtitle = "Waterfall & Cyber Fortress",
            totalLength = 3500f,
            bossArenaX = 2800f,
            bossType = BossType.SHADOW_BEAST_GAVA,
            platforms = platforms,
            enemySpawns = enemies,
            capsuleSpawns = capsules
        )
    }

    private fun createStage3AlienCore(): StageDef {
        val platforms = mutableListOf<Platform>()
        platforms.add(Platform(0f, 420f, 500f, 180f, isOneWay = false))
        platforms.add(Platform(180f, 340f, 160f, 18f, isOneWay = true))
        platforms.add(Platform(320f, 260f, 160f, 18f, isOneWay = true))

        platforms.add(Platform(580f, 420f, 600f, 180f, isOneWay = false))
        platforms.add(Platform(700f, 320f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(900f, 240f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(1050f, 320f, 180f, 18f, isOneWay = true))

        platforms.add(Platform(1280f, 420f, 650f, 180f, isOneWay = false))
        platforms.add(Platform(1400f, 310f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(1600f, 230f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(1780f, 310f, 180f, 18f, isOneWay = true))

        platforms.add(Platform(2000f, 420f, 600f, 180f, isOneWay = false))
        platforms.add(Platform(2150f, 310f, 180f, 18f, isOneWay = true))
        platforms.add(Platform(2350f, 230f, 180f, 18f, isOneWay = true))

        // Boss arena (Alien Lair)
        platforms.add(Platform(2700f, 420f, 950f, 180f, isOneWay = false))
        platforms.add(Platform(2850f, 320f, 220f, 18f, isOneWay = true))
        platforms.add(Platform(3100f, 240f, 220f, 18f, isOneWay = true))

        val enemies = listOf(
            EnemySpawnTrigger(120f, EnemyType.FACEHUGGER, 450f, 410f),
            EnemySpawnTrigger(250f, EnemyType.FLYING_ALIEN, 550f, 150f),
            EnemySpawnTrigger(450f, EnemyType.FACEHUGGER, 750f, 410f),
            EnemySpawnTrigger(650f, EnemyType.FLYING_ALIEN, 850f, 160f),
            EnemySpawnTrigger(850f, EnemyType.FACEHUGGER, 1100f, 410f),
            EnemySpawnTrigger(1050f, EnemyType.FLYING_ALIEN, 1300f, 140f),
            EnemySpawnTrigger(1300f, EnemyType.FACEHUGGER, 1500f, 410f),
            EnemySpawnTrigger(1500f, EnemyType.FLYING_ALIEN, 1700f, 150f),
            EnemySpawnTrigger(1750f, EnemyType.FACEHUGGER, 1950f, 410f),
            EnemySpawnTrigger(1950f, EnemyType.FLYING_ALIEN, 2200f, 160f),
            EnemySpawnTrigger(2200f, EnemyType.FACEHUGGER, 2450f, 410f)
        )

        val capsules = listOf(
            PowerUpCapsuleTrigger(180f, WeaponType.SPREAD, 80f, 160f),
            PowerUpCapsuleTrigger(750f, WeaponType.MACHINE_GUN, 650f, 150f),
            PowerUpCapsuleTrigger(1400f, WeaponType.LASER, 1300f, 170f),
            PowerUpCapsuleTrigger(1900f, WeaponType.BARRIER, 1800f, 160f)
        )

        return StageDef(
            stageIndex = 2,
            name = "STAGE 3",
            subtitle = "Alien Hive & Heart of Demon",
            totalLength = 3800f,
            bossArenaX = 2950f,
            bossType = BossType.ALIEN_HEART_MOTHER,
            platforms = platforms,
            enemySpawns = enemies,
            capsuleSpawns = capsules
        )
    }
}
