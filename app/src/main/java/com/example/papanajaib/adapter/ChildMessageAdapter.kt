
import android.animation.ObjectAnimator
import android.graphics.Paint
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
            binding.tvMessageText.text = message.text
            binding.tvMessageIcon.text = message.icon

            updateCompletionState(message)

            // Set click listener dengan protection terhadap multiple clicks
            binding.btnComplete.setOnClickListener {
                if (binding.btnComplete.isEnabled) {
                    // Disable button immediately to prevent double clicks
                    binding.btnComplete.isEnabled = false

                    // Add button press animation
                    animateButtonPress(binding.btnComplete) {
                        // Re-enable button after animation and callback
                        binding.btnComplete.isEnabled = !message.isCompleted
                        onCompleteClick(message)
                    }
                }
            }
        }

        private fun updateCompletionState(message: Message) {
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

                // Disable button when completed
                binding.btnComplete.isEnabled = false
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

    // Improved update method using DiffUtil for better performance
    fun updateMessages(newMessages: List<Message>) {
        val diffCallback = MessageDiffCallback(messages, newMessages)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        messages.clear()
        messages.addAll(newMessages)
        diffResult.dispatchUpdatesTo(this)
    }

    // DiffUtil callback for efficient updates
    private class MessageDiffCallback(
        private val oldList: List<Message>,
        private val newList: List<Message>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int = oldList.size
        override fun getNewListSize(): Int = newList.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldMessage = oldList[oldItemPosition]
            val newMessage = newList[newItemPosition]
            return oldMessage.text == newMessage.text &&
                    oldMessage.icon == newMessage.icon &&
                    oldMessage.isCompleted == newMessage.isCompleted
        }
    }
}
