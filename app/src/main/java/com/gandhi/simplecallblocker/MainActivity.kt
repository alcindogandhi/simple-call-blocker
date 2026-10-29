package com.gandhi.simplecallblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.gandhi.simplecallblocker.ui.navigation.AppNavigation
import com.gandhi.simplecallblocker.ui.theme.SimpleCallBlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCallBlockerTheme {
                AppNavigation()
            }
        }
    }
}
