package com.freshtrack.ocr.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.freshtrack.ocr.data.ChatContent
import com.freshtrack.ocr.data.ChatMessage
import com.freshtrack.ocr.data.ChatRepository
import com.freshtrack.ocr.data.ChatResult
import com.freshtrack.ocr.data.ChatRole
import com.freshtrack.ocr.data.GeminiPart
import com.freshtrack.ocr.data.ProductDatabase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Holds the conversation so it survives screen rotation.
 *
 * [history] is what is sent to Gemini and only contains turns that succeeded.
 * [messages] is what the screen shows and also contains error bubbles.
 */
class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = ChatRepository(
        productDao = ProductDatabase.getDatabase(application).productDao()
    )

    private val history = mutableListOf<ChatContent>()
    private var nextId = 0L

    private val _messages = MutableStateFlow(listOf(welcomeMessage()))
    val messages: StateFlow<List<ChatMessage>> = _messages.asStateFlow()

    private val _sending = MutableStateFlow(false)
    val sending: StateFlow<Boolean> = _sending.asStateFlow()

    /** Whether the user's saved products are included in the prompt. */
    var sharePantry: Boolean = true

    fun send(text: String) {
        val clean = text.trim()
        if (clean.isEmpty() || _sending.value) return

        _sending.value = true

        val loading = ChatMessage(
            id = nextId++,
            role = ChatRole.ASSISTANT,
            text = "Thinking…",
            isLoading = true
        )

        _messages.update {
            it + ChatMessage(nextId++, ChatRole.USER, clean) + loading
        }

        history += ChatContent(ROLE_USER, listOf(GeminiPart(clean)))

        viewModelScope.launch {
            when (val result = repository.send(history.toList(), sharePantry)) {

                is ChatResult.Reply -> {
                    history += ChatContent(ROLE_MODEL, listOf(GeminiPart(result.text)))
                    replace(loading.id, ChatMessage(loading.id, ChatRole.ASSISTANT, result.text))
                }

                is ChatResult.Failure -> {
                    // Drop the unanswered question so the next request still alternates user/model.
                    history.removeAt(history.lastIndex)
                    replace(
                        loading.id,
                        ChatMessage(loading.id, ChatRole.ASSISTANT, result.message, isError = true)
                    )
                }
            }

            _sending.value = false
        }
    }

    fun newChat() {
        if (_sending.value) return
        history.clear()
        _messages.value = listOf(welcomeMessage())
    }

    private fun replace(id: Long, replacement: ChatMessage) {
        _messages.update { list -> list.map { if (it.id == id) replacement else it } }
    }

    private fun welcomeMessage() = ChatMessage(
        id = nextId++,
        role = ChatRole.ASSISTANT,
        text = "Hi! I'm FreshTrack Assistant. Ask me what's expiring soon, how to store " +
                "something, or what to cook with what you have."
    )

    private companion object {
        const val ROLE_USER = "user"
        const val ROLE_MODEL = "model"
    }
}
