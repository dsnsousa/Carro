package com.example.game.customization

enum class TrackTheme(
    val displayName: String,
    val description: String,
    val skyColorTopHex: Long,
    val skyColorBottomHex: Long,
    val roadColorHex: Long,
    val curbColor1Hex: Long,
    val curbColor2Hex: Long,
    val grassColorHex: Long,
    val sceneryType: SceneryType
) {
    TOKYO_NEON(
        displayName = "Tóquio Neon Expressway",
        description = "Arranha-céus iluminados, painéis holográficos e viadutos sob luzes de néon.",
        skyColorTopHex = 0xFF050814,
        skyColorBottomHex = 0xFF18103A,
        roadColorHex = 0xFF212530,
        curbColor1Hex = 0xFF00E5FF,
        curbColor2Hex = 0xFFFF007F,
        grassColorHex = 0xFF0C101B,
        sceneryType = SceneryType.CYBER_CITY
    ),
    CANYON_PASS(
        displayName = "Canyon Pass Sunset",
        description = "Gargantas profundas e rochedos dourados sob o pôr do sol do deserto.",
        skyColorTopHex = 0xFF4A1525,
        skyColorBottomHex = 0xFFFF7B38,
        roadColorHex = 0xFF383234,
        curbColor1Hex = 0xFFFF3D00,
        curbColor2Hex = 0xFFFFFFFF,
        grassColorHex = 0xFF5D3025,
        sceneryType = SceneryType.CANYON
    ),
    RIVIERA_COAST(
        displayName = "Riviera Grand Prix",
        description = "Costa paradisíaca com palmeiras, iates e vista infinita para o mar azul.",
        skyColorTopHex = 0xFF0072B2,
        skyColorBottomHex = 0xFF70C9F7,
        roadColorHex = 0xFF353A40,
        curbColor1Hex = 0xFFFF1744,
        curbColor2Hex = 0xFFFFFFFF,
        grassColorHex = 0xFF2E7D32,
        sceneryType = SceneryType.COASTAL
    ),
    CYBER_ALPINE(
        displayName = "Pico Alpino Cyberpunk",
        description = "Pistas montanhosas sinuosas cobertas por neve e auroras boreais eletrizantes.",
        skyColorTopHex = 0xFF030D26,
        skyColorBottomHex = 0xFF0D3B5C,
        roadColorHex = 0xFF222831,
        curbColor1Hex = 0xFF00F5D4,
        curbColor2Hex = 0xFF7B2CBF,
        grassColorHex = 0xFFC3D9E8,
        sceneryType = SceneryType.ALPINE_SNOW
    ),
    INDUSTRIAL_PORT(
        displayName = "Porto Industrial 2077",
        description = "Guindastes colossais, containers de carga e chicanes técnicas em alta velocidade.",
        skyColorTopHex = 0xFF1A1A24,
        skyColorBottomHex = 0xFF37474F,
        roadColorHex = 0xFF2B2D42,
        curbColor1Hex = 0xFFFFB700,
        curbColor2Hex = 0xFF212121,
        grassColorHex = 0xFF1C202A,
        sceneryType = SceneryType.INDUSTRIAL
    )
}

enum class SceneryType {
    CYBER_CITY,
    CANYON,
    COASTAL,
    ALPINE_SNOW,
    INDUSTRIAL
}

enum class WeatherCondition(val displayName: String, val roadGripMultiplier: Float, val rainDrops: Boolean) {
    CLEAR_DAY("Céu Limpo", 1.0f, false),
    GOLDEN_SUNSET("Pôr do Sol Dourado", 0.98f, false),
    CYBER_NIGHT("Noite Estrelada", 0.95f, false),
    HEAVY_RAIN("Chuva Intensa & Pistas Molhadas", 0.78f, true)
}

enum class CurvatureDifficulty(val displayName: String, val maxCurve: Float) {
    EASY("Pista Rápida (Curvas Leves)", 1.2f),
    MEDIUM("Equilibrada (Curvas Médias & Chicanes)", 2.4f),
    HARD("Técnica / Pro (Grampos Fechados)", 3.8f)
}

enum class ElevationProfile(val displayName: String, val maxElevationDelta: Float) {
    FLAT("Plana (Velocidade Pura)", 0f),
    ROLLING_HILLS("Colinas Onduladas", 40f),
    EXTREME_MOUNTAIN("Montanhas Íngremes & Saltos", 95f)
}

data class TrackConfig(
    val id: String = "track_tokyo",
    val theme: TrackTheme = TrackTheme.TOKYO_NEON,
    val weather: WeatherCondition = WeatherCondition.CYBER_NIGHT,
    val curvature: CurvatureDifficulty = CurvatureDifficulty.MEDIUM,
    val elevation: ElevationProfile = ElevationProfile.ROLLING_HILLS,
    val laps: Int = 3,
    val segmentCount: Int = 400, // determines track length (~4km)
    val nitroPadsEnabled: Boolean = true,
    val obstaclesEnabled: Boolean = true,
    val roadWidthMeters: Float = 14f // wide realistic racing highway
) {
    val totalDistanceKm: Float
        get() = (segmentCount * 10f) / 1000f
}

val DEFAULT_TRACK_PRESETS = listOf(
    TrackConfig(
        id = "preset_tokyo",
        theme = TrackTheme.TOKYO_NEON,
        weather = WeatherCondition.CYBER_NIGHT,
        curvature = CurvatureDifficulty.MEDIUM,
        elevation = ElevationProfile.ROLLING_HILLS,
        laps = 3,
        segmentCount = 420
    ),
    TrackConfig(
        id = "preset_canyon",
        theme = TrackTheme.CANYON_PASS,
        weather = WeatherCondition.GOLDEN_SUNSET,
        curvature = CurvatureDifficulty.HARD,
        elevation = ElevationProfile.EXTREME_MOUNTAIN,
        laps = 2,
        segmentCount = 480
    ),
    TrackConfig(
        id = "preset_riviera",
        theme = TrackTheme.RIVIERA_COAST,
        weather = WeatherCondition.CLEAR_DAY,
        curvature = CurvatureDifficulty.EASY,
        elevation = ElevationProfile.ROLLING_HILLS,
        laps = 3,
        segmentCount = 380
    ),
    TrackConfig(
        id = "preset_alpine",
        theme = TrackTheme.CYBER_ALPINE,
        weather = WeatherCondition.HEAVY_RAIN,
        curvature = CurvatureDifficulty.HARD,
        elevation = ElevationProfile.EXTREME_MOUNTAIN,
        laps = 3,
        segmentCount = 450
    )
)
