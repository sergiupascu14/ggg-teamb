package com.example.teamb

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.teamb.data.model.ThemeMode
import com.example.teamb.notification.Reminders
import com.example.teamb.ui.navigation.AppRoot
import com.example.teamb.ui.theme.TeamBTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val container = (application as TeamBApp).container
        Reminders.schedule(this)
        setContent {
            val themeMode by container.settingsStore.themeMode.collectAsState(initial = ThemeMode.SYSTEM)
            val darkTheme = themeMode.isDark(isSystemInDarkTheme())
            TeamBTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    AppRoot(container)
                }
            }
        }
    }
}
