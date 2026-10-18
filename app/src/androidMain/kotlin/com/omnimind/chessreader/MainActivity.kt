package com.creativeblisslabs.omnichessreader

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.creativeblisslabs.omnimind.core.ui.util.AndroidContextHolder

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AndroidContextHolder.context = applicationContext
        enableEdgeToEdge()
        setContent {
            ChessReaderApp()
        }
    }
}

@Preview
@Composable
fun AppAndroidPreview() {
    ChessReaderApp()
}
