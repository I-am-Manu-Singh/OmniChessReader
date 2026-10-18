package com.creativeblisslabs.omnimind.core.ui.theme

import androidx.compose.ui.graphics.Color

val DeepSpace       = Color(0xFF070709)
val SpaceNavy       = Color(0xFF0E0E10)
val SpaceSurface    = Color(0xFF131316)
val SpaceCard       = Color(0xFF19191C)
val SpaceBorder     = Color(0xFF26262B)

// ── Accent Colors ─────────────────────────────────────────────────────────────
val ElectricViolet  = Color(0xFFB45309)
val NeonCyan        = Color(0xFF0F9D8A)
val CosmicIndigo    = Color(0xFF2563EB)
val PurpleGlow      = Color(0xFFD97706)
val CyanGlow        = Color(0xFF2DD4BF)
val MintGreen       = Color(0xFF22C55E)
val AmberWarm       = Color(0xFFF59E0B)
val RoseRed         = Color(0xFFE11D48)
val SkyBlue         = Color(0xFF38BDF8)




// ── Gradient Definitions ──────────────────────────────────────────────────────
val GradientVioletCyan   = listOf(AmberWarm, NeonCyan)
val GradientIndigoPurple = listOf(CosmicIndigo, ElectricViolet)
val GradientCyanMint     = listOf(NeonCyan, MintGreen)

// ── Note Colors ───────────────────────────────────────────────────────────────
val NoteColors = listOf(
    Color(0xFF232A36), Color(0xFF2A2430), Color(0xFF21302B),
    Color(0xFF30251F), Color(0xFF1F2D24), Color(0xFF2A2C1F)
)

// ── Category Colors ───────────────────────────────────────────────────────────
val CategoryColors = mapOf(
    "Food"          to Color(0xFFE11D48),
    "Transport"     to Color(0xFF2563EB),
    "Entertainment" to Color(0xFFD97706),
    "Health"        to Color(0xFF22C55E),
    "Shopping"      to Color(0xFFF59E0B),
    "Housing"       to Color(0xFF0F9D8A),
    "Education"     to Color(0xFF38BDF8),
    "General"       to Color(0xFF94A3B8)
)
