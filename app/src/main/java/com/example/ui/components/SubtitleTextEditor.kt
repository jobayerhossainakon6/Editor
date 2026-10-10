package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Animation
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Style
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.engine.AssSubtitleParser
import com.example.model.AssTemplateInfo
import com.example.model.SubtitleSegment
import java.util.Locale

@Composable
fun SubtitleTextEditor(
    subtitles: List<SubtitleSegment>,
    templates: List<AssTemplateInfo>,
    onUpdateSubtitleSegment: (SubtitleSegment) -> Unit,
    onAddSubtitle: () -> Unit,
    onDeleteSubtitle: (id: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 6.dp)
    ) {
        // Section Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Professional Text & Typography",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "${subtitles.size} Independent Captions · Individual Font, Size & Colors",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF94A3B8)
                )
            }

            Button(
                onClick = onAddSubtitle,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("add_subtitle_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Text", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Subtitles List
        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(
                items = subtitles,
                key = { _, item -> item.id }
            ) { index, item ->
                IndividualSubtitleCard(
                    item = item,
                    index = index,
                    templates = templates,
                    onUpdate = onUpdateSubtitleSegment,
                    onDelete = { onDeleteSubtitle(item.id) }
                )
            }
        }
    }
}

@Composable
fun IndividualSubtitleCard(
    item: SubtitleSegment,
    index: Int,
    templates: List<AssTemplateInfo>,
    onUpdate: (SubtitleSegment) -> Unit,
    onDelete: () -> Unit
) {
    var isExpanded by remember { mutableStateOf(false) }
    var textVal by remember(item.text) { mutableStateOf(item.text) }

    val fontOptions = listOf(
        "Impact", "Bebas Neue", "Montserrat", "Anton", "Poppins",
        "Oswald", "Bangers", "Bungee", "Cinzel", "Righteous", "Permanent Marker"
    )

    val colorOptions = listOf(
        Pair("#FFFF00", "Yellow"),
        Pair("#FFFFFF", "White"),
        Pair("#00FFFF", "Cyan"),
        Pair("#F59E0B", "Gold"),
        Pair("#EF4444", "Red"),
        Pair("#10B981", "Green"),
        Pair("#EC4899", "Pink"),
        Pair("#000000", "Black")
    )

    val animOptions = listOf("Pop-up", "Bounce", "Word-Glow", "Typewriter", "Slide-Up", "Fade-In", "Zoom-Punch")
    val effectOptions = listOf("Glow", "3D Stroke", "Neon", "Shadow", "Retro", "Clean", "Glitch")

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("subtitle_item_${item.id}"),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            // Header Row: Tag #, Time Range, Expand Button, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color(0xFF0284C7),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text(
                            text = "Text #${index + 1}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "${AssSubtitleParser.secondsToAssTime(item.startSeconds)} → ${AssSubtitleParser.secondsToAssTime(item.endSeconds)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF94A3B8),
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilledTonalButton(
                        onClick = { isExpanded = !isExpanded },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.height(28.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 8.dp)
                    ) {
                        Icon(
                            imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.FormatPaint,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(if (isExpanded) "Hide Tools" else "Edit Style", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.size(16.dp),
                            tint = Color(0xFFF87171)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Subtitle Text Field
            OutlinedTextField(
                value = textVal,
                onValueChange = {
                    textVal = it
                    onUpdate(item.copy(text = it))
                },
                label = { Text("Caption Text", fontSize = 11.sp) },
                maxLines = 2,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Quick Badges: Font, Size, Color, Animation
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "${item.fontName} · ${item.fontSize}sp",
                        fontSize = 10.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        val c = try { Color(android.graphics.Color.parseColor(item.textColorHex)) } catch (_: Exception) { Color.Yellow }
                        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(c))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(item.animationPreset, fontSize = 10.sp, color = Color.White)
                    }
                }

                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Pos (${(item.posX * 100).toInt()}%, ${(item.posY * 100).toInt()}%)",
                        fontSize = 10.sp,
                        color = Color(0xFFFBBF24),
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Expanded Full Styling Controls
            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    // PINNED DEFAULT STYLE: Classic Video Outline
                    Card(
                        shape = RoundedCornerShape(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (item.textColorHex.equals(com.example.model.ClassicTextStyle.TEXT_COLOR_HEX, ignoreCase = true) &&
                                item.outlineColorHex.equals(com.example.model.ClassicTextStyle.OUTLINE_COLOR_HEX, ignoreCase = true)
                            ) Color(0xFF0284C7) else Color(0xFF1E293B)
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (item.textColorHex.equals(com.example.model.ClassicTextStyle.TEXT_COLOR_HEX, ignoreCase = true) &&
                                item.outlineColorHex.equals(com.example.model.ClassicTextStyle.OUTLINE_COLOR_HEX, ignoreCase = true)
                            ) Color(0xFF38BDF8) else Color(0xFF334155)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onUpdate(
                                    item.copy(
                                        textEffect = com.example.model.ClassicTextStyle.NAME,
                                        textColorHex = com.example.model.ClassicTextStyle.TEXT_COLOR_HEX,
                                        outlineColorHex = com.example.model.ClassicTextStyle.OUTLINE_COLOR_HEX,
                                        outlineWidth = com.example.model.ClassicTextStyle.OUTLINE_WIDTH,
                                        bgBoxColorHex = null
                                    )
                                )
                            }
                            .testTag("subtitle_pinned_classic_outline")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text("📌", fontSize = 14.sp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Column {
                                    Text(
                                        text = "Classic Video Outline (Default)",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "White text fill · 6px Black outline stroke",
                                        fontSize = 9.sp,
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
                                    text = "ABC",
                                    fontWeight = FontWeight.Black,
                                    fontSize = 9.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 1. Font Family Selector
                    Text("Font Family (অনলাইন ও লোকাল ফন্ট):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(fontOptions) { font ->
                            FilterChip(
                                selected = item.fontName == font,
                                onClick = { onUpdate(item.copy(fontName = font)) },
                                label = { Text(font, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF0284C7),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // 2. Font Size Slider
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Font Size: ${item.fontSize}sp", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    }
                    Slider(
                        value = item.fontSize.toFloat(),
                        onValueChange = { onUpdate(item.copy(fontSize = it.toInt())) },
                        valueRange = 16f..72f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF38BDF8))
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // 3. Text Color Palette
                    Text("Text Color (টেক্সট কালার):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colorOptions.forEach { (hex, name) ->
                            val c = try { Color(android.graphics.Color.parseColor(hex)) } catch (_: Exception) { Color.Yellow }
                            val isSelected = item.textColorHex.equals(hex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(26.dp)
                                    .clip(CircleShape)
                                    .background(c)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) Color.White else Color(0xFF475569),
                                        shape = CircleShape
                                    )
                                    .clickable { onUpdate(item.copy(textColorHex = hex)) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 4. Text Animation Preset
                    Text("Animation (অ্যানিমেশন প্রিসেট):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(animOptions) { anim ->
                            FilterChip(
                                selected = item.animationPreset == anim,
                                onClick = { onUpdate(item.copy(animationPreset = anim)) },
                                label = { Text(anim, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF4F46E5),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 5. Text Effect (Glow, 3D, Shadow)
                    Text("Effect (ইফেক্ট ও শ্যাডো):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF94A3B8))
                    Spacer(modifier = Modifier.height(4.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(effectOptions) { fx ->
                            FilterChip(
                                selected = item.textEffect == fx,
                                onClick = { onUpdate(item.copy(textEffect = fx)) },
                                label = { Text(fx, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = Color(0xFF065F46),
                                    selectedLabelColor = Color.White
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // 6. X & Y Positioning Sliders (Pixel accurate)
                    Text("Vertical Position (Y): ${(item.posY * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = item.posY,
                        onValueChange = { onUpdate(item.copy(posY = it)) },
                        valueRange = 0.05f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFBBF24), activeTrackColor = Color(0xFFFBBF24))
                    )

                    Text("Horizontal Position (X): ${(item.posX * 100).toInt()}%", fontSize = 11.sp, color = Color(0xFF94A3B8))
                    Slider(
                        value = item.posX,
                        onValueChange = { onUpdate(item.copy(posX = it)) },
                        valueRange = 0.05f..0.95f,
                        colors = SliderDefaults.colors(thumbColor = Color(0xFFFBBF24), activeTrackColor = Color(0xFFFBBF24))
                    )
                }
            }
        }
    }
}
