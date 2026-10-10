package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.example.data.OnlineAssetRepository
import com.example.model.ClassicTextStyle
import com.example.model.OnlineEffectAsset
import com.example.model.OnlineFontAsset
import com.example.model.SubtitleSegment

@Composable
fun CapcutTextInspectorSheet(
    subtitle: SubtitleSegment,
    onlineFonts: List<OnlineFontAsset>,
    allAnimations: List<String> = emptyList(),
    allTextEffects: List<OnlineEffectAsset> = emptyList(),
    onUpdateSubtitle: (SubtitleSegment) -> Unit,
    onDeleteSubtitle: (String) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Text, 1: Font, 2: Style & Color, 3: Effects (50+), 4: Animation (100+)
    var textVal by remember(subtitle.text) { mutableStateOf(subtitle.text) }

    val colorOptions = listOf(
        Pair("#FFFF00", "Yellow"),
        Pair("#FFFFFF", "White"),
        Pair("#00FFFF", "Cyan"),
        Pair("#F59E0B", "Gold"),
        Pair("#EF4444", "Red"),
        Pair("#10B981", "Green"),
        Pair("#EC4899", "Pink"),
        Pair("#A855F7", "Purple"),
        Pair("#000000", "Black")
    )

    val animationsList = remember(allAnimations) {
        if (allAnimations.isNotEmpty()) allAnimations else listOf(
            "Pop-up", "Bounce", "Word-Glow", "Typewriter",
            "Slide-Up", "Fade-In", "Zoom-Punch", "Karaoke", "Glitch Flicker",
            "Elastic Snap", "Heartbeat Pulse", "Wave Sine", "Drop-Down", "Roll-In",
            "Spin-Clockwise", "Flip-X 3D", "Jelly Wiggle", "Flash In", "Vibrate Fast"
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("capcut_text_inspector_sheet"),
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Inspector Header Bar (CapCut style)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = CircleShape,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TextFields,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Text Styling & Effects",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "${subtitle.fontName} · ${subtitle.fontSize}sp · ${subtitle.animationPreset}",
                            fontSize = 10.sp,
                            color = Color(0xFF38BDF8)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { onDeleteSubtitle(subtitle.id) },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = Color(0xFFF87171),
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(4.dp))

                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Done",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sub-tabs (CapCut style: Text | Font | Style | Effect | Animation)
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E293B),
                contentColor = Color(0xFF38BDF8),
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = Color(0xFF38BDF8),
                        height = 2.5.dp
                    )
                }
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Text", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Font (${onlineFonts.size})", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    text = { Text("Style", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    text = { Text("Effect (50+)", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
                Tab(
                    selected = selectedTab == 4,
                    onClick = { selectedTab = 4 },
                    text = { Text("Animation (100+)", fontSize = 10.sp, fontWeight = FontWeight.Bold) }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tab Panels
            when (selectedTab) {
                0 -> {
                    // TAB 0: Text Content Input & Quick Size
                    OutlinedTextField(
                        value = textVal,
                        onValueChange = {
                            textVal = it
                            onUpdateSubtitle(subtitle.copy(text = it))
                        },
                        label = { Text("Enter Subtitle Text", fontSize = 11.sp) },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size: ${subtitle.fontSize}sp", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            listOf(28, 38, 48, 58).forEach { sz ->
                                Surface(
                                    color = if (subtitle.fontSize == sz) Color(0xFF0284C7) else Color(0xFF1E293B),
                                    shape = RoundedCornerShape(6.dp),
                                    modifier = Modifier.clickable { onUpdateSubtitle(subtitle.copy(fontSize = sz)) }
                                ) {
                                    Text(
                                        text = "${sz}sp",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }

                    Slider(
                        value = subtitle.fontSize.toFloat(),
                        onValueChange = { onUpdateSubtitle(subtitle.copy(fontSize = it.toInt())) },
                        valueRange = 16f..72f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )
                }

                1 -> {
                    // TAB 1: Online Google Fonts Directory
                    Text("Select Font (${onlineFonts.size} Google Fonts):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(onlineFonts) { font ->
                            val isSelected = subtitle.fontName == font.name
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B)
                                ),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) Color.White else Color(0xFF334155)
                                ),
                                modifier = Modifier
                                    .clickable { onUpdateSubtitle(subtitle.copy(fontName = font.name)) }
                            ) {
                                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                                    Text(
                                        text = font.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = font.category,
                                        fontSize = 9.sp,
                                        color = if (isSelected) Color.White.copy(alpha = 0.8f) else Color(0xFF94A3B8)
                                    )
                                }
                            }
                        }
                    }
                }

                2 -> {
                    // TAB 2: Colors & Stroke (Pinned Classic Video Outline + Custom Controls)
                    Card(
                        shape = RoundedCornerShape(10.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (subtitle.textColorHex.equals(ClassicTextStyle.TEXT_COLOR_HEX, ignoreCase = true) &&
                                subtitle.outlineColorHex.equals(ClassicTextStyle.OUTLINE_COLOR_HEX, ignoreCase = true)
                            ) Color(0xFF0284C7) else Color(0xFF1E293B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.5.dp,
                            if (subtitle.textColorHex.equals(ClassicTextStyle.TEXT_COLOR_HEX, ignoreCase = true) &&
                                subtitle.outlineColorHex.equals(ClassicTextStyle.OUTLINE_COLOR_HEX, ignoreCase = true)
                            ) Color(0xFF38BDF8) else Color(0xFF475569)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdateSubtitle(
                                    subtitle.copy(
                                        textEffect = ClassicTextStyle.NAME,
                                        textColorHex = ClassicTextStyle.TEXT_COLOR_HEX,
                                        outlineColorHex = ClassicTextStyle.OUTLINE_COLOR_HEX,
                                        outlineWidth = ClassicTextStyle.OUTLINE_WIDTH,
                                        bgBoxColorHex = null
                                    )
                                )
                            }
                            .testTag("pinned_classic_video_outline")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📌", fontSize = 16.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Classic Video Outline (Default)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "White fill + Thick black border stroke",
                                        fontSize = 10.sp,
                                        color = Color(0xFF94A3B8)
                                    )
                                }
                            }
                            Surface(
                                color = Color.Black,
                                shape = RoundedCornerShape(4.dp),
                                border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                                modifier = Modifier.padding(2.dp)
                            ) {
                                Text(
                                    text = "OUTLINE",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Text Fill Color:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { (hex, _) ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Yellow }
                            val isSelected = subtitle.textColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(28.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdateSubtitle(subtitle.copy(textColorHex = hex)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text("Outline Stroke Width: ${subtitle.outlineWidth.toInt()}px", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = subtitle.outlineWidth,
                        onValueChange = { onUpdateSubtitle(subtitle.copy(outlineWidth = it)) },
                        valueRange = 0f..16f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFBBF24), activeTrackColor = Color(0xFFFBBF24))
                    )

                    // Optional Background Box Pill Toggle (Off / Transparent by default)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Background Box (স্বচ্ছ ব্যাকগ্রাউন্ড):", fontSize = 11.sp, color = Color(0xFF94A3B8))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Surface(
                                color = if (subtitle.bgBoxColorHex == null) Color(0xFF0284C7) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { onUpdateSubtitle(subtitle.copy(bgBoxColorHex = null)) }
                            ) {
                                Text("স্বচ্ছ (None)", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                            Surface(
                                color = if (subtitle.bgBoxColorHex != null) Color(0xFF0284C7) else Color(0xFF1E293B),
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.clickable { onUpdateSubtitle(subtitle.copy(bgBoxColorHex = "#000000")) }
                            ) {
                                Text("ব্ল্যাক বক্স", fontSize = 10.sp, color = Color.White, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                    }
                }

                3 -> {
                    // TAB 3: 50+ Text Effects (Hormozi, Cyber, Neon, 3D, Flame, Ice, etc.)
                    Text("Select from 50+ Pro Text Effects:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(allTextEffects) { fx ->
                            val isSelected = subtitle.textEffect == fx.name || subtitle.textEffect == fx.id
                            FilterChip(
                                selected = isSelected,
                                onClick = {
                                    val newColor = fx.previewColorHex
                                    onUpdateSubtitle(
                                        subtitle.copy(
                                            textEffect = fx.name,
                                            textColorHex = newColor,
                                            bgBoxColorHex = null // Keep background 100% clean and transparent!
                                        )
                                    )
                                },
                                label = { Text(fx.name, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF065F46),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }

                4 -> {
                    // TAB 4: 100+ Text & Motion Animation Presets
                    Text("Select from 100+ CapCut Animations:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(animationsList) { anim ->
                            val isSelected = subtitle.animationPreset == anim
                            FilterChip(
                                selected = isSelected,
                                onClick = { onUpdateSubtitle(subtitle.copy(animationPreset = anim)) },
                                label = { Text(anim, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4F46E5),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}
