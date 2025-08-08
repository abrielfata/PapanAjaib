package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.airbnb.lottie.LottieAnimationView
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

        // Cek apakah sudah ada konfigurasi yang tersimpan
        checkExistingConfiguration()

        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupLottieAnimations()
        setupClickListeners()
    }

    private fun setupLottieAnimations() {
        // Setup main welcome animation
        binding.lottieWelcomeAstro.apply {
            setAnimation("astro.json")
            repeatCount = -1 // Infinite loop
            speed = 0.6f
            playAnimation()
        }

        // Setup parent card animation
        binding.lottieParent.apply {
            setAnimation("astro.json")
            repeatCount = -1
            speed = 1.2f
            playAnimation()
        }

        // Setup child card animation
        binding.lottieChild.apply {
            setAnimation("astro.json")
            repeatCount = -1
            speed = 1.0f
            playAnimation()
        }

        // Setup footer animation
        binding.lottieFooter.apply {
            setAnimation("astro.json")
            repeatCount = -1
            speed = 2.0f
            playAnimation()
        }
    }

    private fun animateSuccess() {
        // Speed up all animations for success feedback
        binding.lottieWelcomeAstro.speed = 2.0f
        binding.lottieParent.speed = 2.5f
        binding.lottieChild.speed = 2.5f
        binding.lottieFooter.speed = 3.0f

        // Reset speeds after animation
        binding.root.postDelayed({
            binding.lottieWelcomeAstro.speed = 0.6f
            binding.lottieParent.speed = 1.2f
            binding.lottieChild.speed = 1.0f
            binding.lottieFooter.speed = 2.0f
        }, 2000)
    }

    private fun animateLoading(isLoading: Boolean) {
        if (isLoading) {
            // Slow down animations during loading
            binding.lottieWelcomeAstro.speed = 0.3f
            binding.lottieParent.speed = 0.5f
            binding.lottieChild.speed = 0.5f
            binding.lottieFooter.speed = 1.0f
        } else {
            // Resume normal speeds
            binding.lottieWelcomeAstro.speed = 0.6f
            binding.lottieParent.speed = 1.2f
            binding.lottieChild.speed = 1.0f
            binding.lottieFooter.speed = 2.0f
        }
    }

    private fun checkExistingConfiguration() {
        val familyId = FamilyManager.getFamilyId(this)

        if (!familyId.isNullOrEmpty()) {
            // Ada konfigurasi tersimpan, verifikasi apakah family masih ada di Firebase
            showLoadingState("Memverifikasi konfigurasi keluarga...")

            database.getReference("families").child(familyId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        hideLoadingState()

                        if (snapshot.exists()) {
                            // Family masih ada, langsung ke MainActivity
                            val familyName = FamilyManager.getFamilyName(this@SetupActivity)
                            val role = if (FamilyManager.isParent(this@SetupActivity)) "Orang Tua" else "Anak"

                            Toast.makeText(this@SetupActivity,
                                "Selamat datang kembali! ($role - $familyName)",
                                Toast.LENGTH_SHORT).show()

                            animateSuccess()

                            // Delay to show success animation
                            binding.root.postDelayed({
                                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                                finish()
                            }, 1500)
                        } else {
                            // Family sudah tidak ada, bersihkan konfigurasi dan tampilkan setup
                            FamilyManager.clearFamilyConfig(this@SetupActivity)
                            showSetupOptions()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        hideLoadingState()
                        // Jika ada error koneksi, tetap tampilkan setup tapi beri peringatan
                        showConnectionWarning()
                        showSetupOptions()
                    }
                })
            return
        }

        // Tidak ada konfigurasi tersimpan, tampilkan setup normal
        showSetupOptions()
    }

    private fun showLoadingState(message: String) {
        animateLoading(true)
        // Implement loading UI jika ada
        // binding.loadingView.visibility = View.VISIBLE
        // binding.setupOptions.visibility = View.GONE
    }

    private fun hideLoadingState() {
        animateLoading(false)
        // binding.loadingView.visibility = View.GONE
        // binding.setupOptions.visibility = View.VISIBLE
    }

    private fun showConnectionWarning() {
        Toast.makeText(this,
            "Tidak dapat memverifikasi konfigurasi. Periksa koneksi internet.",
            Toast.LENGTH_LONG).show()
    }

    private fun showSetupOptions() {
        // Setup UI sudah ada, tidak perlu perubahan
        // Resume normal animation speeds
        animateLoading(false)
    }

    private fun setupClickListeners() {
        // Orang tua - buat keluarga baru
        binding.btnCreateFamily.setOnClickListener {
            // Animate button press
            binding.lottieParent.apply {
                speed = 3.0f
                postDelayed({
                    speed = 1.2f
                }, 1000)
            }
            createNewFamily()
        }

        // Anak - gabung keluarga
        binding.btnJoinFamily.setOnClickListener {
            // Animate button press
            binding.lottieChild.apply {
                speed = 3.0f
                postDelayed({
                    speed = 1.0f
                }, 1000)
            }
            joinExistingFamily()
        }

        // Tambahkan tombol "Gunakan Konfigurasi Lama" jika ada
        setupRestoreButton()
    }

    private fun setupRestoreButton() {
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            // Tampilkan tombol restore jika ada konfigurasi lama
            binding.btnRestoreFamily.visibility = View.VISIBLE
            binding.btnRestoreFamily.text = "Gunakan Konfigurasi Lama ($familyId)"

            binding.btnRestoreFamily.setOnClickListener {
                // Animate restore
                animateSuccess()

                // Delay to show animation
                binding.root.postDelayed({
                    // Langsung gunakan konfigurasi lama tanpa verifikasi
                    startActivity(Intent(this, MainActivity::class.java))
                    finish()
                }, 1500)
            }
        }
    }

    private fun createNewFamily() {
        val familyName = binding.etFamilyName.text.toString().trim()

        if (familyName.isEmpty()) {
            binding.etFamilyName.error = "Nama keluarga tidak boleh kosong"
            return
        }

        // Periksa apakah sudah ada family sebelumnya
        val existingFamilyId = FamilyManager.getFamilyId(this)
        if (!existingFamilyId.isNullOrEmpty()) {
            showReplaceConfirmation(existingFamilyId) {
                createFamily(familyName)
            }
            return
        }

        createFamily(familyName)
    }

    private fun showReplaceConfirmation(existingFamilyId: String, onConfirm: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Ganti Keluarga")
            .setMessage("Anda sudah terdaftar di keluarga dengan kode $existingFamilyId.\n\nApakah Anda ingin membuat keluarga baru dan mengganti konfigurasi lama?")
            .setPositiveButton("Ya, Ganti") { _, _ ->
                FamilyManager.clearFamilyConfig(this)
                onConfirm()
            }
            .setNegativeButton("Batal", null)
            .show()
    }

    private fun createFamily(familyName: String) {
        val familyId = FamilyManager.generateFamilyId()

        // Start loading animation
        animateLoading(true)

        // Simpan data keluarga ke Firebase
        val familyData = mapOf(
            "name" to familyName,
            "createdAt" to System.currentTimeMillis(),
            "parentConnected" to true,
            "childConnected" to false,
            "status" to "active", // Tambahkan status
            "lastActivity" to System.currentTimeMillis()
        )

        binding.btnCreateFamily.isEnabled = false
        binding.btnCreateFamily.text = "Membuat keluarga..."

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

                binding.btnCreateFamily.isEnabled = true
                binding.btnCreateFamily.text = "Buat Keluarga Baru"

                // Stop loading and show success animation
                animateLoading(false)
                animateSuccess()

                // Tampilkan kode keluarga
                showFamilyCode(familyId, familyName)
            }
            .addOnFailureListener { exception ->
                binding.btnCreateFamily.isEnabled = true
                binding.btnCreateFamily.text = "Buat Keluarga Baru"

                // Stop loading animation
                animateLoading(false)

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

        // Periksa apakah sudah ada family sebelumnya
        val existingFamilyId = FamilyManager.getFamilyId(this)
        if (!existingFamilyId.isNullOrEmpty() && existingFamilyId != familyId) {
            showReplaceConfirmation(existingFamilyId) {
                joinFamily(familyId)
            }
            return
        }

        joinFamily(familyId)
    }

    private fun joinFamily(familyId: String) {
        // Start loading animation
        animateLoading(true)

        binding.btnJoinFamily.isEnabled = false
        binding.btnJoinFamily.text = "Bergabung..."

        // Cek apakah keluarga ada
        database.getReference("families").child(familyId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    binding.btnJoinFamily.isEnabled = true
                    binding.btnJoinFamily.text = "Gabung ke Keluarga"

                    if (snapshot.exists()) {
                        val familyName = snapshot.child("name").getValue(String::class.java) ?: "Keluarga"
                        val status = snapshot.child("status").getValue(String::class.java) ?: "active"

                        if (status != "active") {
                            binding.etFamilyId.error = "Keluarga ini tidak aktif"
                            animateLoading(false)
                            return
                        }

                        // Update status child connected
                        val updates = mapOf<String, Any>(
                            "childConnected" to true,
                            "lastActivity" to System.currentTimeMillis()
                        )
                        snapshot.ref.updateChildren(updates)

                        // Simpan konfigurasi lokal
                        FamilyManager.saveFamilyConfig(
                            context = this@SetupActivity,
                            familyId = familyId,
                            isParent = false,
                            familyName = familyName
                        )

                        // Stop loading and show success animation
                        animateLoading(false)
                        animateSuccess()

                        Toast.makeText(this@SetupActivity,
                            "Berhasil bergabung dengan $familyName!",
                            Toast.LENGTH_SHORT).show()

                        // Delay to show success animation
                        binding.root.postDelayed({
                            // Pindah ke main activity
                            startMainActivity()
                        }, 1500)

                    } else {
                        binding.etFamilyId.error = "Kode keluarga tidak ditemukan"
                        animateLoading(false)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    binding.btnJoinFamily.isEnabled = true
                    binding.btnJoinFamily.text = "Gabung ke Keluarga"

                    animateLoading(false)

                    Toast.makeText(this@SetupActivity,
                        "Gagal mengecek kode keluarga: ${error.message}",
                        Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun showFamilyCode(familyId: String, familyName: String) {
        // Show dialog dengan kode keluarga
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Keluarga '$familyName' Berhasil Dibuat!")
            .setMessage("Kode Keluarga Anda:\n\n$familyId\n\nBerikan kode ini kepada anak untuk bergabung.\n\nKode ini akan tersimpan otomatis.")
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

    override fun onResume() {
        super.onResume()
        // Resume all animations
        if (!binding.lottieWelcomeAstro.isAnimating) {
            binding.lottieWelcomeAstro.resumeAnimation()
        }
        if (!binding.lottieParent.isAnimating) {
            binding.lottieParent.resumeAnimation()
        }
        if (!binding.lottieChild.isAnimating) {
            binding.lottieChild.resumeAnimation()
        }
        if (!binding.lottieFooter.isAnimating) {
            binding.lottieFooter.resumeAnimation()
        }
    }

    override fun onPause() {
        super.onPause()
        // Optionally pause animations to save battery
        // binding.lottieWelcomeAstro.pauseAnimation()
        // binding.lottieParent.pauseAnimation()
        // binding.lottieChild.pauseAnimation()
        // binding.lottieFooter.pauseAnimation()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel all animations
        binding.lottieWelcomeAstro.cancelAnimation()
        binding.lottieParent.cancelAnimation()
        binding.lottieChild.cancelAnimation()
        binding.lottieFooter.cancelAnimation()
    }

    override fun onBackPressed() {
        // Jika user menekan back, jangan keluar ke sistem
        // Tapi beri pilihan untuk tetap gunakan konfigurasi lama atau keluar
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Keluar Aplikasi?")
                .setMessage("Anda memiliki konfigurasi keluarga ($familyId).\n\nApakah Anda ingin:\n• Gunakan konfigurasi lama\n• Keluar dari aplikasi")
                .setPositiveButton("Gunakan Konfigurasi Lama") { _, _ ->
                    animateSuccess()
                    binding.root.postDelayed({
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }, 1500)
                }
                .setNegativeButton("Keluar") { _, _ ->
                    super.onBackPressed()
                }
                .show()
        } else {
            super.onBackPressed()
        }
    }
}