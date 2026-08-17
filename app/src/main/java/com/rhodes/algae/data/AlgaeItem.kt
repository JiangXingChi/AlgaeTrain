package com.rhodes.algae.data

/** Represents one algae image card */
data class AlgaeItem(
    val id: String,
    val file: String,
    val phylum: String,
    val phylumLatin: String = "",
    val genus: String,
    val genusLatin: String = "",
    val number: Int,
    val known: Boolean = false
)
