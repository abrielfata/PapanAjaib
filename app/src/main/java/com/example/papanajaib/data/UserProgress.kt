package com.example.papanajaib.data

data class UserProgress(
    val familyId: String = "",
    val totalTasksCompleted: Int = 0,
    val currentStreak: Int = 0,
    val longestStreak: Int = 0,
    val lastTaskDate: String = "", // Format: yyyy-MM-dd
    val perfectDays: Int = 0,
    val categoryProgress: Map<String, Int> = emptyMap(), // kategori -> jumlah selesai
    val achievements: List<String> = emptyList(), // ID achievement yang sudah unlock
    val lastUpdated: Long = System.currentTimeMillis()
)