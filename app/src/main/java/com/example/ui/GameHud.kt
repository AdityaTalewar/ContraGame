package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncStatus
import com.example.game.GameEngine
import com.example.game.Player
import com.example.game.PlayerId
import com.example.game.WeaponType

@Composable
fun GameHud(
    engine: GameEngine,
    syncStatus: SyncStatus,
    onPauseClick: () -> Unit,
    onQuickSaveClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Top HUD Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            // Player 1 Card (Red Commando)
            PlayerHudCard(
                player = engine.player1,
                name = "1P-BILL",
                tagColor = Color(0xFFEF4444)
            )

            // Center: Stage Name, DDA Difficulty Tier, Cloud Sync Badge
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 2.dp)
            ) {
                // Stage indicator
                Text(
                    text = engine.stageDef.name,
                    color = Color(0xFFFBBF24),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )

                // Dynamic Difficulty Adjustment (DDA) badge
                val dda = engine.ddaManager.currentTier
                Box(
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF38BDF8), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${dda.title} [${dda.scoreMultiplier}x]",
                        color = Color(0xFF38BDF8),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Cloud Sync Status Pill
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp),
                    modifier = Modifier.padding(top = 2.dp)
                ) {
                    val (syncIcon, syncColor, syncLabel) = when (syncStatus) {
                        SyncStatus.ONLINE_SYNCED -> Triple(Icons.Default.CloudDone, Color(0xFF10B981), "SYNCED")
                        SyncStatus.SYNCING -> Triple(Icons.Default.Sync, Color(0xFF38BDF8), "SYNCING")
                        SyncStatus.OFFLINE_LOCAL -> Triple(Icons.Default.CloudOff, Color(0xFFF59E0B), "OFFLINE")
                        SyncStatus.PENDING_CHANGES -> Triple(Icons.Default.CloudUpload, Color(0xFFF97316), "PENDING")
                    }
                    Icon(
                        imageVector = syncIcon,
                        contentDescription = "Cloud Sync",
                        tint = syncColor,
                        modifier = Modifier.size(11.dp)
                    )
                    Text(
                        text = syncLabel,
                        color = syncColor,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }

            // Player 2 Card (if Co-op) OR Quick Actions
            if (engine.isCoop) {
                PlayerHudCard(
                    player = engine.player2,
                    name = "2P-LANCE",
                    tagColor = Color(0xFF38BDF8)
                )
            }

            // Quick Actions (Save & Pause)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Quick Save Button
                IconButton(
                    onClick = onQuickSaveClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFF10B981), CircleShape)
                        .testTag("quick_save_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Save,
                        contentDescription = "Quick Save",
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(18.dp)
                    )
                }

                // Pause Button
                IconButton(
                    onClick = onPauseClick,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                        .border(1.dp, Color(0xFFEF4444), CircleShape)
                        .testTag("pause_btn")
                ) {
                    Icon(
                        imageVector = Icons.Default.Pause,
                        contentDescription = "Pause",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }

        // Bottom Boss Health Bar (when Boss is active)
        val curBoss = engine.boss
        if (curBoss != null && !curBoss.isDefeated) {
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 76.dp)
                    .widthIn(max = 420.dp)
                    .fillMaxWidth(0.6f)
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.9f))
                    .border(1.5.dp, Color(0xFFEF4444), RoundedCornerShape(6.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = curBoss.type.displayName.uppercase(),
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        text = "${(curBoss.health / curBoss.maxHealth * 100).toInt()}%",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }
                LinearProgressIndicator(
                    progress = { (curBoss.health / curBoss.maxHealth).coerceIn(0f, 1f) },
                    color = Color(0xFFEF4444),
                    trackColor = Color(0xFF450A0A),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                )
            }
        }

        // Notification Banner Toast
        AnimatedVisibility(
            visible = engine.statusNotification != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = 24.dp)
        ) {
            engine.statusNotification?.let { msg ->
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A).copy(alpha = 0.95f))
                        .border(2.dp, Color(0xFFFBBF24), RoundedCornerShape(8.dp))
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = msg,
                        color = Color(0xFFFBBF24),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }
    }
}

@Composable
fun PlayerHudCard(
    player: Player,
    name: String,
    tagColor: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(6.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
            .border(1.dp, tagColor.copy(alpha = 0.7f), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = name,
                color = tagColor,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace
            )

            // Weapon Badge (e.g. [S], [M], [L], [F])
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(3.dp))
                    .background(
                        when (player.weapon) {
                            WeaponType.SPREAD -> Color(0xFFEF4444)
                            WeaponType.MACHINE_GUN -> Color(0xFFF59E0B)
                            WeaponType.LASER -> Color(0xFF06B6D4)
                            WeaponType.FIREBALL -> Color(0xFFEA580C)
                            else -> Color(0xFF475569)
                        }
                    )
                    .padding(horizontal = 4.dp, vertical = 1.dp)
            ) {
                Text(
                    text = player.weapon.code,
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace
                )
            }
        }

        // Score
        Text(
            text = "${player.score.toString().padStart(6, '0')}",
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            fontFamily = FontFamily.Monospace
        )

        // Lives Icons (Tiny Red/Blue Badges)
        Row(
            horizontalArrangement = Arrangement.spacedBy(3.dp),
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp)
        ) {
            for (i in 1..maxOf(0, player.lives)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(tagColor)
                )
            }
            if (player.barrierTime > 0f) {
                Text(
                    text = "SHIELD",
                    color = Color(0xFF38BDF8),
                    fontSize = 8.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    modifier = Modifier.padding(start = 2.dp)
                )
            }
        }
    }
}
