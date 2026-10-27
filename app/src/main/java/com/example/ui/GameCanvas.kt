package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.example.game.*
import kotlin.math.*
import kotlin.random.Random

@Composable
fun GameCanvas(
    engine: GameEngine,
    showCrtScanlines: Boolean = true,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height

        // Compute scaling from game coordinates (800x480 virtual resolution) to actual canvas
        val scaleX = canvasWidth / 800f
        val scaleY = canvasHeight / 480f
        val virtualScale = minOf(scaleX, scaleY)

        val offsetX = (canvasWidth - (800f * virtualScale)) / 2f
        val offsetY = (canvasHeight - (480f * virtualScale)) / 2f

        // Apply screen shake
        val shakeX = if (engine.screenShakeAmount > 0f) Random.nextFloat() * engine.screenShakeAmount * 2f - engine.screenShakeAmount else 0f
        val shakeY = if (engine.screenShakeAmount > 0f) Random.nextFloat() * engine.screenShakeAmount * 2f - engine.screenShakeAmount else 0f

        engine.screenWidth = 800f
        engine.screenHeight = 480f

        scale(scaleX = virtualScale, scaleY = virtualScale, pivot = Offset.Zero) {
            // Draw Parallax Backgrounds
            drawParallaxBackground(engine, engine.currentStageIndex, shakeX, shakeY)

            // Draw Platforms & Terrain
            drawPlatforms(engine, shakeX, shakeY)

            // Draw Boss
            engine.boss?.let { drawBoss(it, engine.cameraX - shakeX, shakeY) }

            // Draw Enemies
            drawEnemies(engine, shakeX, shakeY)

            // Draw Powerups
            drawPowerUps(engine, shakeX, shakeY)

            // Draw Bullets
            drawBullets(engine, shakeX, shakeY)

            // Draw Players
            drawPlayer(engine.player1, engine.cameraX - shakeX, shakeY, isPlayer1 = true)
            if (engine.isCoop) {
                drawPlayer(engine.player2, engine.cameraX - shakeX, shakeY, isPlayer1 = false)
            }

            // Draw Particles & Explosions
            drawParticles(engine, shakeX, shakeY)
        }

        // CRT Scanline Overlay
        if (showCrtScanlines) {
            drawCrtScanlines(canvasWidth, canvasHeight)
        }
    }
}

private fun DrawScope.drawParallaxBackground(
    engine: GameEngine,
    stageIndex: Int,
    shakeX: Float,
    shakeY: Float
) {
    val camX = engine.cameraX

    when (stageIndex) {
        0 -> { // Jungle
            // Sky gradient (Deep retro dusk to navy)
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E293B), Color(0xFF14532D))
                ),
                size = Size(800f, 480f)
            )

            // Distant Mountains (0.2x speed)
            val mountainOffset = (camX * 0.15f) % 400f
            for (i in -1..3) {
                val bx = i * 400f - mountainOffset + shakeX
                val path = Path().apply {
                    moveTo(bx, 480f)
                    lineTo(bx + 120f, 220f + shakeY)
                    lineTo(bx + 260f, 180f + shakeY)
                    lineTo(bx + 400f, 480f)
                    close()
                }
                drawPath(path, Color(0xFF064E3B).copy(alpha = 0.6f))
            }

            // Midground Jungle Trees & Canopy (0.45x speed)
            val treeOffset = (camX * 0.45f) % 300f
            for (i in -1..4) {
                val tx = i * 300f - treeOffset + shakeX
                // Tree trunk
                drawRect(Color(0xFF451A03), Offset(tx + 40f, 260f + shakeY), Size(24f, 180f))
                // Palm foliage
                drawCircle(Color(0xFF15803D), radius = 55f, center = Offset(tx + 52f, 250f + shakeY))
                drawCircle(Color(0xFF16A34A), radius = 42f, center = Offset(tx + 52f, 235f + shakeY))
            }
        }

        1 -> { // Waterfall / Cyber Fortress
            // Sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF030712), Color(0xFF1E1B4B), Color(0xFF312E81))
                ),
                size = Size(800f, 480f)
            )

            // Waterfall Cascade
            val wfOffset = (camX * 0.3f) % 350f
            for (i in -1..3) {
                val wx = i * 350f - wfOffset + shakeX
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color(0xFF38BDF8).copy(alpha = 0.4f), Color(0xFF0284C7).copy(alpha = 0.7f))
                    ),
                    topLeft = Offset(wx + 80f, 0f),
                    size = Size(90f, 480f)
                )
                // Foam ripples
                for (r in 0..12) {
                    val ry = (r * 38f + (System.currentTimeMillis() % 1000) * 0.2f) % 480f
                    drawRect(Color.White.copy(alpha = 0.5f), Offset(wx + 85f, ry), Size(80f, 4f))
                }
            }
        }

        2 -> { // Alien Core
            // Biomechanical alien flesh sky
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF1F0816), Color(0xFF4A044E), Color(0xFF701A75))
                ),
                size = Size(800f, 480f)
            )
            // Organic pulsing alien ribs in background
            val ribOffset = (camX * 0.35f) % 250f
            for (i in -1..4) {
                val rx = i * 250f - ribOffset + shakeX
                drawRoundRect(
                    color = Color(0xFF831843).copy(alpha = 0.6f),
                    topLeft = Offset(rx, 140f + shakeY),
                    size = Size(36f, 300f),
                    cornerRadius = CornerRadius(18f, 18f)
                )
            }
        }
    }
}

