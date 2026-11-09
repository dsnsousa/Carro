package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Gamepad
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.customization.GameStorage
import com.example.game.customization.TouchControlScheme
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
fun SettingsScreen(
    storage: GameStorage,
    gamepadManager: GamepadManager,
    onBack: () -> Unit
) {
    var selectedScheme by remember { mutableStateOf(storage.getControlScheme()) }
    var sensitivity by remember { mutableFloatStateOf(storage.getSteeringSensitivity()) }
    var vibrationEnabled by remember { mutableStateOf(storage.isVibrationEnabled()) }
    var audioEnabled by remember { mutableStateOf(storage.isAudioEnabled()) }
    var playerNameInput by remember { mutableStateOf(storage.getPlayerName()) }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Column: On-Screen Touch Controls & Profile
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = {
                        storage.setControlScheme(selectedScheme)
                        storage.setSteeringSensitivity(sensitivity)
                        storage.setVibrationEnabled(vibrationEnabled)
                        storage.setAudioEnabled(audioEnabled)
                        storage.setPlayerName(playerNameInput)
                        onBack()
                    },
                    modifier = Modifier
                        .testTag("settings_back_button")
                        .background(DarkSurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("CONFIGURAÇÕES & CONTROLES", fontSize = 18.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                    Text("Personalize controles na tela, gamepad e áudio", fontSize = 12.sp, color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Player Name input
            Text("NOME DO PILOTO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = playerNameInput,
                onValueChange = { playerNameInput = it },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = NeonCyan,
                    unfocusedBorderColor = DarkBorder,
                    focusedTextColor = TextPrimary,
                    unfocusedTextColor = TextPrimary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("settings_player_name_input")
            )

            Spacer(modifier = Modifier.height(16.dp))

            // On-screen touch scheme
            Text("MODO DE CONTROLE NA TELA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))
            TouchControlScheme.values().forEach { scheme ->
                val isSel = selectedScheme == scheme
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                        .border(1.dp, if (isSel) NeonCyan else DarkBorder, RoundedCornerShape(10.dp))
                        .clickable { selectedScheme = scheme }
                        .padding(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = if (isSel) NeonCyan else TextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(scheme.displayName, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary, modifier = Modifier.weight(1f))
                        if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Sensitivity slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("SENSIBILIDADE DA DIREÇÃO", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Text("${(sensitivity * 100).toInt()}%", fontSize = 12.sp, fontWeight = FontWeight.Black, color = NeonCyan)
            }
            Slider(
                value = sensitivity,
                onValueChange = { sensitivity = it },
                valueRange = 0.5f..2.0f,
                colors = SliderDefaults.colors(thumbColor = NeonCyan, activeTrackColor = NeonCyan)
            )
        }

        // Right Column: Hardware Gamepad Status & Vibration / Audio
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text("SUPORTE A CONTROLE (GAMEPAD FÍSICO)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Spacer(modifier = Modifier.height(8.dp))

            // Gamepad status card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkSurfaceVariant)
                    .border(1.dp, if (gamepadManager.isGamepadConnected) Color(0xFF00E676) else DarkBorder, RoundedCornerShape(12.dp))
                    .padding(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.SportsEsports,
                        contentDescription = null,
                        tint = if (gamepadManager.isGamepadConnected) Color(0xFF00E676) else TextSecondary,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (gamepadManager.isGamepadConnected) "CONTROLE CONECTADO" else "NENHUM CONTROLE DETECTADO",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            color = if (gamepadManager.isGamepadConnected) Color(0xFF00E676) else TextSecondary
                        )
                        Text(
                            text = gamepadManager.gamepadDeviceName,
                            fontSize = 11.sp,
                            color = TextPrimary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Gamepad Mapping Reference
            Text("MAPA DE BOTÕES DO CONTROLE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
            Spacer(modifier = Modifier.height(6.dp))
            val mappings = listOf(
                "Analógico Esquerdo / D-Pad" to "Direção (Virar)",
                "Gatilho Direito (R2) / Botão A" to "Acelerador (Gás)",
                "Gatilho Esquerdo (L2) / Botão X" to "Freio / Marcha Ré",
                "Botão B / Barra de Espaço" to "Freio de Mão (Drift)",
                "Botão Y / R1 / Shift" to "Nitro NOS Boost",
                "Botão Select / Tecla C" to "Alternar Câmera 3D",
                "Botão Start / Esc" to "Pausar Jogo"
            )

            mappings.forEach { (btn, action) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(btn, fontSize = 11.sp, color = SpeedYellow, fontWeight = FontWeight.Bold)
                    Text(action, fontSize = 11.sp, color = TextPrimary)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Audio & Vibration toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("VIBRAÇÃO HÁPTICA (DRIFT & BATIDAS)", fontSize = 11.sp, color = TextPrimary)
                }
                Switch(
                    checked = vibrationEnabled,
                    onCheckedChange = { vibrationEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkSurfaceVariant)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SOM DO MOTOR E EFEITOS SONOROS", fontSize = 11.sp, color = TextPrimary)
                }
                Switch(
                    checked = audioEnabled,
                    onCheckedChange = { audioEnabled = it },
                    colors = SwitchDefaults.colors(checkedThumbColor = NeonCyan, checkedTrackColor = DarkSurfaceVariant)
                )
            }
        }
    }
}
