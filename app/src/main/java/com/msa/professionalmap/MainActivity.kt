package com.msa.professionalmap

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import com.msa.professionalmap.feature.map.ui.MapScreen
import com.msa.professionalmap.ui.theme.ProfessionalMapTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ProfessionalMapTheme {
                Surface(color = MaterialTheme.colorScheme.background) {
                    MapScreen(appMonitor = application.appMonitorOrDefault())
                }
            }
        }
    }
}
