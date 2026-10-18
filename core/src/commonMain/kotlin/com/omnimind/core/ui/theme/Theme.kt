package com.creativeblisslabs.omnimind.core.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.Color

// 1. Create a data class holding your exact variable names
data class OmniMindTextColors(
    val primary: Color,
    val secondary: Color,
    val muted: Color,
    val disabled: Color
)

// 2. Initialize the CompositionLocal provider
val LocalTextColors = compositionLocalOf {
    OmniMindTextColors(
        primary = Color(0xFFF3F4F6),
        secondary = Color(0xFFCBD5E1),
        muted = Color(0xFF94A3B8),
        disabled = Color(0xFF64748B)
    )
}

// 3. Expose the clean global properties using Composable getters
val TextPrimary: Color @Composable @ReadOnlyComposable get() = LocalTextColors.current.primary
val TextSecondary: Color @Composable @ReadOnlyComposable get() = LocalTextColors.current.secondary
val TextMuted: Color @Composable @ReadOnlyComposable get() = LocalTextColors.current.muted
val TextDisabled: Color @Composable @ReadOnlyComposable get() = LocalTextColors.current.disabled
private val DarkColorScheme = darkColorScheme(
    // Primary: Warm Caramel Gold
    primary            = Color(0xFFEAA668),
    onPrimary          = Color(0xFF231407),
    primaryContainer   = Color(0xFF553518),
    onPrimaryContainer = Color(0xFFFDFBF7), // Warm milk-white text

    // Secondary: Soft Warm Terracotta
    secondary          = Color(0xFFD48166),
    onSecondary        = Color(0xFF261009),
    secondaryContainer = Color(0xFF5C2615),
    onSecondaryContainer = Color(0xFFFDFBF7),

    // Tertiary: Muted Sage / Warm Olive
    tertiary           = Color(0xFFA3A88E),
    onTertiary         = Color(0xFF1B1E16),
    tertiaryContainer  = Color(0xFF3B3E32),
    onTertiaryContainer = Color(0xFFFDFBF7),

    // Background & Surface: High Contrast, Near-Black Dark Chocolate
    background         = Color(0xFF12100E), // Near-black with a warm undertone
    onBackground       = Color(0xFFFDFBF7), 
    surface            = Color(0xFF181512), // Deep espresso surface
    onSurface          = Color(0xFFFDFBF7),
    surfaceVariant     = Color(0xFF26201B), // Soft warm dark latte for cards
    onSurfaceVariant   = Color(0xFFD6CBB7), // Warm taupe secondary text

    // Outline & Borders: Muted warm charcoal
    outline            = Color(0xFF4A4036),

    // Error: Deep warm crimson
    error              = Color(0xFFE56B55),
    onError            = Color(0xFF2B0A06),
    errorContainer     = Color(0xFF611C12),
    onErrorContainer   = Color(0xFFFDFBF7),

    // Inverse elements
    inverseSurface     = Color(0xFFFDFBF7),
    inverseOnSurface   = Color(0xFF12100E),
    inversePrimary     = Color(0xFF8B5A2B)
)

private val LightColorScheme = lightColorScheme(
    // Primary: Rich warm amber / caramel
    primary            = Color(0xFF8B5A2B), 
    onPrimary          = Color(0xFFFFFFFF),
    primaryContainer   = Color(0xFFF7E7D4),
    onPrimaryContainer = Color(0xFF3E1F05),

    // Secondary: Soft warm terracotta / clay (Replaced Neon Cyan)
    secondary          = Color(0xFFA0522D),
    onSecondary        = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF9E6DE),
    onSecondaryContainer = Color(0xFF451806),

    // Tertiary: Sage / Warm muted olive (Replaced Mint Green)
    tertiary           = Color(0xFF6B705C),
    onTertiary         = Color(0xFFFFFFFF),
    tertiaryContainer  = Color(0xFFFFE8D6), // Cream accent container
    onTertiaryContainer = Color(0xFF25281E),

    // Background & Surface: Professional Cream and Ivory (Replaced Cool Slate)
    background         = Color(0xFFFDFBF7), // Warm cream ivory
    onBackground       = Color(0xFF2C2520), // Soft dark chocolate brown
    surface            = Color(0xFFFFFDF9), // Pure warm milk
    onSurface          = Color(0xFF2C2520),
    surfaceVariant     = Color(0xFFF4EFE6), // Soft latte
    onSurfaceVariant   = Color(0xFF53463D),

    // Outline & Borders: Soft warm taupe
    outline            = Color(0xFFD6CBB7),

    // Error: Muted warm crimson (Replaced bright Rose Red)
    error              = Color(0xFFB84A39),
    onError            = Color(0xFFFFFFFF),
    errorContainer     = Color(0xFFFCE8E5),
    onErrorContainer   = Color(0xFF42120A),

    // Inverse elements
    inverseSurface     = Color(0xFF36302B),
    inverseOnSurface   = Color(0xFFFDFBF7),
    inversePrimary     = Color(0xFFEAA668)
)

@Composable
fun OmniMindTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    // Define the dynamic text color states based on light or dark modes
    val textColors = if (darkTheme) {
        OmniMindTextColors(
            primary = Color(0xFFF3F4F6),       // Your dark mode TextPrimary
            secondary = Color(0xFFCBD5E1),     // Your dark mode TextSecondary
            muted = Color(0xFF94A3B8),         // Your dark mode TextMuted
            disabled = Color(0xFF64748B)       // Your dark mode TextDisabled
        )
    } else {
        OmniMindTextColors(
            primary = Color(0xFF0F172A),       // Your light mode TextPrimary
            secondary = Color(0xFF475569),     // Your light mode TextSecondary
            muted = Color(0xFF64748B),         // Your light mode TextMuted
            disabled = Color(0xFF94A3B8)       // Your light mode TextDisabled
        )
    }

    // Pass the text values down through the CompositionLocalProvider
    CompositionLocalProvider(LocalTextColors provides textColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = OmniMindTypography,
            content = content
        )
    }
}
