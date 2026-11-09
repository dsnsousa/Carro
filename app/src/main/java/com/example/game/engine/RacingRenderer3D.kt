package com.example.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.customization.SceneryType
import com.example.game.customization.TrackConfig
import com.example.game.customization.WeatherCondition
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

class RacingRenderer3D(
    val trackSegments: List<TrackSegment>,
    val trackConfig: TrackConfig
) {
    private val drawDistance = 160 // segments ahead rendered
    private val cameraDepth = 0.84f // perspective FOV scaling
    private val cameraHeight = 1100f // height above road
    private val cameraOffsetBehind = 280f // distance behind car

    fun render(
        drawScope: DrawScope,
        playerCar: CarPhysics,
        opponentCars: List<CarPhysics>,
        ghostCar: CarPhysics? = null,
        cameraViewMode: CameraViewMode = CameraViewMode.CHASE_CAM
    ) {
        with(drawScope) {
            val width = size.width
            val height = size.height
            val halfW = width * 0.5f
            val halfH = height * 0.55f

            val totalSegments = trackSegments.size
            if (totalSegments == 0) return

            val playerZ = playerCar.trackDistance
            val totalTrackZ = totalSegments * TrackBuilder.SEGMENT_LENGTH

            // Camera World Position
            val camZ = (playerZ - cameraOffsetBehind).let { if (it < 0) it + totalTrackZ else it }
            val baseSegmentIdx = ((camZ / TrackBuilder.SEGMENT_LENGTH).toInt()) % totalSegments
            val baseSegment = trackSegments[baseSegmentIdx]

            // Camera Y tracks road elevation
            val camY = baseSegment.worldP1.y + when (cameraViewMode) {
                CameraViewMode.CHASE_CAM -> cameraHeight
                CameraViewMode.CLOSE_CAM -> cameraHeight * 0.7f
                CameraViewMode.HOOD_CAM -> cameraHeight * 0.35f
            }
            // Camera X tracks player lateral offset
            val camX = playerCar.playerX * (trackConfig.roadWidthMeters * 100f)

            // 1. Sky & Horizon Render
            drawSkyAndHorizon(this, width, height, playerCar.playerX, baseSegment.curve)

            // 2. Project Road Segments
            var maxClipY = height
            var accumulatedCurve = 0f
            var accumulatedCurveDelta = 0f

            val visibleSegments = ArrayList<TrackSegment>(drawDistance)

            for (n in 0 until drawDistance) {
                val segIdx = (baseSegmentIdx + n) % totalSegments
                val segment = trackSegments[segIdx]

                // World relative coords
                var segZ1 = segment.worldP1.z - camZ
                if (segZ1 < 0) segZ1 += totalTrackZ
                var segZ2 = segment.worldP2.z - camZ
                if (segZ2 < 0) segZ2 += totalTrackZ

                if (segZ1 <= 10f) continue // behind camera

                accumulatedCurveDelta += segment.curve
                accumulatedCurve += accumulatedCurveDelta * 0.04f

                // Project P1
                projectPoint(
                    segment.worldP1.x + accumulatedCurve - camX,
                    segment.worldP1.y - camY,
                    segZ1,
                    halfW, halfH,
                    trackConfig.roadWidthMeters * 100f,
                    segment.p1Screen
                )

                // Project P2
                projectPoint(
                    segment.worldP2.x + accumulatedCurve - camX,
                    segment.worldP2.y - camY,
                    segZ2,
                    halfW, halfH,
                    trackConfig.roadWidthMeters * 100f,
                    segment.p2Screen
                )

                segment.clipMaxY = maxClipY
                visibleSegments.add(segment)
            }

            // 3. Render Segments Back-to-Front
            for (i in visibleSegments.size - 1 downTo 0) {
                val seg = visibleSegments[i]
                val p1 = seg.p1Screen
                val p2 = seg.p2Screen

                if (p1.screenY >= p2.screenY) continue // occluded by hill crest

                val isEven = (seg.index / 3) % 2 == 0
                val fogFactor = (i.toFloat() / drawDistance).coerceIn(0f, 1f)

                // Road colors from track theme with depth fog
                val roadColor = applyFog(Color(trackConfig.theme.roadColorHex), fogFactor)
                val grassColor = applyFog(Color(trackConfig.theme.grassColorHex), fogFactor)
                val curbColor = if (isEven) {
                    applyFog(Color(trackConfig.theme.curbColor1Hex), fogFactor)
                } else {
                    applyFog(Color(trackConfig.theme.curbColor2Hex), fogFactor)
                }

                // Draw Grass / Roadside Background
                drawRect(
                    color = grassColor,
                    topLeft = Offset(0f, p2.screenY),
                    size = Size(width, p1.screenY - p2.screenY + 1.5f)
                )

                // Draw Curbs & Guardrails
                val curbWidthP1 = p1.screenWidth * 1.25f
                val curbWidthP2 = p2.screenWidth * 1.25f

                val curbPath = Path().apply {
                    moveTo(p1.screenX - curbWidthP1, p1.screenY)
                    lineTo(p2.screenX - curbWidthP2, p2.screenY)
                    lineTo(p2.screenX + curbWidthP2, p2.screenY)
                    lineTo(p1.screenX + curbWidthP1, p1.screenY)
                    close()
                }
                drawPath(curbPath, color = curbColor)

                // Draw Asphalt Road Surface
                val roadPath = Path().apply {
                    moveTo(p1.screenX - p1.screenWidth, p1.screenY)
                    lineTo(p2.screenX - p2.screenWidth, p2.screenY)
                    lineTo(p2.screenX + p2.screenWidth, p2.screenY)
                    lineTo(p1.screenX + p1.screenWidth, p1.screenY)
                    close()
                }
                drawPath(roadPath, color = roadColor)

                // Draw Center Lane Marking
                if (isEven) {
                    val laneW1 = p1.screenWidth * 0.045f
                    val laneW2 = p2.screenWidth * 0.045f
                    val lanePath = Path().apply {
                        moveTo(p1.screenX - laneW1, p1.screenY)
                        lineTo(p2.screenX - laneW2, p2.screenY)
                        lineTo(p2.screenX + laneW2, p2.screenY)
                        lineTo(p1.screenX + laneW1, p1.screenY)
                        close()
                    }
                    drawPath(lanePath, color = Color.White.copy(alpha = (1.0f - fogFactor) * 0.85f))
                }

                // Nitro booster pad on road
                if (seg.hasNitroPad) {
                    val nitroCenterX = p1.screenX + (seg.nitroPadOffset * p1.screenWidth)
                    val nitroPadW = p1.screenWidth * 0.28f
                    drawRoundRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.9f),
                        topLeft = Offset(nitroCenterX - nitroPadW * 0.5f, p2.screenY),
                        size = Size(nitroPadW, p1.screenY - p2.screenY),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }

                // Obstacle chicane
                if (seg.hasObstacle) {
                    val obsCenterX = p1.screenX + (seg.obstacleOffset * p1.screenWidth)
                    val obsW = p1.screenWidth * 0.2f
                    val obsH = (p1.screenY - p2.screenY) * 3f
                    drawRect(
                        color = Color(0xFFFF3D00),
                        topLeft = Offset(obsCenterX - obsW * 0.5f, p1.screenY - obsH),
                        size = Size(obsW, obsH)
                    )
                }

                // Roadside Props
                for (prop in seg.props) {
                    val propX = p1.screenX + (prop.lateralOffset * p1.screenWidth * 1.3f)
                    val propY = p1.screenY
                    val propScale = p1.scale * prop.scaleMultiplier * 1.5f
                    drawRoadsideProp(this, prop.type, propX, propY, propScale, fogFactor, trackConfig.theme.sceneryType)
                }
            }

            // 4. Render Opponent Cars (Sorted by Distance)
            val allOpponents = opponentCars.filter { !it.isPlayer }
            for (opp in allOpponents) {
                var deltaZ = opp.trackDistance - camZ
                if (deltaZ < -totalTrackZ * 0.5f) deltaZ += totalTrackZ
                if (deltaZ > totalTrackZ * 0.5f) deltaZ -= totalTrackZ

                if (deltaZ > 20f && deltaZ < drawDistance * TrackBuilder.SEGMENT_LENGTH) {
                    val oppSegIdx = ((opp.trackDistance / TrackBuilder.SEGMENT_LENGTH).toInt()) % totalSegments
                    val oppSeg = trackSegments[oppSegIdx]
                    val scale = cameraDepth / (deltaZ / 1000f + 0.35f)
                    val screenX = oppSeg.p1Screen.screenX + (opp.playerX * oppSeg.p1Screen.screenWidth)
                    val screenY = oppSeg.p1Screen.screenY

                    Vehicle3DModel.drawRaceCar(
                        drawScope = this,
                        config = opp.config,
                        centerX = screenX,
                        bottomY = screenY,
                        scale = scale * 0.0035f,
                        steerAngle = opp.driftAngle,
                        isBraking = opp.currentSpeedKmH < 100f,
                        isNitro = opp.isNitroActive,
                        isDrifting = opp.isDrifting
                    )
                }
            }

            // 5. Render Ghost Car (if active)
            if (ghostCar != null) {
                var deltaZ = ghostCar.trackDistance - camZ
                if (deltaZ < -totalTrackZ * 0.5f) deltaZ += totalTrackZ
                if (deltaZ > totalTrackZ * 0.5f) deltaZ -= totalTrackZ

                if (deltaZ > 10f && deltaZ < drawDistance * TrackBuilder.SEGMENT_LENGTH) {
                    val ghostSegIdx = ((ghostCar.trackDistance / TrackBuilder.SEGMENT_LENGTH).toInt()) % totalSegments
                    val ghostSeg = trackSegments[ghostSegIdx]
                    val scale = cameraDepth / (deltaZ / 1000f + 0.35f)
                    val screenX = ghostSeg.p1Screen.screenX + (ghostCar.playerX * ghostSeg.p1Screen.screenWidth)
                    val screenY = ghostSeg.p1Screen.screenY

                    Vehicle3DModel.drawRaceCar(
                        drawScope = this,
                        config = ghostCar.config,
                        centerX = screenX,
                        bottomY = screenY,
                        scale = scale * 0.0035f,
                        isGhost = true
                    )
                }
            }

            // 6. Render Player Car (Bottom Center)
            if (cameraViewMode != CameraViewMode.HOOD_CAM) {
                val playerCarY = height - 25f
                val playerCarX = halfW + (playerCar.playerX * 35f) // subtle cockpit sway

                Vehicle3DModel.drawRaceCar(
                    drawScope = this,
                    config = playerCar.config,
                    centerX = playerCarX,
                    bottomY = playerCarY,
                    scale = 1.35f,
                    rollAngle = playerCar.chassisRoll,
                    pitchOffset = playerCar.suspensionPitch,
                    steerAngle = playerCar.driftAngle,
                    isBraking = playerCar.currentSpeedKmH < 50f && playerCar.currentSpeedKmH > 0f,
                    isNitro = playerCar.isNitroActive,
                    isDrifting = playerCar.isDrifting
                )
            }

            // 7. Dynamic Weather & Speed FX
            if (trackConfig.weather == WeatherCondition.HEAVY_RAIN) {
                drawRainStreaks(this, width, height)
            }

            // Nitro / Top Speed Motion Lines
            if (playerCar.isNitroActive || playerCar.currentSpeedKmH > 300f) {
                drawSpeedLines(this, width, height, playerCar.isNitroActive)
            }

            // Collision sparks
            if (playerCar.collisionSparksTimer > 0f) {
                drawCollisionSparks(this, width, height, playerCar.playerX > 0)
            }
        }
    }

    private fun projectPoint(
        worldX: Float,
        worldY: Float,
        worldZ: Float,
        halfW: Float,
        halfH: Float,
        roadWidth: Float,
        target: ScreenPoint
    ) {
        val scale = cameraDepth / worldZ
        target.scale = scale
        target.screenX = halfW + (scale * worldX * halfW)
        target.screenY = halfH - (scale * worldY * halfH)
        target.screenWidth = scale * roadWidth * halfW
    }

    private fun drawSkyAndHorizon(
        drawScope: DrawScope,
        width: Float,
        height: Float,
        playerX: Float,
        curve: Float
    ) {
        with(drawScope) {
            val horizonY = height * 0.52f
            val skyTop = Color(trackConfig.theme.skyColorTopHex)
            val skyBottom = Color(trackConfig.theme.skyColorBottomHex)

            // Sky Gradient
            drawRect(
                brush = Brush.verticalGradient(
                    colors = listOf(skyTop, skyBottom),
                    startY = 0f,
                    endY = horizonY
                ),
                topLeft = Offset(0f, 0f),
                size = Size(width, horizonY)
            )

            // Distant Mountains / City Skyline Silhouette
            val parallaxOffset = (playerX * 120f) + (curve * 40f)
            val skylineY = horizonY - 45f

            when (trackConfig.theme.sceneryType) {
                SceneryType.CYBER_CITY -> {
                    // Futuristic Neon Skyscrapers Silhouette
                    val bCount = 14
                    val bWidth = width / 10f
                    for (b in 0..bCount) {
                        val bx = (b * bWidth * 0.85f - parallaxOffset * 0.5f) % (width + bWidth)
                        val bh = 60f + ((b * 37) % 85)
                        drawRect(
                            color = Color(0xFF0F121C),
                            topLeft = Offset(bx, horizonY - bh),
                            size = Size(bWidth * 0.7f, bh)
                        )
                        // Glowing skyscraper spire
                        drawRect(
                            color = if (b % 2 == 0) Color(0xFF00E5FF).copy(alpha = 0.7f) else Color(0xFFFF007F).copy(alpha = 0.7f),
                            topLeft = Offset(bx + bWidth * 0.32f, horizonY - bh - 14f),
                            size = Size(3f, 14f)
                        )
                    }
                }
                SceneryType.CANYON -> {
                    // Red Rock Mesa Silhouette
                    val mountainPath = Path().apply {
                        moveTo(0f, horizonY)
                        var mx = 0f
                        while (mx <= width) {
                            val mh = 50f + (sin(mx * 0.01f + parallaxOffset * 0.01f) * 35f)
                            lineTo(mx, horizonY - mh)
                            mx += 60f
                        }
                        lineTo(width, horizonY)
                        close()
                    }
                    drawPath(mountainPath, color = Color(0xFF38151D))
                }
                else -> {
                    // Gentle Coastal/Alpine Hills
                    val hillPath = Path().apply {
                        moveTo(0f, horizonY)
                        var hx = 0f
                        while (hx <= width) {
                            val hh = 40f + (sin(hx * 0.015f + parallaxOffset * 0.01f) * 25f)
                            lineTo(hx, horizonY - hh)
                            hx += 80f
                        }
                        lineTo(width, horizonY)
                        close()
                    }
                    drawPath(hillPath, color = Color(0xFF142436))
                }
            }
        }
    }

    private fun drawRoadsideProp(
        drawScope: DrawScope,
        type: RoadsidePropType,
        x: Float,
        y: Float,
        scale: Float,
        fogFactor: Float,
        scenery: SceneryType
    ) {
        if (scale <= 0.0005f) return
        with(drawScope) {
            val alpha = (1.0f - fogFactor * 0.7f).coerceIn(0.1f, 1.0f)
            val propSize = 180f * scale * 100f

            when (type) {
                RoadsidePropType.CYBER_LAMP -> {
                    val poleH = propSize * 1.8f
                    val poleW = 3f * scale * 100f
                    drawLine(
                        color = Color(0xFF3A4252).copy(alpha = alpha),
                        start = Offset(x, y),
                        end = Offset(x, y - poleH),
                        strokeWidth = max(2f, poleW)
                    )
                    // Neon Lamp Head
                    drawCircle(
                        color = Color(0xFF00F0FF).copy(alpha = alpha),
                        radius = max(3f, propSize * 0.12f),
                        center = Offset(x, y - poleH)
                    )
                }
                RoadsidePropType.BILLBOARD_NITRO, RoadsidePropType.BILLBOARD_DRIFT -> {
                    val boardW = propSize * 1.6f
                    val boardH = propSize * 0.9f
                    // Posts
                    drawLine(color = Color(0xFF2C3240).copy(alpha = alpha), start = Offset(x - boardW * 0.35f, y), end = Offset(x - boardW * 0.35f, y - boardH), strokeWidth = 3f)
                    drawLine(color = Color(0xFF2C3240).copy(alpha = alpha), start = Offset(x + boardW * 0.35f, y), end = Offset(x + boardW * 0.35f, y - boardH), strokeWidth = 3f)
                    // Board
                    drawRoundRect(
                        color = if (type == RoadsidePropType.BILLBOARD_NITRO) Color(0xFF00E5FF).copy(alpha = alpha) else Color(0xFFFF6D00).copy(alpha = alpha),
                        topLeft = Offset(x - boardW * 0.5f, y - boardH * 1.4f),
                        size = Size(boardW, boardH),
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                    )
                }
                RoadsidePropType.PALM_TREE -> {
                    val trunkH = propSize * 1.6f
                    drawLine(
                        color = Color(0xFF5D4037).copy(alpha = alpha),
                        start = Offset(x, y),
                        end = Offset(x, y - trunkH),
                        strokeWidth = max(3f, propSize * 0.08f)
                    )
                    // Fronds
                    drawCircle(
                        color = Color(0xFF2E7D32).copy(alpha = alpha),
                        radius = max(4f, propSize * 0.45f),
                        center = Offset(x, y - trunkH)
                    )
                }
                RoadsidePropType.SKYSCRAPER_TOWER -> {
                    val towerW = propSize * 1.3f
                    val towerH = propSize * 3.5f
                    drawRect(
                        color = Color(0xFF141926).copy(alpha = alpha),
                        topLeft = Offset(x - towerW * 0.5f, y - towerH),
                        size = Size(towerW, towerH)
                    )
                }
                else -> {
                    drawCircle(
                        color = Color(0xFF424242).copy(alpha = alpha),
                        radius = max(3f, propSize * 0.3f),
                        center = Offset(x, y - propSize * 0.3f)
                    )
                }
            }
        }
    }

    private fun drawRainStreaks(drawScope: DrawScope, width: Float, height: Float) {
        with(drawScope) {
            val rainColor = Color(0x99B0BEC5)
            for (r in 0..30) {
                val rx = (Math.random().toFloat() * width)
                val ry = (Math.random().toFloat() * height)
                drawLine(
                    color = rainColor,
                    start = Offset(rx, ry),
                    end = Offset(rx - 8f, ry + 26f),
                    strokeWidth = 1.8f
                )
            }
        }
    }

    private fun drawSpeedLines(drawScope: DrawScope, width: Float, height: Float, isNitro: Boolean) {
        with(drawScope) {
            val lineColor = if (isNitro) Color(0x8800E5FF) else Color(0x66FFFFFF)
            val halfW = width * 0.5f
            val halfH = height * 0.5f

            for (i in 0..16) {
                val angle = (Math.random() * Math.PI * 2.0).toFloat()
                val r1 = 180f + (Math.random().toFloat() * 150f)
                val r2 = r1 + 90f + (Math.random().toFloat() * 120f)

                val x1 = halfW + cos(angle) * r1
                val y1 = halfH + sin(angle) * r1
                val x2 = halfW + cos(angle) * r2
                val y2 = halfH + sin(angle) * r2

                drawLine(
                    color = lineColor,
                    start = Offset(x1, y1),
                    end = Offset(x2, y2),
                    strokeWidth = 2.5f
                )
            }
        }
    }

    private fun drawCollisionSparks(drawScope: DrawScope, width: Float, height: Float, onRightSide: Boolean) {
        with(drawScope) {
            val sparkOriginX = if (onRightSide) width * 0.85f else width * 0.15f
            val sparkOriginY = height * 0.8f

            for (s in 0..20) {
                val sparkAngle = ((Math.random() - 0.5) * Math.PI).toFloat()
                val dist = 30f + (Math.random().toFloat() * 70f)
                val color = if (s % 2 == 0) Color(0xFFFFD600) else Color(0xFFFF3D00)
                drawLine(
                    color = color,
                    start = Offset(sparkOriginX, sparkOriginY),
                    end = Offset(sparkOriginX + cos(sparkAngle) * dist, sparkOriginY + sin(sparkAngle) * dist),
                    strokeWidth = 2.5f
                )
            }
        }
    }

    private fun applyFog(color: Color, fogFactor: Float): Color {
        val fog = Color(trackConfig.theme.skyColorBottomHex)
        val f = fogFactor * 0.65f
        return Color(
            red = color.red * (1f - f) + fog.red * f,
            green = color.green * (1f - f) + fog.green * f,
            blue = color.blue * (1f - f) + fog.blue * f,
            alpha = color.alpha
        )
    }
}

enum class CameraViewMode(val displayName: String) {
    CHASE_CAM("Câmera Terceira Pessoa"),
    CLOSE_CAM("Câmera de Ação Próxima"),
    HOOD_CAM("Câmera Capô / Cockpit")
}
