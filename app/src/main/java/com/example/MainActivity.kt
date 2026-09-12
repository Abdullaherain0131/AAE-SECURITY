package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.MainScreen
import com.example.ui.theme.AntivirusTheme
import com.example.ui.viewmodel.AntivirusViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: AntivirusViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        com.example.util.ScannerSoundManager.init(this)

        setContent {
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()
            AntivirusTheme() {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        com.example.util.ScannerSoundManager.release()
    }
}

