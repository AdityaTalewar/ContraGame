package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.game.GameEngine

@Composable
fun PauseMenuDialog(
    engine: GameEngine,
    onResume: () -> Unit,
    onQuickSave: () -> Unit,
    onQuickLoad: () -> Unit,
    onOpenSettings: () -> Unit,
    onQuit: () -> Unit
) {
    Dialog(onDismissRequest = onResume) {
        Column(
            modifier = Modifier
                .widthIn(max = 380.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "MISSION PAUSED",
                color = Color(0xFFEF4444),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )

            Text(
                text = "${engine.stageDef.name}: ${engine.stageDef.subtitle}",
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onResume,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_resume_btn")
            ) {
                Icon(imageVector = Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("RESUME MISSION", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onQuickSave,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_quicksave_btn")
            ) {
                Icon(imageVector = Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("QUICK-SAVE PROGRESS", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = onQuickLoad,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_quickload_btn")
            ) {
                Icon(imageVector = Icons.Default.Restore, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOAD QUICK-SAVE", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }

            OutlinedButton(
                onClick = onOpenSettings,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(imageVector = Icons.Default.Settings, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(6.dp))
                Text("SETTINGS & AUDIO", color = Color.White, fontFamily = FontFamily.Monospace)
            }

            TextButton(
                onClick = onQuit,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("dialog_quit_btn")
            ) {
                Text("ABORT TO TITLE MENU", color = Color(0xFFEF4444), fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun GameOverDialog(
    isVictory: Boolean,
    score: Int,
    kills: Int,
    accuracy: Int,
    stageReached: Int,
    difficultyTier: String,
    onSaveScore: (String) -> Unit,
    onRetry: () -> Unit,
    onMainMenu: () -> Unit
) {
    var playerInitials by remember { mutableStateOf("BIL") }
    var scoreSubmitted by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = {}) {
        Column(
            modifier = Modifier
                .widthIn(max = 400.dp)
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF0F172A))
                .border(2.dp, if (isVictory) Color(0xFFFBBF24) else Color(0xFFEF4444), RoundedCornerShape(12.dp))
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (isVictory) "MISSION ACCOMPLISHED!" else "GAME OVER",
                color = if (isVictory) Color(0xFFFBBF24) else Color(0xFFEF4444),
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                fontSize = 20.sp
            )

            Text(
                text = if (isVictory) "Alien Fortress Destroyed! Earth is Saved!" else "All commandos perished in the line of duty.",
                color = Color(0xFF94A3B8),
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp
            )

            // Mission Stats Breakdown
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF030712))
                    .padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                StatRow("FINAL SCORE:", score.toString().padStart(6, '0'), Color(0xFFFBBF24))
                StatRow("STAGE CLEARED:", "Stage $stageReached", Color.White)
                StatRow("DIFFICULTY TIER:", difficultyTier, Color(0xFF38BDF8))
                StatRow("HOSTILES ELIMINATED:", "$kills Kills", Color(0xFFEF4444))
                StatRow("HIT ACCURACY:", "$accuracy%", Color(0xFF10B981))
            }

            // Hall of Fame Initials Input
            if (!scoreSubmitted) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    Text(
                        text = "RECORD YOUR INITIALS:",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                    OutlinedTextField(
                        value = playerInitials,
                        onValueChange = { if (it.length <= 4) playerInitials = it.uppercase() },
                        singleLine = true,
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFFFBBF24)
                        ),
                        modifier = Modifier
                            .width(130.dp)
                            .padding(top = 4.dp)
                            .testTag("initials_input")
                    )

                    Button(
                        onClick = {
                            onSaveScore(playerInitials.ifBlank { "BIL" })
                            scoreSubmitted = true
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFBBF24)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("submit_score_btn")
                    ) {
                        Text(
                            text = "SUBMIT TO HALL OF FAME",
                            color = Color.Black,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 12.sp
                        )
                    }
                }
            } else {
                Text(
                    text = "SCORE SUBMITTED TO LEADERBOARD!",
                    color = Color(0xFF10B981),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier.padding(vertical = 6.dp)
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                    modifier = Modifier
                        .weight(1f)
                        .testTag("retry_btn")
                ) {
                    Text("RETRY", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = onMainMenu,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("main_menu_btn")
                ) {
                    Text("TITLE MENU", color = Color.White, fontFamily = FontFamily.Monospace)
                }
            }
        }
    }
}

@Composable
fun StatRow(label: String, value: String, valueColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = Color(0xFF94A3B8), fontSize = 11.sp, fontFamily = FontFamily.Monospace)
        Text(text = value, color = valueColor, fontSize = 11.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
    }
}
