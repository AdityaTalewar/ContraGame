package com.example.ui

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.GameSettings
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

@Composable
fun TouchControlsOverlay(
    isCoop: Boolean,
    settings: GameSettings,
    onP1Move: (Float, Float) -> Unit,
    onP1Jump: (Boolean) -> Unit,
    onP1Fire: (Boolean) -> Unit,
    onP2Move: (Float, Float) -> Unit,
    onP2Jump: (Boolean) -> Unit,
    onP2Fire: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE)?.let {
                (it as android.os.VibratorManager).defaultVibrator
            } ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    fun vibrate(durationMs: Long = 20) {
        if (!settings.hapticFeedback) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            // Ignore vibration error
        }
    }

    val controlScale = settings.touchControlSize
    val alpha = settings.touchOpacity

    Box(
        modifier = modifier
            .fillMaxSize()
            .alpha(alpha)
            .padding(12.dp)
    ) {
        if (!isCoop) {
            // Solo Mode: Classic D-Pad on bottom-left, Action Buttons on bottom-right
            val leftAlign = if (settings.swapControls) Alignment.BottomEnd else Alignment.BottomStart
            val rightAlign = if (settings.swapControls) Alignment.BottomStart else Alignment.BottomEnd

            // D-Pad
            Box(
                modifier = Modifier
                    .align(leftAlign)
                    .padding(bottom = 8.dp)
            ) {
                VirtualDPad(
                    scale = controlScale,
                    tagPrefix = "p1_dpad",
                    onDirectionChange = { dx, dy -> onP1Move(dx, dy) }
                )
            }

            // Action Buttons (Fire / Jump)
            Box(
                modifier = Modifier
                    .align(rightAlign)
                    .padding(bottom = 8.dp)
            ) {
                ActionButtons(
                    scale = controlScale,
                    playerColor = Color(0xFFEF4444),
                    tagPrefix = "p1",
                    onJump = { pressed ->
                        if (pressed) vibrate(25)
                        onP1Jump(pressed)
                    },
                    onFire = { pressed ->
                        if (pressed) vibrate(15)
                        onP1Fire(pressed)
                    }
                )
            }
        } else {
            // 2-Player Co-op Mode on same screen:
            // Player 1 controls on Left side (Red)
            // Player 2 controls on Right side (Blue)
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .align(Alignment.BottomCenter),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                // Player 1 Cluster (D-Pad + Mini Fire/Jump)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "1P: BILL",
                        color = Color(0xFFEF4444),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VirtualDPad(
                            scale = controlScale * 0.85f,
                            tagPrefix = "p1_coop_dpad",
                            onDirectionChange = { dx, dy -> onP1Move(dx, dy) }
                        )
                        ActionButtons(
                            scale = controlScale * 0.85f,
                            playerColor = Color(0xFFEF4444),
                            tagPrefix = "p1_coop",
                            onJump = { onP1Jump(it) },
                            onFire = { onP1Fire(it) }
                        )
                    }
                }

                // Player 2 Cluster (D-Pad + Mini Fire/Jump)
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "2P: LANCE",
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionButtons(
                            scale = controlScale * 0.85f,
                            playerColor = Color(0xFF38BDF8),
                            tagPrefix = "p2_coop",
                            onJump = { onP2Jump(it) },
                            onFire = { onP2Fire(it) }
                        )
                        VirtualDPad(
                            scale = controlScale * 0.85f,
                            tagPrefix = "p2_coop_dpad",
                            onDirectionChange = { dx, dy -> onP2Move(dx, dy) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VirtualDPad(
    scale: Float,
    tagPrefix: String,
    onDirectionChange: (Float, Float) -> Unit,
    modifier: Modifier = Modifier
) {
    val sizeDp = (140 * scale).dp
    var thumbOffset by remember { mutableStateOf(androidx.compose.ui.geometry.Offset.Zero) }

    Box(
        modifier = modifier
            .size(sizeDp)
            .clip(CircleShape)
            .background(Color(0xFF0F172A).copy(alpha = 0.85f))
            .border(2.dp, Color(0xFF334155), CircleShape)
            .testTag(tagPrefix)
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { offset ->
                        val center = size.width / 2f
                        val dx = offset.x - center
                        val dy = offset.y - center
                        val dist = sqrt(dx * dx + dy * dy)
                        val maxRadius = center * 0.8f
                        val clampedDist = minOf(dist, maxRadius)
                        val angle = atan2(dy, dx)
                        val normX = (cos(angle) * (clampedDist / maxRadius)).coerceIn(-1f, 1f)
                        val normY = (sin(angle) * (clampedDist / maxRadius)).coerceIn(-1f, 1f)
                        thumbOffset = androidx.compose.ui.geometry.Offset(normX * maxRadius, normY * maxRadius)
                        onDirectionChange(normX, normY)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val center = size.width / 2f
                        val dx = change.position.x - center
                        val dy = change.position.y - center
                        val dist = sqrt(dx * dx + dy * dy)
                        val maxRadius = center * 0.8f
                        val clampedDist = minOf(dist, maxRadius)
                        val angle = atan2(dy, dx)
                        val normX = (cos(angle) * (clampedDist / maxRadius)).coerceIn(-1f, 1f)
                        val normY = (sin(angle) * (clampedDist / maxRadius)).coerceIn(-1f, 1f)
                        thumbOffset = androidx.compose.ui.geometry.Offset(normX * maxRadius, normY * maxRadius)
                        onDirectionChange(normX, normY)
                    },
                    onDragEnd = {
                        thumbOffset = androidx.compose.ui.geometry.Offset.Zero
                        onDirectionChange(0f, 0f)
                    },
                    onDragCancel = {
                        thumbOffset = androidx.compose.ui.geometry.Offset.Zero
                        onDirectionChange(0f, 0f)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        // Cross guides
        Icon(
            imageVector = Icons.Default.North,
            contentDescription = "Up",
            tint = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 4.dp)
                .size((20 * scale).dp)
        )
        Icon(
            imageVector = Icons.Default.South,
            contentDescription = "Down",
            tint = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 4.dp)
                .size((20 * scale).dp)
        )
        Icon(
            imageVector = Icons.Default.West,
            contentDescription = "Left",
            tint = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 4.dp)
                .size((20 * scale).dp)
        )
        Icon(
            imageVector = Icons.Default.East,
            contentDescription = "Right",
            tint = Color(0xFF64748B),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 4.dp)
                .size((20 * scale).dp)
        )

        // Center stick thumb
        Box(
            modifier = Modifier
                .offset(x = thumbOffset.x.dp, y = thumbOffset.y.dp)
                .size((48 * scale).dp)
                .clip(CircleShape)
                .background(Color(0xFFEF4444).copy(alpha = 0.9f))
                .border(2.dp, Color.White, CircleShape)
        )
    }
}

