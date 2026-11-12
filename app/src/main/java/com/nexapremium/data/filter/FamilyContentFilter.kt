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

        // Descriptions often mention mature themes in otherwise general titles. Use
        // the title and provider taxonomy for the heuristic, not plot summaries.
        val text = listOfNotNull(
            item.title,
            item.genres?.joinToString(" "),
            item.tags?.mapNotNull { it.name }?.joinToString(" ")
        ).joinToString(" ").lowercase()
        val normalizedWords = text.split(Regex("\\s+")).map(::normalize).filter { it.isNotEmpty() }
        val normalizedText = normalizedWords.joinToString(" ")
        val matureLabels = setOf("hot", "adult", "mature", "erotic", "ecchi", "hentai", "nsfw")
        val hasMatureGenre = (item.genres.orEmpty() + item.tags.orEmpty().mapNotNull { it.name })
            .any { normalize(it) in matureLabels }

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
