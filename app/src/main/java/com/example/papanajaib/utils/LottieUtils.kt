package com.example.papanajaib.utils

import android.util.Log
import com.airbnb.lottie.LottieAnimationView

/**
 * Utility class untuk mengatur animasi Lottie dengan error handling yang lebih baik
 */
object LottieUtils {

    private const val TAG = "LottieUtils"

    /**
     * Setup animasi Lottie dengan error handling
     */
    fun setupAnimation(
        lottieView: LottieAnimationView?,
        animationFileName: String = "astro.json",
        speed: Float = 1.0f,
        loop: Boolean = true,
        autoPlay: Boolean = true
    ) {
        try {
            lottieView?.apply {
                // Pastikan view sudah ready
                if (isAttachedToWindow) {
                    setAnimation(animationFileName)
                    this.speed = speed
                    repeatCount = if (loop) -1 else 0

                    if (autoPlay) {
                        playAnimation()
                    }

                    Log.d(TAG, "Animation setup successful for: $animationFileName")
                } else {
                    // Delay setup sampai view attached
                    post {
                        setupAnimation(this, animationFileName, speed, loop, autoPlay)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up Lottie animation: ${e.message}")
            // Jangan crash, hanya log error
        }
    }

    /**
     * Play animation dengan error handling
     */
    fun playAnimation(lottieView: LottieAnimationView?) {
        try {
            lottieView?.takeIf { it.isAttachedToWindow }?.playAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error playing animation: ${e.message}")
        }
    }

    /**
     * Pause animation dengan error handling
     */
    fun pauseAnimation(lottieView: LottieAnimationView?) {
        try {
            lottieView?.takeIf { it.isAttachedToWindow }?.pauseAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error pausing animation: ${e.message}")
        }
    }

    /**
     * Resume animation dengan error handling
     */
    fun resumeAnimation(lottieView: LottieAnimationView?) {
        try {
            lottieView?.takeIf { it.isAttachedToWindow && !it.isAnimating }?.resumeAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error resuming animation: ${e.message}")
        }
    }

    /**
     * Cancel animation dengan error handling
     */
    fun cancelAnimation(lottieView: LottieAnimationView?) {
        try {
            lottieView?.cancelAnimation()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling animation: ${e.message}")
        }
    }

    /**
     * Update speed dengan error handling
     */
    fun setSpeed(lottieView: LottieAnimationView?, speed: Float) {
        try {
            lottieView?.speed = speed
        } catch (e: Exception) {
            Log.e(TAG, "Error setting speed: ${e.message}")
        }
    }

    /**
     * Update alpha dengan error handling
     */
    fun setAlpha(lottieView: LottieAnimationView?, alpha: Float) {
        try {
            lottieView?.alpha = alpha
        } catch (e: Exception) {
            Log.e(TAG, "Error setting alpha: ${e.message}")
        }
    }

    /**
     * Simple presets untuk berbagai keperluan
     */
    object Presets {

        fun splash(lottieView: LottieAnimationView?) {
            setupAnimation(lottieView, speed = 1.0f, loop = true)
        }

        fun welcome(lottieView: LottieAnimationView?) {
            setupAnimation(lottieView, speed = 0.6f, loop = true)
        }

        fun status(lottieView: LottieAnimationView?) {
            setupAnimation(lottieView, speed = 1.5f, loop = true)
        }

        fun loading(lottieView: LottieAnimationView?) {
            setupAnimation(lottieView, speed = 2.0f, loop = true)
        }
    }

    /**
     * Batch operations untuk multiple views
     */
    fun pauseAll(vararg lottieViews: LottieAnimationView?) {
        lottieViews.forEach { pauseAnimation(it) }
    }

    fun resumeAll(vararg lottieViews: LottieAnimationView?) {
        lottieViews.forEach { resumeAnimation(it) }
    }

    fun cancelAll(vararg lottieViews: LottieAnimationView?) {
        lottieViews.forEach { cancelAnimation(it) }
    }
}