private fun DrawScope.drawPlatforms(engine: GameEngine, shakeX: Float, shakeY: Float) {
    val camX = engine.cameraX - shakeX

    for (p in engine.stageDef.platforms) {
        val screenX = p.x - camX
        if (screenX + p.width < -50f || screenX > 850f) continue

        if (p.isWater) {
            // River surface with animated water waves
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF0284C7), Color(0xFF0369A1), Color(0xFF0C4A6E))
                ),
                topLeft = Offset(screenX, p.y + shakeY),
                size = Size(p.width, p.height)
            )
            // Animated wave crests
            val wavePhase = (System.currentTimeMillis() % 1200) / 1200f * (2 * Math.PI)
            for (wx in 0 until (p.width / 24).toInt()) {
                val curX = screenX + wx * 24f
                val curY = p.y + sin(wavePhase + wx * 0.8).toFloat() * 3f
                drawRect(Color(0xFF7DD3FC), Offset(curX, curY + shakeY), Size(18f, 3f))
            }
        } else if (p.isOneWay) {
            // Suspended wooden platform / metallic girder
            drawRoundRect(
                color = when (engine.currentStageIndex) {
                    0 -> Color(0xFF78350F) // Wooden plank
                    1 -> Color(0xFF475569) // Metallic girder
                    else -> Color(0xFF831843) // Alien bone slab
                },
                topLeft = Offset(screenX, p.y + shakeY),
                size = Size(p.width, p.height),
                cornerRadius = CornerRadius(4f, 4f)
            )
            // Accent border
            drawRect(
                color = when (engine.currentStageIndex) {
                    0 -> Color(0xFFD97706)
                    1 -> Color(0xFF06B6D4)
                    else -> Color(0xFFF43F5E)
                },
                topLeft = Offset(screenX, p.y + shakeY),
                size = Size(p.width, 3f)
            )
        } else {
            // Solid ground terrain
            drawRect(
                color = when (engine.currentStageIndex) {
                    0 -> Color(0xFF14532D) // Grass top
                    1 -> Color(0xFF1E293B) // Fortress concrete
                    else -> Color(0xFF581C87) // Organic alien soil
                },
                topLeft = Offset(screenX, p.y + shakeY),
                size = Size(p.width, 16f)
            )
            drawRect(
                color = when (engine.currentStageIndex) {
                    0 -> Color(0xFF3E2723) // Dirt layer
                    1 -> Color(0xFF0F172A) // Steel base
                    else -> Color(0xFF3B0764) // Deep alien tissue
                },
                topLeft = Offset(screenX, p.y + 16f + shakeY),
                size = Size(p.width, p.height - 16f)
            )
        }
    }
}

