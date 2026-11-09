package com.example.game.multiplayer

import com.example.game.customization.CarModel
import com.example.game.customization.DecalType
import com.example.game.customization.PaintFinish
import com.example.game.customization.RimDesign
import com.example.game.customization.SpoilerType
import com.example.game.customization.VehicleConfig
import com.example.game.engine.CarPhysics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlin.random.Random

enum class MultiplayerMode(val displayName: String) {
    SOLO_AI("Corrida Rápida vs Rivais IA"),
    ONLINE_ROOM("Sala Multiplayer Online"),
    SPLIT_SCREEN("Tela Dividida (2 Jogadores Locais)"),
    GHOST_BATTLE("Batalha Contra Carro Fantasma")
}

data class PlayerLobbySlot(
    val id: String,
    val name: String,
    val vehicleConfig: VehicleConfig,
    val isReady: Boolean = false,
    val isHost: Boolean = false,
    val pingMs: Int = 24
)

data class GhostFrame(
    val distance: Float,
    val playerX: Float,
    val speed: Float,
    val driftAngle: Float
)

class MultiplayerEngine {
    private val _currentMode = MutableStateFlow(MultiplayerMode.SOLO_AI)
    val currentMode: StateFlow<MultiplayerMode> = _currentMode

    private val _roomCode = MutableStateFlow("APEX")
    val roomCode: StateFlow<String> = _roomCode

    private val _lobbySlots = MutableStateFlow<List<PlayerLobbySlot>>(emptyList())
    val lobbySlots: StateFlow<List<PlayerLobbySlot>> = _lobbySlots

    private val ghostRecording = mutableListOf<GhostFrame>()
    private var ghostPlaybackIndex = 0

    fun setMode(mode: MultiplayerMode) {
        _currentMode.value = mode
    }

    fun hostRoom(hostName: String, hostVehicle: VehicleConfig): String {
        _currentMode.value = MultiplayerMode.ONLINE_ROOM
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789"
        val code = (1..4).map { chars.random() }.joinToString("")
        _roomCode.value = code

        // Seed with Host + simulated online racers
        val slots = mutableListOf(
            PlayerLobbySlot("host_1", hostName, hostVehicle, isReady = true, isHost = true, pingMs = 12)
        )

        val peerNames = listOf("Alex Drift", "Neon Rider", "Shadow GT", "Turbo Max")
        val peerColors = listOf(0xFFFF1744L, 0xFFFF9100L, 0xFF7C4DFFL, 0xFF00E676L)
        val models = CarModel.values()

        for (i in 0 until minOf(3, peerNames.size)) {
            slots.add(
                PlayerLobbySlot(
                    id = "peer_$i",
                    name = peerNames[i],
                    vehicleConfig = VehicleConfig(
                        model = models[i % models.size],
                        primaryColorHex = peerColors[i],
                        finish = PaintFinish.GLOSS,
                        decal = DecalType.TWIN_STRIPES,
                        spoiler = SpoilerType.GT_WING,
                        rimDesign = RimDesign.FIVE_SPOKE
                    ),
                    isReady = true,
                    isHost = false,
                    pingMs = Random.nextInt(28, 65)
                )
            )
        }
        _lobbySlots.value = slots
        return code
    }

    fun joinRoom(code: String, playerName: String, playerVehicle: VehicleConfig): Boolean {
        _currentMode.value = MultiplayerMode.ONLINE_ROOM
        _roomCode.value = code.uppercase()
        val slots = mutableListOf(
            PlayerLobbySlot("host_room", "Sala Host", VehicleConfig(CarModel.FORMULA_LIGHTNING, 0xFFFFEA00L), isReady = true, isHost = true),
            PlayerLobbySlot("local_player", playerName, playerVehicle, isReady = true, isHost = false),
            PlayerLobbySlot("rival_1", "Cyber Ghost", VehicleConfig(CarModel.CYBER_PHANTOM, 0xFF00F0FFL), isReady = true),
            PlayerLobbySlot("rival_2", "V8 Thunder", VehicleConfig(CarModel.VENOM_V8, 0xFFFF3366L), isReady = true)
        )
        _lobbySlots.value = slots
        return true
    }

