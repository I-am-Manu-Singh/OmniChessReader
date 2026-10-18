package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.LocalContext
import coil3.ImageLoader
import coil3.compose.rememberAsyncImagePainter
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.svg.SvgDecoder

@Composable
actual fun getPiecePainter(piece: Char, style: String): Painter? {
    val context = LocalContext.current
    val dir = when (style.lowercase()) {
        "alpha" -> "alpha"
        "cardinal" -> "cardinal"
        "celtic" -> "celtic"
        "companion" -> "companion"
        "cooke" -> "cooke"
        "dubrovny" -> "dubrovny"
        "fresca" -> "fresca"
        "gioco" -> "gioco"
        "governor" -> "governor"
        "icpieces" -> "icpieces"
        "kosal" -> "kosal"
        "maestro" -> "maestro"
        "merida" -> "merida"
        "pirouetti" -> "pirouetti"
        "reillycraig" -> "reillycraig"
        "riohacha" -> "riohacha"
        "staunty" -> "staunty"
        "tatiana" -> "tatiana"
        else -> "cburnett"
    }

    val colorPrefix = if (piece.isUpperCase()) "w" else "b"
    val pieceType = piece.uppercaseChar()
    val filename = "$colorPrefix$pieceType.svg"

    // Use standard asset URI syntax
    val assetUrl = remember(dir, filename) {
        "file:///android_asset/chess/pieces/$dir/$filename"
    }

    val imageLoader = remember(context) {
        SingletonHolder.getInstance(context)
    }

    val request = remember(assetUrl) {
        ImageRequest.Builder(context)
            .data(assetUrl)
            .crossfade(false) // Disable crossfade to avoid layout animation invalidation freezes
            .build()
    }

    return rememberAsyncImagePainter(model = request, imageLoader = imageLoader)
}

// Thread-safe singleton with bitmap rendering optimization for SVGs
private object SingletonHolder {
    @Volatile
    private var instance: ImageLoader? = null

    fun getInstance(context: android.content.Context): ImageLoader {
        return instance ?: synchronized(this) {
            instance ?: ImageLoader.Builder(context.applicationContext)
                .components {
                    // Crucial: renderToBitmap = true forces SVG to parse once into memory
                    // preventing heavy main-thread XML vector calculation on every frame draw.
                    add(SvgDecoder.Factory(renderToBitmap = true))
                }
                .build()
                .also { instance = it }
        }
    }
}

@Composable
actual fun rememberBoardThumbnailPainter(themeName: String): Painter? {
    val (lightColor, darkColor) = when (themeName.lowercase()) {
        "chesscom green", "green" -> Pair(0xFFEEEED2, 0xFF769656)
        "onyx", "custom onyx" -> Pair(0xFF3E3E3E, 0xFF1E1E1E)
        "silver", "high contrast silver" -> Pair(0xFFE0E0E0, 0xFF707070)
        "cyan", "icy cyan" -> Pair(0xFFBCE3E8, 0xFF358593)
        "blue", "lichess blue" -> Pair(0xFF9CB2D8, 0xFF5879A8)
        "brown", "wood", "lichess brown" -> Pair(0xFFF0D9B5, 0xFFB58863)
        "magnus" -> Pair(0xFFF0D9B5, 0xFFB58863)
        "newspaper dark" -> Pair(0xFF424242, 0xFF212121)
        "newspaper light" -> Pair(0xFFF0F0F0, 0xFFCCCCCC)
        "terracotta", "terracotta earth" -> Pair(0xFFE89A78, 0xFFBA5536)
        "vinyl", "uscf vinyl" -> Pair(0xFF526A8F, 0xFF2C3E55)
        "purple", "velvet purple" -> Pair(0xFF9D75C2, 0xFF5B3482)
        else -> Pair(0xFFEEEED2, 0xFF769656)
    }

    val light = Color(lightColor)
    val dark = Color(darkColor)

    return remember(lightColor, darkColor) {
        object : Painter() {
            override val intrinsicSize: androidx.compose.ui.geometry.Size
                get() = androidx.compose.ui.geometry.Size(64f, 64f)

            override fun DrawScope.onDraw() {
                val halfWidth = size.width / 2f
                val halfHeight = size.height / 2f

                drawRect(color = light, topLeft = androidx.compose.ui.geometry.Offset(0f, 0f), size = androidx.compose.ui.geometry.Size(halfWidth, halfHeight))
                drawRect(color = dark, topLeft = androidx.compose.ui.geometry.Offset(halfWidth, 0f), size = androidx.compose.ui.geometry.Size(halfWidth, halfHeight))
                drawRect(color = dark, topLeft = androidx.compose.ui.geometry.Offset(0f, halfHeight), size = androidx.compose.ui.geometry.Size(halfWidth, halfHeight))
                drawRect(color = light, topLeft = androidx.compose.ui.geometry.Offset(halfWidth, halfHeight), size = androidx.compose.ui.geometry.Size(halfWidth, halfHeight))
            }
        }
    }
}