private fun DrawScope.drawPlayer(
    player: Player,
    camX: Float,
    shakeY: Float,
    isPlayer1: Boolean
) {
    if (!player.isAlive && player.state == PlayerState.DEAD) return

    val screenX = player.x - camX
    val screenY = player.y + shakeY

    // Flashing during respawn invulnerability
    if (player.invulnerableTime > 0f) {
        val flash = ((player.invulnerableTime * 15).toInt() % 2) == 0
        if (flash) return
    }

    // Barrier shield effect (orbiting glowing cyan orbs)
    if (player.barrierTime > 0f) {
        val angle = (System.currentTimeMillis() % 1000) / 1000f * (2 * Math.PI)
        for (i in 0..2) {
            val a = angle + (i * 2 * Math.PI / 3)
            val orbX = screenX + cos(a).toFloat() * 32f
            val orbY = screenY + sin(a).toFloat() * 32f
            drawCircle(Color(0xFF38BDF8), radius = 7f, center = Offset(orbX, orbY))
            drawCircle(Color.White, radius = 4f, center = Offset(orbX, orbY))
        }
    }

    // Color definitions
    val headbandColor = if (isPlayer1) Color(0xFFDC2626) else Color(0xFF2563EB) // P1 Red / P2 Blue
    val pantsColor = if (isPlayer1) Color(0xFF1D4ED8) else Color(0xFFB91C1C)    // P1 Blue / P2 Red
    val skinColor = Color(0xFFFED7AA)
    val hairColor = if (isPlayer1) Color(0xFFFBBF24) else Color(0xFF451A03)    // Blonde / Brunette
    val gunMetalColor = Color(0xFF94A3B8)

    if (player.state == PlayerState.JUMPING) {
        // Classic Contra Somersault Spin!
        rotate(player.somersaultAngle, pivot = Offset(screenX, screenY)) {
            // Rounded curled soldier ball
            drawCircle(skinColor, radius = 14f, center = Offset(screenX, screenY))
            drawCircle(headbandColor, radius = 14f, center = Offset(screenX, screenY), style = Stroke(width = 6f))
            drawCircle(pantsColor, radius = 9f, center = Offset(screenX, screenY))
        }
        return
    }

    val dirSign = if (player.facingRight) 1f else -1f

    if (player.state == PlayerState.WATER) {
        // Swimming commando
        drawRect(hairColor, Offset(screenX - 8f, screenY - 14f), Size(16f, 8f))
        drawRect(headbandColor, Offset(screenX - 9f, screenY - 6f), Size(18f, 5f))
        drawRect(skinColor, Offset(screenX - 8f, screenY - 1f), Size(16f, 10f))
        // Gun protruding out of water
        drawRect(gunMetalColor, Offset(screenX + (dirSign * 8f), screenY - 4f), Size(dirSign * 18f, 5f))
        // Water splash ripples around swimmer
        drawCircle(Color(0xFF38BDF8), radius = 18f, center = Offset(screenX, screenY + 12f), style = Stroke(width = 2f))
        return
    }

    if (player.state == PlayerState.PRONE) {
        // Lying down flat
        drawRect(pantsColor, Offset(screenX - (dirSign * 18f), screenY + 10f), Size(dirSign * 18f, 8f))
        drawRect(Color.White, Offset(screenX - (dirSign * 4f), screenY + 8f), Size(dirSign * 14f, 9f))
        drawRect(skinColor, Offset(screenX + (dirSign * 10f), screenY + 6f), Size(dirSign * 10f, 10f))
        drawRect(headbandColor, Offset(screenX + (dirSign * 8f), screenY + 5f), Size(dirSign * 12f, 4f))
        // Gun flat on ground
        drawRect(gunMetalColor, Offset(screenX + (dirSign * 16f), screenY + 10f), Size(dirSign * 22f, 4f))
        return
    }

    // Standing / Running soldier
    // 1. Legs
    val legStride = sin(player.animFrame).toFloat() * (if (player.state == PlayerState.RUNNING) 10f else 0f)
    drawRect(pantsColor, Offset(screenX - 6f - (dirSign * legStride), screenY + 8f), Size(6f, 16f))
    drawRect(pantsColor, Offset(screenX + (dirSign * legStride), screenY + 8f), Size(6f, 16f))
    // Boots
    drawRect(Color(0xFF1E293B), Offset(screenX - 7f - (dirSign * legStride), screenY + 22f), Size(8f, 5f))
    drawRect(Color(0xFF1E293B), Offset(screenX - 1f + (dirSign * legStride), screenY + 22f), Size(8f, 5f))

    // 2. Torso (Muscle tank top)
    drawRect(Color.White, Offset(screenX - 7f, screenY - 6f), Size(14f, 14f))

    // 3. Head & Red/Blue Headband
    drawRect(skinColor, Offset(screenX - 6f, screenY - 20f), Size(12f, 14f))
    drawRect(hairColor, Offset(screenX - 7f, screenY - 24f), Size(14f, 7f))
    drawRect(headbandColor, Offset(screenX - 8f, screenY - 18f), Size(16f, 4f))
    // Headband ribbon flowing behind
    drawRect(headbandColor, Offset(screenX - (dirSign * 14f), screenY - 17f), Size(dirSign * 8f, 3f))

    // 4. Arms & Gun (Positioned based on aiming direction)
    val armPivotX = screenX + (dirSign * 2f)
    val armPivotY = screenY - 2f

    when (player.aimDirection) {
        AimDirection.UP -> {
            drawRect(gunMetalColor, Offset(armPivotX - 2f, armPivotY - 26f), Size(5f, 26f))
            drawRect(skinColor, Offset(armPivotX - 4f, armPivotY - 10f), Size(8f, 10f))
        }
        AimDirection.UP_DIAG -> {
            rotate(if (player.facingRight) -45f else 45f, pivot = Offset(armPivotX, armPivotY)) {
                drawRect(gunMetalColor, Offset(armPivotX, armPivotY - 2f), Size(dirSign * 24f, 5f))
                drawRect(skinColor, Offset(armPivotX, armPivotY - 4f), Size(dirSign * 10f, 8f))
            }
        }
        AimDirection.DOWN_DIAG -> {
            rotate(if (player.facingRight) 45f else -45f, pivot = Offset(armPivotX, armPivotY)) {
                drawRect(gunMetalColor, Offset(armPivotX, armPivotY - 2f), Size(dirSign * 24f, 5f))
                drawRect(skinColor, Offset(armPivotX, armPivotY - 4f), Size(dirSign * 10f, 8f))
            }
        }
        else -> { // Straight
            drawRect(gunMetalColor, Offset(armPivotX, armPivotY - 2f), Size(dirSign * 22f, 5f))
            drawRect(skinColor, Offset(armPivotX, armPivotY - 4f), Size(dirSign * 10f, 8f))
        }
    }
}

