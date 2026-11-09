package com.example.ui.screens

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.VolumeOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.audio.GameAudioSynthesizer
import com.example.game.customization.GameStorage
import com.example.game.customization.TouchControlScheme
import com.example.game.customization.TrackConfig
import com.example.game.customization.VehicleConfig
import com.example.game.engine.CameraViewMode
import com.example.game.engine.CarPhysics
import com.example.game.engine.RacingRenderer3D
import com.example.game.engine.TrackBuilder
import com.example.game.input.GamepadManager
import com.example.game.input.TouchInputState
import com.example.game.multiplayer.MultiplayerEngine
import com.example.game.multiplayer.MultiplayerMode
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.RacingRed
import com.example.ui.theme.SpeedYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

@Composable
fun RacingScreen(
    storage: GameStorage,
    playerVehicle: VehicleConfig,
    trackConfig: TrackConfig,
    multiplayerEngine: MultiplayerEngine,
    multiplayerMode: MultiplayerMode,
    gamepadManager: GamepadManager,
    audioSynthesizer: GameAudioSynthesizer,
    onExitRace: () -> Unit,
    onOpenGarage: () -> Unit
) {
    val context = LocalContext.current
    val vibrator = remember { context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator }

    // Track & 3D Renderer initialization
    val trackSegments = remember(trackConfig) { TrackBuilder.buildTrack(trackConfig) }
    val renderer = remember(trackSegments, trackConfig) { RacingRenderer3D(trackSegments, trackConfig) }

    // Racers grid
    val racers = remember(playerVehicle, trackConfig, multiplayerMode) {
        mutableStateListOf<CarPhysics>().apply {
            addAll(multiplayerEngine.createRacersGrid(playerVehicle, storage.getPlayerName(), multiplayerMode))
        }
    }
    val playerCar = racers.first { it.isPlayer }
    val player2Car = racers.getOrNull(1)?.takeIf { multiplayerMode == MultiplayerMode.SPLIT_SCREEN }

    // Control & UI states
    val touchInputState = remember { TouchInputState() }
    val controlScheme = remember { storage.getControlScheme() }
    var cameraMode by remember { mutableStateOf(CameraViewMode.CHASE_CAM) }
    var isPaused by remember { mutableStateOf(false) }
    var isMuted by remember { mutableStateOf(!storage.isAudioEnabled()) }

    // Start Countdown (3, 2, 1, GO!)
    var countdownValue by remember { mutableIntStateOf(3) }
    var countdownText by remember { mutableStateOf("3") }
    var isRacingStarted by remember { mutableStateOf(false) }

    val totalTrackZ = remember(trackSegments) { trackSegments.size * TrackBuilder.SEGMENT_LENGTH }

    // Start Audio
    DisposableEffect(Unit) {
        audioSynthesizer.isMuted = isMuted
        audioSynthesizer.start()
        onDispose {
            audioSynthesizer.stop()
        }
    }

    // Countdown Timer Coroutine
    LaunchedEffect(Unit) {
        for (c in 3 downTo 1) {
            countdownValue = c
            countdownText = c.toString()
            audioSynthesizer.playCountdownBeep(highPitch = false)
            kotlinx.coroutines.delay(850)
        }
        countdownValue = 0
        countdownText = "VAI!"
        audioSynthesizer.playCountdownBeep(highPitch = true)
        isRacingStarted = true
        playerCar.lapStartTimeMs = System.currentTimeMillis()
        kotlinx.coroutines.delay(650)
        countdownText = ""
    }

    // Main 60 FPS Game Loop
    LaunchedEffect(isPaused, isRacingStarted) {
        if (!isRacingStarted) return@LaunchedEffect

        var lastFrameNanos = System.nanoTime()

        while (true) {
            withFrameNanos { currentNanos ->
                val dt = ((currentNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.005f, 0.05f)
                lastFrameNanos = currentNanos

                if (!isPaused) {
                    // Check Camera toggle from Gamepad or Touch
                    if (gamepadManager.isCameraToggleTriggered || touchInputState.isCameraToggleRequested) {
                        cameraMode = when (cameraMode) {
                            CameraViewMode.CHASE_CAM -> CameraViewMode.CLOSE_CAM
                            CameraViewMode.CLOSE_CAM -> CameraViewMode.HOOD_CAM
                            CameraViewMode.HOOD_CAM -> CameraViewMode.CHASE_CAM
                        }
                        gamepadManager.isCameraToggleTriggered = false
                        touchInputState.isCameraToggleRequested = false
                    }

                    if (gamepadManager.isPauseTriggered) {
                        isPaused = !isPaused
                        gamepadManager.isPauseTriggered = false
                    }

                    // Combined Inputs: Gamepad takes priority, fallback to Touch
                    val effectiveThrottle = max(gamepadManager.throttleAxis, touchInputState.throttlePedal)
                    val effectiveBrake = max(gamepadManager.brakeAxis, touchInputState.brakePedal)
                    val effectiveSteer = if (Math.abs(gamepadManager.steeringAxis) > 0.05f) {
                        gamepadManager.steeringAxis
                    } else {
                        touchInputState.steeringAxis
                    }
                    val effectiveHandbrake = gamepadManager.isHandbrakePressed || touchInputState.isHandbrakeActive
                    val effectiveNitro = gamepadManager.isNitroPressed || touchInputState.isNitroActive

                    // Current track segment curve & elevation
                    val playerSegIdx = ((playerCar.trackDistance / TrackBuilder.SEGMENT_LENGTH).toInt()) % trackSegments.size
                    val playerSegment = trackSegments[playerSegIdx]

                    // Check Nitro Pad hit
                    if (playerSegment.hasNitroPad && Math.abs(playerCar.playerX - playerSegment.nitroPadOffset) < 0.35f) {
                        playerCar.applyNitroPad()
                    }
                    // Check Obstacle hit
                    if (playerSegment.hasObstacle && Math.abs(playerCar.playerX - playerSegment.obstacleOffset) < 0.28f) {
                        playerCar.applyObstacleHit()
                        audioSynthesizer.triggerCrash()
                        if (storage.isVibrationEnabled()) {
                            vibrator?.let { v ->
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                    v.vibrate(VibrationEffect.createOneShot(120, VibrationEffect.DEFAULT_AMPLITUDE))
                                } else {
                                    v.vibrate(120)
                                }
                            }
                        }
                    }

                    // Update Player Car Physics
                    playerCar.update(
                        dtSeconds = dt,
                        throttleInput = effectiveThrottle,
                        brakeInput = effectiveBrake,
                        steeringInput = effectiveSteer,
                        handbrakeInput = effectiveHandbrake,
                        nitroInput = effectiveNitro,
                        currentSegmentCurve = playerSegment.curve,
                        roadGripFactor = trackConfig.weather.roadGripMultiplier,
                        totalTrackLength = totalTrackZ,
                        maxLaps = trackConfig.laps
                    )

                    // Record Ghost telemetry
                    if (multiplayerMode == MultiplayerMode.GHOST_BATTLE) {
                        multiplayerEngine.recordGhostFrame(
                            playerCar.trackDistance,
                            playerCar.playerX,
                            playerCar.currentSpeedKmH,
                            playerCar.driftAngle
                        )
                    }

                    // Update AI Rival Racers
                    multiplayerEngine.updateAiRacers(
                        dtSeconds = dt,
                        racers = racers,
                        totalTrackZ = totalTrackZ,
                        currentSegmentCurve = playerSegment.curve,
                        roadGripFactor = trackConfig.weather.roadGripMultiplier,
                        maxLaps = trackConfig.laps
                    )

                    // Calculate live race positions
                    val sortedRacers = racers.sortedByDescending { it.trackDistance + ((it.currentLap - 1) * totalTrackZ) }
                    for ((index, racer) in sortedRacers.withIndex()) {
                        racer.currentRacePosition = index + 1
                    }

                    // Feed audio synthesizer parameters
                    audioSynthesizer.targetRpmRatio = playerCar.currentRpmRatio
                    audioSynthesizer.isDrifting = playerCar.isDrifting
                    audioSynthesizer.isNitroActive = playerCar.isNitroActive

                    // Drift vibration haptics
                    if (playerCar.isDrifting && storage.isVibrationEnabled()) {
                        vibrator?.let { v ->
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                v.vibrate(VibrationEffect.createOneShot(30, 80))
                            }
                        }
                    }

                    // Check race finish
                    if (playerCar.raceFinished) {
                        storage.setBestLapTimeMs(trackConfig.id, playerCar.bestLapTimeMs)
                        storage.setHighestDriftScore(playerCar.totalDriftScore)
                    }
                }
            }
        }
    }

    // Screen Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. Primary 3D Canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            renderer.render(
                drawScope = this,
                playerCar = playerCar,
                opponentCars = racers,
                cameraViewMode = cameraMode
            )
        }

        // 2. Head-Up Display (HUD)
        RacingHUD(
            playerCar = playerCar,
            trackConfig = trackConfig,
            gamepadManager = gamepadManager,
            cameraMode = cameraMode,
            onToggleCamera = {
                cameraMode = when (cameraMode) {
                    CameraViewMode.CHASE_CAM -> CameraViewMode.CLOSE_CAM
                    CameraViewMode.CLOSE_CAM -> CameraViewMode.HOOD_CAM
                    CameraViewMode.HOOD_CAM -> CameraViewMode.CHASE_CAM
                }
            },
            onPauseClick = { isPaused = true },
            modifier = Modifier.fillMaxSize()
        )

        // 3. On-Screen Touch Controls (Left: Steering, Right: Pedals & Nitro)
        if (!isPaused && !playerCar.raceFinished) {
            OnScreenControlsOverlay(
                touchState = touchInputState,
                controlScheme = controlScheme,
                modifier = Modifier.fillMaxSize()
            )
        }

        // 4. Start Countdown Overlay
        AnimatedVisibility(
            visible = countdownText.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Text(
                text = countdownText,
                fontSize = 80.sp,
                fontWeight = FontWeight.Black,
                color = if (countdownText == "VAI!") Color(0xFF00E676) else SpeedYellow,
                letterSpacing = 4.sp
            )
        }

        // 5. Pause Dialog Menu
        if (isPaused) {
            PauseMenuDialog(
                onResume = { isPaused = false },
                onRestart = {
                    playerCar.trackDistance = 150f
                    playerCar.currentSpeedKmH = 0f
                    playerCar.currentLap = 1
                    playerCar.raceFinished = false
                    isPaused = false
                },
                onExit = onExitRace,
                onGarage = onOpenGarage
            )
        }

        // 6. Post-Race Victory Podium Dialog
        if (playerCar.raceFinished) {
            VictoryPodiumDialog(
                playerCar = playerCar,
                trackConfig = trackConfig,
                onRestart = {
                    playerCar.trackDistance = 150f
                    playerCar.currentSpeedKmH = 0f
                    playerCar.currentLap = 1
                    playerCar.raceFinished = false
                },
                onGarage = onOpenGarage,
                onExit = onExitRace
            )
        }
    }
}

