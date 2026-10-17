package com.lordyhas.sleeplab

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.lordyhas.sleeplab.ui.navigation.AppNavHost
import com.lordyhas.sleeplab.ui.theme.SleepLabTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SleepLabTheme(darkTheme = true) {
                AppNavHost()
            }
        }
    }
}