package com.mazhar.socialconnect.ui.screens.settings

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel(context: Context) : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val prefs: SharedPreferences = context.getSharedPreferences("settings_prefs", Context.MODE_PRIVATE)

    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    private val _privateAccountEnabled = MutableStateFlow(prefs.getBoolean("privateAccount", false))
    val privateAccountEnabled: StateFlow<Boolean> = _privateAccountEnabled.asStateFlow()

    private val _twoFactorEnabled = MutableStateFlow(prefs.getBoolean("twoFactor", false))
    val twoFactorEnabled: StateFlow<Boolean> = _twoFactorEnabled.asStateFlow()

    private val _darkModeEnabled = MutableStateFlow(prefs.getBoolean("darkMode", false))
    val darkModeEnabled: StateFlow<Boolean> = _darkModeEnabled.asStateFlow()

    fun toggleNotifications(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications", enabled).apply()
    }

    fun togglePrivateAccount(enabled: Boolean) {
        _privateAccountEnabled.value = enabled
        prefs.edit().putBoolean("privateAccount", enabled).apply()
    }

    fun toggleTwoFactor(enabled: Boolean) {
        _twoFactorEnabled.value = enabled
        prefs.edit().putBoolean("twoFactor", enabled).apply()
    }

    fun toggleDarkMode(enabled: Boolean) {
        _darkModeEnabled.value = enabled
        prefs.edit().putBoolean("darkMode", enabled).apply()
    }

    fun signOut(onSignOutComplete: () -> Unit) {
        auth.signOut()
        onSignOutComplete()
    }
}
