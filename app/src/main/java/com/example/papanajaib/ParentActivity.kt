package com.example.papanajaib

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.chip.Chip
import com.google.firebase.database.*
import com.example.papanajaib.data.Message
import com.example.papanajaib.databinding.ActivityParentBinding
import com.example.papanajaib.adapter.ParentMessageAdapter
import com.example.papanajaib.utils.Notification
import com.example.papanajaib.utils.FamilyManager

class ParentActivity : AppCompatActivity() {

    private lateinit var binding: ActivityParentBinding
    private lateinit var database: DatabaseReference
    private lateinit var messageList: MutableList<Message>
    private lateinit var adapter: ParentMessageAdapter
    private lateinit var familyId: String

    // Suggested icons dengan categories
    private val suggestedIcons = mapOf(
        "Mainan" to listOf("🧸", "🎮", "🎲", "🪀", "🎨"),
        "Makanan" to listOf("🍔", "🍽️", "🥛", "🍎", "🍪"),
        "Sekolah" to listOf("📚", "✏️", "🎒", "📝", "🖍️"),
        "Rumah" to listOf("🛏️", "🧹", "🧺", "🚿", "🦷"),
        "Pakaian" to listOf("👕", "👔", "🧦", "👞", "🧥"),
        "Lainnya" to listOf("⚽", "🌱", "💡", "🎵", "⭐")
    )

    // Track UI state
    private var isCreatingMessage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityParentBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Initialize with error handling
        if (!initializeApp()) {
            return // App initialization failed, activity will finish
        }

        setupUI()
        setupRecyclerView()
        setupInputFields()
        setupSuggestedIcons()
        setupClickListeners()

        // Cleanup Firebase data untuk memastikan konsistensi
        cleanupFirebaseData()

