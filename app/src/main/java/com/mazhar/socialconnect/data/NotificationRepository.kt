package com.mazhar.socialconnect.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.mazhar.socialconnect.data.model.Notification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NotificationRepository {
    private val auth = FirebaseAuth.getInstance()
    private val db = FirebaseFirestore.getInstance()

    private val _notifications = MutableStateFlow<List<Notification>>(emptyList())
    val notifications: StateFlow<List<Notification>> = _notifications.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

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
        
        // Show loading spinner only if we don't have notifications yet
        if (_notifications.value.isEmpty()) {
            _loading.value = true
        }

        listenerRegistration = db.collection("notifications")
            .whereEqualTo("targetUserId", uid)
            .addSnapshotListener { snapshot, error ->
                _loading.value = false
                if (error != null) {
                    android.util.Log.e("NotificationRepository", "Error listening to notifications: ${error.message}")
                    return@addSnapshotListener
                }
                
                val list = snapshot?.documents
                    ?.mapNotNull { it.toObject(Notification::class.java)?.copy(id = it.id) }
                    ?.sortedByDescending { it.timestamp }
                
                _notifications.value = list ?: emptyList()
            }
    }

    fun startListeningToNotifications() {
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

    fun setNotifications(list: List<Notification>) {
        _notifications.value = list
    }

    fun clear() {
        cleanupListener()
        activeUid = null
        _notifications.value = emptyList()
        _loading.value = false
    }
}
