package com.example.ui

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import com.example.game.GameEngine

class ControllerManager(private val engine: GameEngine) {

    private val p1DeviceIds = mutableSetOf<Int>()
    private val p2DeviceIds = mutableSetOf<Int>()

    fun handleKeyEvent(event: KeyEvent): Boolean {
        val deviceId = event.deviceId
        val isDown = event.action == KeyEvent.ACTION_DOWN

        // Determine if player 1 or player 2
        val isPlayer2 = if (engine.isCoop) {
            if (p2DeviceIds.contains(deviceId)) true
            else if (!p1DeviceIds.contains(deviceId) && p1DeviceIds.isNotEmpty()) {
                p2DeviceIds.add(deviceId)
                true
            } else {
                p1DeviceIds.add(deviceId)
                false
            }
        } else {
            p1DeviceIds.add(deviceId)
            false
        }

        when (event.keyCode) {
            // Player Jump (Button A or B on standard controller, or Space / Z / Up arrow on keyboard)
            KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_W -> {
                if (isPlayer2) engine.p2JumpPressed = isDown else engine.p1JumpPressed = isDown
                return true
            }

            // Player Fire (Button X or Y or Trigger, or J / X / C on keyboard)
            KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_Y,
            KeyEvent.KEYCODE_BUTTON_R1, KeyEvent.KEYCODE_BUTTON_R2,
            KeyEvent.KEYCODE_J, KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_ENTER -> {
                if (isPlayer2) engine.p2FirePressed = isDown else engine.p1FirePressed = isDown
                return true
            }

            // D-Pad buttons
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (isPlayer2) engine.p2MoveY = if (isDown) -1f else 0f else engine.p1MoveY = if (isDown) -1f else 0f
                return true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (isPlayer2) engine.p2MoveY = if (isDown) 1f else 0f else engine.p1MoveY = if (isDown) 1f else 0f
                return true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (isPlayer2) engine.p1MoveX = if (isDown) -1f else 0f else engine.p1MoveX = if (isDown) -1f else 0f
                return true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (isPlayer2) engine.p2MoveX = if (isDown) 1f else 0f else engine.p1MoveX = if (isDown) 1f else 0f
                return true
            }

            // Pause
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ESCAPE -> {
                if (isDown) engine.isPaused = !engine.isPaused
                return true
            }
        }
        return false
    }

    fun handleMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        ) {
            val deviceId = event.deviceId
            val isPlayer2 = engine.isCoop && p2DeviceIds.contains(deviceId)

            // Left Stick & D-Pad Hat
            var xAxis = event.getAxisValue(MotionEvent.AXIS_X)
            var yAxis = event.getAxisValue(MotionEvent.AXIS_Y)
            val hatX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val hatY = event.getAxisValue(MotionEvent.AXIS_HAT_Y)

            if (kotlin.math.abs(hatX) > 0.1f) xAxis = hatX
            if (kotlin.math.abs(hatY) > 0.1f) yAxis = hatY

            // Deadzone
            if (kotlin.math.abs(xAxis) < 0.2f) xAxis = 0f
            if (kotlin.math.abs(yAxis) < 0.2f) yAxis = 0f

            if (isPlayer2) {
                engine.p2MoveX = xAxis
                engine.p2MoveY = yAxis
            } else {
                engine.p1MoveX = xAxis
                engine.p1MoveY = yAxis
            }
            return true
        }
        return false
    }
}
