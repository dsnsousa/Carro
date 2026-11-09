package com.example.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.customization.GameStorage
import com.example.game.customization.TrackConfig
import com.example.game.customization.VehicleConfig
import com.example.game.engine.Vehicle3DModel
import com.example.game.input.GamepadManager
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NitroOrange
import com.example.ui.theme.SpeedYellow
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun HomeScreen(
    storage: GameStorage,
    playerVehicle: VehicleConfig,
    trackConfig: TrackConfig,
    gamepadManager: GamepadManager,
    onStartQuickRace: () -> Unit,
    onOpenMultiplayer: () -> Unit,
    onOpenGarage: () -> Unit,
    onOpenTrackEditor: () -> Unit,
    onOpenSettings: () -> Unit
) {
    // 360 rotation transition for background hero car
    val infiniteTransition = rememberInfiniteTransition()
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Area: 3D Animated Hero Car Showcase & Title
        Box(
            modifier = Modifier
                .weight(1.3f)
                .fillMaxHeight()
                .padding(20.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Game Title & Logo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.FlashOn,
                        contentDescription = null,
                        tint = NitroOrange,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Column {
                        Text(
                            text = "NITRO DRIFT 3D",
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 2.sp
                        )
                        Text(
                            text = "CORRIDA REALISTA • MULTIPLAYER • CUSTOMIZAÇÃO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Car Rotating Showcase Canvas
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF131A29), Color(0xFF080C14))
                            )
                        )
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        Vehicle3DModel.drawShowroom3DTurntable(
                            drawScope = this,
                            config = playerVehicle,
                            centerX = size.width * 0.5f,
                            centerY = size.height * 0.58f,
                            viewAngleDeg = rotationAngle,
                            scale = (size.width / 420f).coerceIn(0.85f, 1.35f)
                        )
                    }

                    // Car Badge & Stat Tag
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.75f))
                            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Column {
                            Text(
                                text = playerVehicle.model.displayName.uppercase(),
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                color = TextPrimary
                            )
                            Text(
                                text = "VEL. MÁX: ${playerVehicle.effectiveTopSpeed.toInt()} KM/H • PISTA: ${trackConfig.theme.displayName}",
                                fontSize = 10.sp,
                                color = NeonCyan
                            )
                        }
                    }

                    // Gamepad Status indicator
                    if (gamepadManager.isGamepadConnected) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0A2B1D).copy(alpha = 0.9f))
                                .border(1.dp, Color(0xFF00E676), RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
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
                                    text = "CONTROLE DETECTADO",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF00E676)
                                )
                            }
                        }
                    }
                }
            }
        }

        // Right Area: Main Navigation Buttons
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(20.dp),
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = "MENU PRINCIPAL",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = TextSecondary,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            // 1. Play Quick Race Button
            Button(
                onClick = onStartQuickRace,
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("menu_play_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = Color.Black, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("CORRIDA RÁPIDA", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 15.sp)
                        Text("Competir contra 5 pilotos de IA na pista atual", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.75f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 2. Multiplayer Button
            Button(
                onClick = onOpenMultiplayer,
                colors = ButtonDefaults.buttonColors(containerColor = NitroOrange),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("menu_multiplayer_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Black, modifier = Modifier.size(26.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("MULTIPLAYER & SALAS", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 14.sp)
                        Text("Salas Online, Tela Dividida 2P e Batalha Fantasma", fontSize = 10.sp, color = Color.Black.copy(alpha = 0.75f))
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 3. Garage & Vehicle Customization
            Button(
                onClick = onOpenGarage,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, NeonCyan.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("menu_garage_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Build, contentDescription = null, tint = NeonCyan, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("GARAGEM DE VEÍCULOS", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Text("Modelos, pinturas, decalques, spoilers e tunagem", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 4. Track Editor / Customization
            Button(
                onClick = onOpenTrackEditor,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .border(1.dp, SpeedYellow.copy(alpha = 0.5f), RoundedCornerShape(14.dp))
                    .testTag("menu_track_editor_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Map, contentDescription = null, tint = SpeedYellow, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text("EDITOR DE PISTAS", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                        Text("Temas 3D, relevo, curvas, clima e obstáculos", fontSize = 10.sp, color = TextSecondary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 5. Settings / Controls
            Button(
                onClick = onOpenSettings,
                colors = ButtonDefaults.buttonColors(containerColor = DarkSurfaceVariant),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
                    .testTag("menu_settings_button")
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Settings, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("CONTROLES & CONFIGURAÇÕES", fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 12.sp)
                }
            }
        }
    }
}
