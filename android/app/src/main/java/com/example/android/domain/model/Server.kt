package com.example.android.domain.model

data class Server(
    val id: String,
    val name: String,
    val username: String,
    val description: String = "",
    val imageUrl: String? = null,
    val address: String = "",
)
