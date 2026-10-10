package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataArray
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.ImageSegment
import com.example.model.SubtitleSegment

@Composable
fun DualJsonManager(
    currentImageJson: String,
    currentTextJson: String,
    currentAudioJson: String = "",
    onApplyImageJson: (String) -> Unit,
    onApplyTextJson: (String) -> Unit,
    onApplyAudioJson: (String) -> Unit = {},
    onExportZip: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedJsonTab by remember { mutableIntStateOf(2) } // 0: image_sync.json, 1: text_sync.json, 2: audio_sync.json
    var imageJsonEditorText by remember(currentImageJson) { mutableStateOf(currentImageJson) }
    var textJsonEditorText by remember(currentTextJson) { mutableStateOf(currentTextJson) }
    var audioJsonEditorText by remember(currentAudioJson) { mutableStateOf(currentAudioJson) }
    var parseError by remember { mutableStateOf<String?>(null) }
    var successMsg by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(14.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Top Card: Summary & Presets
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF131D33)),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.GraphicEq,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Studio Multi-Layer JSON Pipeline",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = "স্মার্ট জেসন অটোমেশন: ইমেজ, সাবটাইটেল এবং মাল্টি-লেয়ার অডিও (BGM, Voiceover, SFX)। অডিও অটোমেশন সিস্টেমে অটো-ডাকিং (Auto-Ducking), ভলিউম কি-ফ্রেম, ফেড-ইন/ফেড-আউট স্বয়ংক্রিয়ভাবে কাজ করে।",
                    fontSize = 11.5.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onExportZip,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f).testTag("export_zip_button")
                    ) {
                        Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Export Project ZIP", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Tab Selector for JSON files (3 Tabs: Image, Text, Audio Automation)
        TabRow(
            selectedTabIndex = selectedJsonTab,
            containerColor = Color(0xFF0F172A),
            contentColor = Color(0xFF38BDF8)
        ) {
            Tab(
                selected = selectedJsonTab == 0,
                onClick = {
                    selectedJsonTab = 0
                    parseError = null
                    successMsg = null
                },
                text = { Text("image_sync.json", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedJsonTab == 1,
                onClick = {
                    selectedJsonTab = 1
                    parseError = null
                    successMsg = null
                },
                text = { Text("text_sync.json", fontWeight = FontWeight.SemiBold, fontSize = 11.sp) }
            )
            Tab(
                selected = selectedJsonTab == 2,
                onClick = {
                    selectedJsonTab = 2
                    parseError = null
                    successMsg = null
                },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("audio_sync.json", fontWeight = FontWeight.Bold, fontSize = 11.sp, color = Color(0xFF10B981))
                    }
                }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        when (selectedJsonTab) {
            0 -> {
                // image_sync.json Editor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ছবির স্থায়িত্বকাল (duration), ট্রানজিশন ও ফিল্টার সিঙ্ক (ছবি অক্ষুণ্ণ থাকবে)",
                        fontSize = 11.sp,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = imageJsonEditorText,
                    onValueChange = {
                        imageJsonEditorText = it
                        parseError = null
                        successMsg = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("image_json_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = {
                            imageJsonEditorText = currentImageJson
                            parseError = null
                            successMsg = null
                        }
                    ) {
                        Text("রিসেট", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                onApplyImageJson(imageJsonEditorText)
                                successMsg = "image_sync.json সফলভাবে প্রয়োগ হয়েছে! ছবি অক্ষুণ্ণ রেখে টাইমিং সিঙ্ক করা হয়েছে।"
                            } catch (e: Exception) {
                                parseError = e.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.testTag("apply_image_json_button")
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("জেসন প্রয়োগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            1 -> {
                // text_sync.json Editor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Info, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(14.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "ক্যাপশন, শুরু ও শেষের টাইমস্ট্যাম্প এবং অ্যানিমেশন সিঙ্ক",
                        fontSize = 11.sp,
                        color = Color(0xFF38BDF8),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))

                OutlinedTextField(
                    value = textJsonEditorText,
                    onValueChange = {
                        textJsonEditorText = it
                        parseError = null
                        successMsg = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                        .testTag("text_json_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = {
                            textJsonEditorText = currentTextJson
                            parseError = null
                            successMsg = null
                        }
                    ) {
                        Text("রিসেট", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                onApplyTextJson(textJsonEditorText)
                                successMsg = "text_sync.json সফলভাবে প্রয়োগ হয়েছে! ক্যাপশন ও স্টাইল সিঙ্ক করা হয়েছে।"
                            } catch (e: Exception) {
                                parseError = e.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                        modifier = Modifier.testTag("apply_text_json_button")
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("জেসন প্রয়োগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
            2 -> {
                // audio_sync.json Multi-Layer Automation Editor
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color(0xFF34D399), modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "মাল্টি-লেয়ার অডিও অটোমেশন (BGM Ducking, Keyframe Curves, Fade In/Out)",
                        fontSize = 11.sp,
                        color = Color(0xFF34D399),
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "লেয়ার নির্দেশিকা: layer 0 = Music (BGM), layer 1 = Voiceover (অটো-ডাকিং সক্রিয় হলে BGM ভলিউম কমে যাবে), layer 2 = SFX। ভলিউম কি-ফ্রেম ও ফেড সেকেন্ডে কাজ করে।",
                    fontSize = 10.5.sp,
                    color = Color(0xFF94A3B8),
                    lineHeight = 15.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Presets quick buttons
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilledTonalButton(
                        onClick = {
                            audioJsonEditorText = """[
  {
    "id": "audio_bgm_01",
    "name": "Lo-Fi Study Chill Beat",
    "track_index": 0,
    "layer_type": "music",
    "start_seconds": 0.0,
    "duration": 15.0,
    "volume": 0.85,
    "fade_in_seconds": 0.8,
    "fade_out_seconds": 1.2,
    "loop": true,
    "ducking": true,
    "duck_volume": 0.25,
    "is_muted": false
  },
  {
    "id": "audio_vo_01",
    "name": "AI Voiceover Speech",
    "track_index": 1,
    "layer_type": "voiceover",
    "start_seconds": 0.5,
    "duration": 7.5,
    "volume": 0.95,
    "fade_in_seconds": 0.2,
    "fade_out_seconds": 0.3,
    "loop": false,
    "is_muted": false
  },
  {
    "id": "audio_sfx_01",
    "name": "Cinematic Punch Hit",
    "track_index": 2,
    "layer_type": "sfx",
    "start_seconds": 0.0,
    "duration": 2.5,
    "volume": 0.90,
    "fade_in_seconds": 0.0,
    "fade_out_seconds": 0.5,
    "loop": false,
    "is_muted": false
  }
]"""
                        }
                    ) {
                        Text("3-Layer Auto-Duck Preset", fontSize = 10.sp)
                    }

                    FilledTonalButton(
                        onClick = {
                            audioJsonEditorText = """[
  {
    "id": "audio_bgm_cyber",
    "name": "Cyberpunk Synth Beat",
    "track_index": 0,
    "layer_type": "music",
    "start_seconds": 0.0,
    "duration": 18.0,
    "volume": 0.90,
    "fade_in_seconds": 1.5,
    "fade_out_seconds": 2.0,
    "loop": true,
    "ducking": true,
    "duck_volume": 0.30,
    "automation_keyframes": [
      { "time": 0.0, "volume": 0.3 },
      { "time": 2.0, "volume": 0.9 },
      { "time": 10.0, "volume": 0.9 },
      { "time": 12.0, "volume": 0.4 },
      { "time": 15.0, "volume": 0.85 }
    ]
  }
]"""
                        }
                    ) {
                        Text("Keyframe Curve Preset", fontSize = 10.sp)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedTextField(
                    value = audioJsonEditorText,
                    onValueChange = {
                        audioJsonEditorText = it
                        parseError = null
                        successMsg = null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(280.dp)
                        .testTag("audio_json_input"),
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 11.sp,
                        color = Color.White
                    ),
                    shape = RoundedCornerShape(10.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    FilledTonalButton(
                        onClick = {
                            audioJsonEditorText = currentAudioJson
                            parseError = null
                            successMsg = null
                        }
                    ) {
                        Text("রিসেট", fontSize = 11.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            try {
                                onApplyAudioJson(audioJsonEditorText)
                                successMsg = "audio_sync.json সফলভাবে প্রয়োগ হয়েছে! মাল্টি-লেয়ার অটোমেশন ও ডাকিং কার্যকর হয়েছে।"
                            } catch (e: Exception) {
                                parseError = e.message
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF059669)),
                        modifier = Modifier.testTag("apply_audio_json_button")
                    ) {
                        Icon(imageVector = Icons.Default.Upload, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("অডিও অটোমেশন প্রয়োগ করুন", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (parseError != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF7F1D1D).copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFEF4444))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = Color(0xFFEF4444), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "ত্রুটি: $parseError",
                        color = Color(0xFFFCA5A5),
                        fontSize = 11.sp
                    )
                }
            }
        }

        if (successMsg != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = Color(0xFF064E3B).copy(alpha = 0.35f),
                shape = RoundedCornerShape(8.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF10B981))
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = successMsg!!,
                        color = Color(0xFF6EE7B7),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