        listenForMessages()
    }

    private fun initializeApp(): Boolean {
        // Get family ID dengan proper error handling
        familyId = FamilyManager.getFamilyId(this) ?: run {
            showErrorAndFinish("Konfigurasi keluarga tidak ditemukan")
            return false
        }

        Log.d("ParentActivity", "🏠 Family ID: $familyId")

        // Initialize Firebase
        database = FirebaseDatabase.getInstance()
            .getReference("families")
            .child(familyId)
            .child("messages")

        Log.d("ParentActivity", "🔗 Firebase path: families/$familyId/messages")

        // Initialize FCM
        try {
            Notification.initializeFCM(this)
            Notification.subscribeToParentNotifications()
        } catch (e: Exception) {
            // Log error but don't fail the app
            Log.e("ParentActivity", "FCM initialization failed", e)
        }

        return true
    }

    private fun setupUI() {
        // Set title with family info
        val familyName = FamilyManager.getFamilyName(this)
        supportActionBar?.title = "Mode Orang Tua - $familyName"

        // Initialize statistics
        updateStatistics(0, 0, 0)
    }

    private fun setupRecyclerView() {
        messageList = mutableListOf()
        adapter = ParentMessageAdapter(messageList) { message ->
            showDeleteConfirmation(message)
        }

        binding.rvParentMessages.apply {
            layoutManager = LinearLayoutManager(this@ParentActivity)
            adapter = this@ParentActivity.adapter
            // Add item animations
            itemAnimator?.changeDuration = 300
        }
    }

    private fun setupInputFields() {
        // Setup message text field with real-time validation
        binding.etMessageText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateMessageText(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        // Setup icon field with validation
        binding.etMessageIcon.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                validateMessageIcon(s?.toString() ?: "")
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun validateMessageText(text: String) {
        val length = text.length
        val trimmedLength = text.trim().length

        // Update counter with color coding
        binding.tilMessageText.helperText = "$length/100 karakter"

        when {
            trimmedLength == 0 -> {
                binding.tilMessageText.error = "Pesan tidak boleh kosong"
                setHelperTextColor(R.color.md_theme_error)
            }
            length > 100 -> {
                binding.tilMessageText.error = "Pesan terlalu panjang"
                setHelperTextColor(R.color.md_theme_error)
            }
            length > 80 -> {
                binding.tilMessageText.error = null
                setHelperTextColor(android.R.color.holo_orange_light)
            }
            else -> {
                binding.tilMessageText.error = null
                setHelperTextColor(R.color.md_theme_onSurfaceVariant)
            }
        }

        updateCreateButtonState()
    }

    private fun validateMessageIcon(icon: String) {
        when {
            icon.length > 2 -> {
                binding.tilMessageIcon.error = "Maksimal 2 karakter emoji"
            }
            else -> {
                binding.tilMessageIcon.error = null
            }
        }
        updateCreateButtonState()
    }

    private fun setHelperTextColor(colorRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        binding.tilMessageText.setHelperTextColor(
            android.content.res.ColorStateList.valueOf(color)
        )
    }

    private fun updateCreateButtonState() {
        val text = binding.etMessageText.text?.toString()?.trim() ?: ""
        val icon = binding.etMessageIcon.text?.toString() ?: ""

        val isValid = text.isNotEmpty() &&
                text.length <= 100 &&
                icon.length <= 2 &&
                !isCreatingMessage

        binding.fabCreateMessage.isEnabled = isValid
        binding.fabCreateMessage.alpha = if (isValid) 1.0f else 0.6f
    }

    private fun setupSuggestedIcons() {
        binding.chipGroupIcons.removeAllViews()

        suggestedIcons.forEach { (category, icons) ->
            // Add category separator if needed
            icons.forEach { icon ->
                val chip = Chip(this).apply {
                    text = icon
                    isClickable = true
                    isCheckable = false

                    // Style the chip
                    setChipBackgroundColorResource(R.color.md_theme_secondaryContainer)
                    setTextColor(ContextCompat.getColor(context, R.color.md_theme_onSecondaryContainer))

                    setOnClickListener {
                        binding.etMessageIcon.setText(icon)
                        // Add haptic feedback
                        performHapticFeedback(android.view.HapticFeedbackConstants.VIRTUAL_KEY)

                        // Visual feedback
                        scaleX = 0.9f
                        scaleY = 0.9f
                        animate()
                            .scaleX(1.0f)
                            .scaleY(1.0f)
                            .setDuration(150)
                            .start()
                    }
                }
                binding.chipGroupIcons.addView(chip)
            }
        }
    }

    private fun setupClickListeners() {
        binding.fabCreateMessage.setOnClickListener {
            if (!isCreatingMessage) {
                createMessage()
            }
        }

        binding.btnResetAllMessages.setOnClickListener {
            showResetAllConfirmation()
        }
    }

    private fun createMessage() {
        val text = binding.etMessageText.text?.toString()?.trim() ?: ""
        val icon = binding.etMessageIcon.text?.toString()?.trim() ?: ""

        // Final validation
        if (!validateInputs(text, icon)) {
            return
        }

        isCreatingMessage = true
        updateCreateButtonState()

        // Show loading state
        binding.fabCreateMessage.text = "Membuat..."

        val messageId = database.push().key ?: run {
            showCreateError("Gagal generate ID pesan")
            return
        }

        Log.d("ParentActivity", "🆕 Creating message:")
        Log.d("ParentActivity", "   ID: $messageId")
        Log.d("ParentActivity", "   Text: $text")
        Log.d("ParentActivity", "   Icon: $icon")

        val message = Message(
            id = messageId,
            text = text,
            icon = icon,
            isCompleted = false, // Explicitly set to false
            familyId = familyId,
            timestamp = System.currentTimeMillis()
        )

        database.child(messageId).setValue(message)
            .addOnSuccessListener {
                Log.d("ParentActivity", "✅ Message created successfully: $messageId")
                onCreateSuccess(text, icon)
            }
            .addOnFailureListener { exception ->
                Log.e("ParentActivity", "❌ Failed to create message", exception)
                onCreateFailure(exception)
            }
    }

    private fun validateInputs(text: String, icon: String): Boolean {
        when {
            text.isEmpty() -> {
                binding.etMessageText.error = "Pesan tidak boleh kosong"
                binding.etMessageText.requestFocus()
                return false
            }
            text.length > 100 -> {
                binding.etMessageText.error = "Pesan terlalu panjang (maksimal 100 karakter)"
                return false
            }
            icon.length > 2 -> {
                binding.etMessageIcon.error = "Icon terlalu panjang (maksimal 2 karakter)"
                return false
            }
        }
        return true
    }

    private fun onCreateSuccess(text: String, icon: String) {
        isCreatingMessage = false
        binding.fabCreateMessage.text = "✨ Buat Pesan"
        updateCreateButtonState()

        Toast.makeText(this, "Pesan berhasil dibuat! 🎉", Toast.LENGTH_SHORT).show()
        clearInputs()
        sendNotificationToChild(text, icon)

        // Scroll to top to show new message
        binding.rvParentMessages.smoothScrollToPosition(0)
    }

    private fun onCreateFailure(exception: Exception) {
        isCreatingMessage = false
        binding.fabCreateMessage.text = "✨ Buat Pesan"
        updateCreateButtonState()

        showCreateError("Gagal membuat pesan: ${exception.message}")
    }

    private fun showCreateError(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }

    private fun clearInputs() {
        binding.etMessageText.text?.clear()
        binding.etMessageIcon.text?.clear()
        binding.etMessageText.clearFocus()

        // Clear any errors
        binding.tilMessageText.error = null
        binding.tilMessageIcon.error = null
    }

    private fun sendNotificationToChild(taskText: String, taskIcon: String) {
        try {
            Notification.triggerNewTaskNotification(taskText, taskIcon)
        } catch (e: Exception) {
            // Don't fail the creation if notification fails
            Log.e("ParentActivity", "Notification failed", e)
        }
    }

    private fun showDeleteConfirmation(message: Message) {
        val displayText = if (message.icon.isNotEmpty()) {
            "${message.icon} ${message.text}"
        } else {
            message.text
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Hapus Pesan")
            .setMessage("Apakah Anda yakin ingin menghapus:\n\n\"$displayText\"")
            .setPositiveButton("Hapus") { _, _ ->
                deleteMessage(message)
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun deleteMessage(message: Message) {
        Log.d("ParentActivity", "🗑️ Deleting message: ${message.id}")

        database.child(message.id).removeValue()
            .addOnSuccessListener {
                Log.d("ParentActivity", "✅ Message deleted successfully")
                Toast.makeText(this, "Pesan dihapus!", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { exception ->
                Log.e("ParentActivity", "❌ Failed to delete message", exception)
                Toast.makeText(this, "Gagal menghapus: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun showResetAllConfirmation() {
        if (messageList.isEmpty()) {
            Toast.makeText(this, "Tidak ada pesan untuk dihapus", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Semua Pesan")
            .setMessage("Apakah Anda yakin ingin menghapus SEMUA ${messageList.size} pesan?\n\nAksi ini tidak dapat dibatalkan.")
            .setPositiveButton("Ya, Hapus Semua") { _, _ ->
                resetAllMessages()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun resetAllMessages() {
        Log.d("ParentActivity", "🧹 Resetting all messages")

        database.removeValue()
            .addOnSuccessListener {
                Log.d("ParentActivity", "✅ All messages reset successfully")
                Toast.makeText(this, "Semua pesan berhasil dihapus! 🧹", Toast.LENGTH_SHORT).show()
                try {
                    Notification.triggerResetNotification()
                } catch (e: Exception) {
                    Log.e("ParentActivity", "Reset notification failed", e)
                }
            }
            .addOnFailureListener { exception ->
                Log.e("ParentActivity", "❌ Failed to reset messages", exception)
                Toast.makeText(this, "Gagal reset: ${exception.message}", Toast.LENGTH_LONG).show()
            }
    }

    private fun updateStatistics(total: Int, completed: Int, pending: Int) {
        val statsText = when {
            total == 0 -> "📝 Belum ada pesan yang dibuat"
            completed == total -> "🎉 Semua tugas selesai! ($completed/$total)"
            completed == 0 -> "📊 Total: $total tugas | Belum ada yang dikerjakan"
            else -> "📊 Total: $total | ✅ Selesai: $completed | ⏳ Pending: $pending"
        }

        binding.tvStatistics.text = statsText

        // Update statistics background color based on completion rate
        val completionRate = if (total > 0) (completed.toFloat() / total) else 0f
        val backgroundRes = when {
            completionRate == 1.0f -> R.color.md_theme_primaryContainer
            completionRate >= 0.7f -> R.color.md_theme_secondaryContainer
            else -> R.color.md_theme_surfaceVariant
        }

        binding.tvStatistics.setBackgroundResource(backgroundRes)
    }

    private fun listenForMessages() {
        Log.d("ParentActivity", "🔗 Starting Firebase listener")

        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ParentActivity", "🔄 Data changed, updating UI")
                Log.d("ParentActivity", "   Snapshot exists: ${snapshot.exists()}")
                Log.d("ParentActivity", "   Children count: ${snapshot.childrenCount}")

                messageList.clear()

                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let {
                        Log.d("ParentActivity", "📝 Message loaded:")
                        Log.d("ParentActivity", "   ID: ${it.id}")
                        Log.d("ParentActivity", "   Text: ${it.text}")
                        Log.d("ParentActivity", "   Icon: ${it.icon}")
                        Log.d("ParentActivity", "   Completed: ${it.isCompleted}")
                        Log.d("ParentActivity", "   FamilyId: ${it.familyId}")

                        if (it.text.isNotEmpty() && it.familyId.isNotEmpty()) {
                            messageList.add(it)
                        } else {
                            Log.w("ParentActivity", "⚠️ Skipping invalid message: ${it.id}")
                        }
                    } ?: Log.w("ParentActivity", "⚠️ Failed to parse message: ${messageSnapshot.key}")
                }

                // Sort by timestamp (newest first)
                messageList.sortByDescending { it.timestamp }

                Log.d("ParentActivity", "📊 Final message list size: ${messageList.size}")

                adapter.notifyDataSetChanged()

                // Update UI state
                updateUIState()

                // Update statistics
                val completed = messageList.count { it.isCompleted }
                val total = messageList.size
                val pending = total - completed
                updateStatistics(total, completed, pending)

                Log.d("ParentActivity", "📈 Statistics - Total: $total, Completed: $completed, Pending: $pending")
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ParentActivity", "❌ Database error: ${error.message}")
                Toast.makeText(
                    this@ParentActivity,
                    "Gagal memuat pesan: ${error.message}",
                    Toast.LENGTH_LONG
                ).show()
            }
        })
    }

    private fun updateUIState() {
        // Show/hide empty state
        if (messageList.isEmpty()) {
            binding.emptyStateView.visibility = View.VISIBLE
            binding.rvParentMessages.visibility = View.GONE
        } else {
            binding.emptyStateView.visibility = View.GONE
            binding.rvParentMessages.visibility = View.VISIBLE
        }

        // Update reset button state
        binding.btnResetAllMessages.isEnabled = messageList.isNotEmpty()
        binding.btnResetAllMessages.alpha = if (messageList.isNotEmpty()) 1.0f else 0.6f
    }

    // Method untuk cleanup Firebase data dan memastikan konsistensi
    private fun cleanupFirebaseData() {
        Log.d("ParentActivity", "🧹 Starting Firebase data cleanup...")

        database.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var cleanupCount = 0

                for (messageSnapshot in snapshot.children) {
                    val messageRef = messageSnapshot.ref

                    // Check if ada field 'completed' yang duplicate
                    if (messageSnapshot.hasChild("completed")) {
                        val completedValue = messageSnapshot.child("completed").getValue(Boolean::class.java) ?: false
                        val isCompletedValue = messageSnapshot.child("isCompleted").getValue(Boolean::class.java) ?: false

                        Log.d("Cleanup", "📝 Cleaning message ${messageSnapshot.key}:")
                        Log.d("Cleanup", "   completed: $completedValue")
                        Log.d("Cleanup", "   isCompleted: $isCompletedValue")

                        // Ambil nilai yang benar (prioritas isCompleted)
                        val correctValue = if (messageSnapshot.hasChild("isCompleted")) isCompletedValue else completedValue

                        // Update dengan nilai yang benar dan hapus field 'completed'
                        val updates = mapOf<String, Any?>(
                            "isCompleted" to correctValue,
                            "completed" to null // null akan menghapus field
                        )

                        messageRef.updateChildren(updates)
                            .addOnSuccessListener {
                                Log.d("Cleanup", "✅ Cleaned up message ${messageSnapshot.key}")
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Cleanup", "❌ Failed to cleanup ${messageSnapshot.key}", exception)
                            }

                        cleanupCount++
                    }

                    // Pastikan setiap message punya field isCompleted
                    if (!messageSnapshot.hasChild("isCompleted")) {
                        Log.d("Cleanup", "⚠️ Message ${messageSnapshot.key} missing isCompleted field, adding default")
                        messageRef.child("isCompleted").setValue(false)
                        cleanupCount++
                    }
                }

                if (cleanupCount > 0) {
                    Log.d("Cleanup", "🧹 Cleaned up $cleanupCount messages")
                } else {
                    Log.d("Cleanup", "✅ No cleanup needed, all data is consistent")
                }
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("Cleanup", "❌ Cleanup failed", error.toException())
            }
        })
    }

    private fun showErrorAndFinish(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
        finish()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up if needed
        isCreatingMessage = false
        Log.d("ParentActivity", "🏁 ParentActivity destroyed")
    }
}