private fun DrawScope.drawEnemies(engine: GameEngine, shakeX: Float, shakeY: Float) {
    val camX = engine.cameraX - shakeX

    for (e in engine.enemies) {
        val sx = e.x - camX
        val sy = e.y + shakeY
        if (sx < -60f || sx > 860f) continue

        when (e.type) {
            EnemyType.RUNNER -> {
                val legCycle = sin(e.animFrame).toFloat() * 8f
                // Red running trooper
                drawRect(Color(0xFFEF4444), Offset(sx - 6f, sy - 8f), Size(12f, 14f)) // Body
                drawRect(Color(0xFF991B1B), Offset(sx - 5f - legCycle, sy + 6f), Size(5f, 14f)) // Legs
                drawRect(Color(0xFF991B1B), Offset(sx + 1f + legCycle, sy + 6f), Size(5f, 14f))
                drawRect(Color(0xFF64748B), Offset(sx - 5f, sy - 18f), Size(10f, 10f)) // Helmet
                drawRect(Color(0xFF1E293B), Offset(sx - 16f, sy - 4f), Size(14f, 4f)) // Rifle
            }

            EnemyType.SNIPER, EnemyType.PALM_SNIPER -> {
                // Green camo sniper in prone/crouch
                drawRect(Color(0xFF15803D), Offset(sx - 10f, sy - 4f), Size(20f, 12f))
                drawRect(Color(0xFF166534), Offset(sx - 6f, sy - 14f), Size(12f, 10f))
                drawRect(Color(0xFF0F172A), Offset(sx - 20f, sy - 2f), Size(16f, 4f)) // Long barrel
            }

            EnemyType.WALL_TURRET -> {
                // Armored dome turret with rotating cannon
                drawCircle(Color(0xFF475569), radius = 16f, center = Offset(sx, sy))
                drawCircle(Color(0xFFEF4444), radius = 6f, center = Offset(sx, sy))
                drawRect(Color(0xFF1E293B), Offset(sx - 24f, sy - 4f), Size(18f, 8f)) // Cannon
            }

            EnemyType.JETPACK_SOLDIER -> {
                // Hovering soldier with thrusters
                drawRect(Color(0xFF0284C7), Offset(sx - 7f, sy - 12f), Size(14f, 18f))
                drawRect(Color(0xFF38BDF8), Offset(sx + 7f, sy - 14f), Size(8f, 14f)) // Jetpack
                // Flame exhaust
                val flame = Random.nextFloat() * 8f + 6f
                drawRect(Color(0xFFF97316), Offset(sx + 9f, sy), Size(4f, flame))
                drawRect(Color(0xFF1E293B), Offset(sx - 16f, sy - 6f), Size(14f, 4f))
            }

            EnemyType.CEILING_POD -> {
                // Mechanical ceiling dropper
                drawRect(Color(0xFF64748B), Offset(sx - 12f, sy - 12f), Size(24f, 24f))
                drawCircle(Color(0xFFEF4444), radius = 5f, center = Offset(sx, sy))
            }

            EnemyType.FLYING_ALIEN -> {
                // Organic winged alien bug
                drawCircle(Color(0xFF86198F), radius = 12f, center = Offset(sx, sy))
                // Flapping wings
                val wingY = sin(e.animFrame * 2f).toFloat() * 8f
                drawOval(Color(0xFFD946EF).copy(alpha = 0.7f), Offset(sx - 18f, sy - 14f + wingY), Size(14f, 8f))
                drawOval(Color(0xFFD946EF).copy(alpha = 0.7f), Offset(sx + 4f, sy - 14f - wingY), Size(14f, 8f))
            }

            EnemyType.FACEHUGGER -> {
                // Scurrying alien facehugger
                drawOval(Color(0xFFA855F7), Offset(sx - 9f, sy - 6f), Size(18f, 12f))
                val legSpread = sin(e.animFrame).toFloat() * 4f
                drawLine(Color(0xFF9333EA), Offset(sx - 8f, sy), Offset(sx - 14f, sy + 6f + legSpread), strokeWidth = 2f)
                drawLine(Color(0xFF9333EA), Offset(sx + 8f, sy), Offset(sx + 14f, sy + 6f - legSpread), strokeWidth = 2f)
            }
        }
    }
}

