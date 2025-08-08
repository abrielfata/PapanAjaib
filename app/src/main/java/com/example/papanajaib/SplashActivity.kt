package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.view.animation.AnimationUtils
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.papanajaib.databinding.ActivitySplashBinding
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Load dan jalankan animasi
        val slideInAnim = AnimationUtils.loadAnimation(this, R.anim.slide_in)
        binding.lottieSplashAstro.startAnimation(slideInAnim)
        binding.tvAppName.startAnimation(slideInAnim)

        // Setelah 3 detik, pindah ke SetupActivity (bukan MainActivity)
        lifecycleScope.launch {
            delay(3000) // Kurangi delay jadi 3 detik

            // Pindah ke SetupActivity sebagai halaman selanjutnya
            val intent = Intent(this@SplashActivity, SetupActivity::class.java)
            startActivity(intent)
            finish() // Tutup SplashActivity agar tidak bisa kembali
        }
    }
}