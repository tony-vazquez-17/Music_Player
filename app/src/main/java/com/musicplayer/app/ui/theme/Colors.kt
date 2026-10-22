package com.musicplayer.app.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

val DarkPurpleBg1 = Color(0xFF020A05) 
val DarkPurpleBg2 = Color(0xFF06301B) 
val DarkPurpleBg3 = Color(0xFF0B5A32) 
val DarkPurpleBg4 = Color(0xFF010503) 

val MiniPlayerBg = Color(0xFF0F3D26)

val AppBackgroundBrush = Brush.verticalGradient(
    colors = listOf(DarkPurpleBg1, DarkPurpleBg2, DarkPurpleBg3, DarkPurpleBg4)
)

val CoverArtBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF00E676), Color(0xFF00A152), Color(0xFF004D25))
)

val MiniPlayerCoverBrush = Brush.linearGradient(
    colors = listOf(Color(0xFF69F0AE), Color(0xFF00C853), Color(0xFF00381C))
)