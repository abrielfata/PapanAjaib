package com.example.papanajaib.adapter

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.papanajaib.R
import com.example.papanajaib.data.Message
import com.example.papanajaib.databinding.ItemParentMessageBinding
import java.text.SimpleDateFormat
import java.util.*

class ParentMessageAdapter(
    private val messages: MutableList<Message>,
    private val onDeleteClick: (Message) -> Unit
) : RecyclerView.Adapter<ParentMessageAdapter.MessageViewHolder>() {

    private val dateFormatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())

    init {
        setHasStableIds(true)
    }

    override fun getItemId(position: Int): Long {
        return messages[position].id.hashCode().toLong()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemParentMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message, position)
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(
        private val binding: ItemParentMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message, position: Int) {
            binding.tvMessageText.text = message.text
            binding.tvMessageIcon.text = message.icon

            // Format timestamp
            val formattedDate = dateFormatter.format(Date(message.timestamp))

            updateMessageAppearance(message, formattedDate)
            setupClickListener(message)

            // Add entrance animation for new items
            animateItemEntrance(position)
        }

        private fun updateMessageAppearance(message: Message, formattedDate: String) {
            if (message.isCompleted) {
                // Completed message appearance
                binding.tvMessageText.paintFlags =
                    binding.tvMessageText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvMessageText.alpha = 0.7f
                binding.tvMessageIcon.alpha = 0.7f

                binding.tvMessageStatus.text = "✅ Selesai pada $formattedDate"
                binding.tvMessageStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_primary)
                )

                // Change card appearance for completed tasks
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_primaryContainer)
                )
                binding.root.alpha = 0.9f

                binding.btnDeleteMessage.text = "Hapus"
                binding.btnDeleteMessage.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_error)
                )

            } else {
                // Pending message appearance
                binding.tvMessageText.paintFlags =
                    binding.tvMessageText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvMessageText.alpha = 1.0f
                binding.tvMessageIcon.alpha = 1.0f

                binding.tvMessageStatus.text = "⏳ Dibuat pada $formattedDate"
                binding.tvMessageStatus.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onSurfaceVariant)
                )

                // Default card appearance for pending tasks
                binding.root.setCardBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_surface)
                )
                binding.root.alpha = 1.0f

                binding.btnDeleteMessage.text = "Hapus"
                binding.btnDeleteMessage.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_error)
                )
            }
        }

        private fun setupClickListener(message: Message) {
            binding.btnDeleteMessage.setOnClickListener {
                // Animate button press
                animateDeleteButton()

                // Call delete handler
                onDeleteClick(message)
            }

            // Add click listener to entire card for better UX
            binding.root.setOnClickListener {
                animateCardPress()
                // Could add navigation to detailed view here
            }
        }

        private fun animateDeleteButton() {
            val scaleDown = ObjectAnimator.ofFloat(binding.btnDeleteMessage, "scaleX", 1f, 0.9f)
            val scaleDownY = ObjectAnimator.ofFloat(binding.btnDeleteMessage, "scaleY", 1f, 0.9f)
            val scaleUp = ObjectAnimator.ofFloat(binding.btnDeleteMessage, "scaleX", 0.9f, 1f)
            val scaleUpY = ObjectAnimator.ofFloat(binding.btnDeleteMessage, "scaleY", 0.9f, 1f)

            val scaleDownSet = AnimatorSet()
            scaleDownSet.playTogether(scaleDown, scaleDownY)
            scaleDownSet.duration = 100

            val scaleUpSet = AnimatorSet()
            scaleUpSet.playTogether(scaleUp, scaleUpY)
            scaleUpSet.duration = 100
            scaleUpSet.interpolator = AccelerateDecelerateInterpolator()

            val finalSet = AnimatorSet()
            finalSet.playSequentially(scaleDownSet, scaleUpSet)
            finalSet.start()
        }

        private fun animateCardPress() {
            val elevation = ObjectAnimator.ofFloat(binding.root, "cardElevation", 2f, 8f)
            val elevationBack = ObjectAnimator.ofFloat(binding.root, "cardElevation", 8f, 2f)

            elevation.duration = 100
            elevationBack.duration = 100
            elevationBack.startDelay = 100

            val animatorSet = AnimatorSet()
            animatorSet.playSequentially(elevation, elevationBack)
            animatorSet.start()
        }

        private fun animateItemEntrance(position: Int) {
            // Stagger animation based on position
            val delay = (position * 50L).coerceAtMost(300L)

            binding.root.alpha = 0f
            binding.root.translationY = 50f

            val fadeIn = ObjectAnimator.ofFloat(binding.root, "alpha", 0f, 1f)
            val slideUp = ObjectAnimator.ofFloat(binding.root, "translationY", 50f, 0f)

            val animatorSet = AnimatorSet()
            animatorSet.playTogether(fadeIn, slideUp)
            animatorSet.duration = 300
            animatorSet.startDelay = delay
            animatorSet.interpolator = AccelerateDecelerateInterpolator()
            animatorSet.start()
        }
    }
}