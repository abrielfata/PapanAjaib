package com.example.papanajaib.adapter

import android.animation.ObjectAnimator
import android.graphics.Paint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.papanajaib.R
import com.example.papanajaib.data.Message
import com.example.papanajaib.databinding.ItemChildMessageBinding

class ChildMessageAdapter(
    private val messages: MutableList<Message>,
    private val onCompleteClick: (Message) -> Unit
) : RecyclerView.Adapter<ChildMessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChildMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(private val binding: ItemChildMessageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            binding.tvMessageText.text = message.text
            binding.tvMessageIcon.text = message.icon

            updateCompletionState(message)

            binding.btnComplete.setOnClickListener {
                // Add button press animation
                animateButtonPress(binding.btnComplete)
                onCompleteClick(message)
            }
        }

        private fun updateCompletionState(message: Message) {
            if (message.isCompleted) {
                // Completed state
                binding.tvMessageText.paintFlags = binding.tvMessageText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvMessageText.alpha = 0.5f
                binding.tvMessageIcon.alpha = 0.5f
                binding.btnComplete.text = "Selesai!"
                binding.btnComplete.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_primary)
                )
                binding.btnComplete.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onPrimary)
                )
                binding.btnComplete.icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_check_circle)
                binding.btnComplete.iconTint = ContextCompat.getColorStateList(itemView.context, R.color.md_theme_onPrimary)

                // Disable further clicks when completed
                binding.btnComplete.isEnabled = false
                binding.btnComplete.alpha = 0.7f

            } else {
                // Incomplete state
                binding.tvMessageText.paintFlags = binding.tvMessageText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvMessageText.alpha = 1.0f
                binding.tvMessageIcon.alpha = 1.0f
                binding.btnComplete.text = "Sudah Dikerjakan"
                binding.btnComplete.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_secondary)
                )
                binding.btnComplete.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onSecondary)
                )
                binding.btnComplete.icon = ContextCompat.getDrawable(itemView.context, R.drawable.ic_check_circle_outline)
                binding.btnComplete.iconTint = ContextCompat.getColorStateList(itemView.context, R.color.md_theme_onSecondary)

                // Enable clicks when incomplete
                binding.btnComplete.isEnabled = true
                binding.btnComplete.alpha = 1.0f
            }
        }

        private fun animateButtonPress(view: android.view.View) {
            val scaleDown = ObjectAnimator.ofFloat(view, "scaleX", 1.0f, 0.95f)
            val scaleDownY = ObjectAnimator.ofFloat(view, "scaleY", 1.0f, 0.95f)
            val scaleUp = ObjectAnimator.ofFloat(view, "scaleX", 0.95f, 1.0f)
            val scaleUpY = ObjectAnimator.ofFloat(view, "scaleY", 0.95f, 1.0f)

            scaleDown.duration = 100
            scaleDownY.duration = 100
            scaleUp.duration = 100
            scaleUpY.duration = 100

            scaleDown.start()
            scaleDownY.start()

            scaleDown.addListener(object : android.animation.AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: android.animation.Animator) {
                    scaleUp.start()
                    scaleUpY.start()
                }
            })
        }
    }

    fun updateMessages(newMessages: List<Message>) {
        messages.clear()
        messages.addAll(newMessages)
        notifyDataSetChanged()
    }


}