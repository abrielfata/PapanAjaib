package com.example.papanajaib.utils

import android.content.Context
import android.util.Log
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging

object Notification {

    fun initializeFCM(context: Context) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.w("FCM", "Fetching FCM registration token failed", task.exception)
                return@addOnCompleteListener
            }

            // Get new FCM registration token
            val token = task.result
            Log.d("FCM", "FCM Registration Token: $token")

            // Save token to SharedPreferences
            saveTokenToPreferences(context, token)

            // Optionally save to Firebase Database for targeting specific devices
            saveTokenToFirebase(token)
        }
    }

    fun subscribeToChildNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("child_notifications")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to child notifications"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("FCM", msg)
            }
    }

    fun subscribeToParentNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("parent_notifications")
            .addOnCompleteListener { task ->
                var msg = "Subscribed to parent notifications"
                if (!task.isSuccessful) {
                    msg = "Subscription failed"
                }
                Log.d("FCM", msg)
            }
    }

    // Simplified notification trigger - Firebase Functions akan handle pengiriman
    fun triggerNewTaskNotification(taskText: String, taskIcon: String) {
        // Simpan data ke Firebase yang akan trigger Cloud Function
        val database = FirebaseDatabase.getInstance().getReference("notifications")
        val notificationData = mapOf(
            "type" to "new_task",
            "title" to "Ada Tugas Baru! ✨",
            "body" to if (taskIcon.isNotEmpty()) "$taskIcon $taskText" else taskText,
            "icon" to taskIcon,
            "timestamp" to System.currentTimeMillis(),
            "target_topic" to "child_notifications"
        )

        database.push().setValue(notificationData)
            .addOnSuccessListener {
                Log.d("FCM", "Notification trigger saved to Firebase")
            }
            .addOnFailureListener { exception ->
                Log.e("FCM", "Failed to save notification trigger", exception)
            }
    }

    fun triggerResetNotification() {
        val database = FirebaseDatabase.getInstance().getReference("notifications")
        val notificationData = mapOf(
            "type" to "reset_tasks",
            "title" to "Papan Ajaib Direset",
            "body" to "Semua tugas telah dihapus! 🧹",
            "icon" to "🧹",
            "timestamp" to System.currentTimeMillis(),
            "target_topic" to "child_notifications"
        )

        database.push().setValue(notificationData)
    }

    private fun saveTokenToPreferences(context: Context, token: String) {
        val sharedPref = context.getSharedPreferences("papan_ajaib_prefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("fcm_token", token)
            apply()
        }
    }

    private fun saveTokenToFirebase(token: String) {
        val database = FirebaseDatabase.getInstance().getReference("device_tokens")
        val tokenData = mapOf(
            "token" to token,
            "timestamp" to System.currentTimeMillis()
        )
        database.child(token).setValue(tokenData)
    }
}
