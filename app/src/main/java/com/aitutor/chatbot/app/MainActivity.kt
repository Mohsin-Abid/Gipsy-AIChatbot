package com.aitutor.chatbot.app

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.aitutor.chatbot.app.navigation.AppNavHost
import com.aitutor.chatbot.app.ui.theme.AITutorTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // With three-button navigation the system paints a pale scrim behind the buttons unless
        // asked not to. Every screen already keeps its content clear of that strip, so the scrim
        // adds nothing but a mismatched band across the bottom — on the accent splash and heroes it
        // reads as a rendering fault. Turning it off lets each screen's own background run through.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }

        setContent {
            AITutorTheme {
                AppNavHost()
            }
        }
    }
}
