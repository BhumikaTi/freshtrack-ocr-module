package com.freshtrack.ocr.ui

import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freshtrack.ocr.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.switchmaterial.SwitchMaterial
import kotlinx.coroutines.launch

class ChatActivity : AppCompatActivity() {

    private val viewModel: ChatViewModel by viewModels()
    private val adapter = ChatAdapter()

    private lateinit var rvMessages: RecyclerView
    private lateinit var etMessage: EditText
    private lateinit var btnSend: FloatingActionButton
    private lateinit var chipScroll: View

    private val suggestions = listOf(
        "What's expiring soon?",
        "Suggest a recipe from my items",
        "How do I store fresh herbs?"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Draw behind the system bars and handle the keyboard ourselves.
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_chat)

        val root = findViewById<View>(R.id.chatRoot)
        ViewCompat.setOnApplyWindowInsetsListener(root) { view, insets ->
            val bars = insets.getInsets(
                WindowInsetsCompat.Type.systemBars() or WindowInsetsCompat.Type.ime()
            )
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom)
            insets
        }

        rvMessages = findViewById(R.id.rvMessages)
        etMessage = findViewById(R.id.etMessage)
        btnSend = findViewById(R.id.btnSend)
        chipScroll = findViewById(R.id.chipScroll)

        rvMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        rvMessages.adapter = adapter

        findViewById<TextView>(R.id.btnChatBack).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnNewChat).setOnClickListener { viewModel.newChat() }

        findViewById<SwitchMaterial>(R.id.switchPantry).apply {
            isChecked = viewModel.sharePantry
            setOnCheckedChangeListener { _, checked -> viewModel.sharePantry = checked }
        }

        btnSend.setOnClickListener { sendCurrentText() }

        buildSuggestionChips()
        observeViewModel()
    }

    private fun sendCurrentText() {
        if (viewModel.sending.value) return

        val text = etMessage.text.toString().trim()
        if (text.isEmpty()) return

        etMessage.text.clear()
        viewModel.send(text)
    }

    private fun buildSuggestionChips() {
        val row = findViewById<LinearLayout>(R.id.chipRow)
        row.removeAllViews()

        suggestions.forEach { suggestion ->
            val chip = MaterialButton(
                this,
                null,
                com.google.android.material.R.attr.materialButtonOutlinedStyle
            ).apply {
                text = suggestion
                isAllCaps = false
                textSize = 13f
                setTextColor(getColor(R.color.green_primary))
                strokeColor = ColorStateList.valueOf(getColor(R.color.border_light))
                cornerRadius = dp(18)

                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply { marginEnd = dp(8) }

                setOnClickListener { viewModel.send(suggestion) }
            }

            row.addView(chip)
        }
    }

    private fun observeViewModel() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {

                launch {
                    viewModel.messages.collect { messages ->
                        adapter.submitList(messages) {
                            rvMessages.scrollToPosition(messages.lastIndex)
                        }

                        // Suggestions are only useful before the first question.
                        chipScroll.visibility =
                            if (messages.size > 1) View.GONE else View.VISIBLE
                    }
                }

                launch {
                    viewModel.sending.collect { sending ->
                        btnSend.isEnabled = !sending
                        btnSend.alpha = if (sending) 0.5f else 1f
                    }
                }
            }
        }
    }

    private fun dp(value: Int): Int =
        (value * resources.displayMetrics.density).toInt()
}
