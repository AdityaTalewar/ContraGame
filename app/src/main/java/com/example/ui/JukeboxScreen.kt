package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.example.audio.ChiptuneSynth

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JukeboxScreen(
    audioSynth: ChiptuneSynth,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentlyPlaying by remember { mutableStateOf<ChiptuneSynth.Track?>(null) }

    val tracks = listOf(
        Triple(ChiptuneSynth.Track.TITLE, "TITLE FANFARE", "Heroic military opening theme"),
        Triple(ChiptuneSynth.Track.JUNGLE, "STAGE 1: JUNGLE OUTPOST", "Driving rock & punchy bassline"),
        Triple(ChiptuneSynth.Track.WATERFALL, "STAGE 2: WATERFALL ASCENT", "Syncopated climbing rhythm"),
        Triple(ChiptuneSynth.Track.ALIEN_CORE, "STAGE 3: ALIEN CORE", "Ominous chromatic extraterrestrial groove"),
        Triple(ChiptuneSynth.Track.BOSS, "BOSS BATTLE ALERT", "Fast-paced adrenaline double-time siren"),
        Triple(ChiptuneSynth.Track.VICTORY, "VICTORY FANFARE", "Triumphant stage clear celebration"),
        Triple(ChiptuneSynth.Track.GAME_OVER, "GAME OVER DEFEAT", "Classic 8-bit descent")
    )

    val sfxList = listOf(
        Pair(ChiptuneSynth.Sfx.SHOOT_NORMAL, "Normal Rifle"),
        Pair(ChiptuneSynth.Sfx.SHOOT_MACHINE, "Machine Gun 'M'"),
        Pair(ChiptuneSynth.Sfx.SHOOT_SPREAD, "Spread Gun 'S'"),
        Pair(ChiptuneSynth.Sfx.SHOOT_LASER, "Laser Beam 'L'"),
        Pair(ChiptuneSynth.Sfx.SHOOT_FIRE, "Fire Gun 'F'"),
        Pair(ChiptuneSynth.Sfx.EXPLOSION, "Enemy Explosion"),
        Pair(ChiptuneSynth.Sfx.EXPLOSION_BIG, "Big Boss Blast"),
        Pair(ChiptuneSynth.Sfx.POWERUP_PICKUP, "Falcon Power-Up"),
        Pair(ChiptuneSynth.Sfx.JUMP, "Jump Somersault"),
        Pair(ChiptuneSynth.Sfx.BOSS_ALARM, "Boss Emergency Alarm"),
        Pair(ChiptuneSynth.Sfx.ONE_UP, "1-Up Extra Life")
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "CHIPTUNE JUKEBOX",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFFEC4899)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("jukebox_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            audioSynth.stopMusic()
                            currentlyPlaying = null
                        },
                        modifier = Modifier.testTag("jukebox_stop_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Stop,
                            contentDescription = "Stop Music",
                            tint = Color(0xFFEF4444)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF030712)
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sound engine intro badge
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, Color(0xFFEC4899).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                        .padding(12.dp)
                ) {
                    Column {
                        Text(
                            text = "NES 8-BIT AUDIO SYNTHESIZER",
                            color = Color(0xFFEC4899),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Text(
                            text = "Real-time PCM synthesis: 2 Pulse channels, 1 Triangle bass, and 1 Noise percussion channel.",
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }

            // Soundtrack section
            item {
                Text(
                    text = "CAMPAIGN SOUNDTRACKS",
                    color = Color(0xFFFBBF24),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp
                )
            }

            items(tracks) { (track, title, desc) ->
                val isPlaying = currentlyPlaying == track
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isPlaying) Color(0xFF1E1B4B) else Color(0xFF0F172A))
                        .border(
                            1.dp,
                            if (isPlaying) Color(0xFFEC4899) else Color(0xFF1E293B),
                            RoundedCornerShape(8.dp)
                        )
                        .clickable {
                            if (isPlaying) {
                                audioSynth.stopMusic()
                                currentlyPlaying = null
                            } else {
                                audioSynth.playTrack(track)
                                currentlyPlaying = track
                            }
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            color = if (isPlaying) Color(0xFFEC4899) else Color.White,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = desc,
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontSize = 10.sp
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(if (isPlaying) Color(0xFFEC4899) else Color(0xFF1E293B)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            // Sound Effects Section
            item {
                Text(
                    text = "RETRO SOUND EFFECTS (SFX TEST)",
                    color = Color(0xFF38BDF8),
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }

            item {
                // SFX Grid
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    sfxList.chunked(2).forEach { rowSfx ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            rowSfx.forEach { (sfx, name) ->
                                Button(
                                    onClick = { audioSynth.playSfx(sfx) },
                                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                                    modifier = Modifier
                                        .weight(1f)
                                        .border(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.VolumeUp,
                                        contentDescription = null,
                                        tint = Color(0xFF38BDF8),
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = name,
                                        color = Color.White,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            if (rowSfx.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
        }
    }
}
