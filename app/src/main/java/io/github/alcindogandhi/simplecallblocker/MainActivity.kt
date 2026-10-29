package io.github.alcindogandhi.simplecallblocker

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.alcindogandhi.simplecallblocker.ui.AppRoot
import io.github.alcindogandhi.simplecallblocker.ui.theme.SimpleCallBlockerTheme

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
