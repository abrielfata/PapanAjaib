package com.example.papanajaib

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.papanajaib.adapter.ChildMessageAdapter
import com.google.firebase.database.*
import com.example.papanajaib.data.Message
import com.example.papanajaib.databinding.ActivityChildBinding
import com.example.papanajaib.utils.FamilyManager

class ChildActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChildBinding
    private lateinit var database: DatabaseReference
    private lateinit var messageList: MutableList<Message>
    private lateinit var adapter: ChildMessageAdapter
    private lateinit var familyId: String
    private var valueEventListener: ValueEventListener? = null

    // Add flag to prevent multiple simultaneous updates
    private var isUpdatingMessage = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChildBinding.inflate(layoutInflater)
        setContentView(binding.root)

        familyId = FamilyManager.getFamilyId(this) ?: run {
            Toast.makeText(this, getString(R.string.family_config_not_found), Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        database = FirebaseDatabase.getInstance()
            .getReference("families")
            .child(familyId)
            .child("messages")

        messageList = mutableListOf()
        adapter = ChildMessageAdapter(messageList) { message ->
            toggleMessageCompletion(message)
        }

        setupUI()
        setupRecyclerView()
        setupSwipeRefresh()
        setupClickListeners()
        setupLottieAnimation() // Setup Lottie animation

        showLoadingState()
        listenForMessages()
    }

    private fun setupUI() {
        supportActionBar?.title = "Papan ${FamilyManager.getFamilyName(this)}"
    }

    private fun setupRecyclerView() {
        binding.rvChildMessages.layoutManager = LinearLayoutManager(this)
        binding.rvChildMessages.adapter = adapter
    }

    private fun setupSwipeRefresh() {
        binding.swipeRefreshLayout.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_light,
            android.R.color.holo_orange_light,
            android.R.color.holo_red_light
        )
        binding.swipeRefreshLayout.setOnRefreshListener {
            Handler(Looper.getMainLooper()).postDelayed({
                binding.swipeRefreshLayout.isRefreshing = false
            }, 1000)
        }
    }

    private fun setupClickListeners() {
        binding.celebrationOverlay.setOnClickListener {
            hideCelebrationOverlay()
        }
        binding.allTasksCompletedView.setOnClickListener {
            hideAllTasksCompletedView()
        }
    }

    private fun setupLottieAnimation() {
        try {
            // Setup Lottie animation from raw resource
            binding.lottieAnimationView.setAnimation(R.raw.astro)
            binding.lottieAnimationView.repeatCount = 1 // Play twice
            binding.lottieAnimationView.speed = 1.0f

            // Set animation listener for better control
            binding.lottieAnimationView.addAnimatorListener(object : android.animation.Animator.AnimatorListener {
                override fun onAnimationStart(animation: android.animation.Animator) {
                    Log.d("ChildActivity", "🎬 Lottie animation started")
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    Log.d("ChildActivity", "🎬 Lottie animation ended")
                    // Auto hide after animation completes
                    Handler(Looper.getMainLooper()).postDelayed({
                        hideCelebrationOverlay()
                    }, 500)
                }

                override fun onAnimationCancel(animation: android.animation.Animator) {}
                override fun onAnimationRepeat(animation: android.animation.Animator) {}
            })

            Log.d("ChildActivity", "✅ Lottie animation setup successful")
        } catch (e: Exception) {
            Log.e("ChildActivity", "❌ Failed to setup Lottie animation", e)
            // Hide Lottie view if setup fails
            binding.lottieAnimationView.visibility = View.GONE
        }
    }

    private fun toggleMessageCompletion(message: Message) {
        // Prevent multiple simultaneous updates
        if (isUpdatingMessage) {
            Log.d("ChildActivity", "Update already in progress, ignoring click")
            return
        }

        isUpdatingMessage = true
        val newStatus = !message.isCompleted

        Log.d("ChildActivity", "Toggling message ${message.id} from ${message.isCompleted} to $newStatus")

        // JANGAN update UI dulu - tunggu Firebase response

        database.child(message.id).child("isCompleted").setValue(newStatus)
            .addOnSuccessListener {
                Log.d("ChildActivity", "✅ Successfully updated message ${message.id} to $newStatus")

                // Tampilkan feedback sukses
                if (newStatus) {
                    showCelebrationOverlay()
                }

                Toast.makeText(this,
                    if (newStatus) "Tugas selesai! 🎉" else "Tugas dibatalkan",
                    Toast.LENGTH_SHORT
                ).show()

                isUpdatingMessage = false
            }
            .addOnFailureListener { exception ->
                Log.e("ChildActivity", "❌ Failed to update message ${message.id}", exception)

                // Reset UI jika gagal update Firebase
                val index = messageList.indexOfFirst { it.id == message.id }
                if (index != -1) {
                    // Revert perubahan UI
                    messageList[index].isCompleted = message.isCompleted // kembalikan ke status awal
                    adapter.notifyItemChanged(index)
                }

                Toast.makeText(this,
                    "Gagal mengubah status: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()

                isUpdatingMessage = false
            }
    }

    private fun listenForMessages() {
        binding.swipeRefreshLayout.isRefreshing = true

        valueEventListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                Log.d("ChildActivity", "🔄 Data changed, updating UI")
                hideLoadingState()

                val newMessages = mutableListOf<Message>()
                for (messageSnapshot in snapshot.children) {
                    val message = messageSnapshot.getValue(Message::class.java)
                    message?.let {
                        Log.d("ChildActivity", "📝 Message: ${it.id} - ${it.text} - Completed: ${it.isCompleted}")
                        newMessages.add(it)
                    }
                }

                // Sort: incomplete tasks first, then by timestamp
                newMessages.sortWith(compareBy<Message> { it.isCompleted }.thenByDescending { it.timestamp })

                // Update adapter dengan updateMessages() method
                adapter.updateMessages(newMessages)

                updateUI()
                binding.swipeRefreshLayout.isRefreshing = false
            }

            override fun onCancelled(error: DatabaseError) {
                Log.e("ChildActivity", "❌ Database error: ${error.message}")
                hideLoadingState()
                binding.swipeRefreshLayout.isRefreshing = false
                Toast.makeText(this@ChildActivity,
                    "Gagal memuat pesan: ${error.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        database.addValueEventListener(valueEventListener!!)
    }

    private fun updateUI() {
        if (messageList.isEmpty()) {
            showEmptyState()
        } else {
            hideEmptyState()
            updateProgress()
            checkAllTasksCompleted()
        }
    }

    private fun updateProgress() {
        val completedTasks = messageList.count { it.isCompleted }
        val totalTasks = messageList.size
        val progressPercentage = if (totalTasks > 0) (completedTasks * 100) / totalTasks else 0

        binding.tvProgress.text = "$completedTasks dari $totalTasks tugas selesai"

        // Update percentage text
        binding.tvProgressPercentage?.text = "$progressPercentage%"

        // Animate progress bar
        val animator = ObjectAnimator.ofInt(
            binding.progressIndicator,
            "progress",
            binding.progressIndicator.progress,
            progressPercentage
        )
        animator.duration = 500
        animator.start()
    }

    private fun checkAllTasksCompleted() {
        if (messageList.isNotEmpty() && messageList.all { it.isCompleted }) {
            showAllTasksCompletedView()
        }
    }

    private fun showLoadingState() {
        binding.progressBar.visibility = View.VISIBLE
        binding.emptyStateView.visibility = View.GONE
        binding.rvChildMessages.visibility = View.GONE
    }

    private fun hideLoadingState() {
        binding.progressBar.visibility = View.GONE
    }

    private fun showEmptyState() {
        binding.emptyStateView.visibility = View.VISIBLE
        binding.rvChildMessages.visibility = View.GONE
        binding.tvProgress.text = "0 dari 0 tugas selesai"
        binding.progressIndicator.progress = 0
        binding.tvProgressPercentage?.text = "0%"
    }

    private fun hideEmptyState() {
        binding.emptyStateView.visibility = View.GONE
        binding.rvChildMessages.visibility = View.VISIBLE
    }

    private fun showCelebrationOverlay() {
        Log.d("ChildActivity", "🎉 Showing celebration overlay with Lottie")

        binding.celebrationOverlay.visibility = View.VISIBLE
        binding.celebrationOverlay.alpha = 0f

        // Show and start Lottie animation if available
        if (binding.lottieAnimationView.visibility != View.GONE) {
            binding.lottieAnimationView.visibility = View.VISIBLE
            binding.celebrationText.visibility = View.GONE // Hide fallback text

            // Start Lottie animation
            binding.lottieAnimationView.playAnimation()
            Log.d("ChildActivity", "🚀 Started Lottie astro animation")
        } else {
            // Use fallback text animation
            binding.celebrationText.visibility = View.VISIBLE
            binding.lottieAnimationView.visibility = View.GONE

            // Auto hide after delay if using fallback
            Handler(Looper.getMainLooper()).postDelayed({
                hideCelebrationOverlay()
            }, 2000)
        }

        // Fade in the overlay
        binding.celebrationOverlay.animate()
            .alpha(1f)
            .setDuration(300)
            .start()
    }

    private fun hideCelebrationOverlay() {
        Log.d("ChildActivity", "🎉 Hiding celebration overlay")

        binding.celebrationOverlay.animate()
            .alpha(0f)
            .setDuration(300)
            .setListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    binding.celebrationOverlay.visibility = View.GONE
                    binding.lottieAnimationView.visibility = View.GONE
                    binding.celebrationText.visibility = View.VISIBLE

                    // Stop Lottie animation to save resources
                    binding.lottieAnimationView.cancelAnimation()
                }
            })
            .start()
    }

    private fun showAllTasksCompletedView() {
        binding.allTasksCompletedView.visibility = View.VISIBLE

        val scaleX = ObjectAnimator.ofFloat(binding.allTasksCompletedView, "scaleX", 0.8f, 1.0f)
        val scaleY = ObjectAnimator.ofFloat(binding.allTasksCompletedView, "scaleY", 0.8f, 1.0f)
        val alpha = ObjectAnimator.ofFloat(binding.allTasksCompletedView, "alpha", 0f, 1f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 600
        animatorSet.start()

        Handler(Looper.getMainLooper()).postDelayed({
            hideAllTasksCompletedView()
        }, 3000)
    }

    private fun hideAllTasksCompletedView() {
        val scaleX = ObjectAnimator.ofFloat(binding.allTasksCompletedView, "scaleX", 1.0f, 0.8f)
        val scaleY = ObjectAnimator.ofFloat(binding.allTasksCompletedView, "scaleY", 1.0f, 0.8f)
        val alpha = ObjectAnimator.ofFloat(binding.allTasksCompletedView, "alpha", 1f, 0f)

        val animatorSet = AnimatorSet()
        animatorSet.playTogether(scaleX, scaleY, alpha)
        animatorSet.duration = 300
        animatorSet.addListener(object : android.animation.AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: android.animation.Animator) {
                binding.allTasksCompletedView.visibility = View.GONE
            }
        })
        animatorSet.start()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Remove listener to prevent memory leaks
        valueEventListener?.let {
            database.removeEventListener(it)
        }

        // Stop Lottie animation to prevent memory leaks
        binding.lottieAnimationView.cancelAnimation()
    }

    override fun onPause() {
        super.onPause()
        // Pause Lottie animation when activity is paused
        if (binding.lottieAnimationView.isAnimating) {
            binding.lottieAnimationView.pauseAnimation()
        }
    }

    override fun onResume() {
        super.onResume()
        // Resume Lottie animation if it was playing when paused
        if (binding.celebrationOverlay.visibility == View.VISIBLE &&
            binding.lottieAnimationView.visibility == View.VISIBLE) {
            binding.lottieAnimationView.resumeAnimation()
        }
    }
}