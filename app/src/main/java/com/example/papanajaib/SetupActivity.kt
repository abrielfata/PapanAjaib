// SOLUSI 1: Update SetupActivity.kt untuk menggunakan raw resource
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

        // Inisialisasi binding TERLEBIH DAHULU
        binding = ActivitySetupBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Setup UI components
        setupLottieAnimations()
        setupClickListeners()

        // KEMUDIAN baru cek konfigurasi existing
        checkExistingConfiguration()
    }

    private fun setupLottieAnimations() {
        // PERBAIKAN: Gunakan raw resource daripada asset
        // Setup main welcome animation
        binding.lottieWelcomeAstro.apply {
            setAnimation(R.raw.astro) // Gunakan resource ID
            repeatCount = -1 // Infinite loop
            speed = 0.6f
            playAnimation()
        }

        // Setup parent card animation
        binding.lottieParent.apply {
            setAnimation(R.raw.astro) // Gunakan resource ID
            repeatCount = -1
            speed = 1.2f
            playAnimation()
        }

        // Setup child card animation
        binding.lottieChild.apply {
            setAnimation(R.raw.astro) // Gunakan resource ID
            repeatCount = -1
            speed = 1.0f
            playAnimation()
        }

        // Setup footer animation
        binding.lottieFooter.apply {
            setAnimation(R.raw.astro) // Gunakan resource ID
            repeatCount = -1
            speed = 2.0f
            playAnimation()
        }
    }

    // ALTERNATIF: Jika ingin tetap menggunakan assets, gunakan method ini
    private fun setupLottieAnimationsFromAssets() {
        // Pastikan file astro.json ada di app/src/main/assets/
        try {
            binding.lottieWelcomeAstro.apply {
                setAnimation("astro.json")
                repeatCount = -1
                speed = 0.6f
                playAnimation()
            }

            binding.lottieParent.apply {
                setAnimation("astro.json")
                repeatCount = -1
                speed = 1.2f
                playAnimation()
            }

            binding.lottieChild.apply {
                setAnimation("astro.json")
                repeatCount = -1
                speed = 1.0f
                playAnimation()
            }

            binding.lottieFooter.apply {
                setAnimation("astro.json")
                repeatCount = -1
                speed = 2.0f
                playAnimation()
            }
        } catch (e: Exception) {
            // Fallback jika file tidak ditemukan
            Toast.makeText(this, "Animation file not found. Using fallback.", Toast.LENGTH_SHORT).show()

            // Gunakan animasi built-in atau disable animasi
            binding.lottieWelcomeAstro.visibility = View.GONE
            binding.lottieParent.visibility = View.GONE
            binding.lottieChild.visibility = View.GONE
            binding.lottieFooter.visibility = View.GONE
        }
    }

    private fun animateSuccess() {
        if (!::binding.isInitialized) {
            return
        }

        // Speed up all animations for success feedback
        binding.lottieWelcomeAstro.speed = 2.0f
        binding.lottieParent.speed = 2.5f
        binding.lottieChild.speed = 2.5f
        binding.lottieFooter.speed = 3.0f

        // Reset speeds after animation
        binding.root.postDelayed({
            if (::binding.isInitialized) {
                binding.lottieWelcomeAstro.speed = 0.6f
                binding.lottieParent.speed = 1.2f
                binding.lottieChild.speed = 1.0f
                binding.lottieFooter.speed = 2.0f
            }
        }, 2000)
    }

    private fun animateLoading(isLoading: Boolean) {
        if (!::binding.isInitialized) {
            return
        }

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
            showLoadingState("Memverifikasi konfigurasi keluarga...")

            database.getReference("families").child(familyId)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        hideLoadingState()

                        if (snapshot.exists()) {
                            val familyName = FamilyManager.getFamilyName(this@SetupActivity)
                            val role = if (FamilyManager.isParent(this@SetupActivity)) "Orang Tua" else "Anak"

                            Toast.makeText(this@SetupActivity,
                                "Selamat datang kembali! ($role - $familyName)",
                                Toast.LENGTH_SHORT).show()

                            animateSuccess()

                            binding.root.postDelayed({
                                startActivity(Intent(this@SetupActivity, MainActivity::class.java))
                                finish()
                            }, 1500)
                        } else {
                            FamilyManager.clearFamilyConfig(this@SetupActivity)
                            showSetupOptions()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        hideLoadingState()
                        showConnectionWarning()
                        showSetupOptions()
                    }
                })
            return
        }

        showSetupOptions()
    }

    private fun showLoadingState(message: String) {
        if (::binding.isInitialized) {
            animateLoading(true)
        }
    }

    private fun hideLoadingState() {
        if (::binding.isInitialized) {
            animateLoading(false)
        }
    }

    private fun showConnectionWarning() {
        Toast.makeText(this,
            "Tidak dapat memverifikasi konfigurasi. Periksa koneksi internet.",
            Toast.LENGTH_LONG).show()
    }

    private fun showSetupOptions() {
        animateLoading(false)
    }

    private fun setupClickListeners() {
        binding.btnCreateFamily.setOnClickListener {
            binding.lottieParent.apply {
                speed = 3.0f
                postDelayed({ speed = 1.2f }, 1000)
            }
            createNewFamily()
        }

        binding.btnJoinFamily.setOnClickListener {
            binding.lottieChild.apply {
                speed = 3.0f
                postDelayed({ speed = 1.0f }, 1000)
            }
            joinExistingFamily()
        }

        setupRestoreButton()
    }

    private fun setupRestoreButton() {
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            binding.btnRestoreFamily.visibility = View.VISIBLE
            binding.btnRestoreFamily.text = "Gunakan Konfigurasi Lama ($familyId)"

            binding.btnRestoreFamily.setOnClickListener {
                animateSuccess()
                binding.root.postDelayed({
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
        animateLoading(true)

        val familyData = mapOf(
            "name" to familyName,
            "createdAt" to System.currentTimeMillis(),
            "parentConnected" to true,
            "childConnected" to false,
            "status" to "active",
            "lastActivity" to System.currentTimeMillis()
        )

        binding.btnCreateFamily.isEnabled = false
        binding.btnCreateFamily.text = "Membuat keluarga..."

        database.getReference("families").child(familyId)
            .setValue(familyData)
            .addOnSuccessListener {
                FamilyManager.saveFamilyConfig(
                    context = this,
                    familyId = familyId,
                    isParent = true,
                    familyName = familyName
                )

                binding.btnCreateFamily.isEnabled = true
                binding.btnCreateFamily.text = "Buat Keluarga Baru"

                animateLoading(false)
                animateSuccess()

                showFamilyCode(familyId, familyName)
            }
            .addOnFailureListener { exception ->
                binding.btnCreateFamily.isEnabled = true
                binding.btnCreateFamily.text = "Buat Keluarga Baru"
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
        animateLoading(true)

        binding.btnJoinFamily.isEnabled = false
        binding.btnJoinFamily.text = "Bergabung..."

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

                        val updates = mapOf<String, Any>(
                            "childConnected" to true,
                            "lastActivity" to System.currentTimeMillis()
                        )
                        snapshot.ref.updateChildren(updates)

                        FamilyManager.saveFamilyConfig(
                            context = this@SetupActivity,
                            familyId = familyId,
                            isParent = false,
                            familyName = familyName
                        )

                        animateLoading(false)
                        animateSuccess()

                        Toast.makeText(this@SetupActivity,
                            "Berhasil bergabung dengan $familyName!",
                            Toast.LENGTH_SHORT).show()

                        binding.root.postDelayed({
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
        if (::binding.isInitialized) {
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
    }

    override fun onPause() {
        super.onPause()
        // Optional: pause untuk save battery
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::binding.isInitialized) {
            binding.lottieWelcomeAstro.cancelAnimation()
            binding.lottieParent.cancelAnimation()
            binding.lottieChild.cancelAnimation()
            binding.lottieFooter.cancelAnimation()
        }
    }

    override fun onBackPressed() {
        val familyId = FamilyManager.getFamilyId(this)
        if (!familyId.isNullOrEmpty()) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Keluar Aplikasi?")
                .setMessage("Anda memiliki konfigurasi keluarga ($familyId).\n\nApakah Anda ingin:\n• Gunakan konfigurasi lama\n• Keluar dari aplikasi")
                .setPositiveButton("Gunakan Konfigurasi Lama") { _, _ ->
                    if (::binding.isInitialized) {
                        animateSuccess()
                        binding.root.postDelayed({
                            startActivity(Intent(this, MainActivity::class.java))
                            finish()
                        }, 1500)
                    } else {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
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