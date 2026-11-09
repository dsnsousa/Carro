package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.customization.CurvatureDifficulty
import com.example.game.customization.ElevationProfile
import com.example.game.customization.GameStorage
import com.example.game.customization.TrackConfig
import com.example.game.customization.TrackTheme
import com.example.game.customization.WeatherCondition
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.SpeedYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TrackEditorScreen(
    storage: GameStorage,
    initialConfig: TrackConfig,
    onBack: () -> Unit,
    onSaveAndRace: (TrackConfig) -> Unit
) {
    var trackConfig by remember { mutableStateOf(initialConfig) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Column: Interactive Track Minimap & Blueprint Radar
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("track_editor_back_button")
                            .background(DarkSurfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "EDITOR DE PISTAS 3D",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = SpeedYellow,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = "${trackConfig.theme.displayName} • ${String.format("%.1f", trackConfig.totalDistanceKm)} KM",
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Track Blueprint Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF0F1524), Color(0xFF070B14))
                            )
                        )
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize().padding(24.dp)) {
                        val centerX = size.width * 0.5f
                        val centerY = size.height * 0.5f
                        val radiusX = size.width * 0.38f
                        val radiusY = size.height * 0.36f

                        // Grid lines
                        val gridColor = Color(0xFF1E283C).copy(alpha = 0.5f)
                        for (gx in 0..6) {
                            drawLine(gridColor, Offset(size.width * (gx / 6f), 0f), Offset(size.width * (gx / 6f), size.height))
                        }
                        for (gy in 0..4) {
                            drawLine(gridColor, Offset(0f, size.height * (gy / 4f)), Offset(size.width, size.height * (gy / 4f)))
                        }

                        // Generate procedural track shape based on curvature & theme
                        val path = Path()
                        val points = 36
                        val curveModifier = trackConfig.curvature.maxCurve * 0.15f

                        for (p in 0..points) {
                            val angle = (p.toFloat() / points) * 2f * PI.toFloat()
                            val noise = sin(angle * 3f) * curveModifier + cos(angle * 2f) * 0.1f
                            val px = centerX + cos(angle) * radiusX * (1f + noise)
                            val py = centerY + sin(angle) * radiusY * (1f - noise * 0.6f)

                            if (p == 0) path.moveTo(px, py) else path.lineTo(px, py)
                        }
                        path.close()

                        // Glow border
                        drawPath(path, color = Color(trackConfig.theme.curbColor1Hex).copy(alpha = 0.3f), style = Stroke(width = 16f))
                        // Main track lane
                        drawPath(path, color = Color(trackConfig.theme.roadColorHex), style = Stroke(width = 10f))
                        // Center dashed line
                        drawPath(path, color = Color.White.copy(alpha = 0.85f), style = Stroke(width = 2.5f))

                        // Start / Finish Line marker
                        drawCircle(color = SpeedYellow, radius = 6f, center = Offset(centerX + radiusX, centerY))
                    }

                    // Blueprint HUD Info
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                    ) {
                        Text(
                            text = "CIRCUITO: ${trackConfig.laps} VOLTAS",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan
                        )
                        Text(
                            text = "ADERÊNCIA: ${(trackConfig.weather.roadGripMultiplier * 100).toInt()}% • ELEVAÇÃO: ${trackConfig.elevation.displayName}",
                            fontSize = 10.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Button(
                    onClick = {
                        storage.saveTrackConfig(trackConfig)
                        onSaveAndRace(trackConfig)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SpeedYellow),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("track_editor_save_button")
                ) {
                    Text(
                        text = "CORRER NESTA PISTA",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Right Column: Track Configuration Options
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("AMBIENTE & CENÁRIO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            TrackTheme.values().forEach { theme ->
                val isSel = trackConfig.theme == theme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                        .border(1.dp, if (isSel) SpeedYellow else DarkBorder, RoundedCornerShape(10.dp))
                        .clickable { trackConfig = trackConfig.copy(theme = theme) }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Landscape,
                            contentDescription = null,
                            tint = if (isSel) SpeedYellow else TextSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(theme.displayName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                            Text(theme.description, color = TextSecondary, fontSize = 10.sp)
                        }
                        if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = SpeedYellow)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("CLIMA & CONDIÇÃO DA PISTA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WeatherCondition.values().forEach { weather ->
                    val isSel = trackConfig.weather == weather
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                            .border(1.dp, if (isSel) SpeedYellow else DarkBorder, RoundedCornerShape(8.dp))
                        .clickable { trackConfig = trackConfig.copy(weather = weather) }
                        .padding(vertical = 10.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = weather.displayName.split(" ")[0],
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) SpeedYellow else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("CURVATURA & DIFICULDADE", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CurvatureDifficulty.values().forEach { curv ->
                    val isSel = trackConfig.curvature == curv
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                            .border(1.dp, if (isSel) SpeedYellow else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { trackConfig = trackConfig.copy(curvature = curv) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            curv.displayName.split(" ")[0],
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) SpeedYellow else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text("PERFIL DE ELEVAÇÃO (COLINAS E SALTOS)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ElevationProfile.values().forEach { elev ->
                    val isSel = trackConfig.elevation == elev
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                            .border(1.dp, if (isSel) SpeedYellow else DarkBorder, RoundedCornerShape(8.dp))
                            .clickable { trackConfig = trackConfig.copy(elevation = elev) }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            elev.displayName.split(" ")[0],
                            fontSize = 11.sp,
                            fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSel) SpeedYellow else TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Number of Laps slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("NÚMERO DE VOLTAS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${trackConfig.laps} Voltas", fontSize = 12.sp, color = SpeedYellow, fontWeight = FontWeight.Black)
            }
            Slider(
                value = trackConfig.laps.toFloat(),
                onValueChange = { trackConfig = trackConfig.copy(laps = it.toInt()) },
                valueRange = 1f..5f,
                steps = 3,
                colors = SliderDefaults.colors(thumbColor = SpeedYellow, activeTrackColor = SpeedYellow)
            )

            // Length Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("EXTENSÃO DA PISTA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${String.format("%.1f", trackConfig.totalDistanceKm)} km", fontSize = 12.sp, color = SpeedYellow, fontWeight = FontWeight.Black)
            }
            Slider(
                value = trackConfig.segmentCount.toFloat(),
                onValueChange = { trackConfig = trackConfig.copy(segmentCount = it.toInt()) },
                valueRange = 250f..650f,
                steps = 7,
                colors = SliderDefaults.colors(thumbColor = SpeedYellow, activeTrackColor = SpeedYellow)
            )

            // Nitro Pads & Obstacles Toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("PADS DE NITRO BOOST NA PISTA", fontSize = 12.sp, color = TextPrimary)
                Switch(
                    checked = trackConfig.nitroPadsEnabled,
                    onCheckedChange = { trackConfig = trackConfig.copy(nitroPadsEnabled = it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = SpeedYellow, checkedTrackColor = DarkSurfaceVariant)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("BARREIRAS & OBSTÁCULOS TÉCNICOS", fontSize = 12.sp, color = TextPrimary)
                Switch(
                    checked = trackConfig.obstaclesEnabled,
                    onCheckedChange = { trackConfig = trackConfig.copy(obstaclesEnabled = it) },
                    colors = SwitchDefaults.colors(checkedThumbColor = SpeedYellow, checkedTrackColor = DarkSurfaceVariant)
                )
            }
        }
    }
}
