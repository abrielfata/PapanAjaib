package com.example.papanajaib.adapter


import android.animation.ObjectAnimator
import android.graphics.Paint
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.DiffUtil
import com.example.papanajaib.R
import com.example.papanajaib.data.Message
import com.example.papanajaib.databinding.ItemChildMessageBinding

class ChildMessageAdapter(
    private val messages: MutableList<Message>,
    private val onCompleteClick: (Message) -> Unit
) : RecyclerView.Adapter<ChildMessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val binding = ItemChildMessageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return MessageViewHolder(binding)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        val message = messages[position]
        holder.bind(message)
    }

    override fun getItemCount(): Int = messages.size

    inner class MessageViewHolder(
        private val binding: ItemChildMessageBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(message: Message) {
            Log.d("ChildAdapter", "🔗 Binding message: ${message.id} - ${message.text} - Completed: ${message.isCompleted}")

            binding.tvMessageText.text = message.text
            binding.tvMessageIcon.text = message.icon

            updateCompletionState(message)

            // Set click listener dengan protection terhadap multiple clicks
            binding.btnComplete.setOnClickListener {
                if (binding.btnComplete.isEnabled) {
                    Log.d("ChildAdapter", "🔘 Button clicked for message: ${message.id}")

                    // Disable button immediately to prevent double clicks
                    binding.btnComplete.isEnabled = false

                    // Add button press animation
                    animateButtonPress(binding.btnComplete) {
                        // Re-enable button after animation and callback
                        binding.btnComplete.postDelayed({
                            binding.btnComplete.isEnabled = true
                        }, 1000)

                        onCompleteClick(message)
                    }
                }
            }
        }

        private fun updateCompletionState(message: Message) {
            Log.d("ChildAdapter", "🎨 Updating UI for message: ${message.id}, isCompleted: ${message.isCompleted}")

            if (message.isCompleted) {
                // Completed state
                binding.tvMessageText.paintFlags =
                    binding.tvMessageText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.tvMessageText.alpha = 0.5f
                binding.tvMessageIcon.alpha = 0.5f

                binding.btnComplete.text = "Selesai! ✅"
                binding.btnComplete.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_primary)
                )
                binding.btnComplete.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onPrimary)
                )
                binding.btnComplete.icon =
                    ContextCompat.getDrawable(itemView.context, R.drawable.ic_check_circle)
                binding.btnComplete.iconTint =
                    ContextCompat.getColorStateList(itemView.context, R.color.md_theme_onPrimary)

                // Keep enabled untuk undo
                binding.btnComplete.isEnabled = true
                binding.btnComplete.alpha = 0.7f

            } else {
                // Incomplete state
                binding.tvMessageText.paintFlags =
                    binding.tvMessageText.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
                binding.tvMessageText.alpha = 1.0f
                binding.tvMessageIcon.alpha = 1.0f

                binding.btnComplete.text = "Sudah Dikerjakan"
                binding.btnComplete.setBackgroundColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_secondary)
                )
                binding.btnComplete.setTextColor(
                    ContextCompat.getColor(itemView.context, R.color.md_theme_onSecondary)
                )
                binding.btnComplete.icon =
                    ContextCompat.getDrawable(itemView.context, R.drawable.ic_check_circle_outline)
                binding.btnComplete.iconTint =
                    ContextCompat.getColorStateList(itemView.context, R.color.md_theme_onSecondary)

                // Enable button when incomplete
                binding.btnComplete.isEnabled = true
                binding.btnComplete.alpha = 1.0f
            }
        }

        private fun animateButtonPress(view: android.view.View, onComplete: () -> Unit) {
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
                    // Call completion callback after animation
                    onComplete()
                }
            })
        }
    }

    // Improved update method dengan logging yang lebih detail
    fun updateMessages(newMessages: List<Message>) {
        Log.d("ChildAdapter", "📱 UpdateMessages called with ${newMessages.size} messages")
        Log.d("ChildAdapter", "📱 Current messages count: ${messages.size}")

        // Log detail messages untuk debug
        newMessages.forEachIndexed { index, message ->
            Log.d("ChildAdapter", "📝 New[$index]: ${message.id} - ${message.text} - Completed: ${message.isCompleted}")
        }

        val diffCallback = MessageDiffCallback(messages.toList(), newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)

        Log.d("ChildAdapter", "✅ Messages updated successfully. New count: ${messages.size}")
    }

    // DiffUtil callback untuk efficient updates
    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val result = oldList[oldItemPosition].id == newList[newItemPosition].id
            Log.d("DiffUtil", "areItemsTheSame[$oldItemPosition-$newItemPosition]: $result")
            return result
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldMessage = oldList[oldItemPosition]
            val newMessage = newList[newItemPosition]
            val result = oldMessage.text == newMessage.text &&
                    oldMessage.icon == newMessage.icon &&
                    oldMessage.isCompleted == newMessage.isCompleted

            Log.d("DiffUtil", "areContentsTheSame[$oldItemPosition-$newItemPosition]: $result")
            Log.d("DiffUtil", "  Old: text='${oldMessage.text}', completed=${oldMessage.isCompleted}")
            Log.d("DiffUtil", "  New: text='${newMessage.text}', completed=${newMessage.isCompleted}")

            return result
        }
    }
}