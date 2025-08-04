package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.papanajaib.databinding.ActivitySetupBinding
import com.example.papanajaib.utils.FamilyManager
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class SetupActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySetupBinding
    private val database = FirebaseDatabase.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Orang tua - buat keluarga baru
        binding.btnCreateFamily.setOnClickListener {
            createNewFamily()
        }

        // Anak - gabung keluarga
        binding.btnJoinFamily.setOnClickListener {
            joinExistingFamily()
        }
    }

    private fun createNewFamily() {
        val familyName = binding.etFamilyName.text.toString().trim()

        if (familyName.isEmpty()) {
            binding.etFamilyName.error = "Nama keluarga tidak boleh kosong"
            return
        }

        val familyId = FamilyManager.generateFamilyId()

        // Simpan data keluarga ke Firebase
        val familyData = mapOf(
            "name" to familyName,
            "createdAt" to System.currentTimeMillis(),
            "parentConnected" to true,
            "childConnected" to false
        )

        database.getReference("families").child(familyId)
            .setValue(familyData)
            .addOnSuccessListener {
                // Simpan konfigurasi lokal
                FamilyManager.saveFamilyConfig(
                    context = this,
                    familyId = familyId,
                    isParent = true,
                    familyName = familyName
                )

                // Tampilkan kode keluarga
                showFamilyCode(familyId)
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Gagal membuat keluarga: ${exception.message}",
                    Toast.LENGTH_SHORT).show()
            }
    }

    private fun joinExistingFamily() {
        val familyId = binding.etFamilyId.text.toString().trim().uppercase()

        if (!FamilyManager.isValidFamilyId(familyId)) {
            binding.etFamilyId.error = "Kode keluarga harus 6 karakter"
            return
        }

        // Cek apakah keluarga ada
        database.getReference("families").child(familyId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val familyName = snapshot.child("name").getValue(String::class.java) ?: "Keluarga"

                        // Update status child connected
                        snapshot.ref.child("childConnected").setValue(true)

                        // Simpan konfigurasi lokal
                        FamilyManager.saveFamilyConfig(
                            context = this@SetupActivity,
                            familyId = familyId,
                            isParent = false,
                            familyName = familyName
                        )

                        Toast.makeText(this@SetupActivity,
                            "Berhasil bergabung dengan $familyName!",
                            Toast.LENGTH_SHORT).show()

                        // Pindah ke main activity
                        startMainActivity()

                    } else {
                        binding.etFamilyId.error = "Kode keluarga tidak ditemukan"
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@SetupActivity,
                        "Gagal mengecek kode keluarga: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showFamilyCode(familyId: String) {
        // Show dialog dengan kode keluarga
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluarga Berhasil Dibuat!")
            .setMessage("Kode Keluarga Anda:\n\n$familyId\n\nBerikan kode ini kepada anak untuk bergabung.")
            .setPositiveButton("Lanjutkan") { _, _ ->
                startMainActivity()
            }
            .setCancelable(false)
            .show()
    }

    private fun startMainActivity() {
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}
