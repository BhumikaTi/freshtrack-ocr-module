package com.freshtrack.ocr.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.freshtrack.ocr.R
import com.freshtrack.ocr.data.ChatMessage
import com.freshtrack.ocr.data.ChatRole

class ChatAdapter : ListAdapter<ChatMessage, ChatAdapter.Holder>(Diff) {

    class Holder(view: View) : RecyclerView.ViewHolder(view) {
        val bubble: TextView = view.findViewById(R.id.tvBubble)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
        Holder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_chat_message, parent, false)
        )

    override fun onBindViewHolder(holder: Holder, position: Int) {
        val message = getItem(position)
        val context = holder.itemView.context
        val isUser = message.role == ChatRole.USER
        val bubble = holder.bubble

        bubble.text = message.text

        val background = GradientDrawable().apply {
            cornerRadius = 18f * context.resources.displayMetrics.density

            when {
                isUser -> setColor(context.getColor(R.color.green_primary))

                message.isError -> {
                    setColor(context.getColor(R.color.card_dark))
                    setStroke(dp(context, 1), context.getColor(R.color.error_bright))
                }

                else -> {
                    setColor(context.getColor(R.color.card_dark))
                    setStroke(dp(context, 1), context.getColor(R.color.border_light))
                }
            }
        }
        bubble.background = background

        bubble.setTextColor(
            when {
                isUser -> Color.WHITE
                message.isError -> context.getColor(R.color.error_bright)
                message.isLoading -> context.getColor(R.color.text_secondary)
                else -> context.getColor(R.color.text_primary)
            }
        )

        bubble.setTypeface(
            null,
            if (message.isLoading) Typeface.ITALIC else Typeface.NORMAL
        )

        // Users on the right, assistant on the left.
        val params = bubble.layoutParams as FrameLayout.LayoutParams
        params.gravity = if (isUser) Gravity.END else Gravity.START
        bubble.layoutParams = params

        // Long-press to copy.
        bubble.setOnLongClickListener {
            if (!message.isLoading) {
                val clipboard =
                    context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("FreshTrack chat", message.text))
                Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
            }
            true
        }
    }

    private fun dp(context: Context, value: Int): Int =
        (value * context.resources.displayMetrics.density).toInt()

    private object Diff : DiffUtil.ItemCallback<ChatMessage>() {
        override fun areItemsTheSame(old: ChatMessage, new: ChatMessage) = old.id == new.id
        override fun areContentsTheSame(old: ChatMessage, new: ChatMessage) = old == new
    }
}
