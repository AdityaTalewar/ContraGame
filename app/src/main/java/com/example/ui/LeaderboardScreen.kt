package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.CloudSyncManager
import com.example.data.LeaderboardEntry
import com.example.data.SyncStatus

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LeaderboardScreen(
    scores: List<LeaderboardEntry>,
    syncStatus: SyncStatus,
    cloudSyncManager: CloudSyncManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedFilter by remember { mutableStateOf("ALL") } // ALL, SOLO, COOP

    val filteredScores = remember(scores, selectedFilter) {
        when (selectedFilter) {
            "SOLO" -> scores.filter { !it.isCoop }
            "COOP" -> scores.filter { it.isCoop }
            else -> scores
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "HALL OF FAME",
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            fontSize = 18.sp,
                            color = Color(0xFFFBBF24)
                        )
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("leaderboard_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                actions = {
                    // Manual Sync Now Button
                    IconButton(
                        onClick = { cloudSyncManager.syncNow() },
                        modifier = Modifier.testTag("leaderboard_sync_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Sync Cloud",
                            tint = Color(0xFF38BDF8)
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A)
                )
            )
        },
        containerColor = Color(0xFF030712)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            // Cloud Sync Status Banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF0F172A))
                    .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val (icon, color, text) = when (syncStatus) {
                        SyncStatus.ONLINE_SYNCED -> Triple(Icons.Default.CloudDone, Color(0xFF10B981), "Global Cloud Leaderboard Synced")
                        SyncStatus.SYNCING -> Triple(Icons.Default.Sync, Color(0xFF38BDF8), "Synchronizing with Cloud...")
                        SyncStatus.OFFLINE_LOCAL -> Triple(Icons.Default.CloudOff, Color(0xFFF59E0B), "Offline Mode (Local Storage Cached)")
                        SyncStatus.PENDING_CHANGES -> Triple(Icons.Default.CloudUpload, Color(0xFFF97316), "Pending Cloud Synchronization")
                    }
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = color,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = text,
                        color = color,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )
                }

                Text(
                    text = "${filteredScores.size} SCORES",
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace
                )
            }

            // Filter Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("ALL", "SOLO", "COOP").forEach { filter ->
                    val isSelected = selectedFilter == filter
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFilter = filter },
                        label = {
                            Text(
                                text = filter,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFFEF4444),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFF94A3B8)
                        ),
                        modifier = Modifier.testTag("filter_${filter.lowercase()}_chip")
                    )
                }
            }

            // Scores List
            if (filteredScores.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.TopCenter
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.MilitaryTech,
                            contentDescription = null,
                            tint = Color(0xFF64748B),
                            modifier = Modifier.size(48.dp)
                        )
                        Text(
                            text = "NO HIGH SCORES RECORDED YET",
                            color = Color(0xFF94A3B8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                        Text(
                            text = "Complete stages in campaign mode to enter the Hall of Fame!",
                            color = Color(0xFF64748B),
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(filteredScores) { index, entry ->
                        val rank = index + 1
                        val medalColor = when (rank) {
                            1 -> Color(0xFFFBBF24) // Gold
                            2 -> Color(0xFFCBD5E1) // Silver
                            3 -> Color(0xFFB45309) // Bronze
                            else -> Color(0xFF64748B)
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0F172A))
                                .border(1.dp, if (rank <= 3) medalColor.copy(alpha = 0.6f) else Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Rank & Tag
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(28.dp)
                                        .clip(CircleShape)
                                        .background(medalColor.copy(alpha = 0.2f))
                                        .border(1.dp, medalColor, CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "#$rank",
                                        color = medalColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Text(
                                            text = entry.playerName,
                                            color = Color.White,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Black,
                                            fontFamily = FontFamily.Monospace
                                        )
                                        if (entry.isCoop) {
                                            Text(
                                                text = "CO-OP",
                                                color = Color(0xFF38BDF8),
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        }
                                    }
                                    Text(
                                        text = "Stage ${entry.stageReached + 1} • ${entry.difficultyTier} • ${entry.kills} Kills",
                                        color = Color(0xFF94A3B8),
                                        fontSize = 10.sp,
                                        fontFamily = FontFamily.Monospace
                                    )
                                }
                            }

                            // Score & Sync Indicator
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = entry.score.toString().padStart(6, '0'),
                                    color = Color(0xFFFBBF24),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace
                                )
                                Icon(
                                    imageVector = if (entry.isSynced) Icons.Default.CloudDone else Icons.Default.CloudOff,
                                    contentDescription = if (entry.isSynced) "Synced" else "Local",
                                    tint = if (entry.isSynced) Color(0xFF10B981) else Color(0xFFF59E0B),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
