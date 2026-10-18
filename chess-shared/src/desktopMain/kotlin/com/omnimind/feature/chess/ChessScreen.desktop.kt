package com.creativeblisslabs.omnimind.feature.chess

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.painter.Painter
import org.jetbrains.compose.resources.painterResource
import omnimind.composeapp.generated.resources.Res
import omnimind.composeapp.generated.resources.*

@Composable
actual fun getPiecePainter(piece: Char, style: String): Painter? {
    val dir = when (style.lowercase()) {
        "alpha" -> "alpha_"
        "cardinal" -> "cardinal_"
        "celtic" -> "celtic_"
        "companion" -> "companion_"
        "cooke" -> "cooke_"
        "dubrovny" -> "dubrovny_"
        "fresca" -> "fresca_"
        "gioco" -> "gioco_"
        "governor" -> "governor_"
        "icpieces" -> "icpieces_"
        "kosal" -> "kosal_"
        "maestro" -> "maestro_"
        "merida" -> "merida_"
        "pirouetti" -> "pirouetti_"
        "reillycraig" -> "reillycraig_"
        "riohacha" -> "riohacha_"
        "staunty" -> "staunty_"
        "tatiana" -> "tatiana_"
        "cburnett" -> "cburnett_"
        else -> ""
    }

    val colorPrefix = if (piece.isUpperCase()) "w" else "b"
    val pieceType = piece.uppercaseChar()
    val resName = "${dir}${colorPrefix}${pieceType}"

    return when (resName) {
        "alpha_bB" -> painterResource(Res.drawable.alpha_bB)
        "alpha_bK" -> painterResource(Res.drawable.alpha_bK)
        "alpha_bN" -> painterResource(Res.drawable.alpha_bN)
        "alpha_bP" -> painterResource(Res.drawable.alpha_bP)
        "alpha_bQ" -> painterResource(Res.drawable.alpha_bQ)
        "alpha_bR" -> painterResource(Res.drawable.alpha_bR)
        "alpha_wB" -> painterResource(Res.drawable.alpha_wB)
        "alpha_wK" -> painterResource(Res.drawable.alpha_wK)
        "alpha_wN" -> painterResource(Res.drawable.alpha_wN)
        "alpha_wP" -> painterResource(Res.drawable.alpha_wP)
        "alpha_wQ" -> painterResource(Res.drawable.alpha_wQ)
        "alpha_wR" -> painterResource(Res.drawable.alpha_wR)
        "cburnett_bB" -> painterResource(Res.drawable.cburnett_bB)
        "cburnett_bK" -> painterResource(Res.drawable.cburnett_bK)
        "cburnett_bN" -> painterResource(Res.drawable.cburnett_bN)
        "cburnett_bP" -> painterResource(Res.drawable.cburnett_bP)
        "cburnett_bQ" -> painterResource(Res.drawable.cburnett_bQ)
        "cburnett_bR" -> painterResource(Res.drawable.cburnett_bR)
        "cburnett_wB" -> painterResource(Res.drawable.cburnett_wB)
        "cburnett_wK" -> painterResource(Res.drawable.cburnett_wK)
        "cburnett_wN" -> painterResource(Res.drawable.cburnett_wN)
        "cburnett_wP" -> painterResource(Res.drawable.cburnett_wP)
        "cburnett_wQ" -> painterResource(Res.drawable.cburnett_wQ)
        "cburnett_wR" -> painterResource(Res.drawable.cburnett_wR)
        "bB" -> painterResource(Res.drawable.bB)
        "bK" -> painterResource(Res.drawable.bK)
        "bN" -> painterResource(Res.drawable.bN)
        "bP" -> painterResource(Res.drawable.bP)
        "bQ" -> painterResource(Res.drawable.bQ)
        "bR" -> painterResource(Res.drawable.bR)
        "wB" -> painterResource(Res.drawable.wB)
        "wK" -> painterResource(Res.drawable.wK)
        "wN" -> painterResource(Res.drawable.wN)
        "wP" -> painterResource(Res.drawable.wP)
        "wQ" -> painterResource(Res.drawable.wQ)
        "wR" -> painterResource(Res.drawable.wR)
        else -> painterResource(Res.drawable.cburnett_wP)
    }
}