    fun createRacersGrid(
        playerVehicle: VehicleConfig,
        playerName: String,
        mode: MultiplayerMode,
        player2Vehicle: VehicleConfig? = null
    ): List<CarPhysics> {
        val racers = mutableListOf<CarPhysics>()

        // 1. Player 1
        val p1 = CarPhysics(playerVehicle, isPlayer = true, racerName = playerName)
        p1.trackDistance = 150f
        p1.playerX = -0.3f
        racers.add(p1)

        when (mode) {
            MultiplayerMode.SPLIT_SCREEN -> {
                val p2Veh = player2Vehicle ?: VehicleConfig(
                    model = CarModel.VENOM_V8,
                    primaryColorHex = 0xFFFF1744L,
                    secondaryColorHex = 0xFFFFFFFFL
                )
                val p2 = CarPhysics(p2Veh, isPlayer = true, racerName = "Jogador 2")
                p2.trackDistance = 150f
                p2.playerX = 0.3f
                racers.add(p2)

                // 2 AI opponents in split-screen
                racers.add(createBotRacer("Rival Alpha", CarModel.SAKURA_DRIFT, 0xFF7C4DFFL, 120f, -0.6f))
                racers.add(createBotRacer("Rival Beta", CarModel.CYBER_PHANTOM, 0xFFFF9100L, 90f, 0.6f))
            }
            MultiplayerMode.ONLINE_ROOM -> {
                // Populate grid from lobby slots
                val slots = _lobbySlots.value
                for ((idx, slot) in slots.withIndex()) {
                    if (slot.name == playerName) continue // Player 1 already added
                    val botCar = CarPhysics(slot.vehicleConfig, isPlayer = false, racerName = slot.name)
                    botCar.trackDistance = maxOf(0f, 150f - (idx + 1) * 35f)
                    botCar.playerX = if (idx % 2 == 0) 0.45f else -0.45f
                    racers.add(botCar)
                }
            }
            MultiplayerMode.SOLO_AI, MultiplayerMode.GHOST_BATTLE -> {
                // Full grid of 5 rival drivers
                racers.add(createBotRacer("Viper Drift", CarModel.SAKURA_DRIFT, 0xFFFF0055L, 130f, 0.45f))
                racers.add(createBotRacer("Cyber Racer", CarModel.CYBER_PHANTOM, 0xFF00E5FFL, 105f, -0.45f))
                racers.add(createBotRacer("Thunder V8", CarModel.VENOM_V8, 0xFFFF7700L, 75f, 0.5f))
                racers.add(createBotRacer("Formula One", CarModel.FORMULA_LIGHTNING, 0xFFFFEA00L, 45f, -0.35f))
                racers.add(createBotRacer("Stealth Phantom", CarModel.APEX_GT, 0xFF9C27B0L, 15f, 0.2f))
            }
        }

        return racers
    }

    private fun createBotRacer(
        name: String,
        model: CarModel,
        colorHex: Long,
        startDist: Float,
        startX: Float
    ): CarPhysics {
        val config = VehicleConfig(
            model = model,
            primaryColorHex = colorHex,
            secondaryColorHex = 0xFFFFFFFF,
            finish = PaintFinish.GLOSS,
            decal = DecalType.TWIN_STRIPES,
            spoiler = SpoilerType.GT_WING
        )
        val car = CarPhysics(config, isPlayer = false, racerName = name)
        car.trackDistance = startDist
        car.playerX = startX
        return car
    }

    fun updateAiRacers(
        dtSeconds: Float,
        racers: List<CarPhysics>,
        totalTrackZ: Float,
        currentSegmentCurve: Float,
        roadGripFactor: Float,
        maxLaps: Int
    ) {
        val player = racers.firstOrNull { it.isPlayer }
        val playerDist = player?.trackDistance ?: 0f

        for (car in racers) {
            if (car.isPlayer) continue

            // Adaptive AI racing behavior: attempts to follow ideal racing line while overtaking
            val targetX = -currentSegmentCurve * 0.45f // Apex cornering
            val steerDir = (targetX - car.playerX).coerceIn(-1.0f, 1.0f)

            // Rubber-banding / Competitive dynamic balancing
            val distDiff = playerDist - car.trackDistance
            val aiThrottle = when {
                distDiff > 400f -> 1.0f // Catch up
                distDiff < -300f -> 0.88f // Maintain intense close race
                else -> 0.95f
            }

            val aiHandbrake = Math.abs(currentSegmentCurve) > 2.0f && car.currentSpeedKmH > 180f
            val aiNitro = distDiff > 100f && car.nitroGauge > 30f

            car.update(
                dtSeconds = dtSeconds,
                throttleInput = aiThrottle,
                brakeInput = if (Math.abs(currentSegmentCurve) > 3.0f && car.currentSpeedKmH > 220f) 0.5f else 0f,
                steeringInput = steerDir,
                handbrakeInput = aiHandbrake,
                nitroInput = aiNitro,
                currentSegmentCurve = currentSegmentCurve,
                roadGripFactor = roadGripFactor,
                totalTrackLength = totalTrackZ,
                maxLaps = maxLaps
            )
        }
    }

    fun recordGhostFrame(distance: Float, playerX: Float, speed: Float, drift: Float) {
        ghostRecording.add(GhostFrame(distance, playerX, speed, drift))
    }

    fun getGhostPlayback(trackDistance: Float): GhostFrame? {
        if (ghostRecording.isEmpty()) return null
        val frame = ghostRecording.minByOrNull { Math.abs(it.distance - trackDistance) }
        return frame
    }
}
