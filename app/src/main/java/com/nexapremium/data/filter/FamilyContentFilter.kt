package com.nexapremium.data.filter

import com.nexapremium.data.api.model.SubjectItemDto

object FamilyContentFilter {
    private val blockedWords = setOf(
        "adult",
        "ecchi",
        "erotica",
        "erotic",
        "fck",
        "fuck",
        "fucked",
        "gangbang",
        "hentai",
        "incest",
        "mature",
        "nsfw",
        "nude",
        "nudity",
        "orgasm",
        "porn",
        "pussy",
        "rape",
        "raped",
        "seduction",
        "seductive",
        "seinen",
        "sex",
        "sexual",
        "uncensored",
        "xxx"
    )

    private val blockedPhrases = listOf(
        "savita bhabhi",
        "brickleberry",
        "mesudachi",
        "kowaremono",
        "kengan ashura",
        "sex doll",
        "sex weather"
    )

    private val blockedTitles = setOf(
        "city girl",
        "desperate sexy runs babes",
        "mother s bargain",
        "sexy girlfriend"
    )

    fun isAllowed(item: SubjectItemDto): Boolean {
        if (item.isPornographic == true || item.webHighRisk == true) return false

        val normalizedTitle = normalize(item.title.orEmpty())
        if (normalizedTitle in blockedTitles) return false

        val text = listOfNotNull(
            item.title,
            item.description,
            item.genres?.joinToString(" "),
            item.tags?.mapNotNull { it.name }?.joinToString(" ")
        ).joinToString(" ").lowercase()
        val normalizedWords = text.split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
        val normalizedText = normalizedWords.joinToString(" ")
        val hasMatureGenre = item.genres.orEmpty().any { normalize(it) in setOf("hot", "adult") }

        return !hasMatureGenre &&
            blockedWords.none { it in normalizedWords } &&
            blockedPhrases.none { phrase ->
                val normalizedPhrase = normalize(phrase)
                normalizedText.contains(normalizedPhrase)
            }
    }

    fun filter(items: List<SubjectItemDto>, enabled: Boolean): List<SubjectItemDto> =
        items.filter { item -> item.hasResource != false && (!enabled || isAllowed(item)) }

    private fun normalize(value: String): String =
        value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim().replace(Regex("\\s+"), " ")
}
