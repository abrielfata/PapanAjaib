package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.papanajaib.databinding.ActivityMainBinding
import com.example.papanajaib.utils.FamilyManager

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Cek apakah sudah dikonfigurasi
        if (!FamilyManager.isFamilyConfigured(this)) {
            startActivity(Intent(this, SetupActivity::class.java))
            finish()
            return
        }

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupUI()
        setupClickListeners()
    }

    private fun setupUI() {
        val familyName = FamilyManager.getFamilyName(this)
        val familyId = FamilyManager.getFamilyId(this)

        binding.tvFamilyInfo.text = "$familyName\nKode: $familyId"

        // Tampilkan tombol sesuai role
        if (FamilyManager.isParent(this)) {
            binding.btnParentMode.visibility = View.VISIBLE
            binding.btnChildMode.visibility = View.GONE
            binding.btnParentMode.text = "Mode Orang Tua"
        } else {
            binding.btnParentMode.visibility = View.GONE
            binding.btnChildMode.visibility = View.VISIBLE
            binding.btnChildMode.text = "Lihat Tugas"
        }

        // Show reset button for development/testing
        binding.btnResetFamily.visibility = View.VISIBLE
    }

    private fun setupClickListeners() {
        binding.btnParentMode.setOnClickListener {
            startActivity(Intent(this, ParentActivity::class.java))
        }

        binding.btnChildMode.setOnClickListener {
            startActivity(Intent(this, ChildActivity::class.java))
        }

        // Reset button for testing
        binding.btnResetFamily.setOnClickListener {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle(getString(R.string.reset_family_title))
                .setMessage(getString(R.string.reset_family_message))
                .setPositiveButton(getString(R.string.yes)) { _, _ ->
                    FamilyManager.clearFamilyConfig(this)
                    startActivity(Intent(this, SetupActivity::class.java))
                    finish()
                }
                .setNegativeButton(getString(R.string.no), null)
                .show()
        }
    }
}