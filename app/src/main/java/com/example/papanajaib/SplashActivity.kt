package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.appcompat.app.AppCompatActivity
import com.example.papanajaib.databinding.ActivitySplashBinding
import com.example.papanajaib.utils.FamilyManager
import com.example.papanajaib.utils.LottieUtils

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDelay = 3000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivitySplashBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for splash screen
        supportActionBar?.hide()

        setupLottieAnimations()
        startSplashSequence()
    }

    private fun setupLottieAnimations() {
        // Setup main splash animation
        LottieUtils.Presets.welcomeAnimation(binding.lottieSplashAstro)

        // Setup loading indicator
        LottieUtils.Presets.statusIndicator(binding.lottieLoadingIndicator)

        // Start with a welcome animation
        LottieUtils.animateSuccess(binding.lottieSplashAstro, 1000L)
    }

    private fun startSplashSequence() {
        val loadingMessages = arrayOf(
            "Memuat...",
            "Memeriksa konfigurasi...",
            "Menghubungkan...",
            "Siap!"
        )

        var currentMessageIndex = 0
        val messageHandler = Handler(Looper.getMainLooper())

        val updateMessage = object : Runnable {
            override fun run() {
                if (currentMessageIndex < loadingMessages.size) {
                    binding.tvLoadingText.text = loadingMessages[currentMessageIndex]

                    // Animate loading indicator speed based on progress
                    val speed = 1.5f + (currentMessageIndex * 0.5f)
                    binding.lottieLoadingIndicator.speed = speed

                    currentMessageIndex++
                    messageHandler.postDelayed(this, 750L) // Update every 750ms
                } else {
                    // Final animation before navigating
                    LottieUtils.animateSuccess(binding.lottieSplashAstro)
                    LottieUtils.animateSuccess(binding.lottieLoadingIndicator)

                    // Navigate to appropriate screen
                    Handler(Looper.getMainLooper()).postDelayed({
                        navigateToNextScreen()
                    }, 1000L)
                }
            }
        }

        // Start the message update sequence
Handler(Looper.getMainLooper()).postDelayed(updateMessage, 500L)    }

    private fun navigateToNextScreen() {
        val familyId = FamilyManager.getFamilyId(this)

        val intent = if (familyId.isNullOrEmpty()) {
            // No family configuration, go to setup
            Intent(this, SetupActivity::class.java)
        } else {
            // Has family configuration, go to main
            Intent(this, MainActivity::class.java)
        }

        startActivity(intent)
        finish()

        // Add smooth transition
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Cancel animations to prevent memory leaks
        LottieUtils.cancelAnimation(
            binding.lottieSplashAstro,
            binding.lottieLoadingIndicator
        )
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Disable back button on splash screen
        // User should wait for splash to complete
    }
}