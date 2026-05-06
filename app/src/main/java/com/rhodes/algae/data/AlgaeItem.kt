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

/** All phyla sorted by display order */
val PHYLUM_ORDER = listOf(
    "硅藻门", "绿藻门", "蓝藻门", "裸藻门",
    "甲藻门", "金藻门", "隐藻门", "黄藻门"
)

/** Chinese → Latin phylum names */
val PHYLUM_LATIN = mapOf(
    "硅藻门" to "Bacillariophyta", "绿藻门" to "Chlorophyta",
    "蓝藻门" to "Cyanophyta", "裸藻门" to "Euglenophyta",
    "甲藻门" to "Pyrrophyta", "金藻门" to "Chrysophyta",
    "隐藻门" to "Cryptophyta", "黄藻门" to "Xanthophyta"
)

/** Short emoji labels for each phylum filter chip */
val PHYLUM_EMOJI = mapOf(
    "硅藻门" to "🧬", "绿藻门" to "🌿", "蓝藻门" to "🔵",
    "裸藻门" to "🟢", "甲藻门" to "🟤", "金藻门" to "🌟",
    "隐藻门" to "👻", "黄藻门" to "💛"
)
