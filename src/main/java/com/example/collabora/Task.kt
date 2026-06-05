package com.example.collabora

data class Task(
    val taskId: String = "",
    val teamId: String = "",
    val title: String = "",
    val description: String = "",
    val deadline: String = "",
    val priority: String = "",
    val status: String = "",
    val createdAt: Long = 0,
    val attachmentLinks: List<String> = emptyList()
)