package com.lordyhas.sonrelab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lordyhas.sonrelab.ui.navigation.AppNavHost
import com.lordyhas.sonrelab.ui.theme.SonreLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SonreLabTheme(darkTheme = true) {
                AppNavHost()
            }
        }
    }
}