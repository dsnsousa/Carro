package com.example.game.customization

import android.content.Context
import android.content.SharedPreferences

enum class TouchControlScheme(val displayName: String) {
    TOUCH_BUTTONS("Setas Virtuais"),
    STEERING_WHEEL("Volante Touch"),
    TILT_GYRO("Sensor Giroscópio (Inclinar)")
}

class GameStorage(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("nitro_drift_prefs", Context.MODE_PRIVATE)

    fun loadVehicleConfig(): VehicleConfig {
        val modelName = prefs.getString("veh_model", CarModel.APEX_GT.name) ?: CarModel.APEX_GT.name
        val model = try { CarModel.valueOf(modelName) } catch (_: Exception) { CarModel.APEX_GT }

        val primaryColor = prefs.getLong("veh_primary_color", 0xFF00E5FF)
        val secondaryColor = prefs.getLong("veh_secondary_color", 0xFFFF3366)
        val finishName = prefs.getString("veh_finish", PaintFinish.GLOSS.name) ?: PaintFinish.GLOSS.name
        val finish = try { PaintFinish.valueOf(finishName) } catch (_: Exception) { PaintFinish.GLOSS }

        val decalName = prefs.getString("veh_decal", DecalType.TWIN_STRIPES.name) ?: DecalType.TWIN_STRIPES.name
        val decal = try { DecalType.valueOf(decalName) } catch (_: Exception) { DecalType.TWIN_STRIPES }

        val spoilerName = prefs.getString("veh_spoiler", SpoilerType.GT_WING.name) ?: SpoilerType.GT_WING.name
        val spoiler = try { SpoilerType.valueOf(spoilerName) } catch (_: Exception) { SpoilerType.GT_WING }

        val rimName = prefs.getString("veh_rim", RimDesign.FIVE_SPOKE.name) ?: RimDesign.FIVE_SPOKE.name
        val rim = try { RimDesign.valueOf(rimName) } catch (_: Exception) { RimDesign.FIVE_SPOKE }

        val rimColor = prefs.getLong("veh_rim_color", 0xFFE0E0E0)
        val neonColor = prefs.getLong("veh_neon_color", 0xFF00F0FF)
        val smokeColor = prefs.getLong("veh_smoke_color", 0xFF00E5FF)
        val number = prefs.getInt("veh_number", 77)

        val engineLevel = prefs.getInt("veh_eng_lvl", 2)
        val turboLevel = prefs.getInt("veh_turbo_lvl", 2)
        val handlingLevel = prefs.getInt("veh_hand_lvl", 2)
        val nitroLevel = prefs.getInt("veh_nitro_lvl", 2)

        return VehicleConfig(
            model = model,
            primaryColorHex = primaryColor,
            secondaryColorHex = secondaryColor,
            finish = finish,
            decal = decal,
            racingNumber = number,
            spoiler = spoiler,
            rimDesign = rim,
            rimColorHex = rimColor,
            neonUnderglowHex = neonColor,
            tireSmokeHex = smokeColor,
            engineLevel = engineLevel,
            turboLevel = turboLevel,
            handlingLevel = handlingLevel,
            nitroLevel = nitroLevel
        )
    }

    fun saveVehicleConfig(config: VehicleConfig) {
        prefs.edit()
            .putString("veh_model", config.model.name)
            .putLong("veh_primary_color", config.primaryColorHex)
            .putLong("veh_secondary_color", config.secondaryColorHex)
            .putString("veh_finish", config.finish.name)
            .putString("veh_decal", config.decal.name)
            .putInt("veh_number", config.racingNumber)
            .putString("veh_spoiler", config.spoiler.name)
            .putString("veh_rim", config.rimDesign.name)
            .putLong("veh_rim_color", config.rimColorHex)
            .putLong("veh_neon_color", config.neonUnderglowHex)
            .putLong("veh_smoke_color", config.tireSmokeHex)
            .putInt("veh_eng_lvl", config.engineLevel)
            .putInt("veh_turbo_lvl", config.turboLevel)
            .putInt("veh_hand_lvl", config.handlingLevel)
            .putInt("veh_nitro_lvl", config.nitroLevel)
            .apply()
    }

    fun loadTrackConfig(): TrackConfig {
        val themeName = prefs.getString("track_theme", TrackTheme.TOKYO_NEON.name) ?: TrackTheme.TOKYO_NEON.name
        val theme = try { TrackTheme.valueOf(themeName) } catch (_: Exception) { TrackTheme.TOKYO_NEON }

        val weatherName = prefs.getString("track_weather", WeatherCondition.CYBER_NIGHT.name) ?: WeatherCondition.CYBER_NIGHT.name
        val weather = try { WeatherCondition.valueOf(weatherName) } catch (_: Exception) { WeatherCondition.CYBER_NIGHT }

        val curvName = prefs.getString("track_curv", CurvatureDifficulty.MEDIUM.name) ?: CurvatureDifficulty.MEDIUM.name
        val curvature = try { CurvatureDifficulty.valueOf(curvName) } catch (_: Exception) { CurvatureDifficulty.MEDIUM }

        val elevName = prefs.getString("track_elev", ElevationProfile.ROLLING_HILLS.name) ?: ElevationProfile.ROLLING_HILLS.name
        val elevation = try { ElevationProfile.valueOf(elevName) } catch (_: Exception) { ElevationProfile.ROLLING_HILLS }

        val laps = prefs.getInt("track_laps", 3)
        val segmentCount = prefs.getInt("track_segments", 420)
        val nitroPads = prefs.getBoolean("track_nitro_pads", true)
        val obstacles = prefs.getBoolean("track_obstacles", true)

        return TrackConfig(
            id = "custom_track",
            theme = theme,
            weather = weather,
            curvature = curvature,
            elevation = elevation,
            laps = laps,
            segmentCount = segmentCount,
            nitroPadsEnabled = nitroPads,
            obstaclesEnabled = obstacles
        )
    }

    fun saveTrackConfig(config: TrackConfig) {
        prefs.edit()
            .putString("track_theme", config.theme.name)
            .putString("track_weather", config.weather.name)
            .putString("track_curv", config.curvature.name)
            .putString("track_elev", config.elevation.name)
            .putInt("track_laps", config.laps)
            .putInt("track_segments", config.segmentCount)
            .putBoolean("track_nitro_pads", config.nitroPadsEnabled)
            .putBoolean("track_obstacles", config.obstaclesEnabled)
            .apply()
    }

    fun getControlScheme(): TouchControlScheme {
        val name = prefs.getString("ctrl_scheme", TouchControlScheme.TOUCH_BUTTONS.name)
            ?: TouchControlScheme.TOUCH_BUTTONS.name
        return try { TouchControlScheme.valueOf(name) } catch (_: Exception) { TouchControlScheme.TOUCH_BUTTONS }
    }

    fun setControlScheme(scheme: TouchControlScheme) {
        prefs.edit().putString("ctrl_scheme", scheme.name).apply()
    }

    fun getSteeringSensitivity(): Float = prefs.getFloat("ctrl_sensitivity", 1.0f)
    fun setSteeringSensitivity(value: Float) = prefs.edit().putFloat("ctrl_sensitivity", value).apply()

    fun isVibrationEnabled(): Boolean = prefs.getBoolean("ctrl_vibrate", true)
    fun setVibrationEnabled(enabled: Boolean) = prefs.edit().putBoolean("ctrl_vibrate", enabled).apply()

    fun isAudioEnabled(): Boolean = prefs.getBoolean("audio_enabled", true)
    fun setAudioEnabled(enabled: Boolean) = prefs.edit().putBoolean("audio_enabled", enabled).apply()

    fun getBestLapTimeMs(trackId: String): Long = prefs.getLong("best_lap_$trackId", 0L)
    fun setBestLapTimeMs(trackId: String, timeMs: Long) {
        val current = getBestLapTimeMs(trackId)
        if (current == 0L || timeMs < current) {
            prefs.edit().putLong("best_lap_$trackId", timeMs).apply()
        }
    }

    fun getHighestDriftScore(): Int = prefs.getInt("best_drift_score", 12500)
    fun setHighestDriftScore(score: Int) {
        if (score > getHighestDriftScore()) {
            prefs.edit().putInt("best_drift_score", score).apply()
        }
    }

    fun getPlayerName(): String = prefs.getString("player_name", "Piloto 01") ?: "Piloto 01"
    fun setPlayerName(name: String) = prefs.edit().putString("player_name", name).apply()
}
