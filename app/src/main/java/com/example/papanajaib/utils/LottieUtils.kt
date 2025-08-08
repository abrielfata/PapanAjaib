package com.example.papanajaib.utils

import com.airbnb.lottie.LottieAnimationView
import com.airbnb.lottie.LottieDrawable

/**
 * Utility class untuk mengatur animasi Lottie dalam aplikasi Papan Ajaib
 */
object LottieUtils {

    /**
     * Setup animasi Lottie dengan konfigurasi standar
     */
    fun setupStandardAnimation(
        lottieView: LottieAnimationView,
        animationRes: String = "astro.json",
        speed: Float = 1.0f,
        loop: Boolean = true,
        autoPlay: Boolean = true
    ) {
        lottieView.apply {
            setAnimation(animationRes)
            this.speed = speed
            repeatCount = if (loop) LottieDrawable.INFINITE else 0
            if (autoPlay) {
                playAnimation()
            }
        }
    }

    /**
     * Animasi untuk status loading
     */
    fun setLoadingState(lottieView: LottieAnimationView, isLoading: Boolean) {
        if (isLoading) {
            lottieView.apply {
                speed = 0.3f
                alpha = 0.7f
            }
        } else {
            lottieView.apply {
                speed = 1.0f
                alpha = 1.0f
            }
        }
    }

    /**
     * Animasi untuk status sukses
     */
    fun animateSuccess(lottieView: LottieAnimationView, duration: Long = 2000L) {
        lottieView.apply {
            val originalSpeed = speed
            speed = 2.5f
            postDelayed({
                speed = originalSpeed
            }, duration)
        }
    }

    /**
     * Animasi untuk status error
     */
    fun animateError(lottieView: LottieAnimationView, duration: Long = 1500L) {
        lottieView.apply {
            val originalSpeed = speed
            val originalAlpha = alpha
            speed = 0.2f
            alpha = 0.5f
            postDelayed({
                speed = originalSpeed
                alpha = originalAlpha
            }, duration)
        }
    }

    /**
     * Animasi untuk button press feedback
     */
    fun animateButtonPress(lottieView: LottieAnimationView, pressSpeed: Float = 3.0f, duration: Long = 1000L) {
        lottieView.apply {
            val originalSpeed = speed
            speed = pressSpeed
            postDelayed({
                speed = originalSpeed
            }, duration)
        }
    }

    /**
     * Set animasi berdasarkan status koneksi
     */
    fun setConnectionStatus(lottieView: LottieAnimationView, isConnected: Boolean) {
        if (isConnected) {
            lottieView.apply {
                speed = 1.5f
                alpha = 1.0f
                if (!isAnimating) {
                    resumeAnimation()
                }
            }
        } else {
            lottieView.apply {
                speed = 0.5f
                alpha = 0.6f
            }
        }
    }

    /**
     * Pause semua animasi untuk menghemat battery
     */
    fun pauseAnimation(vararg lottieViews: LottieAnimationView) {
        lottieViews.forEach { it.pauseAnimation() }
    }

    /**
     * Resume semua animasi
     */
    fun resumeAnimation(vararg lottieViews: LottieAnimationView) {
        lottieViews.forEach {
            if (!it.isAnimating) {
                it.resumeAnimation()
            }
        }
    }

    /**
     * Cancel semua animasi (untuk onDestroy)
     */
    fun cancelAnimation(vararg lottieViews: LottieAnimationView) {
        lottieViews.forEach { it.cancelAnimation() }
    }

    /**
     * Preset animasi untuk berbagai jenis feedback
     */
    object Presets {

        fun welcomeAnimation(lottieView: LottieAnimationView) {
            setupStandardAnimation(lottieView, speed = 0.6f)
        }

        fun statusIndicator(lottieView: LottieAnimationView) {
            setupStandardAnimation(lottieView, speed = 1.5f)
        }

        fun buttonIcon(lottieView: LottieAnimationView) {
            setupStandardAnimation(lottieView, speed = 1.2f)
        }

        fun footerDecoration(lottieView: LottieAnimationView) {
            setupStandardAnimation(lottieView, speed = 2.0f)
        }

        fun headerMain(lottieView: LottieAnimationView) {
            setupStandardAnimation(lottieView, speed = 0.8f)
        }
    }

    /**
     * Animasi berdasarkan user role
     */
    object RoleAnimations {

        fun parentModeAnimation(lottieView: LottieAnimationView) {
            lottieView.apply {
                speed = 1.0f
                alpha = 1.0f
                playAnimation()
            }
        }

        fun childModeAnimation(lottieView: LottieAnimationView) {
            lottieView.apply {
                speed = 1.2f
                alpha = 1.0f
                playAnimation()
            }
        }
    }

    /**
     * Animasi berdasarkan state aplikasi
     */
    object StateAnimations {

        fun activeFamily(vararg lottieViews: LottieAnimationView) {
            lottieViews.forEach { lottieView ->
                lottieView.apply {
                    speed = 1.0f
                    alpha = 1.0f
                    if (!isAnimating) resumeAnimation()
                }
            }
        }

        fun inactiveFamily(vararg lottieViews: LottieAnimationView) {
            lottieViews.forEach { lottieView ->
                lottieView.apply {
                    speed = 0.3f
                    alpha = 0.4f
                }
            }
        }

        fun familyDeleted(vararg lottieViews: LottieAnimationView) {
            lottieViews.forEach { it.pauseAnimation() }
        }
    }
}