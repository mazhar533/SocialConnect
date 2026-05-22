package com.mazhar.socialconnect.data

import android.annotation.SuppressLint
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mazhar.socialconnect.data.model.User
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object UserRepository {
    private val auth = FirebaseAuth.getInstance()
    @SuppressLint("StaticFieldLeak")
    private val firestore = FirebaseFirestore.getInstance()

    private val _currentUserData = MutableStateFlow<User?>(null)
    val currentUserData: StateFlow<User?> = _currentUserData.asStateFlow()

    private var activeUid: String? = null
    private var listenerRegistration: ListenerRegistration? = null

    init {
        // Automatically manage listener based on firebase auth state changes
        auth.addAuthStateListener { firebaseAuth ->
            val uid = firebaseAuth.currentUser?.uid
            if (uid == null) {
                clear()
            } else {
                startListening(uid)
            }
        }
    }

    private fun startListening(uid: String) {
        if (activeUid == uid && listenerRegistration != null) {
            return
        }
        cleanupListener()
        activeUid = uid
        listenerRegistration = firestore.collection("users").document(uid)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    android.util.Log.e("UserRepository", "Error listening to current user: ${e.message}")
                    return@addSnapshotListener
                }
                if (snapshot != null && snapshot.exists()) {
                    _currentUserData.value = snapshot.toObject(User::class.java)
                }
            }
    }

    fun startListeningToCurrentUser() {
        val uid = auth.currentUser?.uid
        if (uid != null) {
            startListening(uid)
        } else {
            clear()
        }
    }

    private fun cleanupListener() {
        listenerRegistration?.remove()
        listenerRegistration = null
    }

    fun updateCurrentUser(user: User) {
        _currentUserData.value = user
    }

    fun clear() {
        cleanupListener()
        activeUid = null
        _currentUserData.value = null
    }
}
