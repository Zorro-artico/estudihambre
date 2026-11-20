package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.data.remote.configureAppCheck
import com.example.data.repository.AppContainer
import com.example.ui.navigation.AppNavigation
import com.example.ui.theme.EstudihambreLifeTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase AI calls
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        val appContainer = AppContainer(applicationContext)

        setContent {
            EstudihambreLifeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation(container = appContainer)
                }
            }
        }
    }
}
