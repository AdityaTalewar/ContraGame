package com.example.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.audio.ChiptuneSynth
import com.example.data.GameSettings
import com.example.data.SettingsRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    audioSynth: ChiptuneSynth,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentSettings by settingsRepository.settings.collectAsState()
    val scrollState = rememberScrollState()

    var biometricMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "SETTINGS & CONTROLS",
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        fontSize = 18.sp,
                        color = Color(0xFFA855F7)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("settings_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF0F172A))
            )
        },
        containerColor = Color(0xFF030712)
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. TOUCH CONTROLS CUSTOMIZATION
            SettingsCard(
                title = "ON-SCREEN CONTROLS",
                icon = Icons.Default.Gamepad,
                accentColor = Color(0xFFEF4444)
            ) {
                // Button Size Slider
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Touch Control Size", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("${(currentSettings.touchControlSize * 100).toInt()}%", color = Color(0xFFEF4444), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = currentSettings.touchControlSize,
                        onValueChange = { settingsRepository.updateSettings(currentSettings.copy(touchControlSize = it)) },
                        valueRange = 0.75f..1.5f,
                        steps = 3,
                        modifier = Modifier.testTag("touch_size_slider")
                    )
                }

                // Opacity Slider
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Control Opacity", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("${(currentSettings.touchOpacity * 100).toInt()}%", color = Color(0xFFEF4444), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = currentSettings.touchOpacity,
                        onValueChange = { settingsRepository.updateSettings(currentSettings.copy(touchOpacity = it)) },
                        valueRange = 0.25f..1.0f,
                        modifier = Modifier.testTag("touch_opacity_slider")
                    )
                }

                // Auto-Fire Toggle
                SettingsSwitchRow(
                    label = "Auto-Fire (Continuous Hold)",
                    description = "Holding Fire continuously shoots without rapid tapping",
                    checked = currentSettings.autoFireEnabled,
                    tag = "auto_fire_switch",
                    onCheckedChange = { settingsRepository.updateSettings(currentSettings.copy(autoFireEnabled = it)) }
                )

                // Haptic Feedback Toggle
                SettingsSwitchRow(
                    label = "Haptic Vibration Feedback",
                    description = "Tactile gun recoil and impact vibrations",
                    checked = currentSettings.hapticFeedback,
                    tag = "haptic_switch",
                    onCheckedChange = { settingsRepository.updateSettings(currentSettings.copy(hapticFeedback = it)) }
                )

                // Swap Left/Right Controls
                SettingsSwitchRow(
                    label = "Swap D-Pad and Buttons",
                    description = "Places D-Pad on right and Fire/Jump on left",
                    checked = currentSettings.swapControls,
                    tag = "swap_controls_switch",
                    onCheckedChange = { settingsRepository.updateSettings(currentSettings.copy(swapControls = it)) }
                )
            }

            // 2. AUDIO & CHIPTUNE SYNTH
            SettingsCard(
                title = "CHIPTUNE AUDIO & SYNTH",
                icon = Icons.Default.VolumeUp,
                accentColor = Color(0xFFEC4899)
            ) {
                // Music Volume
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Music Volume", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("${(currentSettings.musicVolume * 100).toInt()}%", color = Color(0xFFEC4899), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = currentSettings.musicVolume,
                        onValueChange = {
                            settingsRepository.updateSettings(currentSettings.copy(musicVolume = it))
                            audioSynth.musicVolume = it
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.testTag("music_volume_slider")
                    )
                }

                // SFX Volume
                Column(modifier = Modifier.padding(vertical = 4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Sound Effects (SFX) Volume", color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace)
                        Text("${(currentSettings.sfxVolume * 100).toInt()}%", color = Color(0xFFEC4899), fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = currentSettings.sfxVolume,
                        onValueChange = {
                            settingsRepository.updateSettings(currentSettings.copy(sfxVolume = it))
                            audioSynth.sfxVolume = it
                        },
                        valueRange = 0f..1f,
                        modifier = Modifier.testTag("sfx_volume_slider")
                    )
                }
            }

            // 3. GRAPHICS & RETRO EFFECTS
            SettingsCard(
                title = "RETRO GRAPHICS & DISPLAY",
                icon = Icons.Default.Tv,
                accentColor = Color(0xFF38BDF8)
            ) {
                SettingsSwitchRow(
                    label = "Retro CRT Scanlines & Vignette",
                    description = "Authentic 1980s cathode-ray tube phosphor scanlines",
                    checked = currentSettings.crtScanlines,
                    tag = "crt_scanlines_switch",
                    onCheckedChange = { settingsRepository.updateSettings(currentSettings.copy(crtScanlines = it)) }
                )

                SettingsSwitchRow(
                    label = "Explosion Screen Shake",
                    description = "Dynamic screen impact tremors during heavy blasts",
                    checked = currentSettings.screenShake,
                    tag = "screen_shake_switch",
                    onCheckedChange = { settingsRepository.updateSettings(currentSettings.copy(screenShake = it)) }
                )

                SettingsSwitchRow(
                    label = "Dark Arcade Mode",
                    description = "High-contrast dark neon arcade aesthetic",
                    checked = currentSettings.darkMode,
                    tag = "dark_mode_switch",
                    onCheckedChange = { settingsRepository.updateSettings(currentSettings.copy(darkMode = it)) }
                )
            }

            // 4. BIOMETRIC PRIVACY & SECURITY
            SettingsCard(
                title = "BIOMETRIC PRIVACY & SECURITY",
                icon = Icons.Default.Fingerprint,
                accentColor = Color(0xFF10B981)
            ) {
                SettingsSwitchRow(
                    label = "Biometric Lock (Fingerprint / Face ID)",
                    description = "Requires biometric authentication to load saves or modify Hall of Fame records",
                    checked = currentSettings.biometricSecurity,
                    tag = "biometric_switch",
                    onCheckedChange = { isEnabled ->
                        if (isEnabled && context is FragmentActivity) {
                            BiometricAuthManager.promptBiometric(
                                activity = context,
                                title = "Enable Biometric Security",
                                subtitle = "Scan fingerprint or face to lock arcade data",
                                onSuccess = {
                                    settingsRepository.updateSettings(currentSettings.copy(biometricSecurity = true))
                                    biometricMessage = "Biometric lock successfully activated!"
                                },
                                onError = { err ->
                                    biometricMessage = "Biometric setup failed: $err"
                                }
                            )
                        } else {
                            settingsRepository.updateSettings(currentSettings.copy(biometricSecurity = isEnabled))
                        }
                    }
                )

                if (currentSettings.biometricSecurity) {
                    Button(
                        onClick = {
                            if (context is FragmentActivity) {
                                BiometricAuthManager.promptBiometric(
                                    activity = context,
                                    title = "Biometric Test Verification",
                                    subtitle = "Confirm fingerprint or face recognition",
                                    onSuccess = { biometricMessage = "Authentication confirmed! Vault unlocked." },
                                    onError = { biometricMessage = "Authentication failed: $it" }
                                )
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp)
                            .testTag("test_biometric_btn")
                    ) {
                        Icon(imageVector = Icons.Default.Security, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("TEST BIOMETRIC VERIFICATION", fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    }
                }

                biometricMessage?.let {
                    Text(
                        text = it,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            // 5. PHYSICAL GAMEPAD & KEYBOARD GUIDE
            SettingsCard(
                title = "PHYSICAL CONTROLLER GUIDE",
                icon = Icons.Default.SportsEsports,
                accentColor = Color(0xFFFBBF24)
            ) {
                Text(
                    text = "• D-Pad / Analog Stick: 8-Directional Aim & Movement\n• Button A / B / Space / W: Jump & Somersault\n• Button X / Y / Trigger / J: Fire Weapon\n• Down + Jump: Drop down through one-way platforms\n• Down: Prone crawl under enemy bullets\n• Start / Esc: Pause mission\n• Multi-Controller: Automatically maps Controller 1 to Bill (1P) and Controller 2 to Lance (2P) for local co-op!",
                    color = Color(0xFFCBD5E1),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    lineHeight = 16.sp
                )
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFF0F172A))
            .border(1.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(10.dp))
            .padding(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = 10.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(18.dp))
            Text(
                text = title,
                color = accentColor,
                fontWeight = FontWeight.Black,
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            )
        }
        content()
    }
}

@Composable
fun SettingsSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    tag: String,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(text = label, color = Color.White, fontSize = 12.sp, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Bold)
            Text(text = description, color = Color(0xFF94A3B8), fontSize = 10.sp, fontFamily = FontFamily.Monospace)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            modifier = Modifier.testTag(tag),
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFEF4444)
            )
        )
    }
}
