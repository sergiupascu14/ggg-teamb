package com.example.teamb.ui.util

/** Title-cases an ALL-CAPS dataset name for display (data stays canonical). */
fun String.toDisplayName(): String =
    trim().split(Regex("\\s+")).joinToString(" ") { word ->
        word.lowercase().replaceFirstChar { it.uppercaseChar() }
    }
