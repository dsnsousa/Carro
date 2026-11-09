package com.example.game.input

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

class TouchInputState {
    var steeringAxis by mutableFloatStateOf(0f)
    var throttlePedal by mutableFloatStateOf(0f)
    var brakePedal by mutableFloatStateOf(0f)
    var isHandbrakeActive by mutableStateOf(false)
    var isNitroActive by mutableStateOf(false)
    var isCameraToggleRequested by mutableStateOf(false)

    // Touch button flags
    var isLeftPressed by mutableStateOf(false)
    var isRightPressed by mutableStateOf(false)
    var wheelAngleDegrees by mutableFloatStateOf(0f) // -90..+90
    var gyroTiltAxis by mutableFloatStateOf(0f)

    fun updateSteeringFromButtons() {
        steeringAxis = when {
            isLeftPressed && !isRightPressed -> -1f
            isRightPressed && !isLeftPressed -> 1f
            else -> 0f
        }
    }

    fun updateSteeringFromWheel(angleDeg: Float) {
        wheelAngleDegrees = angleDeg.coerceIn(-90f, 90f)
        steeringAxis = (wheelAngleDegrees / 90f).coerceIn(-1f, 1f)
    }

    fun updateSteeringFromTilt(tilt: Float, sensitivity: Float) {
        gyroTiltAxis = (tilt * sensitivity * 0.25f).coerceIn(-1f, 1f)
        steeringAxis = gyroTiltAxis
    }

    fun reset() {
        steeringAxis = 0f
        throttlePedal = 0f
        brakePedal = 0f
        isHandbrakeActive = false
        isNitroActive = false
        isLeftPressed = false
        isRightPressed = false
        wheelAngleDegrees = 0f
        gyroTiltAxis = 0f
    }
}
