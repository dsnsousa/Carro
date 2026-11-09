package com.example.game.engine

import com.example.game.customization.VehicleConfig
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class CarPhysics(
    val config: VehicleConfig,
    var isPlayer: Boolean = true,
    var racerName: String = "Você"
) {
    // Track position: trackDistance is along track Z (0..totalTrackLength)
    var trackDistance: Float = 0f
    var playerX: Float = 0f // -1.0 = left curb, 0.0 = center, +1.0 = right curb

    // Speed in km/h
    var currentSpeedKmH: Float = 0f
    var topSpeedKmH: Float = config.effectiveTopSpeed

    // Physics parameters
    var accelerationRate: Float = config.effectiveAcceleration
    var handlingFactor: Float = config.effectiveHandling

    // Drift state
    var isDrifting: Boolean = false
    var driftAngle: Float = 0f // -1.0 .. +1.0
    var currentDriftScore: Int = 0
    var totalDriftScore: Int = 0
    var driftMultiplier: Float = 1.0f

    // Nitro state
    var nitroGauge: Float = config.effectiveNitroCapacity // 0..max
    var maxNitroGauge: Float = config.effectiveNitroCapacity
    var isNitroActive: Boolean = false
    var nitroBoostTimer: Float = 0f

    // Gear & RPM
    var currentGear: Int = 1
    var currentRpmRatio: Float = 0.2f // 0.0 to 1.0

    // Race progress
    var currentLap: Int = 1
    var lapStartTimeMs: Long = 0L
    var bestLapTimeMs: Long = 0L
    var lastLapTimeMs: Long = 0L
    var raceFinished: Boolean = false
    var totalRaceTimeMs: Long = 0L
    var currentRacePosition: Int = 1

    // Visual animation states
    var chassisRoll: Float = 0f // tilts on cornering/drifting
    var suspensionPitch: Float = 0f // dips on braking, lifts on acceleration
    var collisionSparksTimer: Float = 0f

    fun update(
        dtSeconds: Float,
        throttleInput: Float,
        brakeInput: Float,
        steeringInput: Float,
        handbrakeInput: Boolean,
        nitroInput: Boolean,
        currentSegmentCurve: Float,
        roadGripFactor: Float,
        totalTrackLength: Float,
        maxLaps: Int
    ) {
        if (raceFinished) {
            // Decelerate smoothly after finish
            currentSpeedKmH = max(0f, currentSpeedKmH - 80f * dtSeconds)
            updateRpmAndGear()
            return
        }

        val onRoad = abs(playerX) <= 1.0f
        val surfaceDrag = if (onRoad) 1.0f else 2.6f // Grass slows down car

        // Nitro handling
        if (nitroInput && nitroGauge > 2f && currentSpeedKmH > 20f) {
            isNitroActive = true
            nitroGauge = max(0f, nitroGauge - 25f * dtSeconds)
        } else {
            isNitroActive = false
            // Slow passive recharge + recharge bonus from drifting
            val recharge = if (isDrifting) 18f * dtSeconds else 4f * dtSeconds
            nitroGauge = min(maxNitroGauge, nitroGauge + recharge)
        }

        // Acceleration & Speed Calculation
        val effectiveTopSpeed = if (isNitroActive) topSpeedKmH * 1.22f else topSpeedKmH
        val nitroAccelBonus = if (isNitroActive) 18.0f else 0f

        if (throttleInput > 0.05f) {
            val speedRatio = (currentSpeedKmH / effectiveTopSpeed).coerceIn(0f, 1f)
            val powerCurve = (1.0f - speedRatio * 0.7f) // High acceleration at lower speeds
            val accel = (accelerationRate + nitroAccelBonus) * powerCurve * throttleInput * surfaceDrag.let { 1f / it }
            currentSpeedKmH = min(effectiveTopSpeed, currentSpeedKmH + accel * 3.6f * dtSeconds)
            suspensionPitch = (suspensionPitch + 0.15f * throttleInput).coerceAtMost(1.0f)
        } else {
            // Natural engine friction & air drag
            val naturalDecel = (15f + (currentSpeedKmH * currentSpeedKmH * 0.00035f)) * surfaceDrag
            currentSpeedKmH = max(0f, currentSpeedKmH - naturalDecel * dtSeconds)
            suspensionPitch *= 0.85f
        }

        // Braking / Reversing
        if (brakeInput > 0.05f) {
            if (currentSpeedKmH > 3f) {
                val brakeForce = 45f * brakeInput * roadGripFactor
                currentSpeedKmH = max(0f, currentSpeedKmH - brakeForce * 3.6f * dtSeconds)
                suspensionPitch = (suspensionPitch - 0.25f * brakeInput).coerceAtLeast(-1.0f)
            } else {
                // Reverse gear
                currentSpeedKmH = max(-45f, currentSpeedKmH - 15f * brakeInput * dtSeconds)
            }
        }

        // Steering & Centrifugal Force
        val speedFactor = (abs(currentSpeedKmH) / topSpeedKmH).coerceIn(0.05f, 1.0f)
        val steerResponsiveness = handlingFactor * (1.2f - speedFactor * 0.35f) * roadGripFactor

        // Drift logic
        val isInitiatingDrift = handbrakeInput && abs(steeringInput) > 0.2f && currentSpeedKmH > 60f
        if (isInitiatingDrift || (isDrifting && (abs(steeringInput) > 0.1f || handbrakeInput) && currentSpeedKmH > 40f)) {
            isDrifting = true
            val targetDriftAngle = if (steeringInput != 0f) Math.signum(steeringInput) else Math.signum(currentSegmentCurve)
            driftAngle += (targetDriftAngle - driftAngle) * 0.15f
            currentDriftScore += (abs(driftAngle) * currentSpeedKmH * 0.5f * dtSeconds).toInt()
            driftMultiplier = min(4.0f, driftMultiplier + 0.3f * dtSeconds)
        } else {
            if (isDrifting && currentDriftScore > 0) {
                totalDriftScore += (currentDriftScore * driftMultiplier).toInt()
                currentDriftScore = 0
                driftMultiplier = 1.0f
            }
            isDrifting = false
            driftAngle *= 0.85f
        }

        // Lateral track offset
        val steerDelta = steeringInput * steerResponsiveness * (currentSpeedKmH / 100f) * 1.4f * dtSeconds
        // Centrifugal push: curves push car to outside of turn
        val centrifugalPush = (currentSegmentCurve * (currentSpeedKmH / 150f) * 1.8f) * dtSeconds

        playerX += steerDelta - centrifugalPush

        // Chassis roll animation
        val targetRoll = (steeringInput * 0.5f + (driftAngle * 0.6f)).coerceIn(-1f, 1f)
        chassisRoll += (targetRoll - chassisRoll) * 0.2f

        // Collision with track borders
        if (abs(playerX) > 1.85f) {
            // Guardrail collision bounce
            playerX = Math.signum(playerX) * 1.82f
            currentSpeedKmH = max(20f, currentSpeedKmH * 0.72f)
            collisionSparksTimer = 0.35f
        }

        if (collisionSparksTimer > 0f) {
            collisionSparksTimer -= dtSeconds
        }

        // Advance distance
        val distanceDelta = (currentSpeedKmH / 3.6f) * dtSeconds
        trackDistance += distanceDelta

        // Lap completion check
        if (trackDistance >= totalTrackLength) {
            trackDistance -= totalTrackLength
            val lapTime = System.currentTimeMillis() - lapStartTimeMs
            lastLapTimeMs = lapTime
            if (bestLapTimeMs == 0L || lapTime < bestLapTimeMs) {
                bestLapTimeMs = lapTime
            }
            lapStartTimeMs = System.currentTimeMillis()

            if (currentLap < maxLaps) {
                currentLap++
            } else {
                raceFinished = true
            }
        }

        updateRpmAndGear()
    }

    private fun updateRpmAndGear() {
        val speed = abs(currentSpeedKmH)
        val gearMax = topSpeedKmH / 6f
        currentGear = ((speed / gearMax).toInt() + 1).coerceIn(1, 6)

        val gearMinSpeed = (currentGear - 1) * gearMax
        val gearSpeedRange = gearMax
        val inGearRatio = ((speed - gearMinSpeed) / gearSpeedRange).coerceIn(0f, 1f)

        currentRpmRatio = (0.25f + inGearRatio * 0.75f).coerceIn(0.2f, 1.0f)
    }

    fun applyNitroPad() {
        currentSpeedKmH = min(topSpeedKmH * 1.25f, currentSpeedKmH + 45f)
        isNitroActive = true
        nitroGauge = min(maxNitroGauge, nitroGauge + 30f)
    }

    fun applyObstacleHit() {
        currentSpeedKmH = max(20f, currentSpeedKmH * 0.55f)
        collisionSparksTimer = 0.5f
    }
}
