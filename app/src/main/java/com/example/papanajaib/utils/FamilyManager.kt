package com.example.papanajaib.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random

object FamilyManager {
    private const val PREFS_NAME = "papan_ajaib_family"
    private const val BACKUP_PREFS_NAME = "papan_ajaib_family_backup"

    // Current config keys
    private const val KEY_FAMILY_ID = "family_id"
    private const val KEY_IS_PARENT = "is_parent"
    private const val KEY_FAMILY_NAME = "family_name"
    private const val KEY_CREATED_AT = "created_at"
    private const val KEY_LAST_USED = "last_used"

    // Backup keys
    private const val KEY_BACKUP_FAMILY_ID = "backup_family_id"
    private const val KEY_BACKUP_IS_PARENT = "backup_is_parent"
    private const val KEY_BACKUP_FAMILY_NAME = "backup_family_name"
    private const val KEY_BACKUP_CREATED_AT = "backup_created_at"
    private const val KEY_BACKUP_TIMESTAMP = "backup_timestamp"

    // Constants
    private const val FAMILY_ID_LENGTH = 6
    private const val CHARSET = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"
    private const val DEFAULT_FAMILY_NAME = "Keluarga Kita"

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private fun getBackupPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(BACKUP_PREFS_NAME, Context.MODE_PRIVATE)
    }

    // Generate unique family ID dengan better randomization
    fun generateFamilyId(): String {
        return (1..FAMILY_ID_LENGTH)
            .map { CHARSET.random() }
            .joinToString("")
    }

    // Save family configuration dengan validation dan backup
    fun saveFamilyConfig(
        context: Context,
        familyId: String,
        isParent: Boolean,
        familyName: String = ""
    ): Boolean {
        return if (isValidFamilyId(familyId)) {
            val timestamp = System.currentTimeMillis()

            // Backup existing config jika ada
            backupCurrentConfig(context)

            // Save new config
            getPrefs(context).edit().apply {
                putString(KEY_FAMILY_ID, familyId.uppercase())
                putBoolean(KEY_IS_PARENT, isParent)
                putString(KEY_FAMILY_NAME, familyName.ifEmpty { DEFAULT_FAMILY_NAME })
                putLong(KEY_CREATED_AT, timestamp)
                putLong(KEY_LAST_USED, timestamp)
                apply()
            }

            Log.d("FamilyManager", "✅ Family config saved: ID=$familyId, Parent=$isParent, Name=$familyName")
            true
        } else {
            Log.e("FamilyManager", "❌ Invalid family ID: $familyId")
            false
        }
    }

    // Backup current configuration sebelum save yang baru
    private fun backupCurrentConfig(context: Context) {
        val currentPrefs = getPrefs(context)
        val backupPrefs = getBackupPrefs(context)

        val currentFamilyId = currentPrefs.getString(KEY_FAMILY_ID, null)
        if (!currentFamilyId.isNullOrEmpty()) {
            Log.d("FamilyManager", "💾 Backing up current config: $currentFamilyId")

            backupPrefs.edit().apply {
                putString(KEY_BACKUP_FAMILY_ID, currentFamilyId)
                putBoolean(KEY_BACKUP_IS_PARENT, currentPrefs.getBoolean(KEY_IS_PARENT, false))
                putString(KEY_BACKUP_FAMILY_NAME, currentPrefs.getString(KEY_FAMILY_NAME, DEFAULT_FAMILY_NAME))
                putLong(KEY_BACKUP_CREATED_AT, currentPrefs.getLong(KEY_CREATED_AT, 0))
                putLong(KEY_BACKUP_TIMESTAMP, System.currentTimeMillis())
                apply()
            }
        }
    }

    // Get current family ID
    fun getFamilyId(context: Context): String? {
        val familyId = getPrefs(context).getString(KEY_FAMILY_ID, null)

        // Update last used timestamp
        if (!familyId.isNullOrEmpty()) {
            updateLastUsed(context)
        }

        return familyId
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
        val familyId = getFamilyId(context)
        val isConfigured = !familyId.isNullOrEmpty()

        Log.d("FamilyManager", "🔍 Family configured check: $isConfigured (ID: $familyId)")
        return isConfigured
    }

    // Clear family configuration dengan backup
    fun clearFamilyConfig(context: Context) {
        Log.d("FamilyManager", "🧹 Clearing family config")

        // Backup before clearing
        backupCurrentConfig(context)

        // Clear current config
        getPrefs(context).edit().clear().apply()

        Log.d("FamilyManager", "✅ Family config cleared")
    }

    // Check if backup exists
    fun hasBackupConfig(context: Context): Boolean {
        val backupFamilyId = getBackupPrefs(context).getString(KEY_BACKUP_FAMILY_ID, null)
        val hasBackup = !backupFamilyId.isNullOrEmpty()

        Log.d("FamilyManager", "💾 Backup exists: $hasBackup")
        return hasBackup
    }

    // Get backup family info
    fun getBackupFamilyInfo(context: Context): BackupInfo? {
        val backupPrefs = getBackupPrefs(context)
        val familyId = backupPrefs.getString(KEY_BACKUP_FAMILY_ID, null)

        return if (!familyId.isNullOrEmpty()) {
            BackupInfo(
                familyId = familyId,
                familyName = backupPrefs.getString(KEY_BACKUP_FAMILY_NAME, DEFAULT_FAMILY_NAME) ?: DEFAULT_FAMILY_NAME,
                isParent = backupPrefs.getBoolean(KEY_BACKUP_IS_PARENT, false),
                createdAt = backupPrefs.getLong(KEY_BACKUP_CREATED_AT, 0),
                backupTimestamp = backupPrefs.getLong(KEY_BACKUP_TIMESTAMP, 0)
            )
        } else null
    }

    // Restore from backup
    fun restoreFromBackup(context: Context): Boolean {
        val backupInfo = getBackupFamilyInfo(context)

        return if (backupInfo != null) {
            Log.d("FamilyManager", "🔄 Restoring from backup: ${backupInfo.familyId}")

            val success = saveFamilyConfig(
                context = context,
                familyId = backupInfo.familyId,
                isParent = backupInfo.isParent,
                familyName = backupInfo.familyName
            )

            if (success) {
                // Update created_at dengan nilai original
                getPrefs(context).edit().apply {
                    putLong(KEY_CREATED_AT, backupInfo.createdAt)
                    apply()
                }

                Log.d("FamilyManager", "✅ Successfully restored from backup")
            }

            success
        } else {
            Log.w("FamilyManager", "❌ No backup to restore")
            false
        }
    }

    // Clear backup
    fun clearBackup(context: Context) {
        Log.d("FamilyManager", "🗑️ Clearing backup")
        getBackupPrefs(context).edit().clear().apply()
    }

    // Improved validation
    fun isValidFamilyId(familyId: String?): Boolean {
        val isValid = familyId != null &&
                familyId.length == FAMILY_ID_LENGTH &&
                familyId.all { it.isLetterOrDigit() }

        if (!isValid) {
            Log.w("FamilyManager", "❌ Invalid family ID format: '$familyId'")
        }

        return isValid
    }

    // Get role display name
    fun getRoleDisplayName(context: Context): String {
        return if (isParent(context)) "Orang Tua" else "Anak"
    }

    // Get family creation date
    fun getFamilyCreatedAt(context: Context): Long {
        return getPrefs(context).getLong(KEY_CREATED_AT, 0)
    }

    // Get last used timestamp
    fun getLastUsed(context: Context): Long {
        return getPrefs(context).getLong(KEY_LAST_USED, 0)
    }

    // Update last used timestamp
    private fun updateLastUsed(context: Context) {
        getPrefs(context).edit().apply {
            putLong(KEY_LAST_USED, System.currentTimeMillis())
            apply()
        }
    }

    // Get formatted family info untuk display
    fun getFormattedFamilyInfo(context: Context): String {
        val familyId = getFamilyId(context)
        val familyName = getFamilyName(context)
        val isParent = isParent(context)
        val createdAt = getFamilyCreatedAt(context)

        val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
        val createdDate = if (createdAt > 0) formatter.format(Date(createdAt)) else "Tidak diketahui"

        return buildString {
            append("👨‍👩‍👧‍👦 Nama: $familyName\n")
            append("🔑 Kode: $familyId\n")
            append("👤 Role: ${if (isParent) "Orang Tua" else "Anak"}\n")
            append("📅 Dibuat: $createdDate")
        }
    }

    // Check family health (untuk diagnostic)
    fun checkFamilyHealth(context: Context): FamilyHealthStatus {
        val familyId = getFamilyId(context)
        val familyName = getFamilyName(context)
        val lastUsed = getLastUsed(context)
        val createdAt = getFamilyCreatedAt(context)

        return when {
            familyId.isNullOrEmpty() -> FamilyHealthStatus.NOT_CONFIGURED
            !isValidFamilyId(familyId) -> FamilyHealthStatus.INVALID_ID
            familyName.isEmpty() -> FamilyHealthStatus.MISSING_NAME
            createdAt == 0L -> FamilyHealthStatus.MISSING_METADATA
            System.currentTimeMillis() - lastUsed > (7 * 24 * 60 * 60 * 1000L) -> FamilyHealthStatus.INACTIVE // 7 days
            else -> FamilyHealthStatus.HEALTHY
        }
    }

    // Data classes
    data class BackupInfo(
        val familyId: String,
        val familyName: String,
        val isParent: Boolean,
        val createdAt: Long,
        val backupTimestamp: Long
    ) {
        fun getFormattedBackupDate(): String {
            val formatter = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            return formatter.format(Date(backupTimestamp))
        }
    }

    enum class FamilyHealthStatus {
        HEALTHY,
        NOT_CONFIGURED,
        INVALID_ID,
        MISSING_NAME,
        MISSING_METADATA,
        INACTIVE
    }
}