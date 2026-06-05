package com.example.collabora

data class ChatMessage(

    val messageId: String = "",

    val message: String = "",

    val senderId: String = "",

    val senderName: String = "",

    val teamId: String = "",

    val timestamp: Long = 0
)