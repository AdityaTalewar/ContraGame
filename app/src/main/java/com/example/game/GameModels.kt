package com.example.game

enum class PlayerId {
    PLAYER_1, PLAYER_2
}

enum class WeaponType(
    val code: String,
    val displayName: String,
    val fireRateMs: Long,
    val bulletSpeed: Float,
    val baseDamage: Float
) {
    RIFLE("R", "Normal Rifle", 200L, 14f, 10f),
    MACHINE_GUN("M", "Machine Gun", 85L, 16f, 12f),
    SPREAD("S", "Spread Gun", 220L, 13f, 14f),
    LASER("L", "Laser Gun", 160L, 20f, 32f),
    FIREBALL("F", "Fire Gun", 180L, 11f, 26f),
    BARRIER("B", "Barrier Shield", 0L, 0f, 0f)
}

enum class PlayerState {
    IDLE, RUNNING, JUMPING, CROUCHING, PRONE, WATER, DEAD, RESPAWNING
}

enum class AimDirection {
    STRAIGHT, UP, DOWN, UP_DIAG, DOWN_DIAG
}

data class Player(
    val id: PlayerId,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var facingRight: Boolean = true,
    var aimDirection: AimDirection = AimDirection.STRAIGHT,
    var state: PlayerState = PlayerState.IDLE,
    var weapon: WeaponType = WeaponType.RIFLE,
    var lives: Int = 3,
    var score: Int = 0,
    var health: Float = 100f,
    var maxHealth: Float = 100f,
    var barrierTime: Float = 0f,         // seconds remaining on barrier
    var invulnerableTime: Float = 0f,    // respawn flashing invulnerability
    var somersaultAngle: Float = 0f,     // rotation during jump
    var animFrame: Float = 0f,
    var isGrounded: Boolean = false,
    var lastFireTime: Long = 0L,
    var kills: Int = 0,
    var shotsFired: Int = 0,
    var shotsHit: Int = 0,
    var respawnTimer: Float = 0f
) {
    val isAlive: Boolean get() = lives > 0 || state != PlayerState.DEAD
    val isInvincible: Boolean get() = barrierTime > 0f || invulnerableTime > 0f
}

data class Bullet(
    val id: Long,
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val weaponType: WeaponType,
    val ownerId: PlayerId?, // null if enemy bullet
    val damage: Float,
    val isEnemy: Boolean,
    val radius: Float = 4f,
    var life: Float = 1.6f, // lifetime in seconds
    val penetrates: Boolean = false
)

enum class EnemyType(
    val maxHp: Float,
    val scoreValue: Int,
    val width: Float,
    val height: Float
) {
    RUNNER(10f, 100, 22f, 36f),
    SNIPER(20f, 200, 24f, 28f),
    PALM_SNIPER(25f, 250, 22f, 32f),
    WALL_TURRET(60f, 500, 32f, 32f),
    CEILING_POD(30f, 300, 26f, 26f),
    FLYING_ALIEN(35f, 400, 28f, 24f),
    JETPACK_SOLDIER(50f, 450, 26f, 34f),
    FACEHUGGER(10f, 150, 18f, 14f)
}

data class Enemy(
    val id: Long,
    val type: EnemyType,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var health: Float = type.maxHp,
    var shootCooldown: Float = 2.0f,
    var facingRight: Boolean = false,
    var animFrame: Float = 0f,
    var isGrounded: Boolean = false,
    var stateTime: Float = 0f
)

enum class BossType(val stageIndex: Int, val displayName: String, val maxHp: Float) {
    GORGON_WALL(0, "Gorgon Defense Wall", 800f),
    SHADOW_BEAST_GAVA(1, "Shadow Beast Gava", 1200f),
    ALIEN_HEART_MOTHER(2, "Emperor Demon Heart", 1600f)
}

data class BossSubComponent(
    val id: Int,
    val name: String,
    val relX: Float,
    val relY: Float,
    val width: Float,
    val height: Float,
    var health: Float,
    val maxHealth: Float,
    var isDestroyed: Boolean = false
)

data class Boss(
    val type: BossType,
    var x: Float,
    var y: Float,
    var health: Float = type.maxHp,
    val maxHealth: Float = type.maxHp,
    var phase: Int = 1,
    var attackTimer: Float = 0f,
    var subComponents: MutableList<BossSubComponent> = mutableListOf(),
    var isDefeated: Boolean = false,
    var deathAnimTimer: Float = 0f,
    var animFrame: Float = 0f
)

data class PowerUpItem(
    val id: Long,
    val type: WeaponType,
    var x: Float,
    var y: Float,
    var vx: Float = 0f,
    var vy: Float = 0f,
    var isFloatingCapsule: Boolean = false, // Floating red falcon capsule
    var life: Float = 15f
)

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    val color: Long,
    var size: Float,
    var alpha: Float = 1.0f,
    var life: Float = 0.5f,
    val maxLife: Float = 0.5f
)

data class Platform(
    val x: Float,
    val y: Float,
    val width: Float,
    val height: Float,
    val isOneWay: Boolean = true, // Can jump up through or drop down with Down + Jump
    val isWater: Boolean = false
)