@Composable
private fun RacingHUD(
    playerCar: CarPhysics,
    trackConfig: TrackConfig,
    gamepadManager: GamepadManager,
    cameraMode: CameraViewMode,
    onToggleCamera: () -> Unit,
    onPauseClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.padding(14.dp)) {
        // Top Left: Race Position, Lap Counter, Timer
        Row(
            modifier = Modifier.align(Alignment.TopStart),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Position Badge
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.5.dp, SpeedYellow, RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${playerCar.currentRacePosition}º",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = SpeedYellow
                    )
                    Text(
                        text = " / 6",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextSecondary,
                        modifier = Modifier.padding(bottom = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Lap Counter
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.75f))
                    .border(1.dp, DarkBorder, RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "VOLTA ${playerCar.currentLap}/${trackConfig.laps}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = NeonCyan
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Gamepad Connected Indicator Badge
            if (gamepadManager.isGamepadConnected) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF0D251C).copy(alpha = 0.85f))
                        .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = Color(0xFF00E676),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "CONTROLE ATIVO",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E676)
                        )
                    }
                }
            }
        }

        // Top Right: Camera Switch & Pause Buttons
        Row(
            modifier = Modifier.align(Alignment.TopEnd),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleCamera,
                modifier = Modifier
                    .testTag("hud_camera_button")
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Mudar Câmera", tint = TextPrimary)
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onPauseClick,
                modifier = Modifier
                    .testTag("hud_pause_button")
                    .background(Color.Black.copy(alpha = 0.7f), CircleShape)
            ) {
                Icon(Icons.Default.Pause, contentDescription = "Pausar", tint = TextPrimary)
            }
        }

        // Center Top: Drift Score Multiplier Banner
        if (playerCar.isDrifting || playerCar.totalDriftScore > 0) {
            Column(
                modifier = Modifier.align(Alignment.TopCenter),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "DRIFT +${playerCar.currentDriftScore} x${String.format("%.1f", playerCar.driftMultiplier)}!",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Black,
                    color = NitroOrange,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "TOTAL: ${playerCar.totalDriftScore} PTS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpeedYellow
                )
            }
        }

        // Bottom Center: Realistic Digital Speedometer & Tachometer
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 8.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color.Black.copy(alpha = 0.8f))
                .border(1.5.dp, NeonCyan.copy(alpha = 0.6f), RoundedCornerShape(16.dp))
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "${playerCar.currentSpeedKmH.toInt()}",
                        fontSize = 38.sp,
                        fontWeight = FontWeight.Black,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "KM/H",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }

                // RPM / Tachometer Curved Bar
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "M${playerCar.currentGear}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        color = SpeedYellow
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { playerCar.currentRpmRatio },
                        modifier = Modifier
                            .width(130.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = if (playerCar.currentRpmRatio > 0.85f) RacingRed else NeonCyan,
                        trackColor = Color(0xFF1E283C)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Nitro NOS Gauge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = if (playerCar.isNitroActive) NeonCyan else TextSecondary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    LinearProgressIndicator(
                        progress = { playerCar.nitroGauge / playerCar.maxNitroGauge },
                        modifier = Modifier
                            .width(130.dp)
                            .height(6.dp)
                            .clip(RoundedCornerShape(3.dp)),
                        color = NitroOrange,
                        trackColor = Color(0xFF1E283C)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnScreenControlsOverlay(
    touchState: TouchInputState,
    controlScheme: TouchControlScheme,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Bottom
    ) {
        // Left Side: Steering Controls
        when (controlScheme) {
            TouchControlScheme.STEERING_WHEEL -> {
                // Interactive Touch Steering Wheel
                var wheelRotation by remember { mutableFloatStateOf(0f) }
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .testTag("touch_steering_wheel")
                        .pointerInput(Unit) {
                            detectDragGestures(
                                onDragEnd = {
                                    wheelRotation = 0f
                                    touchState.updateSteeringFromWheel(0f)
                                },
                                onDragCancel = {
                                    wheelRotation = 0f
                                    touchState.updateSteeringFromWheel(0f)
                                }
                            ) { change, dragAmount ->
                                change.consume()
                                wheelRotation = (wheelRotation + dragAmount.x * 0.7f).coerceIn(-90f, 90f)
                                touchState.updateSteeringFromWheel(wheelRotation)
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxSize()
                            .rotate(wheelRotation)
                    ) {
                        val center = Offset(size.width * 0.5f, size.height * 0.5f)
                        val radius = size.width * 0.44f

                        // Outer Wheel Rim
                        drawCircle(
                            color = Color(0xFF181D28).copy(alpha = 0.85f),
                            radius = radius,
                            center = center
                        )
                        drawCircle(
                            color = NeonCyan,
                            radius = radius,
                            center = center,
                            style = Stroke(width = 12f)
                        )
                        // Top Center Red Stripe Marker
                        drawCircle(
                            color = RacingRed,
                            radius = 6f,
                            center = Offset(center.x, center.y - radius)
                        )
                        // Center Hub
                        drawCircle(
                            color = Color(0xFF0F121C),
                            radius = radius * 0.45f,
                            center = center
                        )
                    }
                }
            }

            TouchControlScheme.TOUCH_BUTTONS -> {
                // Left & Right Arrow Touch Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.padding(bottom = 12.dp)
                ) {
                    // Left Arrow Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("touch_arrow_left")
                            .clip(CircleShape)
                            .background(
                                if (touchState.isLeftPressed) NeonCyan.copy(alpha = 0.7f)
                                else Color.Black.copy(alpha = 0.6f)
                            )
                            .border(2.dp, NeonCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        touchState.isLeftPressed = true
                                        touchState.updateSteeringFromButtons()
                                        tryAwaitRelease()
                                        touchState.isLeftPressed = false
                                        touchState.updateSteeringFromButtons()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronLeft,
                            contentDescription = "Esquerda",
                            tint = if (touchState.isLeftPressed) Color.Black else TextPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }

                    // Right Arrow Button
                    Box(
                        modifier = Modifier
                            .size(76.dp)
                            .testTag("touch_arrow_right")
                            .clip(CircleShape)
                            .background(
                                if (touchState.isRightPressed) NeonCyan.copy(alpha = 0.7f)
                                else Color.Black.copy(alpha = 0.6f)
                            )
                            .border(2.dp, NeonCyan, CircleShape)
                            .pointerInput(Unit) {
                                detectTapGestures(
                                    onPress = {
                                        touchState.isRightPressed = true
                                        touchState.updateSteeringFromButtons()
                                        tryAwaitRelease()
                                        touchState.isRightPressed = false
                                        touchState.updateSteeringFromButtons()
                                    }
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            contentDescription = "Direita",
                            tint = if (touchState.isRightPressed) Color.Black else TextPrimary,
                            modifier = Modifier.size(46.dp)
                        )
                    }
                }
            }

            TouchControlScheme.TILT_GYRO -> {
                // Gyroscope tilt mode indicator
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Black.copy(alpha = 0.6f))
                        .border(1.dp, NeonCyan, RoundedCornerShape(12.dp))
                        .padding(12.dp)
                ) {
                    Text("INCLINE O APARELHO PARA DIRIGIR", fontSize = 10.sp, color = NeonCyan, fontWeight = FontWeight.Bold)
                }
            }
        }

        // Right Side: Throttle Pedal, Brake Pedal, Handbrake & Nitro
        Row(
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.padding(bottom = 8.dp)
        ) {
            // Handbrake / Drift Button
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .testTag("touch_handbrake_button")
                    .clip(CircleShape)
                    .background(
                        if (touchState.isHandbrakeActive) SpeedYellow.copy(alpha = 0.8f)
                        else Color.Black.copy(alpha = 0.65f)
                    )
                    .border(2.dp, SpeedYellow, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                touchState.isHandbrakeActive = true
                                tryAwaitRelease()
                                touchState.isHandbrakeActive = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "DRIFT",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = if (touchState.isHandbrakeActive) Color.Black else SpeedYellow
                )
            }

            // Nitro Boost Button
            Box(
                modifier = Modifier
                    .size(62.dp)
                    .testTag("touch_nitro_button")
                    .clip(CircleShape)
                    .background(
                        if (touchState.isNitroActive) NitroOrange.copy(alpha = 0.9f)
                        else Color.Black.copy(alpha = 0.65f)
                    )
                    .border(2.dp, NitroOrange, CircleShape)
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                touchState.isNitroActive = true
                                tryAwaitRelease()
                                touchState.isNitroActive = false
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = "Nitro NOS",
                        tint = if (touchState.isNitroActive) Color.Black else NitroOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Text(
                        text = "NOS",
                        fontWeight = FontWeight.Black,
                        fontSize = 10.sp,
                        color = if (touchState.isNitroActive) Color.Black else NitroOrange
                    )
                }
            }

            // Brake Pedal
            Box(
                modifier = Modifier
                    .width(62.dp)
                    .height(90.dp)
                    .testTag("touch_brake_pedal")
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (touchState.brakePedal > 0.1f) RacingRed.copy(alpha = 0.8f)
                        else Color(0xFF221216).copy(alpha = 0.75f)
                    )
                    .border(2.dp, RacingRed, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                touchState.brakePedal = 1f
                                tryAwaitRelease()
                                touchState.brakePedal = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "FREIO",
                    fontWeight = FontWeight.Black,
                    fontSize = 11.sp,
                    color = if (touchState.brakePedal > 0.1f) Color.Black else RacingRed
                )
            }

            // Throttle Pedal (Acelerador)
            Box(
                modifier = Modifier
                    .width(68.dp)
                    .height(115.dp)
                    .testTag("touch_throttle_pedal")
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (touchState.throttlePedal > 0.1f) NeonCyan.copy(alpha = 0.85f)
                        else Color(0xFF0C242B).copy(alpha = 0.75f)
                    )
                    .border(2.dp, NeonCyan, RoundedCornerShape(12.dp))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                touchState.throttlePedal = 1f
                                tryAwaitRelease()
                                touchState.throttlePedal = 0f
                            }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "GÁS",
                    fontWeight = FontWeight.Black,
                    fontSize = 14.sp,
                    color = if (touchState.throttlePedal > 0.1f) Color.Black else NeonCyan
                )
            }
        }
    }
}

