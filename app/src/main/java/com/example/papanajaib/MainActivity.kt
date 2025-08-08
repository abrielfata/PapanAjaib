package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.papanajaib.databinding.ActivityMainBinding
import com.example.papanajaib.utils.FamilyManager
import com.example.papanajaib.utils.LottieUtils
import com.google.firebase.database.*

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var database: DatabaseReference
    private var familyStatusListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Verifikasi konfigurasi keluarga
        if (!verifyFamilyConfiguration()) {
            return // Activity akan finish jika konfigurasi tidak valid
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupLottieAnimations()
        setupClickListeners()
        monitorFamilyStatus()
    }

    // Update MainActivity.kt - bagian setupLottieAnimations()

    private fun setupLottieAnimations() {
        // PERBAIKAN: Gunakan raw resource daripada asset
        // Setup main Lottie animation
        binding.lottieAstro.apply {
            setAnimation(R.raw.astro) // Gunakan resource ID
            repeatCount = -1 // Infinite loop
            speed = 0.8f
            playAnimation()
        }

        // Setup status indicator animation
        binding.lottieStatusIndicator.apply {
            setAnimation(R.raw.astro) // Gunakan resource ID
            repeatCount = -1
            speed = 1.5f
            playAnimation()
        }
    }


    private fun updateLottieBasedOnStatus(isConnected: Boolean) {
        try {
            if (isConnected) {
                LottieUtils.setSpeed(binding.lottieAstro, 1.0f)
                LottieUtils.setAlpha(binding.lottieAstro, 1.0f)
                LottieUtils.resumeAnimation(binding.lottieAstro)

                LottieUtils.setSpeed(binding.lottieStatusIndicator, 2.0f)
                LottieUtils.setAlpha(binding.lottieStatusIndicator, 1.0f)
                LottieUtils.resumeAnimation(binding.lottieStatusIndicator)
            } else {
                LottieUtils.setSpeed(binding.lottieAstro, 0.3f)
                LottieUtils.setAlpha(binding.lottieAstro, 0.5f)

                LottieUtils.setSpeed(binding.lottieStatusIndicator, 0.5f)
                LottieUtils.setAlpha(binding.lottieStatusIndicator, 0.7f)
            }
        } catch (e: Exception) {
            Log.e("MainActivity", "Error updating Lottie status: ${e.message}")
        }
    }

    private fun verifyFamilyConfiguration(): Boolean {
        val familyId = FamilyManager.getFamilyId(this)
        val familyName = FamilyManager.getFamilyName(this)

        Log.d("MainActivity", "Verifying family config - ID: $familyId, Name: $familyName")

        if (familyId.isNullOrEmpty()) {
            Log.w("MainActivity", "No family ID found, redirecting to setup")
            redirectToSetup("Konfigurasi keluarga tidak ditemukan")
            return false
        }

        // Initialize Firebase reference
        database = FirebaseDatabase.getInstance()
            .getReference("families")
            .child(familyId)

        return true
    }

    private fun redirectToSetup(reason: String) {
        Log.d("MainActivity", "Redirecting to setup: $reason")
        startActivity(Intent(this, SetupActivity::class.java))
        finish()
    }

    private fun setupUI() {
        val familyName = FamilyManager.getFamilyName(this)
        val familyId = FamilyManager.getFamilyId(this)
        val isParent = FamilyManager.isParent(this)

        // Setup action bar
        supportActionBar?.title = "Papan Ajaib - $familyName"
        supportActionBar?.subtitle = if (isParent) "Mode Orang Tua" else "Mode Anak"

        // Setup family info
        binding.tvFamilyInfo.text = buildString {
            append("👨‍👩‍👧‍👦 $familyName\n")
            append("🔑 Kode: $familyId\n")
            append("👤 Role: ${if (isParent) "Orang Tua" else "Anak"}")
        }

        // Setup status info (will be updated by listener)
        binding.tvConnectionStatus.text = "🔄 Memeriksa status..."
        binding.tvConnectionStatus.visibility = View.VISIBLE

        // Tampilkan tombol sesuai role
        if (isParent) {
            binding.btnParentMode.visibility = View.VISIBLE
            binding.btnChildMode.visibility = View.VISIBLE
            binding.btnParentMode.text = "🛠️ Mode Orang Tua"
            binding.btnChildMode.text = "👀 Lihat Sebagai Anak"
        } else {
            binding.btnParentMode.visibility = View.GONE
            binding.btnChildMode.visibility = View.VISIBLE
            binding.btnChildMode.text = "📋 Lihat Tugas"
        }

        // Show reset button only for parents
        binding.btnResetFamily.visibility = if (isParent) View.VISIBLE else View.GONE
    }

    private fun monitorFamilyStatus() {
        val familyId = FamilyManager.getFamilyId(this) ?: return

        familyStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    handleFamilyDeleted()
                    return
                }
                updateFamilyStatus(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Family status monitoring failed", error.toException())
                binding.tvConnectionStatus.text = "⚠️ Gagal memantau status keluarga"
                updateLottieBasedOnStatus(false)
            }
        }

        database.addValueEventListener(familyStatusListener!!)
        database.child("lastActivity").setValue(System.currentTimeMillis())
    }

    private fun handleFamilyDeleted() {
        Log.w("MainActivity", "Family was deleted from Firebase")

        // Pause animations when family is deleted
        LottieUtils.pauseAnimation(binding.lottieAstro)
        LottieUtils.pauseAnimation(binding.lottieStatusIndicator)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluarga Tidak Ditemukan")
            .setMessage("Keluarga Anda telah dihapus atau tidak dapat diakses.\n\nAnda akan diarahkan untuk setup ulang.")
            .setPositiveButton("OK") { _, _ ->
                FamilyManager.clearFamilyConfig(this)
                redirectToSetup("Keluarga telah dihapus")
            }
            .setCancelable(false)
            .show()
    }

    private fun updateFamilyStatus(snapshot: DataSnapshot) {
        val familyName = snapshot.child("name").getValue(String::class.java) ?: "Keluarga"
        val parentConnected = snapshot.child("parentConnected").getValue(Boolean::class.java) ?: false
        val childConnected = snapshot.child("childConnected").getValue(Boolean::class.java) ?: false
        val status = snapshot.child("status").getValue(String::class.java) ?: "active"
        val lastActivity = snapshot.child("lastActivity").getValue(Long::class.java) ?: 0L

        Log.d("MainActivity", "Family status - Parent: $parentConnected, Child: $childConnected, Status: $status")

        if (status != "active") {
            handleInactiveFamily()
            return
        }

        // Update UI dengan status koneksi
        val statusText = buildString {
            append("📊 Status Keluarga:\n")
            append("👨 Orang Tua: ${if (parentConnected) "✅ Terhubung" else "❌ Tidak aktif"}\n")
            append("👶 Anak: ${if (childConnected) "✅ Terhubung" else "❌ Belum bergabung"}")

            if (lastActivity > 0) {
                val timeDiff = System.currentTimeMillis() - lastActivity
                val minutesAgo = timeDiff / (1000 * 60)
                if (minutesAgo > 0) {
                    append("\n🕒 Aktivitas terakhir: ${minutesAgo} menit lalu")
                }
            }
        }

        binding.tvConnectionStatus.text = statusText

        // Enable/disable tombol berdasarkan status
        val isConnected = if (FamilyManager.isParent(this)) parentConnected else childConnected

        // Update Lottie animations based on connection status
        updateLottieBasedOnStatus(isConnected)

        binding.btnParentMode.isEnabled = isConnected
        binding.btnChildMode.isEnabled = isConnected

        if (!isConnected) {
            binding.btnParentMode.alpha = 0.5f
            binding.btnChildMode.alpha = 0.5f
        } else {
            binding.btnParentMode.alpha = 1.0f
            binding.btnChildMode.alpha = 1.0f
        }
    }

    private fun handleInactiveFamily() {
        // Pause animations when family is inactive
        LottieUtils.pauseAnimation(binding.lottieAstro)
        LottieUtils.pauseAnimation(binding.lottieStatusIndicator)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluarga Tidak Aktif")
            .setMessage("Keluarga Anda telah dinonaktifkan.\n\nHubungi admin keluarga atau buat keluarga baru.")
            .setPositiveButton("Setup Ulang") { _, _ ->
                FamilyManager.clearFamilyConfig(this)
                redirectToSetup("Keluarga tidak aktif")
            }
            .setCancelable(false)
            .show()
    }

    private fun setupClickListeners() {
        binding.btnParentMode.setOnClickListener {
            // Simple animation feedback
            try {
                LottieUtils.setSpeed(binding.lottieAstro, 2.0f)
                binding.root.postDelayed({
                    LottieUtils.setSpeed(binding.lottieAstro, 0.8f)
                }, 1000)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error animating parent mode click: ${e.message}")
            }
            startActivity(Intent(this, ParentActivity::class.java))
        }

        binding.btnChildMode.setOnClickListener {
            // Simple animation feedback
            try {
                LottieUtils.setSpeed(binding.lottieAstro, 1.5f)
                binding.root.postDelayed({
                    LottieUtils.setSpeed(binding.lottieAstro, 0.8f)
                }, 1000)
            } catch (e: Exception) {
                Log.e("MainActivity", "Error animating child mode click: ${e.message}")
            }
            startActivity(Intent(this, ChildActivity::class.java))
        }

        binding.btnResetFamily.setOnClickListener {
            showResetFamilyDialog()
        }
    }

    private fun showResetFamilyDialog() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Reset Keluarga")
            .setMessage("Pilih jenis reset yang ingin dilakukan:")
            .setPositiveButton("Reset Pesan Saja") { _, _ ->
                resetMessages()
            }
            .setNeutralButton("Reset Semua (Keluarga Baru)") { _, _ ->
                resetEntireFamily()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun resetMessages() {
        val familyId = FamilyManager.getFamilyId(this) ?: return

        FirebaseDatabase.getInstance()
            .getReference("families")
            .child(familyId)
            .child("messages")
            .removeValue()
            .addOnSuccessListener {
                android.widget.Toast.makeText(this, "Semua pesan telah dihapus! 🧹", android.widget.Toast.LENGTH_SHORT).show()

                // Animate success
                try {
                    LottieUtils.setSpeed(binding.lottieAstro, 3.0f)
                    binding.root.postDelayed({
                        LottieUtils.setSpeed(binding.lottieAstro, 0.8f)
                    }, 2000)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error animating reset success: ${e.message}")
                }
            }
            .addOnFailureListener { exception ->
                android.widget.Toast.makeText(this, "Gagal reset pesan: ${exception.message}", android.widget.Toast.LENGTH_LONG).show()
            }
    }

    private fun resetEntireFamily() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("⚠️ Peringatan")
            .setMessage("Ini akan menghapus SEMUA data keluarga dan Anda harus membuat keluarga baru.\n\nAksi ini TIDAK DAPAT dibatalkan!")
            .setPositiveButton("Ya, Reset Total") { _, _ ->
                FamilyManager.clearFamilyConfig(this)
                redirectToSetup("Reset total dilakukan")
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.action_refresh -> {
                monitorFamilyStatus()
                android.widget.Toast.makeText(this, "Status direfresh", android.widget.Toast.LENGTH_SHORT).show()

                // Animate refresh
                try {
                    LottieUtils.setSpeed(binding.lottieStatusIndicator, 3.0f)
                    binding.root.postDelayed({
                        LottieUtils.setSpeed(binding.lottieStatusIndicator, 1.5f)
                    }, 1500)
                } catch (e: Exception) {
                    Log.e("MainActivity", "Error animating refresh: ${e.message}")
                }
                true
            }
            R.id.action_family_info -> {
                showFamilyInfo()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showFamilyInfo() {
        val familyId = FamilyManager.getFamilyId(this)
        val familyName = FamilyManager.getFamilyName(this)
        val isParent = FamilyManager.isParent(this)

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Informasi Keluarga")
            .setMessage(buildString {
                append("👨‍👩‍👧‍👦 Nama: $familyName\n")
                append("🔑 Kode: $familyId\n")
                append("👤 Role: ${if (isParent) "Orang Tua" else "Anak"}\n\n")
                append("💡 Tip: Bagikan kode keluarga kepada anggota keluarga lain untuk bergabung.")
            })
            .setPositiveButton("OK", null)
            .setNeutralButton("Salin Kode") { _, _ ->
                val clipboard = getSystemService(android.content.Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                val clip = android.content.ClipData.newPlainText("Family Code", familyId)
                clipboard.setPrimaryClip(clip)
                android.widget.Toast.makeText(this, "Kode keluarga disalin!", android.widget.Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    override fun onResume() {
        super.onResume()
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            database.child("lastActivity").setValue(System.currentTimeMillis())

            val connectionKey = if (FamilyManager.isParent(this)) "parentConnected" else "childConnected"
            database.child(connectionKey).setValue(true)

            // Resume animations safely
            LottieUtils.resumeAnimation(binding.lottieAstro)
            LottieUtils.resumeAnimation(binding.lottieStatusIndicator)
        }
    }

    override fun onPause() {
        super.onPause()
        // Optional: pause animations to save battery
        // LottieUtils.pauseAnimation(binding.lottieAstro)
        // LottieUtils.pauseAnimation(binding.lottieStatusIndicator)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener dan cancel animations
        familyStatusListener?.let {
            database.removeEventListener(it)
        }

        LottieUtils.cancelAnimation(binding.lottieAstro)
        LottieUtils.cancelAnimation(binding.lottieStatusIndicator)
    }

    override fun onBackPressed() {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluar Aplikasi?")
            .setMessage("Apakah Anda ingin menutup Papan Ajaib?")
            .setPositiveButton("Ya") { _, _ ->
                super.onBackPressed()
            }
            .setNegativeButton("Tidak", null)
            .show()
    }
}