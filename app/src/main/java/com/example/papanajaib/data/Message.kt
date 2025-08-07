package com.example.papanajaib.data

import com.google.firebase.database.Exclude
import com.google.firebase.database.PropertyName

data class Message(
    val id: String = "",
    val text: String = "",
    val icon: String = "",

    // Pastikan hanya pakai isCompleted, bukan completed
    @get:PropertyName("isCompleted")
    @set:PropertyName("isCompleted")
    var isCompleted: Boolean = false,

    val timestamp: Long = System.currentTimeMillis(),
    val familyId: String = ""
) {
    // Helper function untuk format display - exclude dari Firebase
    @Exclude
    fun getDisplayText(): String {
        return if (icon.isNotEmpty()) "$icon $text" else text
    }
}
