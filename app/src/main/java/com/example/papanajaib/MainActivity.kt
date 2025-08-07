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
        setupClickListeners()
        monitorFamilyStatus()
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

        // Show reset button only for parents or in debug mode
        binding.btnResetFamily.visibility = if (isParent) View.VISIBLE else View.GONE
    }

    private fun monitorFamilyStatus() {
        val familyId = FamilyManager.getFamilyId(this) ?: return

        familyStatusListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (!snapshot.exists()) {
                    // Family tidak ada lagi di Firebase
                    handleFamilyDeleted()
                    return
                }

                updateFamilyStatus(snapshot)
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("MainActivity", "Family status monitoring failed", error.toException())
                binding.tvConnectionStatus.text = "⚠️ Gagal memantau status keluarga"
            }
        }

        database.addValueEventListener(familyStatusListener!!)

        // Update last activity
        database.child("lastActivity").setValue(System.currentTimeMillis())
    }

    private fun handleFamilyDeleted() {
        Log.w("MainActivity", "Family was deleted from Firebase")

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
            startActivity(Intent(this, ParentActivity::class.java))
        }

        binding.btnChildMode.setOnClickListener {
            startActivity(Intent(this, ChildActivity::class.java))
        }

        // Reset button untuk orang tua
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
                // Refresh family status
                monitorFamilyStatus()
                android.widget.Toast.makeText(this, "Status direfresh", android.widget.Toast.LENGTH_SHORT).show()
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
        // Update activity timestamp when resuming
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            database.child("lastActivity").setValue(System.currentTimeMillis())

            // Update connection status
            val connectionKey = if (FamilyManager.isParent(this)) "parentConnected" else "childConnected"
            database.child(connectionKey).setValue(true)
        }
    }

    override fun onPause() {
        super.onPause()
        // Optionally update connection status when pausing
        // (Uncomment jika ingin strict tracking)
        /*
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            val connectionKey = if (FamilyManager.isParent(this)) "parentConnected" else "childConnected"
            database.child(connectionKey).setValue(false)
        }
        */
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener untuk prevent memory leaks
        familyStatusListener?.let {
            database.removeEventListener(it)
        }
    }

    override fun onBackPressed() {
        // Override back button behavior
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