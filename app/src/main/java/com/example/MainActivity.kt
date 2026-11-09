package com.example

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.MotionEvent
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.game.audio.GameAudioSynthesizer
import com.example.game.customization.GameStorage
import com.example.game.customization.TouchControlScheme
import com.example.game.customization.TrackConfig
import com.example.game.customization.VehicleConfig
import com.example.game.input.GamepadManager
import com.example.game.multiplayer.MultiplayerEngine
import com.example.game.multiplayer.MultiplayerMode
import com.example.ui.screens.GarageScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.MultiplayerScreen
import com.example.ui.screens.RacingScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.screens.TrackEditorScreen
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.NitroRacingTheme

enum class GameScreen {
    HOME,
    RACE,
    GARAGE,
    TRACK_EDITOR,
    MULTIPLAYER,
    SETTINGS
}

class MainActivity : ComponentActivity(), SensorEventListener {

    private val gamepadManager = GamepadManager()
    private lateinit var storage: GameStorage
    private lateinit var audioSynthesizer: GameAudioSynthesizer
    private val multiplayerEngine = MultiplayerEngine()

    private var sensorManager: SensorManager? = null
    private var accelerometer: Sensor? = null
    private var latestTiltX = 0f

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        storage = GameStorage(this)
        audioSynthesizer = GameAudioSynthesizer { storage.isAudioEnabled() }

        // Sensor for Gyroscope / Tilt steering
        sensorManager = getSystemService(Context.SENSOR_SERVICE) as? SensorManager
        accelerometer = sensorManager?.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        setContent {
            NitroRacingTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = DarkBackground
                ) {
                    RacingGameApp(
                        storage = storage,
                        gamepadManager = gamepadManager,
                        audioSynthesizer = audioSynthesizer,
                        multiplayerEngine = multiplayerEngine
                    )
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        gamepadManager.checkConnectedControllers()
        accelerometer?.let { sensor ->
            sensorManager?.registerListener(this, sensor, SensorManager.SENSOR_DELAY_GAME)
        }
    }

    override fun onPause() {
        super.onPause()
        sensorManager?.unregisterListener(this)
        audioSynthesizer.stop()
    }

    override fun onDestroy() {
        super.onDestroy()
        audioSynthesizer.stop()
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event?.sensor?.type == Sensor.TYPE_ACCELEROMETER) {
            // In landscape mode, Y axis or X axis represents lateral steering tilt
            latestTiltX = event.values[1] // In landscape, phone tilt left/right maps to Y value
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    // Gamepad & Joystick Motion Events (analog sticks, analog gas/brake triggers)
    override fun dispatchGenericMotionEvent(event: MotionEvent): Boolean {
        if (gamepadManager.onGenericMotionEvent(event)) {
            return true
        }
        return super.dispatchGenericMotionEvent(event)
    }

    // Gamepad Buttons and Keyboard Keys (A/B/X/Y, D-Pad, Arrow keys)
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (gamepadManager.onKeyDown(event.keyCode, event)) {
                return true
            }
        } else if (event.action == KeyEvent.ACTION_UP) {
            if (gamepadManager.onKeyUp(event.keyCode, event)) {
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }
}

@Composable
fun RacingGameApp(
    storage: GameStorage,
    gamepadManager: GamepadManager,
    audioSynthesizer: GameAudioSynthesizer,
    multiplayerEngine: MultiplayerEngine
) {
    var currentScreen by remember { mutableStateOf(GameScreen.HOME) }
    var playerVehicle by remember { mutableStateOf(storage.loadVehicleConfig()) }
    var trackConfig by remember { mutableStateOf(storage.loadTrackConfig()) }
    var activeMultiplayerMode by remember { mutableStateOf(MultiplayerMode.SOLO_AI) }

    when (currentScreen) {
        GameScreen.HOME -> {
            HomeScreen(
                storage = storage,
                playerVehicle = playerVehicle,
                trackConfig = trackConfig,
                gamepadManager = gamepadManager,
                onStartQuickRace = {
                    activeMultiplayerMode = MultiplayerMode.SOLO_AI
                    currentScreen = GameScreen.RACE
                },
                onOpenMultiplayer = { currentScreen = GameScreen.MULTIPLAYER },
                onOpenGarage = { currentScreen = GameScreen.GARAGE },
                onOpenTrackEditor = { currentScreen = GameScreen.TRACK_EDITOR },
                onOpenSettings = { currentScreen = GameScreen.SETTINGS }
            )
        }

        GameScreen.GARAGE -> {
            BackHandler { currentScreen = GameScreen.HOME }
            GarageScreen(
                storage = storage,
                initialConfig = playerVehicle,
                onBack = { currentScreen = GameScreen.HOME },
                onSaveAndRace = { updatedVehicle ->
                    playerVehicle = updatedVehicle
                    currentScreen = GameScreen.RACE
                }
            )
        }

        GameScreen.TRACK_EDITOR -> {
            BackHandler { currentScreen = GameScreen.HOME }
            TrackEditorScreen(
                storage = storage,
                initialConfig = trackConfig,
                onBack = { currentScreen = GameScreen.HOME },
                onSaveAndRace = { updatedTrack ->
                    trackConfig = updatedTrack
                    currentScreen = GameScreen.RACE
                }
            )
        }

        GameScreen.MULTIPLAYER -> {
            BackHandler { currentScreen = GameScreen.HOME }
            MultiplayerScreen(
                multiplayerEngine = multiplayerEngine,
                playerVehicle = playerVehicle,
                playerName = storage.getPlayerName(),
                onBack = { currentScreen = GameScreen.HOME },
                onStartRace = { mode ->
                    activeMultiplayerMode = mode
                    currentScreen = GameScreen.RACE
                }
            )
        }

        GameScreen.SETTINGS -> {
            BackHandler { currentScreen = GameScreen.HOME }
            SettingsScreen(
                storage = storage,
                gamepadManager = gamepadManager,
                onBack = { currentScreen = GameScreen.HOME }
            )
        }

        GameScreen.RACE -> {
            BackHandler { currentScreen = GameScreen.HOME }
            RacingScreen(
                storage = storage,
                playerVehicle = playerVehicle,
                trackConfig = trackConfig,
                multiplayerEngine = multiplayerEngine,
                multiplayerMode = activeMultiplayerMode,
                gamepadManager = gamepadManager,
                audioSynthesizer = audioSynthesizer,
                onExitRace = { currentScreen = GameScreen.HOME },
                onOpenGarage = { currentScreen = GameScreen.GARAGE }
            )
        }
    }
}
