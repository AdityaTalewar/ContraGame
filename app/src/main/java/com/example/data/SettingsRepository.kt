package com.example.data

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class GameSettings(
    val touchControlSize: Float = 1.0f,     // 0.8f (small), 1.0f (medium), 1.25f (large), 1.5f (xl)
    val touchOpacity: Float = 0.75f,        // 0.2f to 1.0f
    val autoFireEnabled: Boolean = false,   // continuous firing when holding shoot
    val hapticFeedback: Boolean = true,     // vibration on gun recoil / damage
    val swapControls: Boolean = false,      // swap left/right controls for left-handed users
    val crtScanlines: Boolean = true,       // retro CRT scanlines and phosphorescent glow
    val screenShake: Boolean = true,        // screen shake on explosions
    val darkMode: Boolean = true,           // dark neon arcade theme
    val musicVolume: Float = 0.8f,          // 0f to 1f
    val sfxVolume: Float = 0.9f,            // 0f to 1f
    val biometricSecurity: Boolean = false, // Biometric lock for saves and records
    val p1Name: String = "BIL",             // Default 3-letter retro tag for Bill
    val p2Name: String = "LAN"              // Default 3-letter retro tag for Lance
)

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("retro_strike_prefs", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(loadSettings())
    val settings: StateFlow<GameSettings> = _settings.asStateFlow()

    private fun loadSettings(): GameSettings {
        return GameSettings(
            touchControlSize = prefs.getFloat("touch_size", 1.0f),
            touchOpacity = prefs.getFloat("touch_opacity", 0.75f),
            autoFireEnabled = prefs.getBoolean("auto_fire", false),
            hapticFeedback = prefs.getBoolean("haptic_feedback", true),
            swapControls = prefs.getBoolean("swap_controls", false),
            crtScanlines = prefs.getBoolean("crt_scanlines", true),
            screenShake = prefs.getBoolean("screen_shake", true),
            darkMode = prefs.getBoolean("dark_mode", true),
            musicVolume = prefs.getFloat("music_volume", 0.8f),
            sfxVolume = prefs.getFloat("sfx_volume", 0.9f),
            biometricSecurity = prefs.getBoolean("biometric_security", false),
            p1Name = prefs.getString("p1_name", "BIL") ?: "BIL",
            p2Name = prefs.getString("p2_name", "LAN") ?: "LAN"
        )
    }

    fun updateSettings(newSettings: GameSettings) {
        prefs.edit().apply {
            putFloat("touch_size", newSettings.touchControlSize)
            putFloat("touch_opacity", newSettings.touchOpacity)
            putBoolean("auto_fire", newSettings.autoFireEnabled)
            putBoolean("haptic_feedback", newSettings.hapticFeedback)
            putBoolean("swap_controls", newSettings.swapControls)
            putBoolean("crt_scanlines", newSettings.crtScanlines)
            putBoolean("screen_shake", newSettings.screenShake)
            putBoolean("dark_mode", newSettings.darkMode)
            putFloat("music_volume", newSettings.musicVolume)
            putFloat("sfx_volume", newSettings.sfxVolume)
            putBoolean("biometric_security", newSettings.biometricSecurity)
            putString("p1_name", newSettings.p1Name)
            putString("p2_name", newSettings.p2Name)
            apply()
        }
        _settings.value = newSettings
    }
}