private fun DrawScope.drawBoss(boss: Boss, camX: Float, shakeY: Float) {
    val bx = boss.x - camX
    val by = boss.y + shakeY

    when (boss.type) {
        BossType.GORGON_WALL -> {
            // Massive reinforced fortress wall gate
            drawRect(
                brush = Brush.horizontalGradient(
                    colors = listOf(Color(0xFF334155), Color(0xFF1E293B), Color(0xFF0F172A))
                ),
                topLeft = Offset(bx - 30f, 100f),
                size = Size(140f, 320f)
            )
            // Heavy steel rivets
            for (ry in 120..400 step 40) {
                drawCircle(Color(0xFF94A3B8), radius = 4f, center = Offset(bx - 20f, ry.toFloat()))
                drawCircle(Color(0xFF94A3B8), radius = 4f, center = Offset(bx + 40f, ry.toFloat()))
            }

            // Subcomponents
            for (c in boss.subComponents) {
                val cx = bx + c.relX
                val cy = by + c.relY
                if (c.isDestroyed) {
                    // Burned wreckage
                    drawCircle(Color(0xFF18181B), radius = 14f, center = Offset(cx, cy))
                    continue
                }

                when (c.name) {
                    "Lower Cannon", "Upper Cannon" -> {
                        drawCircle(Color(0xFF475569), radius = 16f, center = Offset(cx, cy))
                        drawRect(Color(0xFF0F172A), Offset(cx - 26f, cy - 4f), Size(22f, 8f)) // Cannon barrel
                        drawCircle(Color(0xFFEF4444), radius = 5f, center = Offset(cx, cy))
                    }
                    "Bunker Sniper" -> {
                        drawRect(Color(0xFF0F172A), Offset(cx - 14f, cy - 8f), Size(28f, 16f))
                        drawRect(Color(0xFFEF4444), Offset(cx - 6f, cy - 4f), Size(12f, 8f))
                    }
                    "Core Shutter" -> {
                        // Glowing red power core with pulsing light
                        val pulse = (sin(boss.animFrame * 4f) * 0.3f + 0.7f).coerceIn(0.4f, 1f)
                        drawCircle(Color(0xFFEF4444).copy(alpha = pulse), radius = 22f, center = Offset(cx, cy))
                        drawCircle(Color.White, radius = 10f, center = Offset(cx, cy))
                        // Armored sliding shutter plates
                        val shutterOpen = (sin(boss.animFrame * 1.2f) > 0.3)
                        if (!shutterOpen) {
                            drawRect(Color(0xFF475569), Offset(cx - 18f, cy - 18f), Size(18f, 36f))
                            drawRect(Color(0xFF475569), Offset(cx, cy - 18f), Size(18f, 36f))
                        }
                    }
                }
            }
        }

        BossType.SHADOW_BEAST_GAVA -> {
            // Giant biomechanical horned demon skull
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF475569), Color(0xFF0F172A))
                ),
                topLeft = Offset(bx - 50f, by - 60f),
                size = Size(100f, 120f)
            )
            // Glowing crimson cyclops eye
            val eyeGlow = (sin(boss.animFrame * 5f) * 0.3f + 0.7f).coerceIn(0.5f, 1f)
            drawCircle(Color(0xFFEF4444).copy(alpha = eyeGlow), radius = 18f, center = Offset(bx - 10f, by - 16f))
            drawCircle(Color.White, radius = 7f, center = Offset(bx - 10f, by - 16f))

            // Subcomponents (Extendable claws)
            for (c in boss.subComponents) {
                val cx = bx + c.relX
                val cy = by + c.relY
                if (c.isDestroyed) continue
                if (c.name.contains("Claw")) {
                    drawOval(Color(0xFF64748B), Offset(cx - 18f, cy - 18f), Size(36f, 36f))
                    // Pincer blades
                    drawRect(Color(0xFF94A3B8), Offset(cx - 24f, cy - 12f), Size(12f, 6f))
                    drawRect(Color(0xFF94A3B8), Offset(cx - 24f, cy + 6f), Size(12f, 6f))
                }
            }
        }

        BossType.ALIEN_HEART_MOTHER -> {
            // Massive beating Xenomorph Alien Mother Heart
            val beat = (sin(boss.animFrame * 6f) * 8f)
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFFEC4899), Color(0xFF831843), Color(0xFF4C0519))
                ),
                topLeft = Offset(bx - 45f - beat / 2f, by - 55f - beat / 2f),
                size = Size(90f + beat, 110f + beat)
            )
            // Writhing pulsating arteries
            for (i in 0..4) {
                val wave = sin(boss.animFrame * 3f + i).toFloat() * 12f
                drawLine(
                    color = Color(0xFFBE185D),
                    start = Offset(bx - 20f, by - 30f + (i * 18f)),
                    end = Offset(bx - 80f, by - 40f + (i * 22f) + wave),
                    strokeWidth = 6f
                )
            }
        }
    }
}

