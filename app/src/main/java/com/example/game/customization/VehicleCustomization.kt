package com.example.game.customization

import androidx.compose.ui.graphics.Color

enum class CarModel(
    val displayName: String,
    val description: String,
    val baseTopSpeed: Float, // km/h
    val baseAcceleration: Float, // m/s^2
    val baseHandling: Float, // 0..1
    val baseDriftFactor: Float
) {
    APEX_GT(
        displayName = "Apex GT Hypercar",
        description = "Superesportivo aerodinâmico com velocidade máxima colossal e tração integral.",
        baseTopSpeed = 345f,
        baseAcceleration = 14.5f,
        baseHandling = 0.88f,
        baseDriftFactor = 0.75f
    ),
    VENOM_V8(
        displayName = "Venom V8 Muscle",
        description = "Monstro com motor V8 aspirado, aceleração explosiva e facilidade para drifts longos.",
        baseTopSpeed = 320f,
        baseAcceleration = 16.0f,
        baseHandling = 0.72f,
        baseDriftFactor = 0.95f
    ),
    SAKURA_DRIFT(
        displayName = "Sakura Drift Tuner",
        description = "Ícone das ruas de Tóquio, suspensão rebaixada feita para curvas e drifts perfeitos.",
        baseTopSpeed = 310f,
        baseAcceleration = 13.8f,
        baseHandling = 0.94f,
        baseDriftFactor = 1.0f
    ),
    FORMULA_LIGHTNING(
        displayName = "Formula Lightning",
        description = "Monoposto ultraleve com downforce maciço para curvas em velocidade extrema.",
        baseTopSpeed = 360f,
        baseAcceleration = 17.5f,
        baseHandling = 0.98f,
        baseDriftFactor = 0.60f
    ),
    CYBER_PHANTOM(
        displayName = "Cyber Phantom",
        description = "Conceito futurista com linhas angulares e propulsão de plasma.",
        baseTopSpeed = 350f,
        baseAcceleration = 15.2f,
        baseHandling = 0.85f,
        baseDriftFactor = 0.82f
    )
}

enum class PaintFinish(val displayName: String, val specularFactor: Float) {
    GLOSS("Metálico Brilhante", 0.9f),
    MATTE("Fosco Satin", 0.3f),
    CHROME("Cromado Espelhado", 1.0f),
    CARBON("Fibra de Carbono", 0.5f)
}

enum class DecalType(val displayName: String) {
    NONE("Nenhum"),
    TWIN_STRIPES("Faixas Esportivas Duplas"),
    FLAME_DECALS("Labaredas Nitro"),
    CYBER_HEX("Hexágonos Cyber"),
    DRIFT_CAMO("Camuflagem Drift")
}

enum class SpoilerType(val displayName: String, val downforceMultiplier: Float) {
    STOCK("Original", 1.0f),
    GT_WING("Aerofólio GT Carbono", 1.15f),
    DUCKTAIL("Spoiler Ducktail", 1.08f),
    EXTREME_TIME_ATTACK("Asa Time Attack Maciça", 1.25f)
}

enum class RimDesign(val displayName: String) {
    FIVE_SPOKE("5 Raios Star"),
    MESH_SPORTS("Mesh Multi-Raios"),
    TURBOFAN("Turbofan Aero"),
    CYBER_BLADE("Cyber Blade")
}

data class VehicleConfig(
    val model: CarModel = CarModel.APEX_GT,
    val primaryColorHex: Long = 0xFF00E5FF, // Cyan
    val secondaryColorHex: Long = 0xFFFF3366, // Pink/Red accent
    val finish: PaintFinish = PaintFinish.GLOSS,
    val decal: DecalType = DecalType.TWIN_STRIPES,
    val racingNumber: Int = 77,
    val spoiler: SpoilerType = SpoilerType.GT_WING,
    val rimDesign: RimDesign = RimDesign.FIVE_SPOKE,
    val rimColorHex: Long = 0xFFE0E0E0,
    val neonUnderglowHex: Long = 0xFF00F0FF, // 0 for off
    val tireSmokeHex: Long = 0xFF00E5FF,
    // Upgrades (0 to 5 levels)
    val engineLevel: Int = 2,
    val turboLevel: Int = 2,
    val handlingLevel: Int = 2,
    val nitroLevel: Int = 2
) {
    val effectiveTopSpeed: Float
        get() = model.baseTopSpeed + (engineLevel * 12f) + (turboLevel * 6f)

    val effectiveAcceleration: Float
        get() = model.baseAcceleration + (turboLevel * 1.5f) + (engineLevel * 0.8f)

    val effectiveHandling: Float
        get() = (model.baseHandling + (handlingLevel * 0.04f) * spoiler.downforceMultiplier).coerceAtMost(1.0f)

    val effectiveNitroCapacity: Float
        get() = 100f + (nitroLevel * 20f)
}

val PRESET_CAR_COLORS = listOf(
    0xFF00E5FF to "Ciano Neon",
    0xFFFF1744 to "Vermelho Corrida",
    0xFFFF9100 to "Laranja Fogo",
    0xFFFFEA00 to "Amarelo Solar",
    0xFF00E676 to "Verde Ácido",
    0xFF7C4DFF to "Roxo Elétrico",
    0xFF111111 to "Preto Stealth",
    0xFFF5F5F5 to "Branco Pérola",
    0xFF2979FF to "Azul Cobalto",
    0xFFFF4081 to "Rosa Choque"
)

val PRESET_NEON_COLORS = listOf(
    0x00000000L to "Desativado",
    0xFF00F0FF to "Neon Ciano",
    0xFFFF007F to "Neon Magenta",
    0xFF00FF66 to "Neon Verde",
    0xFFFFB700 to "Neon Dourado",
    0xFF9D00FF to "Neon Roxo",
    0xFFFF2200 to "Neon Vermelho"
)
