package com.example.game

import com.example.audio.ChiptuneSynth
import kotlin.math.*
import kotlin.random.Random

class GameEngine(
    private val audioSynth: ChiptuneSynth,
    val ddaManager: DynamicDifficultyManager
) {
    var isCoop: Boolean = false
    var currentStageIndex: Int = 0
    var stageDef: StageDef = StageCatalog.getStage(0)

    val player1 = Player(
        id = PlayerId.PLAYER_1,
        x = 100f,
        y = 380f,
        facingRight = true,
        weapon = WeaponType.RIFLE
    )

    val player2 = Player(
        id = PlayerId.PLAYER_2,
        x = 60f,
        y = 380f,
        facingRight = true,
        weapon = WeaponType.RIFLE
    )

    val bullets = mutableListOf<Bullet>()
    val enemies = mutableListOf<Enemy>()
    val powerUps = mutableListOf<PowerUpItem>()
    val particles = mutableListOf<Particle>()
    var boss: Boss? = null

    var cameraX: Float = 0f
    var cameraY: Float = 0f
    var screenWidth: Float = 800f
    var screenHeight: Float = 480f

    var isPaused: Boolean = false
    var isGameOver: Boolean = false
    var isStageCleared: Boolean = false
    var isCampaignVictory: Boolean = false
    var screenShakeAmount: Float = 0f

    var statusNotification: String? = null
    var notificationTimer: Float = 0f

    private var nextBulletId = 1L
    private var nextEnemyId = 1L
    private var nextPowerUpId = 1L
    private val triggeredSpawns = mutableSetOf<Int>()
    private val triggeredCapsules = mutableSetOf<Int>()

    // Inputs for Player 1
    var p1MoveX = 0f
    var p1MoveY = 0f
    var p1JumpPressed = false
    var p1FirePressed = false

    // Inputs for Player 2 (Local Co-op)
    var p2MoveX = 0f
    var p2MoveY = 0f
    var p2JumpPressed = false
    var p2FirePressed = false

    init {
        loadStage(0)
    }

    fun loadStage(stageIndex: Int) {
        currentStageIndex = stageIndex
        stageDef = StageCatalog.getStage(stageIndex)
        bullets.clear()
        enemies.clear()
        powerUps.clear()
        particles.clear()
        boss = null
        triggeredSpawns.clear()
        triggeredCapsules.clear()
        cameraX = 0f
        isStageCleared = false
        isGameOver = false

        player1.x = 80f
        player1.y = 360f
        player1.vx = 0f
        player1.vy = 0f
        player1.state = PlayerState.IDLE
        player1.health = 100f
        player1.invulnerableTime = 3f

        if (isCoop) {
            player2.x = 40f
            player2.y = 360f
            player2.vx = 0f
            player2.vy = 0f
            player2.state = PlayerState.IDLE
            player2.health = 100f
            player2.invulnerableTime = 3f
        }

        // Start stage soundtrack
        val track = when (stageIndex) {
            0 -> ChiptuneSynth.Track.JUNGLE
            1 -> ChiptuneSynth.Track.WATERFALL
            2 -> ChiptuneSynth.Track.ALIEN_CORE
            else -> ChiptuneSynth.Track.JUNGLE
        }
        audioSynth.playTrack(track)
        showNotification("${stageDef.name}: ${stageDef.subtitle}")
    }

    fun showNotification(msg: String, duration: Float = 2.5f) {
        statusNotification = msg
        notificationTimer = duration
    }

    fun update(dt: Float) {
        if (isPaused) return

        if (notificationTimer > 0f) {
            notificationTimer -= dt
            if (notificationTimer <= 0f) statusNotification = null
        }

        if (screenShakeAmount > 0f) {
            screenShakeAmount = maxOf(0f, screenShakeAmount - (dt * 15f))
        }

        if (isGameOver || isCampaignVictory) {
            updateParticles(dt)
            return
        }

        if (isStageCleared) {
            updateStageClear(dt)
            return
        }

        // 1. Update Players
        updatePlayer(player1, p1MoveX, p1MoveY, p1JumpPressed, p1FirePressed, dt)
        if (isCoop) {
            updatePlayer(player2, p2MoveX, p2MoveY, p2JumpPressed, p2FirePressed, dt)
        }

        // Check game over
        val p1Lost = player1.lives <= 0 && player1.state == PlayerState.DEAD
        val p2Lost = !isCoop || (player2.lives <= 0 && player2.state == PlayerState.DEAD)
        if (p1Lost && p2Lost) {
            isGameOver = true
            audioSynth.playTrack(ChiptuneSynth.Track.GAME_OVER)
            showNotification("MISSION FAILED - GAME OVER", 4f)
            return
        }

        // 2. Camera tracking
        updateCamera()

        // 3. Spawners & Triggers
        checkTriggers()

        // 4. Update Bullets
        updateBullets(dt)

        // 5. Update Enemies
        updateEnemies(dt)

        // 6. Update Boss
        updateBoss(dt)

        // 7. Update PowerUps
        updatePowerUps(dt)

        // 8. Update Particles
        updateParticles(dt)
    }

    private fun updatePlayer(
        player: Player,
        moveX: Float,
        moveY: Float,
        jumpPressed: Boolean,
        firePressed: Boolean,
        dt: Float
    ) {
        if (player.state == PlayerState.DEAD) {
            player.respawnTimer -= dt
            if (player.respawnTimer <= 0f && player.lives > 0) {
                // Respawn
                player.state = PlayerState.RESPAWNING
                player.health = 100f
                player.invulnerableTime = 3.5f
                player.weapon = WeaponType.RIFLE
                val refX = if (player.id == PlayerId.PLAYER_2 && player1.isAlive) player1.x else cameraX + 80f
                player.x = refX.coerceIn(cameraX + 40f, cameraX + screenWidth - 40f)
                player.y = 200f
                player.vy = 0f
                player.vx = 0f
            }
            return
        }

        if (player.invulnerableTime > 0f) {
            player.invulnerableTime -= dt
            if (player.invulnerableTime <= 0f && player.state == PlayerState.RESPAWNING) {
                player.state = PlayerState.IDLE
            }
        }

        if (player.barrierTime > 0f) {
            player.barrierTime -= dt
        }

        // Aiming direction logic (Contra 8-way aiming)
        player.aimDirection = when {
            moveY < -0.4f && abs(moveX) > 0.3f -> AimDirection.UP_DIAG
            moveY > 0.4f && abs(moveX) > 0.3f -> AimDirection.DOWN_DIAG
            moveY < -0.5f -> AimDirection.UP
            moveY > 0.5f && player.isGrounded -> AimDirection.DOWN // Prone crouch
            else -> AimDirection.STRAIGHT
        }

        if (moveX > 0.2f) player.facingRight = true
        else if (moveX < -0.2f) player.facingRight = false

        // Horizontal speed
        val speed = if (player.aimDirection == AimDirection.DOWN) 0f else 170f
        player.vx = when {
            moveX > 0.3f -> speed
            moveX < -0.3f -> -speed
            else -> 0f
        }

        // Drop down through one-way platform if crouching and jump pressed
        if (jumpPressed && moveY > 0.5f && player.isGrounded) {
            player.y += 6f
            player.isGrounded = false
            player.vy = 60f
        } else if (jumpPressed && player.isGrounded) {
            // Jump
            player.vy = -380f
            player.isGrounded = false
            player.state = PlayerState.JUMPING
            player.somersaultAngle = 0f
            audioSynth.playSfx(ChiptuneSynth.Sfx.JUMP)
        }

        // Apply Gravity
        player.vy += 850f * dt

        // Move
        player.x += player.vx * dt
        player.y += player.vy * dt

        // Keep player inside camera horizontal bounds
        val minX = cameraX + 16f
        val maxX = cameraX + screenWidth - 16f
        if (player.x < minX) player.x = minX
        if (player.x > maxX) player.x = maxX

        // Check Platform Collisions
        val playerFeet = player.y + 24f
        var onPlatform = false

        for (plat in stageDef.platforms) {
            val platLeft = plat.x
            val platRight = plat.x + plat.width
            val platTop = plat.y

            if (player.x >= platLeft && player.x <= platRight) {
                if (plat.isWater) {
                    if (player.y >= platTop - 12f) {
                        player.y = platTop - 12f
                        player.vy = 0f
                        player.isGrounded = true
                        player.state = PlayerState.WATER
                        onPlatform = true
                        break
                    }
                } else if (player.vy >= 0f && playerFeet >= platTop && playerFeet <= platTop + 20f) {
                    player.y = platTop - 24f
                    player.vy = 0f
                    player.isGrounded = true
                    onPlatform = true
                    if (player.state == PlayerState.JUMPING) {
                        player.state = if (abs(player.vx) > 10f) PlayerState.RUNNING else PlayerState.IDLE
                    }
                    break
                }
            }
        }

        if (!onPlatform) {
            player.isGrounded = false
            if (player.state != PlayerState.JUMPING && player.state != PlayerState.WATER) {
                player.state = PlayerState.JUMPING
            }
        }

        // Animation update
        if (player.state == PlayerState.JUMPING) {
            player.somersaultAngle += (if (player.facingRight) 720f else -720f) * dt
        } else if (player.isGrounded) {
            player.somersaultAngle = 0f
            player.state = when {
                player.aimDirection == AimDirection.DOWN -> PlayerState.PRONE
                abs(player.vx) > 10f -> PlayerState.RUNNING
                else -> PlayerState.IDLE
            }
            if (player.state == PlayerState.RUNNING) {
                player.animFrame += dt * 10f
            }
        }

        // Bottom pit kill
        if (player.y > 520f) {
            killPlayer(player)
        }

        // Weapon firing
        if (firePressed) {
            firePlayerWeapon(player)
        }
    }

    private fun firePlayerWeapon(player: Player) {
        val now = System.currentTimeMillis()
        val cooldown = player.weapon.fireRateMs
        if (now - player.lastFireTime < cooldown) return

        player.lastFireTime = now
        player.shotsFired++

        val spawnX = player.x + (if (player.facingRight) 16f else -16f)
        val spawnY = when (player.aimDirection) {
            AimDirection.UP -> player.y - 18f
            AimDirection.DOWN -> player.y + 12f // prone shot
            AimDirection.UP_DIAG -> player.y - 10f
            AimDirection.DOWN_DIAG -> player.y + 8f
            AimDirection.STRAIGHT -> player.y - 2f
        }

        val baseDirX = if (player.facingRight) 1f else -1f
        val (dirX, dirY) = when (player.aimDirection) {
            AimDirection.UP -> 0f to -1f
            AimDirection.DOWN -> baseDirX to 0f // Prone shoots straight ahead
            AimDirection.UP_DIAG -> (baseDirX * 0.707f) to -0.707f
            AimDirection.DOWN_DIAG -> (baseDirX * 0.707f) to 0.707f
            AimDirection.STRAIGHT -> baseDirX to 0f
        }

        when (player.weapon) {
            WeaponType.RIFLE -> {
                bullets.add(
                    Bullet(
                        id = nextBulletId++,
                        x = spawnX,
                        y = spawnY,
                        vx = dirX * player.weapon.bulletSpeed * 40f,
                        vy = dirY * player.weapon.bulletSpeed * 40f,
                        weaponType = WeaponType.RIFLE,
                        ownerId = player.id,
                        damage = player.weapon.baseDamage,
                        isEnemy = false,
                        radius = 4f
                    )
                )
                audioSynth.playSfx(ChiptuneSynth.Sfx.SHOOT_NORMAL)
            }

            WeaponType.MACHINE_GUN -> {
                // Rapid continuous fire with slight spray
                val jitter = Random.nextFloat() * 0.08f - 0.04f
                bullets.add(
                    Bullet(
                        id = nextBulletId++,
                        x = spawnX,
                        y = spawnY,
                        vx = (dirX + jitter) * player.weapon.bulletSpeed * 40f,
                        vy = (dirY + jitter) * player.weapon.bulletSpeed * 40f,
                        weaponType = WeaponType.MACHINE_GUN,
                        ownerId = player.id,
                        damage = player.weapon.baseDamage,
                        isEnemy = false,
                        radius = 4.5f
                    )
                )
                audioSynth.playSfx(ChiptuneSynth.Sfx.SHOOT_MACHINE)
            }

            WeaponType.SPREAD -> {
                // Iconic 5-way spread shot
                val baseAngle = atan2(dirY, dirX)
                val spreadAngles = listOf(-0.35f, -0.18f, 0f, 0.18f, 0.35f)
                for (offset in spreadAngles) {
                    val angle = baseAngle + offset
                    bullets.add(
                        Bullet(
                            id = nextBulletId++,
                            x = spawnX,
                            y = spawnY,
                            vx = cos(angle) * player.weapon.bulletSpeed * 40f,
                            vy = sin(angle) * player.weapon.bulletSpeed * 40f,
                            weaponType = WeaponType.SPREAD,
                            ownerId = player.id,
                            damage = player.weapon.baseDamage,
                            isEnemy = false,
                            radius = 5.5f
                        )
                    )
                }
                audioSynth.playSfx(ChiptuneSynth.Sfx.SHOOT_SPREAD)
            }

            WeaponType.LASER -> {
                // High-velocity piercing laser beam
                bullets.add(
                    Bullet(
                        id = nextBulletId++,
                        x = spawnX,
                        y = spawnY,
                        vx = dirX * player.weapon.bulletSpeed * 40f,
                        vy = dirY * player.weapon.bulletSpeed * 40f,
                        weaponType = WeaponType.LASER,
                        ownerId = player.id,
                        damage = player.weapon.baseDamage,
                        isEnemy = false,
                        radius = 6f,
                        penetrates = true
                    )
                )
                audioSynth.playSfx(ChiptuneSynth.Sfx.SHOOT_LASER)
            }

            WeaponType.FIREBALL -> {
                // Spiraling fiery projectile
                bullets.add(
                    Bullet(
                        id = nextBulletId++,
                        x = spawnX,
                        y = spawnY,
                        vx = dirX * player.weapon.bulletSpeed * 40f,
                        vy = dirY * player.weapon.bulletSpeed * 40f,
                        weaponType = WeaponType.FIREBALL,
                        ownerId = player.id,
                        damage = player.weapon.baseDamage,
                        isEnemy = false,
                        radius = 7f
                    )
                )
                audioSynth.playSfx(ChiptuneSynth.Sfx.SHOOT_FIRE)
            }

            WeaponType.BARRIER -> {}
        }
    }

    private fun killPlayer(player: Player) {
        if (player.state == PlayerState.DEAD || player.isInvincible) return

        player.state = PlayerState.DEAD
        player.lives--
        player.respawnTimer = 2.0f
        audioSynth.playSfx(ChiptuneSynth.Sfx.PLAYER_DEATH)
        screenShakeAmount = 6f
        ddaManager.recordPlayerDeath()

        // Create player death explosion particles
        for (i in 0..16) {
            particles.add(
                Particle(
                    x = player.x,
                    y = player.y,
                    vx = Random.nextFloat() * 160f - 80f,
                    vy = Random.nextFloat() * 160f - 120f,
                    color = if (player.id == PlayerId.PLAYER_1) 0xFFEF4444 else 0xFF38BDF8,
                    size = Random.nextFloat() * 5f + 3f,
                    life = 0.6f,
                    maxLife = 0.6f
                )
            )
        }
    }

    private fun damagePlayer(player: Player, amount: Float) {
        if (player.isInvincible || player.state == PlayerState.DEAD) return

        player.health -= amount
        audioSynth.playSfx(ChiptuneSynth.Sfx.PLAYER_HIT)
        ddaManager.recordPlayerDamaged()

        if (player.health <= 0f) {
            killPlayer(player)
        } else {
            player.invulnerableTime = 1.0f // Brief grace invulnerability
        }
    }

    private fun updateCamera() {
        if (boss != null) {
            // Camera is locked during boss battle
            val targetX = stageDef.bossArenaX - 80f
            cameraX += (targetX - cameraX) * 0.1f
            return
        }

        // Track active players
        val activePlayers = listOfNotNull(
            if (player1.isAlive) player1 else null,
            if (isCoop && player2.isAlive) player2 else null
        )

        if (activePlayers.isNotEmpty()) {
            val avgX = activePlayers.map { it.x }.average().toFloat()
            // In Contra, screen generally scrolls forward only
            val targetX = (avgX - screenWidth * 0.35f).coerceAtLeast(cameraX)
            cameraX = maxOf(cameraX, targetX.coerceAtMost(stageDef.bossArenaX - 80f))
        }

        // Trigger boss if reaching arena
        if (boss == null && cameraX >= stageDef.bossArenaX - 100f) {
            spawnBoss()
        }
    }

    private fun checkTriggers() {
        val checkX = cameraX + screenWidth + 60f

        // Check enemy spawns
        for ((idx, spawn) in stageDef.enemySpawns.withIndex()) {
            if (!triggeredSpawns.contains(idx) && checkX >= spawn.triggerX) {
                triggeredSpawns.add(idx)
                enemies.add(
                    Enemy(
                        id = nextEnemyId++,
                        type = spawn.type,
                        x = spawn.spawnX,
                        y = spawn.spawnY,
                        facingRight = spawn.facingRight,
                        vx = if (spawn.type == EnemyType.RUNNER) -110f else 0f
                    )
                )
            }
        }

        // Check powerup flying capsules
        for ((idx, cap) in stageDef.capsuleSpawns.withIndex()) {
            if (!triggeredCapsules.contains(idx) && checkX >= cap.triggerX) {
                triggeredCapsules.add(idx)
                powerUps.add(
                    PowerUpItem(
                        id = nextPowerUpId++,
                        type = cap.weaponType,
                        x = cap.startX + cameraX,
                        y = cap.startY,
                        vx = 70f,
                        vy = 0f,
                        isFloatingCapsule = true
                    )
                )
            }
        }
    }

    private fun spawnBoss() {
        audioSynth.playSfx(ChiptuneSynth.Sfx.BOSS_ALARM)
        audioSynth.playTrack(ChiptuneSynth.Track.BOSS)
        showNotification("WARNING! BOSS DETECTED!", 3.0f)

        val b = Boss(
            type = stageDef.bossType,
            x = stageDef.bossArenaX + 380f,
            y = 300f
        )

        when (stageDef.bossType) {
            BossType.GORGON_WALL -> {
                b.subComponents = mutableListOf(
                    BossSubComponent(1, "Lower Cannon", -40f, 60f, 32f, 24f, 180f, 180f),
                    BossSubComponent(2, "Upper Cannon", -40f, -40f, 32f, 24f, 180f, 180f),
                    BossSubComponent(3, "Bunker Sniper", -30f, 10f, 28f, 22f, 140f, 140f),
                    BossSubComponent(4, "Core Shutter", 0f, 0f, 40f, 40f, 300f, 300f)
                )
            }
            BossType.SHADOW_BEAST_GAVA -> {
                b.subComponents = mutableListOf(
                    BossSubComponent(1, "Left Claw", -120f, 40f, 48f, 48f, 260f, 260f),
                    BossSubComponent(2, "Right Claw", 120f, 40f, 48f, 48f, 260f, 260f),
                    BossSubComponent(3, "Laser Eye", 0f, -30f, 36f, 36f, 320f, 320f),
                    BossSubComponent(4, "Plasma Mouth", 0f, 20f, 44f, 34f, 360f, 360f)
                )
            }
            BossType.ALIEN_HEART_MOTHER -> {
                b.subComponents = mutableListOf(
                    BossSubComponent(1, "Upper Tentacle", -50f, -80f, 36f, 50f, 280f, 280f),
                    BossSubComponent(2, "Lower Tentacle", -50f, 80f, 36f, 50f, 280f, 280f),
                    BossSubComponent(3, "Acid Sac", -60f, 0f, 40f, 40f, 340f, 340f),
                    BossSubComponent(4, "Queen Heart Core", 10f, 0f, 64f, 64f, 700f, 700f)
                )
            }
        }
        boss = b
    }

    private fun updateBullets(dt: Float) {
        val it = bullets.iterator()
        while (it.hasNext()) {
            val b = it.next()
            b.life -= dt
            if (b.life <= 0f) {
                it.remove()
                continue
            }

            b.x += b.vx * dt
            b.y += b.vy * dt

            // Remove bullets off screen
            if (b.x < cameraX - 80f || b.x > cameraX + screenWidth + 80f || b.y < -50f || b.y > screenHeight + 50f) {
                it.remove()
                continue
            }

            if (b.isEnemy) {
                // Check collision with players
                val activePlayers = listOfNotNull(
                    if (player1.isAlive) player1 else null,
                    if (isCoop && player2.isAlive) player2 else null
                )

                var hitPlayer = false
                for (p in activePlayers) {
                    val dx = b.x - p.x
                    val dy = b.y - p.y
                    val hitRadius = if (p.state == PlayerState.PRONE) 14f else 22f

                    if (sqrt(dx * dx + dy * dy) < hitRadius) {
                        damagePlayer(p, b.damage)
                        hitPlayer = true
                        break
                    }
                }
                if (hitPlayer) {
                    createSparks(b.x, b.y, 0xFFEF4444)
                    it.remove()
                    continue
                }
            } else {
                // Player bullet hitting enemies
                var hitSomething = false

                // 1. Check hitting regular enemies
                for (enemy in enemies) {
                    val edx = b.x - enemy.x
                    val edy = b.y - enemy.y
                    if (abs(edx) < enemy.type.width && abs(edy) < enemy.type.height) {
                        enemy.health -= b.damage
                        hitSomething = true
                        createSparks(b.x, b.y, 0xFFF59E0B)

                        val player = if (b.ownerId == PlayerId.PLAYER_2) player2 else player1
                        player.shotsHit++

                        if (enemy.health <= 0f) {
                            killEnemy(enemy, player)
                        }
                        break
                    }
                }

                // 2. Check hitting floating powerup capsule
                if (!hitSomething) {
                    for (pUp in powerUps) {
                        if (pUp.isFloatingCapsule) {
                            val cdx = b.x - pUp.x
                            val cdy = b.y - pUp.y
                            if (sqrt(cdx * cdx + cdy * cdy) < 20f) {
                                // Break capsule open! Drops weapon to ground
                                pUp.isFloatingCapsule = false
                                pUp.vy = -60f
                                pUp.vx = 0f
                                hitSomething = true
                                createSparks(pUp.x, pUp.y, 0xFF38BDF8)
                                audioSynth.playSfx(ChiptuneSynth.Sfx.EXPLOSION)
                                break
                            }
                        }
                    }
                }

                // 3. Check hitting Boss
                val curBoss = boss
                if (!hitSomething && curBoss != null && !curBoss.isDefeated) {
                    for (comp in curBoss.subComponents) {
                        if (comp.isDestroyed) continue
                        val cx = curBoss.x + comp.relX
                        val cy = curBoss.y + comp.relY
                        if (abs(b.x - cx) < comp.width && abs(b.y - cy) < comp.height) {
                            comp.health -= b.damage
                            curBoss.health = maxOf(0f, curBoss.health - b.damage)
                            hitSomething = true
                            createSparks(b.x, b.y, 0xFFEF4444)

                            val player = if (b.ownerId == PlayerId.PLAYER_2) player2 else player1
                            player.shotsHit++
                            player.score += (20 * ddaManager.currentTier.scoreMultiplier).toInt()

                            if (comp.health <= 0f) {
                                comp.isDestroyed = true
                                createExplosion(cx, cy, big = true)
                                audioSynth.playSfx(ChiptuneSynth.Sfx.EXPLOSION_BIG)
                            }

                            if (curBoss.health <= 0f) {
                                defeatBoss(curBoss)
                            }
                            break
                        }
                    }
                }

                if (hitSomething && !b.penetrates) {
                    it.remove()
                }
            }
        }
    }

    private fun killEnemy(enemy: Enemy, killer: Player) {
        killer.kills++
        val scoreBonus = (enemy.type.scoreValue * ddaManager.currentTier.scoreMultiplier).toInt()
        killer.score += scoreBonus
        ddaManager.recordEnemyKilled()

        createExplosion(enemy.x, enemy.y)
        audioSynth.playSfx(ChiptuneSynth.Sfx.EXPLOSION)

        // Occasional extra weapon drop based on DDA
        if (Random.nextFloat() < (0.12f * ddaManager.currentTier.bonusDropRate)) {
            val randomWeapon = listOf(WeaponType.SPREAD, WeaponType.MACHINE_GUN, WeaponType.LASER, WeaponType.FIREBALL, WeaponType.BARRIER).random()
            powerUps.add(
                PowerUpItem(
                    id = nextPowerUpId++,
                    type = randomWeapon,
                    x = enemy.x,
                    y = enemy.y,
                    vy = -80f,
                    isFloatingCapsule = false
                )
            )
        }
        enemies.remove(enemy)
    }

    private fun defeatBoss(b: Boss) {
        b.isDefeated = true
        b.deathAnimTimer = 3.5f
        audioSynth.playSfx(ChiptuneSynth.Sfx.EXPLOSION_BIG)
        audioSynth.playTrack(ChiptuneSynth.Track.VICTORY)
        screenShakeAmount = 14f

        val bonus = (10000 * ddaManager.currentTier.scoreMultiplier).toInt()
        player1.score += bonus
        if (isCoop) player2.score += bonus

        showNotification("BOSS DESTROYED! STAGE CLEARED!", 3.5f)
    }

    private fun updateBoss(dt: Float) {
        val b = boss ?: return
        b.animFrame += dt

        if (b.isDefeated) {
            b.deathAnimTimer -= dt
            // Continuous destruction explosions
            if (Random.nextFloat() < 0.4f) {
                val rx = b.x + Random.nextFloat() * 120f - 60f
                val ry = b.y + Random.nextFloat() * 120f - 60f
                createExplosion(rx, ry, big = true)
                audioSynth.playSfx(ChiptuneSynth.Sfx.EXPLOSION)
            }
            if (b.deathAnimTimer <= 0f) {
                isStageCleared = true
            }
            return
        }

        b.attackTimer += dt * ddaManager.currentTier.enemyFireRateMultiplier

        // Boss attack routines
        val targetPlayer = if (isCoop && Random.nextBoolean() && player2.isAlive) player2 else player1

        when (b.type) {
            BossType.GORGON_WALL -> {
                // Lower cannon fires arcing bomb every 2.4s
                if (b.attackTimer > 2.4f) {
                    val comp = b.subComponents.find { it.name == "Lower Cannon" && !it.isDestroyed }
                    if (comp != null) {
                        val cx = b.x + comp.relX
                        val cy = b.y + comp.relY
                        val dx = targetPlayer.x - cx
                        val dy = targetPlayer.y - cy
                        val dist = maxOf(1f, sqrt(dx * dx + dy * dy))
                        bullets.add(
                            Bullet(
                                id = nextBulletId++,
                                x = cx,
                                y = cy,
                                vx = (dx / dist) * 160f * ddaManager.currentTier.bulletSpeedMultiplier,
                                vy = (dy / dist) * 160f * ddaManager.currentTier.bulletSpeedMultiplier,
                                weaponType = WeaponType.RIFLE,
                                ownerId = null,
                                damage = 25f,
                                isEnemy = true,
                                radius = 6f
                            )
                        )
                    }
                    b.attackTimer = 0f
                }
            }

            BossType.SHADOW_BEAST_GAVA -> {
                // Titan sways up and down, fires sweeping laser
                b.y = 280f + sin(b.animFrame * 1.5f) * 40f
                if (b.attackTimer > 2.0f) {
                    val cx = b.x - 30f
                    val cy = b.y
                    for (angle in listOf(-0.2f, 0f, 0.2f)) {
                        bullets.add(
                            Bullet(
                                id = nextBulletId++,
                                x = cx,
                                y = cy,
                                vx = -200f * cos(angle) * ddaManager.currentTier.bulletSpeedMultiplier,
                                vy = 200f * sin(angle) * ddaManager.currentTier.bulletSpeedMultiplier,
                                weaponType = WeaponType.LASER,
                                ownerId = null,
                                damage = 30f,
                                isEnemy = true,
                                radius = 6.5f
                            )
                        )
                    }
                    b.attackTimer = 0f
                }
            }

            BossType.ALIEN_HEART_MOTHER -> {
                // Pulsing heart spits acid and spawns facehuggers
                if (b.attackTimer > 2.2f) {
                    val cx = b.x - 40f
                    val cy = b.y - 10f
                    bullets.add(
                        Bullet(
                            id = nextBulletId++,
                            x = cx,
                            y = cy,
                            vx = -180f * ddaManager.currentTier.bulletSpeedMultiplier,
                            vy = (Random.nextFloat() * 100f - 50f),
                            weaponType = WeaponType.FIREBALL,
                            ownerId = null,
                            damage = 35f,
                            isEnemy = true,
                            radius = 7f
                        )
                    )
                    // Spawn a facehugger crawler
                    enemies.add(
                        Enemy(
                            id = nextEnemyId++,
                            type = EnemyType.FACEHUGGER,
                            x = cx,
                            y = cy,
                            vx = -140f
                        )
                    )
                    b.attackTimer = 0f
                }
            }
        }
    }

    private fun updateEnemies(dt: Float) {
        val it = enemies.iterator()
        while (it.hasNext()) {
            val e = it.next()
            e.animFrame += dt * 8f
            e.stateTime += dt

            // Update position
            e.x += e.vx * dt
            e.y += e.vy * dt

            // Offscreen despawn
            if (e.x < cameraX - 120f || e.x > cameraX + screenWidth + 180f) {
                it.remove()
                continue
            }

            // Enemy AI by type
            when (e.type) {
                EnemyType.RUNNER, EnemyType.FACEHUGGER -> {
                    // Runner jumps over obstacles or keeps charging
                    e.vy += 800f * dt
                    var onGround = false
                    for (plat in stageDef.platforms) {
                        if (e.x >= plat.x && e.x <= plat.x + plat.width) {
                            if (e.y + 16f >= plat.y && e.y + 16f <= plat.y + 20f) {
                                e.y = plat.y - 16f
                                e.vy = 0f
                                onGround = true
                                break
                            }
                        }
                    }
                    if (!onGround && e.y > 450f) {
                        it.remove()
                        continue
                    }
                }

                EnemyType.SNIPER, EnemyType.PALM_SNIPER, EnemyType.WALL_TURRET, EnemyType.JETPACK_SOLDIER, EnemyType.FLYING_ALIEN -> {
                    // Periodic shooting at closest player
                    e.shootCooldown -= dt * ddaManager.currentTier.enemyFireRateMultiplier
                    if (e.shootCooldown <= 0f) {
                        e.shootCooldown = 2.4f + Random.nextFloat() * 1.2f
                        val target = if (isCoop && Random.nextBoolean() && player2.isAlive) player2 else player1
                        val dx = target.x - e.x
                        val dy = target.y - e.y
                        val dist = maxOf(1f, sqrt(dx * dx + dy * dy))

                        bullets.add(
                            Bullet(
                                id = nextBulletId++,
                                x = e.x,
                                y = e.y,
                                vx = (dx / dist) * 150f * ddaManager.currentTier.bulletSpeedMultiplier,
                                vy = (dy / dist) * 150f * ddaManager.currentTier.bulletSpeedMultiplier,
                                weaponType = WeaponType.RIFLE,
                                ownerId = null,
                                damage = 20f,
                                isEnemy = true,
                                radius = 4f
                            )
                        )
                    }
                }

                EnemyType.CEILING_POD -> {
                    // Drops down when player is nearby
                    val target = if (isCoop && Random.nextBoolean() && player2.isAlive) player2 else player1
                    if (abs(e.x - target.x) < 80f && e.vy == 0f) {
                        e.vy = 180f
                    }
                }
            }

            // Collision check: Enemy bumping into players
            val activePlayers = listOfNotNull(
                if (player1.isAlive) player1 else null,
                if (isCoop && player2.isAlive) player2 else null
            )
            for (p in activePlayers) {
                if (abs(e.x - p.x) < 18f && abs(e.y - p.y) < 22f) {
                    damagePlayer(p, 25f)
                }
            }
        }
    }

    private fun updatePowerUps(dt: Float) {
        val it = powerUps.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
                continue
            }

            if (p.isFloatingCapsule) {
                // Wave flight path
                p.x += p.vx * dt
                p.y += sin(p.x * 0.04f) * 1.5f
            } else {
                // Falling powerup with gravity
                p.vy += 450f * dt
                p.y += p.vy * dt

                // Land on platforms
                for (plat in stageDef.platforms) {
                    if (p.x >= plat.x && p.x <= plat.x + plat.width) {
                        if (p.y + 12f >= plat.y && p.y + 12f <= plat.y + 20f) {
                            p.y = plat.y - 12f
                            p.vy = 0f
                            break
                        }
                    }
                }
            }

            // Pickup collision with players
            val activePlayers = listOfNotNull(
                if (player1.isAlive) player1 else null,
                if (isCoop && player2.isAlive) player2 else null
            )

            for (player in activePlayers) {
                if (abs(p.x - player.x) < 26f && abs(p.y - player.y) < 26f && !p.isFloatingCapsule) {
                    // Equip weapon / apply barrier
                    if (p.type == WeaponType.BARRIER) {
                        player.barrierTime = 15f
                        showNotification("${if (player.id == PlayerId.PLAYER_1) "P1" else "P2"}: BARRIER SHIELD ACTIVATED!")
                    } else {
                        player.weapon = p.type
                        showNotification("${if (player.id == PlayerId.PLAYER_1) "P1" else "P2"}: ${p.type.displayName} ACQUIRED!")
                    }
                    player.score += (1000 * ddaManager.currentTier.scoreMultiplier).toInt()
                    audioSynth.playSfx(ChiptuneSynth.Sfx.POWERUP_PICKUP)
                    createSparks(p.x, p.y, 0xFF38BDF8)
                    it.remove()
                    break
                }
            }
        }
    }

    private fun updateParticles(dt: Float) {
        val it = particles.iterator()
        while (it.hasNext()) {
            val p = it.next()
            p.life -= dt
            if (p.life <= 0f) {
                it.remove()
                continue
            }
            p.x += p.vx * dt
            p.y += p.vy * dt
            p.alpha = (p.life / p.maxLife).coerceIn(0f, 1f)
        }
    }

    private fun updateStageClear(dt: Float) {
        updateParticles(dt)
        // Check progression to next stage
        if (currentStageIndex < 2) {
            loadStage(currentStageIndex + 1)
        } else {
            isCampaignVictory = true
            showNotification("CONGRATULATIONS! RED FALCON DEFEATED!", 5f)
        }
    }

    private fun createSparks(x: Float, y: Float, color: Long) {
        for (i in 0..6) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = Random.nextFloat() * 120f - 60f,
                    vy = Random.nextFloat() * 120f - 60f,
                    color = color,
                    size = Random.nextFloat() * 4f + 2f,
                    life = 0.25f,
                    maxLife = 0.25f
                )
            )
        }
    }

    private fun createExplosion(x: Float, y: Float, big: Boolean = false) {
        screenShakeAmount = if (big) 12f else 5f
        val count = if (big) 24 else 12
        val colors = listOf(0xFFEF4444, 0xFFF59E0B, 0xFFFBBF24, 0xFFFFFFFF)
        for (i in 0 until count) {
            particles.add(
                Particle(
                    x = x,
                    y = y,
                    vx = Random.nextFloat() * (if (big) 240f else 140f) - (if (big) 120f else 70f),
                    vy = Random.nextFloat() * (if (big) 240f else 140f) - (if (big) 120f else 70f),
                    color = colors.random(),
                    size = Random.nextFloat() * (if (big) 8f else 5f) + 3f,
                    life = if (big) 0.7f else 0.45f,
                    maxLife = if (big) 0.7f else 0.45f
                )
            )
        }
    }
}
