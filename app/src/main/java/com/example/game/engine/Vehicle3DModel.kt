package com.example.game.engine

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import com.example.game.customization.CarModel
import com.example.game.customization.DecalType
import com.example.game.customization.PaintFinish
import com.example.game.customization.RimDesign
import com.example.game.customization.SpoilerType
import com.example.game.customization.VehicleConfig
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

object Vehicle3DModel {

    /**
     * Draws the car in racing perspective from behind/chase-cam.
     * [centerX], [bottomY]: Screen anchor coordinates.
     * [scale]: Perspective scaling factor based on Z depth.
     * [rollAngle]: Chassis roll in radians when turning/drifting.
     * [pitchOffset]: Suspension bounce.
     * [steerAngle]: Front wheel turn angle.
     * [isBraking]: Taillights ignite bright red.
     * [isNitro]: Flaming exhaust trails.
     * [isDrifting]: Tire smoke clouds.
     */
    fun drawRaceCar(
        drawScope: DrawScope,
        config: VehicleConfig,
        centerX: Float,
        bottomY: Float,
        scale: Float,
        rollAngle: Float = 0f,
        pitchOffset: Float = 0f,
        steerAngle: Float = 0f,
        isBraking: Boolean = false,
        isNitro: Boolean = false,
        isDrifting: Boolean = false,
        isGhost: Boolean = false
    ) {
        with(drawScope) {
            val alpha = if (isGhost) 0.45f else 1.0f

            val baseWidth = 140f * scale
            val baseHeight = 70f * scale
            val halfW = baseWidth * 0.5f

            val primaryColor = Color(config.primaryColorHex).copy(alpha = alpha)
            val secondaryColor = Color(config.secondaryColorHex).copy(alpha = alpha)
            val darkShadow = Color(0xFF07090E).copy(alpha = 0.85f * alpha)
            val rimColor = Color(config.rimColorHex).copy(alpha = alpha)

            // Neon Underglow
            if (config.neonUnderglowHex != 0x00000000L) {
                val neon = Color(config.neonUnderglowHex).copy(alpha = 0.6f * alpha)
                drawOval(
                    brush = Brush.radialGradient(
                        colors = listOf(neon, Color.Transparent),
                        center = Offset(centerX, bottomY + 4f * scale),
                        radius = baseWidth * 0.75f
                    ),
                    topLeft = Offset(centerX - baseWidth * 0.75f, bottomY - 10f * scale),
                    size = androidx.compose.ui.geometry.Size(baseWidth * 1.5f, 28f * scale)
                )
            }

            // Ground Shadow
            drawOval(
                color = Color.Black.copy(alpha = 0.75f * alpha),
                topLeft = Offset(centerX - halfW * 1.15f, bottomY - 6f * scale),
                size = androidx.compose.ui.geometry.Size(baseWidth * 1.15f, 18f * scale)
            )

            // Tire Smoke when drifting or burnout
            if (isDrifting) {
                val smokeColor = Color(config.tireSmokeHex).copy(alpha = 0.55f * alpha)
                for (s in 0..4) {
                    val offsetX = (s - 2) * 16f * scale
                    drawCircle(
                        color = smokeColor,
                        radius = (14f + s * 4f) * scale,
                        center = Offset(centerX - halfW * 0.8f + offsetX, bottomY - (6f + s * 5f) * scale)
                    )
                    drawCircle(
                        color = smokeColor,
                        radius = (14f + s * 4f) * scale,
                        center = Offset(centerX + halfW * 0.8f + offsetX, bottomY - (6f + s * 5f) * scale)
                    )
                }
            }

            // Nitro Exhaust Flame
            if (isNitro) {
                val flameBrush = Brush.verticalGradient(
                    colors = listOf(Color(0xFF00E5FF), Color(0xFFFF9100), Color(0xFFFF1744), Color.Transparent)
                )
                val leftPipe = Offset(centerX - halfW * 0.45f, bottomY - 12f * scale)
                val rightPipe = Offset(centerX + halfW * 0.45f, bottomY - 12f * scale)

                val flameH = (35f + (Math.random().toFloat() * 20f)) * scale
                val flameW = 12f * scale

                val pLeft = Path().apply {
                    moveTo(leftPipe.x - flameW * 0.5f, leftPipe.y)
                    lineTo(leftPipe.x, leftPipe.y + flameH)
                    lineTo(leftPipe.x + flameW * 0.5f, leftPipe.y)
                    close()
                }
                drawPath(pLeft, brush = flameBrush)

                val pRight = Path().apply {
                    moveTo(rightPipe.x - flameW * 0.5f, rightPipe.y)
                    lineTo(rightPipe.x, rightPipe.y + flameH)
                    lineTo(rightPipe.x + flameW * 0.5f, rightPipe.y)
                    close()
                }
                drawPath(pRight, brush = flameBrush)
            }

            // Chassis & Wheels
            val carCenterY = bottomY - baseHeight * 0.5f + pitchOffset * 10f * scale
            val rollOffset = rollAngle * 12f * scale

            // Wheels (Left and Right rear tires)
            val tireWidth = 24f * scale
            val tireHeight = 36f * scale
            val tireY = bottomY - tireHeight * 0.85f

            // Left Tire
            drawRoundRect(
                color = Color(0xFF141416).copy(alpha = alpha),
                topLeft = Offset(centerX - halfW * 0.98f, tireY - rollOffset),
                size = androidx.compose.ui.geometry.Size(tireWidth, tireHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
            )
            // Left Rim
            drawCircle(
                color = rimColor,
                radius = tireWidth * 0.35f,
                center = Offset(centerX - halfW * 0.98f + tireWidth * 0.5f, tireY - rollOffset + tireHeight * 0.5f)
            )

            // Right Tire
            drawRoundRect(
                color = Color(0xFF141416).copy(alpha = alpha),
                topLeft = Offset(centerX + halfW * 0.98f - tireWidth, tireY + rollOffset),
                size = androidx.compose.ui.geometry.Size(tireWidth, tireHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(6f * scale, 6f * scale)
            )
            // Right Rim
            drawCircle(
                color = rimColor,
                radius = tireWidth * 0.35f,
                center = Offset(centerX + halfW * 0.98f - tireWidth * 0.5f, tireY + rollOffset + tireHeight * 0.5f)
            )

            // Rear Diffuser & Exhaust
            val diffuserPath = Path().apply {
                moveTo(centerX - halfW * 0.75f, bottomY - 6f * scale - rollOffset * 0.5f)
                lineTo(centerX + halfW * 0.75f, bottomY - 6f * scale + rollOffset * 0.5f)
                lineTo(centerX + halfW * 0.65f, bottomY - 20f * scale + rollOffset * 0.5f)
                lineTo(centerX - halfW * 0.65f, bottomY - 20f * scale - rollOffset * 0.5f)
                close()
            }
            drawPath(diffuserPath, color = darkShadow)

            // Main Lower Bumper & Body Shell
            val bodyPath = Path().apply {
                moveTo(centerX - halfW * 0.92f, bottomY - 18f * scale - rollOffset)
                lineTo(centerX + halfW * 0.92f, bottomY - 18f * scale + rollOffset)
                lineTo(centerX + halfW * 0.85f, bottomY - 48f * scale + rollOffset)
                lineTo(centerX - halfW * 0.85f, bottomY - 48f * scale - rollOffset)
                close()
            }

            // Finish Gradient Shading
            val finishBrush = when (config.finish) {
                PaintFinish.GLOSS -> Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(red = primaryColor.red * 0.75f, green = primaryColor.green * 0.75f, blue = primaryColor.blue * 0.75f)
                    )
                )
                PaintFinish.MATTE -> Brush.verticalGradient(
                    colors = listOf(
                        primaryColor.copy(alpha = 0.9f),
                        primaryColor.copy(alpha = 0.95f)
                    )
                )
                PaintFinish.CHROME -> Brush.linearGradient(
                    colors = listOf(Color.White.copy(alpha = 0.7f), primaryColor, Color.White.copy(alpha = 0.7f))
                )
                PaintFinish.CARBON -> Brush.linearGradient(
                    colors = listOf(Color(0xFF22252A), primaryColor.copy(alpha = 0.85f), Color(0xFF15171A))
                )
            }
            drawPath(bodyPath, brush = finishBrush)

            // Upper Cockpit / Greenhouse / Windshield
            val cockpitTopW = halfW * 0.52f
            val cockpitBottomW = halfW * 0.75f
            val cockpitTopY = bottomY - baseHeight * 1.05f
            val cockpitBottomY = bottomY - 46f * scale

            val cockpitPath = Path().apply {
                moveTo(centerX - cockpitTopW, cockpitTopY - rollOffset * 1.2f)
                lineTo(centerX + cockpitTopW, cockpitTopY + rollOffset * 1.2f)
                lineTo(centerX + cockpitBottomW, cockpitBottomY + rollOffset)
                lineTo(centerX - cockpitBottomW, cockpitBottomY - rollOffset)
                close()
            }
            // Glass tint
            val glassBrush = Brush.verticalGradient(
                colors = listOf(Color(0xFF0F1522).copy(alpha = 0.92f * alpha), Color(0xFF2C394F).copy(alpha = 0.85f * alpha))
            )
            drawPath(cockpitPath, brush = glassBrush)
            drawPath(cockpitPath, color = primaryColor, style = Stroke(width = 2.5f * scale))

            // Decal / Racing Stripes
            if (config.decal == DecalType.TWIN_STRIPES) {
                val stripeW = 8f * scale
                val gap = 6f * scale
                // Left stripe
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(centerX - gap - stripeW, cockpitBottomY - 2f * scale - rollOffset),
                    size = androidx.compose.ui.geometry.Size(stripeW, 28f * scale)
                )
                // Right stripe
                drawRect(
                    color = secondaryColor,
                    topLeft = Offset(centerX + gap, cockpitBottomY - 2f * scale + rollOffset),
                    size = androidx.compose.ui.geometry.Size(stripeW, 28f * scale)
                )
            } else if (config.decal == DecalType.FLAME_DECALS) {
                val flameDecalPath = Path().apply {
                    moveTo(centerX - 16f * scale, cockpitBottomY + 12f * scale)
                    lineTo(centerX, cockpitBottomY + 2f * scale)
                    lineTo(centerX + 16f * scale, cockpitBottomY + 12f * scale)
                    lineTo(centerX, cockpitBottomY + 24f * scale)
                    close()
                }
                drawPath(flameDecalPath, color = secondaryColor)
            }

            // Racing Number badge
            drawCircle(
                color = Color.White.copy(alpha = 0.9f * alpha),
                radius = 11f * scale,
                center = Offset(centerX, bottomY - 32f * scale)
            )
            drawCircle(
                color = Color.Black.copy(alpha = 0.9f * alpha),
                radius = 10f * scale,
                center = Offset(centerX, bottomY - 32f * scale)
            )

            // Taillights (Glowing Red or Amber)
            val lightColor = if (isBraking) Color(0xFFFF0033) else Color(0xFFFF2A2A)
            val lightHeight = 9f * scale
            val lightWidth = 26f * scale

            // Left Taillight
            drawRoundRect(
                color = lightColor,
                topLeft = Offset(centerX - halfW * 0.78f, bottomY - 44f * scale - rollOffset),
                size = androidx.compose.ui.geometry.Size(lightWidth, lightHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale, 3f * scale)
            )
            // Left Glow Halo
            if (isBraking) {
                drawCircle(
                    color = Color(0xFFFF0033).copy(alpha = 0.45f),
                    radius = lightWidth * 0.9f,
                    center = Offset(centerX - halfW * 0.78f + lightWidth * 0.5f, bottomY - 44f * scale - rollOffset + lightHeight * 0.5f)
                )
            }

            // Right Taillight
            drawRoundRect(
                color = lightColor,
                topLeft = Offset(centerX + halfW * 0.78f - lightWidth, bottomY - 44f * scale + rollOffset),
                size = androidx.compose.ui.geometry.Size(lightWidth, lightHeight),
                cornerRadius = androidx.compose.ui.geometry.CornerRadius(3f * scale, 3f * scale)
            )
            // Right Glow Halo
            if (isBraking) {
                drawCircle(
                    color = Color(0xFFFF0033).copy(alpha = 0.45f),
                    radius = lightWidth * 0.9f,
                    center = Offset(centerX + halfW * 0.78f - lightWidth * 0.5f, bottomY - 44f * scale + rollOffset + lightHeight * 0.5f)
                )
            }

            // Spoiler Wing
            if (config.spoiler != SpoilerType.STOCK) {
                val spoilerWidth = when (config.spoiler) {
                    SpoilerType.DUCKTAIL -> halfW * 1.2f
                    SpoilerType.GT_WING -> halfW * 1.55f
                    SpoilerType.EXTREME_TIME_ATTACK -> halfW * 1.8f
                    else -> halfW * 1.4f
                }
                val spoilerY = bottomY - 52f * scale
                val spoilerH = if (config.spoiler == SpoilerType.EXTREME_TIME_ATTACK) 12f * scale else 8f * scale

                // Spoiler struts / mounts
                val strutOffset = halfW * 0.4f
                drawLine(
                    color = Color(0xFF1E2129),
                    start = Offset(centerX - strutOffset, bottomY - 44f * scale - rollOffset),
                    end = Offset(centerX - strutOffset, spoilerY - rollOffset * 1.2f),
                    strokeWidth = 3f * scale
                )
                drawLine(
                    color = Color(0xFF1E2129),
                    start = Offset(centerX + strutOffset, bottomY - 44f * scale + rollOffset),
                    end = Offset(centerX + strutOffset, spoilerY + rollOffset * 1.2f),
                    strokeWidth = 3f * scale
                )

                // Wing blade
                val wingPath = Path().apply {
                    moveTo(centerX - spoilerWidth * 0.5f, spoilerY - rollOffset * 1.2f)
                    lineTo(centerX + spoilerWidth * 0.5f, spoilerY + rollOffset * 1.2f)
                    lineTo(centerX + spoilerWidth * 0.48f, spoilerY - spoilerH + rollOffset * 1.2f)
                    lineTo(centerX - spoilerWidth * 0.48f, spoilerY - spoilerH - rollOffset * 1.2f)
                    close()
                }
                drawPath(wingPath, color = Color(0xFF15181E))
                drawPath(wingPath, color = secondaryColor, style = Stroke(width = 1.5f * scale))
            }
        }
    }

    /**
     * 360-degree interactive 3D turntable for Garage Showroom.
     * [angleDeg]: Horizontal rotation angle (0..360).
     */
    fun drawShowroom3DTurntable(
        drawScope: DrawScope,
        config: VehicleConfig,
        centerX: Float,
        centerY: Float,
        viewAngleDeg: Float,
        scale: Float
    ) {
        with(drawScope) {
            val rad = Math.toRadians(viewAngleDeg.toDouble()).toFloat()
            val cosA = cos(rad)
            val sinA = sin(rad)

            val primaryColor = Color(config.primaryColorHex)
            val secondaryColor = Color(config.secondaryColorHex)
            val darkColor = Color(0xFF161922)

            val carLength = 220f * scale
            val carWidth = 110f * scale
            val carHeight = 65f * scale

            // Rotating Turntable Base Platform
            drawOval(
                brush = Brush.radialGradient(
                    colors = listOf(Color(0xFF222B3D), Color(0xFF0F141F), Color.Transparent),
                    center = Offset(centerX, centerY + 55f * scale),
                    radius = 200f * scale
                ),
                topLeft = Offset(centerX - 190f * scale, centerY + 30f * scale),
                size = androidx.compose.ui.geometry.Size(380f * scale, 70f * scale)
            )

            // Neon Underglow Ring
            if (config.neonUnderglowHex != 0x00000000L) {
                val neon = Color(config.neonUnderglowHex)
                drawOval(
                    color = neon.copy(alpha = 0.55f),
                    topLeft = Offset(centerX - 160f * scale, centerY + 40f * scale),
                    size = androidx.compose.ui.geometry.Size(320f * scale, 50f * scale),
                    style = Stroke(width = 4f * scale)
                )
            }

            // Project 3D Bounding Box Vertices
            // Local 3D Coordinates: X = width, Y = height, Z = length
            val halfL = carLength * 0.5f
            val halfW = carWidth * 0.5f
            val topH = -carHeight

            // Rotated points on screen
            fun project(x: Float, y: Float, z: Float): Offset {
                val rotX = x * cosA - z * sinA
                val rotZ = x * sinA + z * cosA
                // Isometric perspective slant
                val scrX = centerX + rotX
                val scrY = centerY + y + rotZ * 0.35f
                return Offset(scrX, scrY)
            }

            // 8 Box Vertices
            val bfl = project(-halfW, 0f, -halfL) // bottom front left
            val bfr = project(halfW, 0f, -halfL)  // bottom front right
            val bbl = project(-halfW, 0f, halfL)  // bottom back left
            val bbr = project(halfW, 0f, halfL)   // bottom back right

            val tfl = project(-halfW * 0.9f, topH * 0.5f, -halfL * 0.9f)
            val tfr = project(halfW * 0.9f, topH * 0.5f, -halfL * 0.9f)
            val tbl = project(-halfW * 0.9f, topH * 0.5f, halfL * 0.9f)
            val tbr = project(halfW * 0.9f, topH * 0.5f, halfL * 0.9f)

            // Cabin Roof Points
            val rfl = project(-halfW * 0.6f, topH, -halfL * 0.2f)
            val rfr = project(halfW * 0.6f, topH, -halfL * 0.2f)
            val rbl = project(-halfW * 0.6f, topH, halfL * 0.4f)
            val rbr = project(halfW * 0.6f, topH, halfL * 0.4f)

            // Draw Base Shadow
            drawPath(
                Path().apply {
                    moveTo(bfl.x, bfl.y)
                    lineTo(bfr.x, bfr.y)
                    lineTo(bbr.x, bbr.y)
                    lineTo(bbl.x, bbl.y)
                    close()
                },
                color = Color.Black.copy(alpha = 0.6f)
            )

            // Draw Lower Body Faces with dynamic directional lighting
            // Side 1
            drawPath(
                Path().apply {
                    moveTo(bfl.x, bfl.y)
                    lineTo(bfr.x, bfr.y)
                    lineTo(tfr.x, tfr.y)
                    lineTo(tfl.x, tfl.y)
                    close()
                },
                color = primaryColor.copy(alpha = 0.9f)
            )
            // Side 2
            drawPath(
                Path().apply {
                    moveTo(bfr.x, bfr.y)
                    lineTo(bbr.x, bbr.y)
                    lineTo(tbr.x, tbr.y)
                    lineTo(tfr.x, tfr.y)
                    close()
                },
                color = primaryColor.copy(red = primaryColor.red * 0.8f, green = primaryColor.green * 0.8f, blue = primaryColor.blue * 0.8f)
            )
            // Side 3
            drawPath(
                Path().apply {
                    moveTo(bbr.x, bbr.y)
                    lineTo(bbl.x, bbl.y)
                    lineTo(tbl.x, tbl.y)
                    lineTo(tbr.x, tbr.y)
                    close()
                },
                color = primaryColor.copy(red = primaryColor.red * 0.65f, green = primaryColor.green * 0.65f, blue = primaryColor.blue * 0.65f)
            )
            // Side 4
            drawPath(
                Path().apply {
                    moveTo(bbl.x, bbl.y)
                    lineTo(bfl.x, bfl.y)
                    lineTo(tfl.x, tfl.y)
                    lineTo(tbl.x, tbl.y)
                    close()
                },
                color = primaryColor.copy(red = primaryColor.red * 0.75f, green = primaryColor.green * 0.75f, blue = primaryColor.blue * 0.75f)
            )

            // Hood & Trunk Top Surface
            drawPath(
                Path().apply {
                    moveTo(tfl.x, tfl.y)
                    lineTo(tfr.x, tfr.y)
                    lineTo(tbr.x, tbr.y)
                    lineTo(tbl.x, tbl.y)
                    close()
                },
                color = primaryColor
            )

            // Cabin Roof & Glass Windshield
            drawPath(
                Path().apply {
                    moveTo(rfl.x, rfl.y)
                    lineTo(rfr.x, rfr.y)
                    lineTo(tfr.x, tfr.y)
                    lineTo(tfl.x, tfl.y)
                    close()
                },
                color = Color(0xFF1E2838) // Windshield
            )
            drawPath(
                Path().apply {
                    moveTo(rfl.x, rfl.y)
                    lineTo(rfr.x, rfr.y)
                    lineTo(rbr.x, rbr.y)
                    lineTo(rbl.x, rbl.y)
                    close()
                },
                color = darkColor // Roof
            )
            drawPath(
                Path().apply {
                    moveTo(rfr.x, rfr.y)
                    lineTo(rbr.x, rbr.y)
                    lineTo(tbr.x, tbr.y)
                    lineTo(tfr.x, tfr.y)
                    close()
                },
                color = Color(0xFF121B27) // Side Window
            )

            // Decal Highlight on Hood
            if (config.decal != DecalType.NONE) {
                drawLine(
                    color = secondaryColor,
                    start = Offset((tfl.x + tfr.x) * 0.5f, (tfl.y + tfr.y) * 0.5f),
                    end = Offset((rfl.x + rfr.x) * 0.5f, (rfl.y + rfr.y) * 0.5f),
                    strokeWidth = 10f * scale
                )
            }

            // Outline for Crisp High-tech Shading
            drawPath(
                Path().apply {
                    moveTo(tfl.x, tfl.y); lineTo(tfr.x, tfr.y); lineTo(tbr.x, tbr.y); lineTo(tbl.x, tbl.y); close()
                },
                color = Color.White.copy(alpha = 0.3f),
                style = Stroke(width = 2f * scale)
            )
        }
    }
}
