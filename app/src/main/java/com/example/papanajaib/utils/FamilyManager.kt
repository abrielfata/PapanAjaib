package com.example.papanajaib.utils

import android.content.Context
import android.content.SharedPreferences
import kotlin.random.Random

object FamilyManager {
    private const val PREFS_NAME = "papan_ajaib_family"
    private const val KEY_FAMILY_ID = "family_id"
    private const val KEY_IS_PARENT = "is_parent"
    private const val KEY_FAMILY_NAME = "family_name"

    // Constants
    private const val FAMILY_ID_LENGTH = 6
    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val DEFAULT_FAMILY_NAME = "Keluarga Kita"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Generate unique family ID dengan better randomization
    fun generateFamilyId(): String {
        return (1..FAMILY_ID_LENGTH)
            .map { CHARSET.random() }
            .joinToString("")
    }

    // Save family configuration dengan validation
    fun saveFamilyConfig(
        context: Context,
        familyId: String,
        isParent: Boolean,
        familyName: String = ""
    ): Boolean {
        return if (isValidFamilyId(familyId)) {
            getPrefs(context).edit().apply {
                putString(KEY_FAMILY_ID, familyId.uppercase())
                putBoolean(KEY_IS_PARENT, isParent)
                putString(KEY_FAMILY_NAME, familyName.ifEmpty { DEFAULT_FAMILY_NAME })
                apply()
            }
            true
        } else {
            false
        }
    }

    // Get current family ID
    fun getFamilyId(context: Context): String? {
        return getPrefs(context).getString(KEY_FAMILY_ID, null)
    }

    // Check if user is parent
    fun isParent(context: Context): Boolean {
        return getPrefs(context).getBoolean(KEY_IS_PARENT, false)
    }

    // Get family name dengan fallback
    fun getFamilyName(context: Context): String {
        return getPrefs(context).getString(KEY_FAMILY_NAME, DEFAULT_FAMILY_NAME)
            ?: DEFAULT_FAMILY_NAME
    }

    // Check if family is configured
    fun isFamilyConfigured(context: Context): Boolean {
        return !getFamilyId(context).isNullOrEmpty()
    }

    // Clear family configuration
    fun clearFamilyConfig(context: Context) {
        getPrefs(context).edit().clear().apply()
    }

    // Improved validation
    fun isValidFamilyId(familyId: String?): Boolean {
        return familyId != null &&
                familyId.length == FAMILY_ID_LENGTH &&
                familyId.all { it.isLetterOrDigit() }
    }

    // New: Get role display name
    fun getRoleDisplayName(context: Context): String {
        return if (isParent(context)) "Orang Tua" else "Anak"
    }
}
