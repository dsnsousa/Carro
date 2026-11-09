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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import com.example.game.customization.VehicleConfig
import com.example.game.multiplayer.MultiplayerEngine
import com.example.game.multiplayer.MultiplayerMode
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
fun MultiplayerScreen(
    multiplayerEngine: MultiplayerEngine,
    playerVehicle: VehicleConfig,
    playerName: String,
    onBack: () -> Unit,
    onStartRace: (MultiplayerMode) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var joinCodeInput by remember { mutableStateOf("") }
    val roomCode by multiplayerEngine.roomCode.collectAsState()
    val lobbySlots by multiplayerEngine.lobbySlots.collectAsState()

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Column: Mode Selection & Setup
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .testTag("multiplayer_back_button")
                        .background(DarkSurfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "MULTIPLAYER & SALAS",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Black,
                        color = NeonCyan,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "Dispute corridas em tempo real com amigos ou rivais",
                        fontSize = 12.sp,
                        color = TextSecondary
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Mode Tabs
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkSurface,
                contentColor = NeonCyan,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = NeonCyan,
                        height = 3.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("SALA ONLINE", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("TELA DIVIDIDA (2P)", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("TIME ATTACK FANTASMA", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            when (selectedTab) {
                0 -> {
                    // Online Room Controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Button(
                            onClick = {
                                multiplayerEngine.hostRoom(playerName, playerVehicle)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("multiplayer_create_room_button")
                        ) {
                            Icon(Icons.Default.Groups, contentDescription = null, tint = Color.Black)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("CRIAR NOVA SALA PRIVADA", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text("OU ENTRE COM O CÓDIGO DA SALA", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextSecondary)
                        Spacer(modifier = Modifier.height(6.dp))

                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = joinCodeInput,
                                onValueChange = { if (it.length <= 4) joinCodeInput = it.uppercase() },
                                label = { Text("CÓDIGO (EX: APEX)") },
                                singleLine = true,
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = NeonCyan,
                                    unfocusedBorderColor = DarkBorder,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("multiplayer_code_input")
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Button(
                                onClick = {
                                    if (joinCodeInput.isNotEmpty()) {
                                        multiplayerEngine.joinRoom(joinCodeInput, playerName, playerVehicle)
                                    }
                                },
                                enabled = joinCodeInput.length >= 3,
                                colors = ButtonDefaults.buttonColors(containerColor = NitroOrange),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .height(52.dp)
                                    .testTag("multiplayer_join_room_button")
                            ) {
                                Text("ENTRAR", fontWeight = FontWeight.Bold, color = Color.Black)
                            }
                        }
                    }
                }

                1 -> {
                    // Split Screen info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.SportsEsports, contentDescription = null, tint = SpeedYellow)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("MODO 2 JOGADORES NA MESMA TELA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "A tela será dividida em duas visões 3D independentes em tempo real.\n\n" +
                                            "• Jogador 1: Controles de toque na metade esquerda ou Controle Gamepad 1\n" +
                                            "• Jogador 2: Controles de toque na metade direita ou Controle Gamepad 2",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }

                2 -> {
                    // Ghost battle info
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkSurfaceVariant)
                                .padding(14.dp)
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.Speed, contentDescription = null, tint = NeonCyan)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("BATALHA CONTRA O CARRO FANTASMA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    "Corra contra a gravação holográfica da sua melhor volta ou do recorde mundial! Aperfeiçoe suas tangentes e pontos de frenagem.",
                                    fontSize = 11.sp,
                                    color = TextSecondary,
                                    lineHeight = 16.sp
                                )
                            }
                        }
                    }
                }
            }

            // Start Race button
            Button(
                onClick = {
                    val mode = when (selectedTab) {
                        0 -> MultiplayerMode.ONLINE_ROOM
                        1 -> MultiplayerMode.SPLIT_SCREEN
                        else -> MultiplayerMode.GHOST_BATTLE
                    }
                    onStartRace(mode)
                },
                colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("multiplayer_start_race_button")
            ) {
                Text("INICIAR CORRIDA MULTIPLAYER", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 14.sp)
            }
        }

        // Right Column: Active Room Lobby & Drivers
        Column(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("GRID DE PILOTOS NA SALA", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(DarkSurfaceVariant)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text("CÓDIGO: $roomCode", fontSize = 11.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val slots = if (lobbySlots.isNotEmpty()) lobbySlots else listOf(
                    com.example.game.multiplayer.PlayerLobbySlot("1", playerName, playerVehicle, isReady = true, isHost = true),
                    com.example.game.multiplayer.PlayerLobbySlot("2", "Alex Drift", VehicleConfig(com.example.game.customization.CarModel.SAKURA_DRIFT, 0xFFFF0055L), isReady = true),
                    com.example.game.multiplayer.PlayerLobbySlot("3", "Cyber Phantom", VehicleConfig(com.example.game.customization.CarModel.CYBER_PHANTOM, 0xFF00E5FFL), isReady = true),
                    com.example.game.multiplayer.PlayerLobbySlot("4", "Thunder V8", VehicleConfig(com.example.game.customization.CarModel.VENOM_V8, 0xFFFF9100L), isReady = true)
                )

                items(slots) { slot ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, if (slot.isHost) NeonCyan else DarkBorder, RoundedCornerShape(10.dp))
                            .padding(12.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(Color(slot.vehicleConfig.primaryColorHex)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Person, contentDescription = null, tint = Color.Black, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(slot.name, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                    if (slot.isHost) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("[HOST]", fontSize = 9.sp, fontWeight = FontWeight.Black, color = NeonCyan)
                                    }
                                }
                                Text("${slot.vehicleConfig.model.displayName} • #${slot.vehicleConfig.racingNumber}", fontSize = 11.sp, color = TextSecondary)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.SignalCellularAlt, contentDescription = null, tint = Color(0xFF00E676), modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("${slot.pingMs}ms", fontSize = 10.sp, color = Color(0xFF00E676))
                            }
                        }
                    }
                }
            }
        }
    }
}
