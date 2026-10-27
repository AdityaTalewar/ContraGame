package com.example.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.SyncStatus

@Composable
fun MainMenuScreen(
    syncStatus: SyncStatus,
    hasQuickSave: Boolean,
    onStartSolo: () -> Unit,
    onStartCoop: () -> Unit,
    onResumeSave: () -> Unit,
    onOpenLeaderboard: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenJukebox: () -> Unit,
    onBiometricUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    // Blinking "INSERT COIN" / "PRESS START" animation
    val infiniteTransition = rememberInfiniteTransition(label = "title_blink")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF030712), Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .widthIn(max = 500.dp)
                .fillMaxWidth()
                .verticalScroll(scrollState)
                .padding(vertical = 12.dp)
        ) {
            // Retro Title Banner
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF0A0F1D))
                    .border(2.dp, Color(0xFFEF4444), RoundedCornerShape(12.dp))
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "RETRO STRIKE",
                        color = Color(0xFFEF4444),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 2.sp
                    )
                    Text(
                        text = "FORCE '87 // RUN & GUN",
                        color = Color(0xFF38BDF8),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = 3.sp
                    )
                }
            }

            Text(
                text = "★ 1987 ARCADE WARFARE ★",
                color = Color(0xFFFBBF24).copy(alpha = alphaAnim),
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(top = 10.dp, bottom = 16.dp)
            )

            // Menu Action Cards
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // 1. SOLO MISSION
                MenuButton(
                    title = "1 PLAYER MISSION (SOLO)",
                    subtitle = "Bill Rizer • Jungle to Alien Core",
                    icon = Icons.Default.Person,
                    accentColor = Color(0xFFEF4444),
                    tag = "menu_solo_btn",
                    onClick = onStartSolo
                )

                // 2. 2-PLAYER CO-OP
                MenuButton(
                    title = "2 PLAYERS CO-OP (SHARED SCREEN)",
                    subtitle = "Bill & Lance • Dual Controllers / On-Screen",
                    icon = Icons.Default.Groups,
                    accentColor = Color(0xFF38BDF8),
                    tag = "menu_coop_btn",
                    onClick = onStartCoop
                )

                // 3. QUICK LOAD
                if (hasQuickSave) {
                    MenuButton(
                        title = "CONTINUE CAMPAIGN",
                        subtitle = "Resume from last quick-save checkpoint",
                        icon = Icons.Default.PlayArrow,
                        accentColor = Color(0xFF10B981),
                        tag = "menu_continue_btn",
                        onClick = onResumeSave
                    )
                }

                // 4. LEADERBOARD
                MenuButton(
                    title = "HALL OF FAME (ONLINE & LOCAL)",
                    subtitle = "Top scores, stage clears & cloud sync",
                    icon = Icons.Default.EmojiEvents,
                    accentColor = Color(0xFFF59E0B),
                    tag = "menu_leaderboard_btn",
                    onClick = onOpenLeaderboard
                )

                // 5. CONTROLS & SETTINGS
                MenuButton(
                    title = "CONTROLS & SETTINGS",
                    subtitle = "Button scale, opacity, auto-fire, CRT filter",
                    icon = Icons.Default.Settings,
                    accentColor = Color(0xFFA855F7),
                    tag = "menu_settings_btn",
                    onClick = onOpenSettings
                )

                // 6. CHIPTUNE JUKEBOX
                MenuButton(
                    title = "CHIPTUNE JUKEBOX & SOUND TEST",
                    subtitle = "Listen to authentic 8-bit tracks & retro SFX",
                    icon = Icons.Default.MusicNote,
                    accentColor = Color(0xFFEC4899),
                    tag = "menu_jukebox_btn",
                    onClick = onOpenJukebox
                )

                // 7. BIOMETRIC SECURITY
                MenuButton(
                    title = "BIOMETRIC SECURITY VAULT",
                    subtitle = "Fingerprint & Face ID save file protection",
                    icon = Icons.Default.Fingerprint,
                    accentColor = Color(0xFF06B6D4),
                    tag = "menu_biometric_btn",
                    onClick = onBiometricUnlock
                )
            }

            // Cloud Sync Status Footer
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .padding(top = 18.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF0F172A).copy(alpha = 0.8f))
                    .border(1.dp, Color(0xFF334155), RoundedCornerShape(16.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                val (icon, color, text) = when (syncStatus) {
                    SyncStatus.ONLINE_SYNCED -> Triple(Icons.Default.CloudDone, Color(0xFF10B981), "Online Cloud Sync: Active")
                    SyncStatus.SYNCING -> Triple(Icons.Default.Sync, Color(0xFF38BDF8), "Cloud Syncing...")
                    SyncStatus.OFFLINE_LOCAL -> Triple(Icons.Default.CloudOff, Color(0xFFF59E0B), "Offline Mode (Local Storage)")
                    SyncStatus.PENDING_CHANGES -> Triple(Icons.Default.CloudUpload, Color(0xFFF97316), "Pending Cloud Sync")
                }
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = color,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = text,
                    color = color,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}

@Composable
fun MenuButton(
    title: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    tag: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF0F172A).copy(alpha = 0.9f))
            .border(1.5.dp, accentColor.copy(alpha = 0.75f), RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .testTag(tag)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(accentColor.copy(alpha = 0.2f))
                .border(1.dp, accentColor, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accentColor,
                modifier = Modifier.size(20.dp)
            )
        }

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
            Text(
                text = subtitle,
                color = Color(0xFF94A3B8),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace
            )
        }

        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = accentColor,
            modifier = Modifier.size(18.dp)
        )
    }
}
