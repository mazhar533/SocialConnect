package com.mazhar.socialconnect.ui.screens.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    object Success : AuthState()
    data class Error(val message: String) : AuthState()
}

class AuthViewModel : ViewModel() {
    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()

    private val _authState = MutableStateFlow<AuthState>(AuthState.Idle)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()
    
    val currentUser = auth.currentUser

    fun resetState() {
        _authState.value = AuthState.Idle
    }

    fun login(email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            if (email.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Fields cannot be empty")
                return@launch
            }
            auth.signInWithEmailAndPassword(email, password).await()
            _authState.value = AuthState.Success
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Login failed")
        }
    }

    fun signUp(name: String, email: String, password: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            if (name.isBlank() || email.isBlank() || password.isBlank()) {
                _authState.value = AuthState.Error("Fields cannot be empty")
                return@launch
            }
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val uid = result.user?.uid
            if (uid != null) {
                val user = User(uid = uid, name = name, email = email)
                firestore.collection("users").document(uid).set(user).await()
                _authState.value = AuthState.Success
            } else {
                _authState.value = AuthState.Error("User creation failed")
            }
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Sign up failed")
        }
    }

    fun recoverPassword(email: String) = viewModelScope.launch {
        _authState.value = AuthState.Loading
        try {
            if (email.isBlank()) {
                _authState.value = AuthState.Error("Email cannot be empty")
                return@launch
            }
            auth.sendPasswordResetEmail(email).await()
            _authState.value = AuthState.Success
        } catch (e: Exception) {
            _authState.value = AuthState.Error(e.localizedMessage ?: "Password recovery failed")
        }
    }
}