private fun DrawScope.drawBullets(engine: GameEngine, shakeX: Float, shakeY: Float) {
    val camX = engine.cameraX - shakeX

    for (b in engine.bullets) {
        val sx = b.x - camX
        val sy = b.y + shakeY
        if (sx < -20f || sx > 820f) continue

        if (b.isEnemy) {
            // Glowing red/yellow enemy plasma orb
            drawCircle(Color(0xFFEF4444), radius = b.radius + 2f, center = Offset(sx, sy))
            drawCircle(Color(0xFFFBBF24), radius = b.radius, center = Offset(sx, sy))
            drawCircle(Color.White, radius = b.radius * 0.4f, center = Offset(sx, sy))
        } else {
            when (b.weaponType) {
                WeaponType.RIFLE -> {
                    // Classic small bright red pellets
                    drawCircle(Color(0xFFEF4444), radius = b.radius, center = Offset(sx, sy))
                    drawCircle(Color.White, radius = b.radius * 0.5f, center = Offset(sx, sy))
                }
                WeaponType.MACHINE_GUN -> {
                    // Yellow/Orange high-speed tracer
                    drawOval(Color(0xFFF59E0B), Offset(sx - 5f, sy - 3f), Size(10f, 6f))
                    drawOval(Color.White, Offset(sx - 3f, sy - 1.5f), Size(6f, 3f))
                }
                WeaponType.SPREAD -> {
                    // Expanding orange/red energy balls
                    drawCircle(Color(0xFFEF4444), radius = b.radius + 1.5f, center = Offset(sx, sy))
                    drawCircle(Color(0xFFF97316), radius = b.radius, center = Offset(sx, sy))
                    drawCircle(Color.White, radius = b.radius * 0.5f, center = Offset(sx, sy))
                }
                WeaponType.LASER -> {
                    // Piercing cyan high-intensity energy beam segment
                    val angle = atan2(b.vy, b.vx)
                    rotate(angle * 180f / Math.PI.toFloat(), pivot = Offset(sx, sy)) {
                        drawRoundRect(
                            color = Color(0xFF06B6D4),
                            topLeft = Offset(sx - 16f, sy - 3f),
                            size = Size(32f, 6f),
                            cornerRadius = CornerRadius(3f, 3f)
                        )
                        drawRoundRect(
                            color = Color.White,
                            topLeft = Offset(sx - 12f, sy - 1.5f),
                            size = Size(24f, 3f),
                            cornerRadius = CornerRadius(1.5f, 1.5f)
                        )
                    }
                }
                WeaponType.FIREBALL -> {
                    // Fiery spinning orb
                    drawCircle(Color(0xFFDC2626), radius = b.radius + 2f, center = Offset(sx, sy))
                    drawCircle(Color(0xFFFBBF24), radius = b.radius, center = Offset(sx, sy))
                }
                WeaponType.BARRIER -> {}
            }
        }
    }
}

