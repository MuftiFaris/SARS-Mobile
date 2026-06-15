package com.informatika.sars.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)

class ChatViewModel : ViewModel() {
    private val _messages = MutableStateFlow<List<ChatMessage>>(listOf(
        ChatMessage("Halo! Saya asisten AI SARS. Ada yang bisa saya bantu terkait jadwal atau ruangan?", false)
    ))
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isTyping = MutableStateFlow(false)
    val isTyping: StateFlow<Boolean> = _isTyping

    fun sendMessage(text: String) {
        if (text.isBlank()) return

        val userMessage = ChatMessage(text, true)
        _messages.value = _messages.value + userMessage

        viewModelScope.launch {
            _isTyping.value = true
            delay(1500) // Simulating AI processing
            
            val response = generateMockResponse(text)
            _messages.value = _messages.value + ChatMessage(response, false)
            _isTyping.value = false
        }
    }

    private fun generateMockResponse(query: String): String {
        val q = query.lowercase()
        return when {
            q.contains("jadwal") && q.contains("hari ini") -> 
                "Jadwal Anda hari ini adalah 'Advanced Algorithm Design' di Hall B-12 pukul 09:00 - 11:00."
            q.contains("ruangan") || q.contains("lab") -> 
                "Laboratorium 404 saat ini tersedia. Anda dapat melakukan request peminjaman melalui menu Request."
            q.contains("terima kasih") || q.contains("thanks") -> 
                "Sama-sama! Senang bisa membantu Anda."
            else -> "Maaf, saya sedang belajar untuk memahami konteks tersebut lebih baik. Bisa coba tanyakan tentang jadwal kuliah atau ketersediaan ruangan?"
        }
    }
}
