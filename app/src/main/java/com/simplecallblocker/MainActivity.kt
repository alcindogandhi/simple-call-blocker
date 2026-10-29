package com.simplecallblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.simplecallblocker.ui.AppRoot
import com.simplecallblocker.ui.theme.SimpleCallBlockerTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SimpleCallBlockerTheme {
                AppRoot()
            }
        }
    }
}
