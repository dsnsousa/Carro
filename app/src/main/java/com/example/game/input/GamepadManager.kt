package com.example.game.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class GamepadManager {
    var isGamepadConnected by mutableStateOf(false)
        private set
    var gamepadDeviceName by mutableStateOf("Nenhum")
        private set

    // Normalized Inputs: -1.0 .. +1.0 for steering, 0.0 .. 1.0 for pedals
    var steeringAxis by mutableFloatStateOf(0f)
    var throttleAxis by mutableFloatStateOf(0f)
    var brakeAxis by mutableFloatStateOf(0f)
    var isHandbrakePressed by mutableStateOf(false)
    var isNitroPressed by mutableStateOf(false)
    var isCameraToggleTriggered by mutableStateOf(false)
    var isPauseTriggered by mutableStateOf(false)

    // Discrete key tracking for keyboard & DPAD
    private var keyLeft = false
    private var keyRight = false
    private var keyUp = false
    private var keyDown = false

    fun checkConnectedControllers() {
        val deviceIds = InputDevice.getDeviceIds()
        var foundGamepad = false
        var name = "Nenhum"
        for (id in deviceIds) {
            val dev = InputDevice.getDevice(id) ?: continue
            val sources = dev.sources
            if ((sources and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD ||
                (sources and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK
            ) {
                foundGamepad = true
                name = dev.name
                break
            }
        }
        isGamepadConnected = foundGamepad
        gamepadDeviceName = if (foundGamepad) name else "Teclado / Virtual"
    }

    fun onGenericMotionEvent(event: MotionEvent): Boolean {
        if ((event.source and InputDevice.SOURCE_JOYSTICK) == InputDevice.SOURCE_JOYSTICK ||
            (event.source and InputDevice.SOURCE_GAMEPAD) == InputDevice.SOURCE_GAMEPAD
        ) {
            isGamepadConnected = true
            gamepadDeviceName = event.device?.name ?: "Controle USB/BT"

            // Left analog stick for steering
            val rawX = event.getAxisValue(MotionEvent.AXIS_X)
            val dpadX = event.getAxisValue(MotionEvent.AXIS_HAT_X)
            val effectiveX = when {
                Math.abs(rawX) > 0.15f -> rawX
                Math.abs(dpadX) > 0.15f -> dpadX
                else -> 0f
            }
            steeringAxis = effectiveX.coerceIn(-1.0f, 1.0f)

            // Right trigger or AXIS_GAS for throttle
            val rTrigger = event.getAxisValue(MotionEvent.AXIS_RTRIGGER)
            val gasAxis = event.getAxisValue(MotionEvent.AXIS_GAS)
            val analogY = -event.getAxisValue(MotionEvent.AXIS_Y) // Up on stick
            val effectiveThrottle = maxOf(rTrigger, gasAxis, if (analogY > 0.2f) analogY else 0f)
            if (effectiveThrottle > 0.05f) {
                throttleAxis = effectiveThrottle.coerceIn(0f, 1f)
            } else if (!keyUp) {
                throttleAxis = 0f
            }

            // Left trigger or AXIS_BRAKE for brake
            val lTrigger = event.getAxisValue(MotionEvent.AXIS_LTRIGGER)
            val brakeAx = event.getAxisValue(MotionEvent.AXIS_BRAKE)
            val reverseStick = event.getAxisValue(MotionEvent.AXIS_Y) // Down on stick
            val effectiveBrake = maxOf(lTrigger, brakeAx, if (reverseStick > 0.2f) reverseStick else 0f)
            if (effectiveBrake > 0.05f) {
                brakeAxis = effectiveBrake.coerceIn(0f, 1f)
            } else if (!keyDown) {
                brakeAxis = 0f
            }

            return true
        }
        return false
    }

    fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        var handled = false
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                keyLeft = true
                updateDiscreteSteering()
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                keyRight = true
                updateDiscreteSteering()
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R1 -> {
                keyUp = true
                throttleAxis = 1f
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_L1 -> {
                keyDown = true
                brakeAxis = 1f
                handled = true
            }
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_SPACE -> {
                isHandbrakePressed = true
                handled = true
            }
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_R2 -> {
                isNitroPressed = true
                handled = true
            }
            KeyEvent.KEYCODE_BUTTON_SELECT, KeyEvent.KEYCODE_C -> {
                isCameraToggleTriggered = true
                handled = true
            }
            KeyEvent.KEYCODE_BUTTON_START, KeyEvent.KEYCODE_ESCAPE -> {
                isPauseTriggered = true
                handled = true
            }
        }
        if (handled) {
            isGamepadConnected = true
        }
        return handled
    }

    fun onKeyUp(keyCode: Int, event: KeyEvent): Boolean {
        var handled = false
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_A -> {
                keyLeft = false
                updateDiscreteSteering()
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_D -> {
                keyRight = false
                updateDiscreteSteering()
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_BUTTON_A, KeyEvent.KEYCODE_BUTTON_R1 -> {
                keyUp = false
                if (throttleAxis >= 0.9f) throttleAxis = 0f
                handled = true
            }
            KeyEvent.KEYCODE_DPAD_DOWN, KeyEvent.KEYCODE_S, KeyEvent.KEYCODE_BUTTON_X, KeyEvent.KEYCODE_BUTTON_L1 -> {
                keyDown = false
                if (brakeAxis >= 0.9f) brakeAxis = 0f
                handled = true
            }
            KeyEvent.KEYCODE_BUTTON_B, KeyEvent.KEYCODE_SPACE -> {
                isHandbrakePressed = false
                handled = true
            }
            KeyEvent.KEYCODE_BUTTON_Y, KeyEvent.KEYCODE_SHIFT_LEFT, KeyEvent.KEYCODE_BUTTON_R2 -> {
                isNitroPressed = false
                handled = true
            }
        }
        return handled
    }

    private fun updateDiscreteSteering() {
        steeringAxis = when {
            keyLeft && !keyRight -> -1f
            keyRight && !keyLeft -> 1f
            else -> 0f
        }
    }

    fun reset() {
        steeringAxis = 0f
        throttleAxis = 0f
        brakeAxis = 0f
        isHandbrakePressed = false
        isNitroPressed = false
        isCameraToggleTriggered = false
        isPauseTriggered = false
        keyLeft = false
        keyRight = false
        keyUp = false
        keyDown = false
    }
}
