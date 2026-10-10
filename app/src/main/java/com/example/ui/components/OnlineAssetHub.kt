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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CallSplit
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.Filter
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.FontDownload
import androidx.compose.material.icons.filled.OnlinePrediction
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.ScrollableTabRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.OnlineEffectAsset
import com.example.model.OnlineFontAsset

@Composable
fun OnlineAssetHub(
    onlineFonts: List<OnlineFontAsset>,
    onlineTextEffects: List<OnlineEffectAsset>,
    onlineVideoFilters: List<OnlineEffectAsset>,
    onlineTransitions: List<OnlineEffectAsset> = emptyList(),
    onlineAudioTracks: List<OnlineEffectAsset> = emptyList(),
    downloadingAssetId: String? = null,
    downloadProgress: Int = 0,
    activeAssetStatus: String? = null,
    onApplyFont: (String) -> Unit,
    onApplyTextEffect: (OnlineEffectAsset) -> Unit,
    onApplyVideoFilter: (String) -> Unit,
    onApplyTransition: (String) -> Unit = {},
    onAddAudioTrack: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedCategoryTab by remember { mutableIntStateOf(0) }
    var searchQuery by remember { mutableStateOf("") }

    val filteredFonts = remember(onlineFonts, searchQuery) {
        if (searchQuery.isBlank()) onlineFonts
        else onlineFonts.filter { it.name.contains(searchQuery, ignoreCase = true) || it.category.contains(searchQuery, ignoreCase = true) }
    }

    val filteredTextFx = remember(onlineTextEffects, searchQuery) {
        if (searchQuery.isBlank()) onlineTextEffects
        else onlineTextEffects.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    val filteredFilters = remember(onlineVideoFilters, searchQuery) {
        if (searchQuery.isBlank()) onlineVideoFilters
        else onlineVideoFilters.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    val filteredTransitions = remember(onlineTransitions, searchQuery) {
        if (searchQuery.isBlank()) onlineTransitions
        else onlineTransitions.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    val filteredAudio = remember(onlineAudioTracks, searchQuery) {
        if (searchQuery.isBlank()) onlineAudioTracks
        else onlineAudioTracks.filter { it.name.contains(searchQuery, ignoreCase = true) || it.description.contains(searchQuery, ignoreCase = true) }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF070B14))
            .padding(12.dp)
    ) {
        // Decorative Hero Banner
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFF0284C7), Color(0xFF4F46E5), Color(0xFF9333EA))
                    )
                )
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        color = Color.White.copy(alpha = 0.2f),
                        shape = CircleShape,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CloudDownload,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Online Asset & Cloud Hub",
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp,
                            color = Color.White
                        )
                        Text(
                            text = "৩০+ ফন্ট · ৫০+ টেক্সট ইফেক্ট · ৬০+ ফিল্টার · ৩০+ ট্রানজিশন",
                            fontSize = 10.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Surface(
                    color = Color.Black.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(Color(0xFF10B981)))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Online", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }

        // Live CapCut Download / Applying Status Indicator Card
        AnimatedVisibility(visible = downloadingAssetId != null || activeAssetStatus != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (downloadingAssetId != null) {
                        CircularProgressIndicator(
                            progress = { downloadProgress / 100f },
                            modifier = Modifier.size(20.dp),
                            color = Color(0xFF38BDF8),
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = if (downloadingAssetId != null) "অনলাইন থেকে লোড হচ্ছে ($downloadProgress%)..." else "কাজ সম্পন্ন হয়েছে! ✅",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = activeAssetStatus ?: "Downloading and linking cloud effect...",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Search Bar for Instant Lookup
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search 170+ effects, filters, transitions...", fontSize = 11.sp, color = Color(0xFF64748B)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF94A3B8), modifier = Modifier.size(16.dp)) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            shape = RoundedCornerShape(10.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF38BDF8),
                unfocusedBorderColor = Color(0xFF1E293B),
                focusedContainerColor = Color(0xFF0F172A),
                unfocusedContainerColor = Color(0xFF0F172A),
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )

        Spacer(modifier = Modifier.height(6.dp))

        // 4 Category Tabs (Fonts | Text Effects | Video Filters | Transitions)
        ScrollableTabRow(
            selectedTabIndex = selectedCategoryTab,
            containerColor = Color(0xFF0F172A),
            contentColor = Color(0xFF38BDF8),
            edgePadding = 0.dp,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    modifier = Modifier.tabIndicatorOffset(tabPositions[selectedCategoryTab]),
                    color = Color(0xFF38BDF8),
                    height = 2.5.dp
                )
            }
        ) {
            Tab(
                selected = selectedCategoryTab == 0,
                onClick = { selectedCategoryTab = 0 },
                text = { Text("Fonts (${filteredFonts.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategoryTab == 1,
                onClick = { selectedCategoryTab = 1 },
                text = { Text("Text Effects (${filteredTextFx.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategoryTab == 2,
                onClick = { selectedCategoryTab = 2 },
                text = { Text("Video Filters (${filteredFilters.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategoryTab == 3,
                onClick = { selectedCategoryTab = 3 },
                text = { Text("Transitions (${filteredTransitions.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
            Tab(
                selected = selectedCategoryTab == 4,
                onClick = { selectedCategoryTab = 4 },
                text = { Text("Audio & SFX (${filteredAudio.size})", fontSize = 11.sp, fontWeight = FontWeight.Bold) }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Dynamic Tab Content
        when (selectedCategoryTab) {
            0 -> {
                // Online Google Fonts Directory
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFonts) { font ->
                        val isDownloading = downloadingAssetId == font.name

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDownloading) Color(0xFF38BDF8) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplyFont(font.name) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = font.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 14.sp,
                                            color = Color.White
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            color = Color(0xFF1E293B),
                                            shape = RoundedCornerShape(4.dp)
                                        ) {
                                            Text(
                                                text = font.category,
                                                fontSize = 9.sp,
                                                color = Color(0xFF38BDF8),
                                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = font.sampleText,
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFBBF24)
                                    )
                                }

                                Button(
                                    onClick = { onApplyFont(font.name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDownloading) Color(0xFF0F766E) else Color(0xFF0284C7)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$downloadProgress%", fontSize = 11.sp)
                                    } else {
                                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            1 -> {
                // 50+ Online Text Effects & Styles
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTextFx) { effect ->
                        val isDownloading = downloadingAssetId == effect.id
                        val cardColor = try {
                            Color(android.graphics.Color.parseColor(effect.previewColorHex)).copy(alpha = 0.15f)
                        } catch (_: Exception) {
                            Color(0xFF1E293B)
                        }

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDownloading) Color(0xFFFBBF24) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplyTextEffect(effect) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = cardColor,
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.size(36.dp),
                                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8).copy(alpha = 0.5f))
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = effect.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = effect.description,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onApplyTextEffect(effect) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDownloading) Color(0xFF7C3AED) else Color(0xFF4F46E5)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$downloadProgress%", fontSize = 11.sp)
                                    } else {
                                        Text("Use Style", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                // 60+ Online Video & Image Effects (Fade In, Fade Out, Ken Burns, LUTs)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredFilters) { filter ->
                        val isDownloading = downloadingAssetId == filter.id

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDownloading) Color(0xFF10B981) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplyVideoFilter(filter.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = Color(0xFF0284C7).copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Videocam,
                                            contentDescription = null,
                                            tint = Color(0xFF38BDF8),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = filter.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = filter.description,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onApplyVideoFilter(filter.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isDownloading) Color(0xFF047857) else Color(0xFF0284C7)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$downloadProgress%", fontSize = 11.sp)
                                    } else {
                                        Text("Apply", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            3 -> {
                // 30+ Online Transitions (With 'None' Clean Cut prominently available)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredTransitions) { trans ->
                        val isDownloading = downloadingAssetId == trans.id
                        val isNone = trans.id == "None"

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isNone) Color(0xFF064E3B).copy(alpha = 0.3f) else Color(0xFF131D33)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isNone) Color(0xFF10B981) else if (isDownloading) Color(0xFFFBBF24) else Color(0xFF1E293B)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onApplyTransition(trans.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = if (isNone) Color(0xFF059669).copy(alpha = 0.25f) else Color(0xFF38BDF8).copy(alpha = 0.15f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isNone) Icons.Default.Check else Icons.Default.FlashOn,
                                            contentDescription = null,
                                            tint = if (isNone) Color(0xFF10B981) else Color(0xFFFBBF24),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = trans.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = if (isNone) Color(0xFF10B981) else Color.White
                                        )
                                        Text(
                                            text = trans.description,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onApplyTransition(trans.id) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isNone) Color(0xFF059669) else if (isDownloading) Color(0xFFD97706) else Color(0xFF0284C7)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$downloadProgress%", fontSize = 11.sp)
                                    } else {
                                        Text(if (isNone) "Select None" else "Use Transition", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            4 -> {
                // Royalty-Free Music & Cinematic Sound Effects (SFX)
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredAudio) { audio ->
                        val isDownloading = downloadingAssetId == audio.id

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF0F231D)
                            ),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isDownloading) Color(0xFF10B981) else Color(0xFF047857).copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onAddAudioTrack(audio.name) }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Surface(
                                        color = Color(0xFF10B981).copy(alpha = 0.2f),
                                        shape = CircleShape,
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.AutoAwesome,
                                            contentDescription = null,
                                            tint = Color(0xFF34D399),
                                            modifier = Modifier.padding(8.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = audio.name,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = Color.White
                                        )
                                        Text(
                                            text = audio.description,
                                            fontSize = 10.sp,
                                            color = Color(0xFF94A3B8),
                                            maxLines = 1
                                        )
                                    }
                                }

                                Button(
                                    onClick = { onAddAudioTrack(audio.name) },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = Color(0xFF059669)
                                    ),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier.height(34.dp)
                                ) {
                                    if (isDownloading) {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(16.dp),
                                            color = Color.White,
                                            strokeWidth = 2.dp
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("$downloadProgress%", fontSize = 11.sp)
                                    } else {
                                        Text("+ Add to Track", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
