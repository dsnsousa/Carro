package com.example.game.engine

import com.example.game.customization.CurvatureDifficulty
import com.example.game.customization.ElevationProfile
import com.example.game.customization.SceneryType
import com.example.game.customization.TrackConfig
import kotlin.math.PI
import kotlin.math.sin

object TrackBuilder {
    const val SEGMENT_LENGTH = 150f // World Z depth per segment

    fun buildTrack(config: TrackConfig): List<TrackSegment> {
        val totalSegments = config.segmentCount.coerceIn(200, 800)
        val segments = ArrayList<TrackSegment>(totalSegments)

        val curveMax = config.curvature.maxCurve
        val elevMax = config.elevation.maxElevationDelta

        // Precompute track elevation and curve profiles
        val curves = FloatArray(totalSegments)
        val elevations = FloatArray(totalSegments)

        // Generate curved and elevated sections
        var i = 0
        while (i < totalSegments) {
            val sectionLength = (30..70).random()
            val endIdx = (i + sectionLength).coerceAtMost(totalSegments)

            // Section type: straight, gentle curve, hairpin, or chicane
            val sectionType = (0..3).random()
            val targetCurve = when (sectionType) {
                0 -> 0f // Straight
                1 -> if (Math.random() > 0.5) curveMax * 0.5f else -curveMax * 0.5f
                2 -> if (Math.random() > 0.5) curveMax else -curveMax // Sharp curve
                else -> if (Math.random() > 0.5) curveMax * 0.8f else -curveMax * 0.8f // S-bend
            }

            // Elevation wave
            val hillFreq = (1..3).random()
            val hillAmp = if (config.elevation != ElevationProfile.FLAT) {
                (Math.random().toFloat() * 2f - 1f) * elevMax
            } else 0f

            for (idx in i until endIdx) {
                val progress = (idx - i).toFloat() / (endIdx - i)
                val smoothCurve = targetCurve * sin(progress * PI.toFloat())
                curves[idx] = smoothCurve

                val hillY = hillAmp * sin(progress * PI.toFloat() * hillFreq)
                elevations[idx] = hillY
            }
            i = endIdx
        }

        // Loop smoothing: bring start and end segments to 0 elevation and 0 curve
        val blendCount = 30
        for (b in 0 until blendCount) {
            val factor = b.toFloat() / blendCount
            curves[b] *= factor
            elevations[b] *= factor
            val tailIdx = totalSegments - 1 - b
            curves[tailIdx] *= factor
            elevations[tailIdx] *= factor
        }

        var currentX = 0f
        var currentY = 0f
        var currentZ = 0f

        for (idx in 0 until totalSegments) {
            val nextZ = currentZ + SEGMENT_LENGTH
            val curve = curves[idx]
            val elev = elevations[idx]

            val p1 = Point3D(currentX, currentY, currentZ)
            val p2 = Point3D(currentX, currentY + elev, nextZ)

            // Place Nitro pads every ~40-60 segments
            val hasNitro = config.nitroPadsEnabled && (idx % 48 == 20)
            val nitroOffset = if (idx % 2 == 0) 0.35f else -0.35f

            // Place occasional obstacle
            val hasObstacle = config.obstaclesEnabled && (idx % 75 == 40) && idx > 50
            val obsOffset = if (idx % 2 == 0) -0.5f else 0.5f

            // Generate roadside props depending on scenery
            val props = mutableListOf<RoadsideProp>()
            when (config.theme.sceneryType) {
                SceneryType.CYBER_CITY -> {
                    if (idx % 4 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.CYBER_LAMP, -1.35f))
                        props.add(RoadsideProp(RoadsidePropType.CYBER_LAMP, 1.35f))
                    }
                    if (idx % 18 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.SKYSCRAPER_TOWER, -2.4f, 1.8f))
                        props.add(RoadsideProp(RoadsidePropType.SKYSCRAPER_TOWER, 2.5f, 2.1f))
                    }
                    if (idx % 35 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.NEON_OVERHEAD_ARCH, 0f, 1.2f))
                    }
                    if (idx % 24 == 12) {
                        props.add(RoadsideProp(RoadsidePropType.BILLBOARD_NITRO, -1.8f, 1.0f))
                    }
                }
                SceneryType.CANYON -> {
                    if (idx % 6 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.CANYON_ROCK, if (idx % 2 == 0) -1.8f else 1.9f, 1.5f))
                    }
                    if (idx % 20 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.BILLBOARD_DRIFT, 1.7f, 1.1f))
                    }
                }
                SceneryType.COASTAL -> {
                    if (idx % 5 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.PALM_TREE, -1.6f, 1.3f))
                        props.add(RoadsideProp(RoadsidePropType.PALM_TREE, 1.6f, 1.3f))
                    }
                    if (idx % 30 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.GRANDSTAND, -2.2f, 1.5f))
                    }
                }
                SceneryType.ALPINE_SNOW -> {
                    if (idx % 4 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.SNOW_PINE, if (idx % 2 == 0) -1.7f else 1.7f, 1.4f))
                    }
                    if (idx % 25 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.CYBER_LAMP, 1.4f))
                    }
                }
                SceneryType.INDUSTRIAL -> {
                    if (idx % 8 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.CYBER_LAMP, -1.4f))
                        props.add(RoadsideProp(RoadsidePropType.CANYON_ROCK, 1.9f, 1.2f))
                    }
                    if (idx % 20 == 0) {
                        props.add(RoadsideProp(RoadsidePropType.BILLBOARD_NITRO, -1.9f))
                    }
                }
            }

            segments.add(
                TrackSegment(
                    index = idx,
                    worldP1 = p1,
                    worldP2 = p2,
                    curve = curve,
                    elevation = elev,
                    bankingAngle = curve * 0.12f,
                    hasNitroPad = hasNitro,
                    nitroPadOffset = nitroOffset,
                    hasObstacle = hasObstacle,
                    obstacleOffset = obsOffset,
                    props = props
                )
            )

            currentZ = nextZ
        }

        return segments
    }
}
