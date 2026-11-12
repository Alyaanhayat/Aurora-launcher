package com.example.model

import java.util.UUID

data class AppFolder(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packageNames: List<String> = emptyList(),
    val colorSeed: Long = 0xFF7C4DFF
)
