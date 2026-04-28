package com.mazhar.socialconnect

import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.mazhar.socialconnect.ui.navigation.AppNavigation
import com.mazhar.socialconnect.ui.theme.SocialConnectTheme
import kotlinx.coroutines.flow.MutableStateFlow

class MainActivity : ComponentActivity() {
    
    // Hold a strong reference to the listener to prevent garbage collection
    private var preferenceChangeListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
    private val darkModeFlow = MutableStateFlow(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        darkModeFlow.value = prefs.getBoolean("darkMode", false)
        
        preferenceChangeListener = SharedPreferences.OnSharedPreferenceChangeListener { p, key ->
            if (key == "darkMode") {
                darkModeFlow.value = p.getBoolean("darkMode", false)
            }
        }
        prefs.registerOnSharedPreferenceChangeListener(preferenceChangeListener)

        enableEdgeToEdge()
        setContent {
            val isDarkMode by darkModeFlow.collectAsState()
            
            SocialConnectTheme(darkTheme = isDarkMode, dynamicColor = false) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val prefs = getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)
        prefs.unregisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}