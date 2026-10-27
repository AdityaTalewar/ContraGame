package com.example

import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.fragment.app.FragmentActivity
import com.example.audio.ChiptuneSynth
import com.example.data.AppDatabase
import com.example.data.CloudSyncManager
import com.example.data.SettingsRepository
import com.example.game.DynamicDifficultyManager
import com.example.game.GameEngine
import com.example.ui.ControllerManager
import com.example.ui.RetroCommandoApp

class MainActivity : FragmentActivity() {

    private lateinit var audioSynth: ChiptuneSynth
    private lateinit var database: AppDatabase
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var cloudSyncManager: CloudSyncManager
    private lateinit var ddaManager: DynamicDifficultyManager
    private lateinit var gameEngine: GameEngine
    private lateinit var controllerManager: ControllerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        audioSynth = ChiptuneSynth()
        database = AppDatabase.getInstance(this)
        settingsRepository = SettingsRepository(this)
        cloudSyncManager = CloudSyncManager(this, database.leaderboardDao(), database.gameSaveDao())
        ddaManager = DynamicDifficultyManager()
        gameEngine = GameEngine(audioSynth, ddaManager)
        controllerManager = ControllerManager(gameEngine)

        // Apply saved volumes
        val settings = settingsRepository.settings.value
        audioSynth.musicVolume = settings.musicVolume
        audioSynth.sfxVolume = settings.sfxVolume

        setContent {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color(0xFF030712)
            ) {
                RetroCommandoApp(
                    engine = gameEngine,
                    audioSynth = audioSynth,
                    database = database,
                    settingsRepository = settingsRepository,
                    cloudSyncManager = cloudSyncManager,
                    controllerManager = controllerManager
                )
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (::controllerManager.isInitialized && controllerManager.handleKeyEvent(event)) {
            return true
        }
        return super.dispatchKeyEvent(event)
    }

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if (::controllerManager.isInitialized && controllerManager.handleMotionEvent(event)) {
            return true
        }
        return super.onGenericMotionEvent(event)
    }

    override fun onResume() {
        super.onResume()
        if (::audioSynth.isInitialized && !audioSynth.isMuted) {
            // Audio resumes smoothly
        }
    }

    override fun onStop() {
        super.onStop()
        if (::audioSynth.isInitialized) {
            audioSynth.stopMusic()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::audioSynth.isInitialized) {
            audioSynth.release()
        }
    }
}
