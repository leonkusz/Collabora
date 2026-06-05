package com.example.collabora

data class SubTask(
    val subTaskId: String = "",
    val taskId: String = "",
    val teamId: String = "",
    val subTaskText: String = "",
    val assignedToId: String = "",
    val assignedToName: String = "",
    val status: String = "Menunggu",
    val createdAt: Long = 0,
    val attachmentLink: String = ""
)