@Composable
private fun PauseMenuDialog(
    onResume: () -> Unit,
    onRestart: () -> Unit,
    onExit: () -> Unit,
    onGarage: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.75f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(360.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(DarkSurface)
                .border(1.5.dp, DarkBorder, RoundedCornerShape(16.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("CORRIDA PAUSADA", fontSize = 20.sp, fontWeight = FontWeight.Black, color = TextPrimary)
                Spacer(modifier = Modifier.height(18.dp))

                Button(
                    onClick = onResume,
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("CONTINUAR CORRIDA", fontWeight = FontWeight.Bold, color = Color.Black)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onRestart,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("REINICIAR VOLTA", fontWeight = FontWeight.Bold, color = TextPrimary)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onGarage,
                    colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("CUSTOMIZAR CARRO (GARAGEM)", fontWeight = FontWeight.Bold, color = SpeedYellow)
                }

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = onExit,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2C1417)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth().height(44.dp)
                ) {
                    Text("SAIR PARA O MENU PRINCIPAL", fontWeight = FontWeight.Bold, color = RacingRed)
                }
            }
        }
    }
}

@Composable
private fun VictoryPodiumDialog(
    playerCar: CarPhysics,
    trackConfig: TrackConfig,
    onRestart: () -> Unit,
    onGarage: () -> Unit,
    onExit: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f)),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .width(420.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(DarkSurface)
                .border(2.dp, if (playerCar.currentRacePosition == 1) SpeedYellow else NeonCyan, RoundedCornerShape(20.dp))
                .padding(24.dp)
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.EmojiEvents,
                    contentDescription = null,
                    tint = if (playerCar.currentRacePosition == 1) SpeedYellow else NeonCyan,
                    modifier = Modifier.size(54.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (playerCar.currentRacePosition == 1) "VITÓRIA! 1º LUGAR!" else "FIM DE CORRIDA! ${playerCar.currentRacePosition}º LUGAR",
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Black,
                    color = TextPrimary
                )
                Text(
                    text = trackConfig.theme.displayName,
                    fontSize = 12.sp,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Summary
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("MELHOR VOLTA", fontSize = 10.sp, color = TextSecondary)
                        val lapSec = playerCar.bestLapTimeMs / 1000f
                        Text(if (lapSec > 0) "${String.format("%.2f", lapSec)}s" else "--", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("PONTOS DRIFT", fontSize = 10.sp, color = TextSecondary)
                        Text("${playerCar.totalDriftScore}", fontSize = 16.sp, fontWeight = FontWeight.Black, color = NitroOrange)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("VEL. MÁXIMA", fontSize = 10.sp, color = TextSecondary)
                        Text("${playerCar.topSpeedKmH.toInt()} km/h", fontSize = 16.sp, fontWeight = FontWeight.Black, color = SpeedYellow)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onRestart,
                        colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("CORRER DE NOVO", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onGarage,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("GARAGEM", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                    }
                    Button(
                        onClick = onExit,
                        colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).height(44.dp)
                    ) {
                        Text("MENU", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 11.sp)
                    }
                }
            }
        }
    }
}
