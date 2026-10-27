package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.example.audio.ChiptuneSynth
import com.example.data.*
import com.example.game.*
import kotlinx.coroutines.launch

enum class Screen {
    MAIN_MENU,
    GAMEPLAY,
    LEADERBOARD,
    SETTINGS,
    JUKEBOX
}

@Composable
fun RetroCommandoApp(
    engine: GameEngine,
    audioSynth: ChiptuneSynth,
    database: AppDatabase,
    settingsRepository: SettingsRepository,
    cloudSyncManager: CloudSyncManager,
    controllerManager: ControllerManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentScreen by remember { mutableStateOf(Screen.MAIN_MENU) }
    val settings by settingsRepository.settings.collectAsState()
    val syncStatus by cloudSyncManager.syncStatus.collectAsState()

    val leaderboardScores by database.leaderboardDao().getAllHighScores().collectAsState(initial = emptyList())
    var quickSaveEntity by remember { mutableStateOf<GameSaveEntity?>(null) }

    // Check for quick save on launch
    LaunchedEffect(Unit) {
        quickSaveEntity = database.gameSaveDao().getSave(1)
        audioSynth.playTrack(ChiptuneSynth.Track.TITLE)
    }

    // 60 FPS Game Loop
    LaunchedEffect(currentScreen, engine.isPaused, engine.isGameOver) {
        if (currentScreen == Screen.GAMEPLAY) {
            var lastTime = System.nanoTime()
            while (true) {
                withFrameNanos { now ->
                    val dt = ((now - lastTime) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
                    lastTime = now

                    // Auto-fire handling if enabled
                    if (settings.autoFireEnabled) {
                        if (engine.p1FirePressed) {
                            // engine updates firing directly
                        }
                    }

                    engine.update(dt)
                }
            }
        }
    }

    fun startMission(isCoop: Boolean) {
        engine.isCoop = isCoop
        engine.ddaManager.reset()
        engine.player1.lives = 3
        engine.player1.score = 0
        engine.player1.weapon = WeaponType.RIFLE
        if (isCoop) {
            engine.player2.lives = 3
            engine.player2.score = 0
            engine.player2.weapon = WeaponType.RIFLE
        }
        engine.loadStage(0)
        currentScreen = Screen.GAMEPLAY
    }

    fun quickSave() {
        coroutineScope.launch {
            val save = GameSaveEntity(
                slotId = 1,
                stageIndex = engine.currentStageIndex,
                stageProgressX = engine.cameraX,
                isCoop = engine.isCoop,
                p1Weapon = engine.player1.weapon.name,
                p1Lives = engine.player1.lives,
                p1Score = engine.player1.score,
                p1Health = engine.player1.health.toInt(),
                p2Weapon = engine.player2.weapon.name,
                p2Lives = engine.player2.lives,
                p2Score = engine.player2.score,
                p2Health = engine.player2.health.toInt(),
                difficultyTier = engine.ddaManager.currentTier.name,
                isSynced = false
            )
            database.gameSaveDao().saveGame(save)
            quickSaveEntity = save
            cloudSyncManager.syncNow()
            engine.showNotification("CAMPAIGN PROGRESS QUICK-SAVED!")
            Toast.makeText(context, "Game state saved!", Toast.LENGTH_SHORT).show()
        }
    }

    fun applySave(save: GameSaveEntity) {
        engine.isCoop = save.isCoop
        engine.ddaManager.setDirectTier(
            DifficultyTier.entries.find { it.name == save.difficultyTier } ?: DifficultyTier.BALANCED
        )
        engine.loadStage(save.stageIndex)
        engine.player1.lives = save.p1Lives
        engine.player1.score = save.p1Score
        engine.player1.weapon = WeaponType.entries.find { it.name == save.p1Weapon } ?: WeaponType.RIFLE
        engine.player1.health = save.p1Health.toFloat()

        if (save.isCoop) {
            engine.player2.lives = save.p2Lives
            engine.player2.score = save.p2Score
            engine.player2.weapon = WeaponType.entries.find { it.name == save.p2Weapon } ?: WeaponType.RIFLE
            engine.player2.health = save.p2Health.toFloat()
        }

        currentScreen = Screen.GAMEPLAY
        engine.isPaused = false
        engine.showNotification("RESUMED FROM CHECKPOINT!")
    }

    fun quickLoad() {
        coroutineScope.launch {
            val save = database.gameSaveDao().getSave(1)
            if (save != null) {
                if (settings.biometricSecurity && context is FragmentActivity) {
                    BiometricAuthManager.promptBiometric(
                        activity = context,
                        title = "Authenticate Campaign Load",
                        subtitle = "Scan biometric credentials to resume save state",
                        onSuccess = {
                            applySave(save)
                        },
                        onError = {
                            Toast.makeText(context, "Biometric failed: $it", Toast.LENGTH_SHORT).show()
                        }
                    )
                } else {
                    applySave(save)
                }
            } else {
                Toast.makeText(context, "No save file found!", Toast.LENGTH_SHORT).show()
            }
        }
    }


    fun recordHighScore(playerName: String) {
        coroutineScope.launch {
            val totalScore = engine.player1.score + (if (engine.isCoop) engine.player2.score else 0)
            val kills = engine.player1.kills + (if (engine.isCoop) engine.player2.kills else 0)
            val totalShots = engine.player1.shotsFired + (if (engine.isCoop) engine.player2.shotsFired else 0)
            val totalHits = engine.player1.shotsHit + (if (engine.isCoop) engine.player2.shotsHit else 0)
            val accuracy = if (totalShots > 0) ((totalHits.toFloat() / totalShots) * 100).toInt() else 75

            val entry = LeaderboardEntry(
                playerName = playerName,
                score = totalScore,
                stageReached = engine.currentStageIndex,
                isCoop = engine.isCoop,
                difficultyTier = engine.ddaManager.currentTier.name,
                kills = kills,
                accuracyPercent = accuracy,
                isSynced = false
            )
            database.leaderboardDao().insertScore(entry)
            cloudSyncManager.syncNow()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF030712))
    ) {
        when (currentScreen) {
            Screen.MAIN_MENU -> {
                MainMenuScreen(
                    syncStatus = syncStatus,
                    hasQuickSave = quickSaveEntity != null,
                    onStartSolo = { startMission(isCoop = false) },
                    onStartCoop = { startMission(isCoop = true) },
                    onResumeSave = { quickLoad() },
                    onOpenLeaderboard = { currentScreen = Screen.LEADERBOARD },
                    onOpenSettings = { currentScreen = Screen.SETTINGS },
                    onOpenJukebox = { currentScreen = Screen.JUKEBOX },
                    onBiometricUnlock = {
                        if (context is FragmentActivity) {
                            BiometricAuthManager.promptBiometric(
                                activity = context,
                                title = "Arcade Security Vault",
                                subtitle = "Confirm fingerprint or face recognition",
                                onSuccess = {
                                    Toast.makeText(context, "Biometric Vault Unlocked!", Toast.LENGTH_SHORT).show()
                                },
                                onError = {
                                    Toast.makeText(context, "Verification failed: $it", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                )
            }

            Screen.GAMEPLAY -> {
                BackHandler {
                    engine.isPaused = !engine.isPaused
                }

                Box(modifier = Modifier.fillMaxSize()) {
                    // Game Canvas Rendering
                    GameCanvas(
                        engine = engine,
                        showCrtScanlines = settings.crtScanlines
                    )

                    // Touch Controls Overlay
                    TouchControlsOverlay(
                        isCoop = engine.isCoop,
                        settings = settings,
                        onP1Move = { dx, dy -> engine.p1MoveX = dx; engine.p1MoveY = dy },
                        onP1Jump = { engine.p1JumpPressed = it },
                        onP1Fire = { engine.p1FirePressed = it },
                        onP2Move = { dx, dy -> engine.p2MoveX = dx; engine.p2MoveY = dy },
                        onP2Jump = { engine.p2JumpPressed = it },
                        onP2Fire = { engine.p2FirePressed = it }
                    )

                    // HUD Top Bar & Boss Gauge
                    GameHud(
                        engine = engine,
                        syncStatus = syncStatus,
                        onPauseClick = { engine.isPaused = true },
                        onQuickSaveClick = { quickSave() }
                    )

                    // In-Game Pause Dialog
                    if (engine.isPaused) {
                        PauseMenuDialog(
                            engine = engine,
                            onResume = { engine.isPaused = false },
                            onQuickSave = { quickSave() },
                            onQuickLoad = { quickLoad() },
                            onOpenSettings = { currentScreen = Screen.SETTINGS },
                            onQuit = {
                                engine.isPaused = false
                                audioSynth.playTrack(ChiptuneSynth.Track.TITLE)
                                currentScreen = Screen.MAIN_MENU
                            }
                        )
                    }

                    // Game Over / Victory Dialog
                    if (engine.isGameOver || engine.isCampaignVictory) {
                        val finalScore = engine.player1.score + (if (engine.isCoop) engine.player2.score else 0)
                        val totalKills = engine.player1.kills + (if (engine.isCoop) engine.player2.kills else 0)
                        val totalShots = engine.player1.shotsFired + (if (engine.isCoop) engine.player2.shotsFired else 0)
                        val totalHits = engine.player1.shotsHit + (if (engine.isCoop) engine.player2.shotsHit else 0)
                        val accuracy = if (totalShots > 0) ((totalHits.toFloat() / totalShots) * 100).toInt() else 75

                        GameOverDialog(
                            isVictory = engine.isCampaignVictory,
                            score = finalScore,
                            kills = totalKills,
                            accuracy = accuracy,
                            stageReached = engine.currentStageIndex + 1,
                            difficultyTier = engine.ddaManager.currentTier.name,
                            onSaveScore = { tag -> recordHighScore(tag) },
                            onRetry = {
                                engine.isGameOver = false
                                engine.isCampaignVictory = false
                                startMission(engine.isCoop)
                            },
                            onMainMenu = {
                                engine.isGameOver = false
                                engine.isCampaignVictory = false
                                audioSynth.playTrack(ChiptuneSynth.Track.TITLE)
                                currentScreen = Screen.MAIN_MENU
                            }
                        )
                    }
                }
            }

            Screen.LEADERBOARD -> {
                BackHandler { currentScreen = Screen.MAIN_MENU }
                LeaderboardScreen(
                    scores = leaderboardScores,
                    syncStatus = syncStatus,
                    cloudSyncManager = cloudSyncManager,
                    onBack = { currentScreen = Screen.MAIN_MENU }
                )
            }

            Screen.SETTINGS -> {
                BackHandler { currentScreen = Screen.MAIN_MENU }
                SettingsScreen(
                    settingsRepository = settingsRepository,
                    audioSynth = audioSynth,
                    onBack = { currentScreen = Screen.MAIN_MENU }
                )
            }

            Screen.JUKEBOX -> {
                BackHandler { currentScreen = Screen.MAIN_MENU }
                JukeboxScreen(
                    audioSynth = audioSynth,
                    onBack = { currentScreen = Screen.MAIN_MENU }
                )
            }
        }
    }
}
