package com.example.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.game.customization.CarModel
import com.example.game.customization.DecalType
import com.example.game.customization.GameStorage
import com.example.game.customization.PRESET_CAR_COLORS
import com.example.game.customization.PRESET_NEON_COLORS
import com.example.game.customization.PaintFinish
import com.example.game.customization.RimDesign
import com.example.game.customization.SpoilerType
import com.example.game.customization.VehicleConfig
import com.example.game.engine.Vehicle3DModel
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
fun GarageScreen(
    storage: GameStorage,
    initialConfig: VehicleConfig,
    onBack: () -> Unit,
    onSaveAndRace: (VehicleConfig) -> Unit
) {
    var vehicleConfig by remember { mutableStateOf(initialConfig) }
    var selectedCategoryTab by remember { mutableIntStateOf(0) }
    var turntableAngle by remember { mutableFloatStateOf(45f) }

    val categories = listOf("Modelo", "Pintura & Acabamento", "Adesivos & Spoilers", "Rodas & Neon", "Upgrades de Performance")

    Row(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
    ) {
        // Left Column: 3D Car Turntable Showroom
        Box(
            modifier = Modifier
                .weight(1.1f)
                .fillMaxHeight()
                .padding(16.dp)
        ) {
            // Interactive 3D Turntable
            Column(modifier = Modifier.fillMaxSize()) {
                // Header with Back button
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .testTag("garage_back_button")
                            .background(DarkSurfaceVariant, CircleShape)
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Voltar", tint = TextPrimary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "GARAGEM & CUSTOMIZAÇÃO",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black,
                            color = NeonCyan,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = vehicleConfig.model.displayName,
                            fontSize = 13.sp,
                            color = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // 3D Viewport with Drag to Rotate
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color(0xFF151C2C), Color(0xFF0A0D15))
                            )
                        )
                        .border(1.dp, DarkBorder, RoundedCornerShape(16.dp))
                        .pointerInput(Unit) {
                            detectDragGestures { _, dragAmount ->
                                turntableAngle = (turntableAngle + dragAmount.x * 0.45f) % 360f
                                if (turntableAngle < 0f) turntableAngle += 360f
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        Vehicle3DModel.drawShowroom3DTurntable(
                            drawScope = this,
                            config = vehicleConfig,
                            centerX = size.width * 0.5f,
                            centerY = size.height * 0.55f,
                            viewAngleDeg = turntableAngle,
                            scale = (size.width / 440f).coerceIn(0.8f, 1.4f)
                        )
                    }

                    // Hint overlay
                    Text(
                        text = "↻ Arraste na tela para girar em 360°",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Bottom Action Button
                Button(
                    onClick = {
                        storage.saveVehicleConfig(vehicleConfig)
                        onSaveAndRace(vehicleConfig)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = NeonCyan),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("garage_save_button")
                ) {
                    Text(
                        text = "SALVAR CONFIGURAÇÃO",
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        fontSize = 14.sp
                    )
                }
            }
        }

        // Right Column: Customization Controls & Upgrades
        Column(
            modifier = Modifier
                .weight(1.2f)
                .fillMaxHeight()
                .background(DarkSurface)
                .padding(16.dp)
        ) {
            // Category Tabs
            ScrollableTabRow(
                selectedTabIndex = selectedCategoryTab,
                containerColor = Color.Transparent,
                contentColor = NeonCyan,
                edgePadding = 0.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        Modifier.tabIndicatorOffset(tabPositions[selectedCategoryTab]),
                        color = NeonCyan,
                        height = 3.dp
                    )
                }
            ) {
                categories.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedCategoryTab == index,
                        onClick = { selectedCategoryTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedCategoryTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedCategoryTab == index) NeonCyan else TextSecondary
                            )
                        }
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content with Smooth Vertical Scroll
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                when (selectedCategoryTab) {
                    0 -> {
                        // Vehicle Model Selector
                        Text("SELECIONE O SUPERESPORTIVO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        CarModel.values().forEach { model ->
                            val isSelected = vehicleConfig.model == model
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) DarkSurfaceVariant else Color(0xFF0F131C))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = if (isSelected) NeonCyan else DarkBorder,
                                        shape = RoundedCornerShape(12.dp)
                                    )
                                    .clickable { vehicleConfig = vehicleConfig.copy(model = model) }
                                    .padding(12.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        Icons.Default.DirectionsCar,
                                        contentDescription = null,
                                        tint = if (isSelected) NeonCyan else TextSecondary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(model.displayName, fontWeight = FontWeight.Bold, color = TextPrimary, fontSize = 13.sp)
                                        Text(model.description, color = TextSecondary, fontSize = 11.sp)
                                    }
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                    }
                                }
                            }
                        }
                    }

                    1 -> {
                        // Paint & Finish
                        Text("COR DA CARROCERIA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PRESET_CAR_COLORS) { (colorHex, name) ->
                                val isSelected = vehicleConfig.primaryColorHex == colorHex
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Color(colorHex))
                                        .border(
                                            width = if (isSelected) 3.dp else 1.dp,
                                            color = if (isSelected) Color.White else Color.Black.copy(alpha = 0.5f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            vehicleConfig = vehicleConfig.copy(primaryColorHex = colorHex)
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSelected) {
                                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("TIPO DE ACABAMENTO DA PINTURA", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            PaintFinish.values().forEach { finish ->
                                val isSel = vehicleConfig.finish == finish
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                                        .border(1.dp, if (isSel) NeonCyan else DarkBorder, RoundedCornerShape(8.dp))
                                        .clickable { vehicleConfig = vehicleConfig.copy(finish = finish) }
                                        .padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        finish.displayName,
                                        fontSize = 11.sp,
                                        fontWeight = if (isSel) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isSel) NeonCyan else TextPrimary
                                    )
                                }
                            }
                        }
                    }

                    2 -> {
                        // Decals & Spoilers
                        Text("ADESIVOS & LIVERY", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        DecalType.values().forEach { decal ->
                            val isSel = vehicleConfig.decal == decal
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                                    .border(1.dp, if (isSel) NeonCyan else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { vehicleConfig = vehicleConfig.copy(decal = decal) }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(decal.displayName, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                    if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("AEROFÓLIO & SPOILER TRASEIRO", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        SpoilerType.values().forEach { spoiler ->
                            val isSel = vehicleConfig.spoiler == spoiler
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                                    .border(1.dp, if (isSel) NeonCyan else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { vehicleConfig = vehicleConfig.copy(spoiler = spoiler) }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(spoiler.displayName, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                                        Text("Downforce: +${((spoiler.downforceMultiplier - 1f) * 100).toInt()}% de estabilidade", fontSize = 10.sp, color = TextSecondary)
                                    }
                                    if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }
                    }

                    3 -> {
                        // Wheels & Underglow
                        Text("DESIGN DAS RODAS / AROS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        RimDesign.values().forEach { rim ->
                            val isSel = vehicleConfig.rimDesign == rim
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSel) DarkSurfaceVariant else Color(0xFF0F131C))
                                    .border(1.dp, if (isSel) NeonCyan else DarkBorder, RoundedCornerShape(8.dp))
                                    .clickable { vehicleConfig = vehicleConfig.copy(rimDesign = rim) }
                                    .padding(10.dp)
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(rim.displayName, fontSize = 12.sp, color = TextPrimary, modifier = Modifier.weight(1f))
                                    if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = NeonCyan)
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Text("ILUMINAÇÃO NEON UNDERGLOW", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(PRESET_NEON_COLORS) { (neonHex, name) ->
                                val isSel = vehicleConfig.neonUnderglowHex == neonHex
                                val color = if (neonHex == 0x00000000L) Color.DarkGray else Color(neonHex)
                                Box(
                                    modifier = Modifier
                                        .size(42.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                        .border(if (isSel) 3.dp else 1.dp, if (isSel) Color.White else DarkBorder, CircleShape)
                                        .clickable { vehicleConfig = vehicleConfig.copy(neonUnderglowHex = neonHex) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isSel) Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                                }
                            }
                        }
                    }

                    4 -> {
                        // Performance Upgrades
                        Text("PREPARAÇÃO & UPGRADES MECÂNICOS", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
                        Spacer(modifier = Modifier.height(10.dp))

                        UpgradeItem(
                            title = "Motor & ECU (Velocidade Máxima)",
                            currentLevel = vehicleConfig.engineLevel,
                            statDisplay = "${vehicleConfig.effectiveTopSpeed.toInt()} km/h",
                            onUpgrade = {
                                if (vehicleConfig.engineLevel < 5) {
                                    vehicleConfig = vehicleConfig.copy(engineLevel = vehicleConfig.engineLevel + 1)
                                }
                            }
                        )

                        UpgradeItem(
                            title = "Twin Turbo (Aceleração e Torque)",
                            currentLevel = vehicleConfig.turboLevel,
                            statDisplay = "${String.format("%.1f", vehicleConfig.effectiveAcceleration)} m/s²",
                            onUpgrade = {
                                if (vehicleConfig.turboLevel < 5) {
                                    vehicleConfig = vehicleConfig.copy(turboLevel = vehicleConfig.turboLevel + 1)
                                }
                            }
                        )

                        UpgradeItem(
                            title = "Suspensão & Pneus (Aderência / Curvas)",
                            currentLevel = vehicleConfig.handlingLevel,
                            statDisplay = "${(vehicleConfig.effectiveHandling * 100).toInt()}%",
                            onUpgrade = {
                                if (vehicleConfig.handlingLevel < 5) {
                                    vehicleConfig = vehicleConfig.copy(handlingLevel = vehicleConfig.handlingLevel + 1)
                                }
                            }
                        )

                        UpgradeItem(
                            title = "Tanque & Injeção Nitro (NOS Boost)",
                            currentLevel = vehicleConfig.nitroLevel,
                            statDisplay = "${vehicleConfig.effectiveNitroCapacity.toInt()} psi",
                            onUpgrade = {
                                if (vehicleConfig.nitroLevel < 5) {
                                    vehicleConfig = vehicleConfig.copy(nitroLevel = vehicleConfig.nitroLevel + 1)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun UpgradeItem(
    title: String,
    currentLevel: Int,
    statDisplay: String,
    onUpgrade: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, DarkBorder, RoundedCornerShape(10.dp))
            .padding(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(statDisplay, fontSize = 12.sp, color = NeonCyan, fontWeight = FontWeight.Black)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                LinearProgressIndicator(
                    progress = { currentLevel / 5f },
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = NitroOrange,
                    trackColor = Color(0xFF0F131C)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Button(
                    onClick = onUpgrade,
                    enabled = currentLevel < 5,
                    colors = ButtonDefaults.buttonColors(containerColor = NitroOrange),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.height(32.dp)
                ) {
                    Text(if (currentLevel < 5) "NV. ${currentLevel + 1} +" else "MÁX", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.Black)
                }
            }
        }
    }
}