private fun DrawScope.drawPowerUps(engine: GameEngine, shakeX: Float, shakeY: Float) {
    val camX = engine.cameraX - shakeX

    for (p in engine.powerUps) {
        val sx = p.x - camX
        val sy = p.y + shakeY
        if (sx < -40f || sx > 840f) continue

        if (p.isFloatingCapsule) {
            // Iconic Red Falcon winged powerup capsule flying through the air
            drawRoundRect(
                color = Color(0xFFDC2626),
                topLeft = Offset(sx - 18f, sy - 10f),
                size = Size(36f, 20f),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Falcon wings
            drawOval(Color(0xFFF87171), Offset(sx - 24f, sy - 5f), Size(10f, 10f))
            drawOval(Color(0xFFF87171), Offset(sx + 14f, sy - 5f), Size(10f, 10f))
            // Center core
            drawCircle(Color.White, radius = 5f, center = Offset(sx, sy))
        } else {
            // Weapon Badge on ground with letter ('S', 'M', 'L', 'F', 'B')
            // Pulsing glow badge
            val badgeColor = when (p.type) {
                WeaponType.SPREAD -> Color(0xFFEF4444)
                WeaponType.MACHINE_GUN -> Color(0xFFF59E0B)
                WeaponType.LASER -> Color(0xFF06B6D4)
                WeaponType.FIREBALL -> Color(0xFFEA580C)
                WeaponType.BARRIER -> Color(0xFF38BDF8)
                else -> Color(0xFF10B981)
            }
            drawCircle(badgeColor, radius = 15f, center = Offset(sx, sy))
            drawCircle(Color.White, radius = 12f, center = Offset(sx, sy))
            drawCircle(badgeColor, radius = 10f, center = Offset(sx, sy))

            // Draw letter code rectangle block
            drawRect(Color.White, Offset(sx - 4f, sy - 4f), Size(8f, 8f))
        }
    }
}

private fun DrawScope.drawParticles(engine: GameEngine, shakeX: Float, shakeY: Float) {
    val camX = engine.cameraX - shakeX
    for (p in engine.particles) {
        val sx = p.x - camX
        val sy = p.y + shakeY
        drawCircle(
            color = Color(p.color).copy(alpha = p.alpha),
            radius = p.size,
            center = Offset(sx, sy)
        )
    }
}

private fun DrawScope.drawCrtScanlines(width: Float, height: Float) {
    // Subtle retro CRT scanlines every 4 pixels
    for (y in 0 until height.toInt() step 4) {
        drawLine(
            color = Color.Black.copy(alpha = 0.22f),
            start = Offset(0f, y.toFloat()),
            end = Offset(width, y.toFloat()),
            strokeWidth = 1.2f
        )
    }

    // Vignette dark corners for curved CRT screen illusion
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.35f)),
            center = Offset(width / 2f, height / 2f),
            radius = maxOf(width, height) * 0.7f
        ),
        size = Size(width, height)
    )
}