@Composable
fun ActionButtons(
    scale: Float,
    playerColor: Color,
    tagPrefix: String,
    onJump: (Boolean) -> Unit,
    onFire: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val btnSize = (64 * scale).dp

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy((14 * scale).dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // B Button (Jump)
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(CircleShape)
                .background(Color(0xFF1E293B).copy(alpha = 0.85f))
                .border(2.dp, Color(0xFF38BDF8), CircleShape)
                .testTag("${tagPrefix}_jump_btn")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onJump(true)
                            tryAwaitRelease()
                            onJump(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "B",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (20 * scale).sp
                )
                Text(
                    text = "JUMP",
                    color = Color(0xFF38BDF8),
                    fontSize = (9 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // A Button (Fire)
        Box(
            modifier = Modifier
                .size(btnSize)
                .clip(CircleShape)
                .background(playerColor.copy(alpha = 0.85f))
                .border(2.dp, Color.White, CircleShape)
                .testTag("${tagPrefix}_fire_btn")
                .pointerInput(Unit) {
                    detectTapGestures(
                        onPress = {
                            onFire(true)
                            tryAwaitRelease()
                            onFire(false)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "A",
                    color = Color.White,
                    fontWeight = FontWeight.Black,
                    fontSize = (20 * scale).sp
                )
                Text(
                    text = "FIRE",
                    color = Color.White.copy(alpha = 0.9f),
                    fontSize = (9 * scale).sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
