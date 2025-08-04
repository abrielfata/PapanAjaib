package com.example.papanajaib.data

data class Message(
    val id: String = "",
    val text: String = "",
    val icon: String = "",
    var isCompleted: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val familyId: String = ""
) {
    // Helper function untuk format display
    fun getDisplayText(): String {
        return if (icon.isNotEmpty()) "$icon $text" else text
    }
}