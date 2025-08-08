package com.example.papanajaib

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.example.papanajaib.databinding.ActivitySplashBinding
import com.example.papanajaib.utils.FamilyManager

class SplashActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySplashBinding
    private val splashDelay = 3000L // 3 seconds
    private val TAG = "SplashActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        try {
            Log.d(TAG, "SplashActivity onCreate started")

            binding = ActivitySplashBinding.inflate(layoutInflater)
            setContentView(binding.root)

            // Hide action bar for splash screen
            supportActionBar?.hide()

            setupBasicLottieAnimations()
            startSimpleSplashSequence()

            Log.d(TAG, "SplashActivity onCreate completed")

        } catch (e: Exception) {
            Log.e(TAG, "Error in onCreate", e)
            // Fallback: langsung ke next screen jika ada error
            navigateToNextScreenSafely()
        }
    }

    private fun setupBasicLottieAnimations() {
        try {
            Log.d(TAG, "Setting up Lottie animations")

            // Setup main splash animation dengan error handling
            binding.lottieSplashAstro?.apply {
                setAnimation("astro.json")
                repeatCount = -1 // Infinite loop
                speed = 1.0f
                playAnimation()
                Log.d(TAG, "Main Lottie animation started")
            }

            // Setup loading indicator dengan error handling
            binding.lottieLoadingIndicator?.apply {
                setAnimation("astro.json")
                repeatCount = -1
                speed = 2.0f
                alpha = 0.8f
                playAnimation()
                Log.d(TAG, "Loading indicator animation started")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Lottie animations", e)
            // Continue without animations if they fail
        }
    }

    private fun startSimpleSplashSequence() {
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
                try {
                    if (currentMessageIndex < loadingMessages.size) {
                        // Safe update of loading text
                        binding.tvLoadingText?.text = loadingMessages[currentMessageIndex]

                        // Update animation speed safely
                        try {
                            val speed = 1.0f + (currentMessageIndex * 0.3f)
                            binding.lottieLoadingIndicator?.speed = speed
                        } catch (e: Exception) {
                            Log.w(TAG, "Could not update animation speed", e)
                        }

                        currentMessageIndex++
                        messageHandler.postDelayed(this, 750L) // Update every 750ms
                    } else {
                        // Final phase - navigate to next screen
                        Log.d(TAG, "Splash sequence completed, navigating...")
                        navigateToNextScreenSafely()
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in splash sequence", e)
                    // Fallback navigation if sequence fails
                    navigateToNextScreenSafely()
                }
            }
        }

        // Start the message update sequence with delay
        messageHandler.postDelayed(updateMessage, 500L)
    }

    private fun navigateToNextScreenSafely() {
        try {
            Log.d(TAG, "Navigating to next screen")

            val familyId = FamilyManager.getFamilyId(this)
            Log.d(TAG, "Family ID: $familyId")

            val intent = if (familyId.isNullOrEmpty()) {
                Log.d(TAG, "No family ID found, going to SetupActivity")
                Intent(this, SetupActivity::class.java)
            } else {
                Log.d(TAG, "Family ID found, going to MainActivity")
                Intent(this, MainActivity::class.java)
            }

            startActivity(intent)
            finish()

            // Add smooth transition
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)

        } catch (e: Exception) {
            Log.e(TAG, "Error navigating to next screen", e)

            // Ultimate fallback - go to SetupActivity
            try {
                startActivity(Intent(this, SetupActivity::class.java))
                finish()
            } catch (e2: Exception) {
                Log.e(TAG, "Critical error: cannot navigate", e2)
                // If even this fails, just finish the activity
                finish()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            Log.d(TAG, "SplashActivity onDestroy")

            // Safe cleanup of animations
            binding.lottieSplashAstro?.cancelAnimation()
            binding.lottieLoadingIndicator?.cancelAnimation()

        } catch (e: Exception) {
            Log.e(TAG, "Error in onDestroy", e)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        // Disable back button on splash screen
        Log.d(TAG, "Back button pressed - ignoring on splash screen")
        // No super.onBackPressed() call to disable back button
    }
}