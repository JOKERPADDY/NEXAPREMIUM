package com.nexapremium.data.filter

import com.nexapremium.data.api.model.SubjectItemDto
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FamilyContentFilterTest {
    @Test
    fun hidesMatureTermsAndHighRiskFlags() {
        val items = listOf(
            SubjectItemDto(title = "A family adventure"),
            SubjectItemDto(title = "Savita Bhabhi"),
            SubjectItemDto(title = "A mature drama"),
            SubjectItemDto(title = "Family Show", webHighRisk = true),
            SubjectItemDto(title = "F*ck My Pussy"),
            SubjectItemDto(title = "Sexy Girlfriend", genres = listOf("Hot", "Romance")),
            SubjectItemDto(title = "Sex Next Door", genres = listOf("Documentary")),
            SubjectItemDto(title = "Desperate Sexy Runs Babes", genres = listOf("Nollywood")),
            SubjectItemDto(title = "City Girl", genres = listOf("Romance")),
            SubjectItemDto(title = "MOTHER'S BARGAIN", genres = listOf("Drama"))
        )

        assertEquals(
            listOf("A family adventure"),
            FamilyContentFilter.filter(items, enabled = true).map { it.title }
        )
    }

    @Test
    fun leavesContentUnfilteredWhenDisabled() {
        val item = SubjectItemDto(title = "Savita Bhabhi")

        assertEquals(listOf(item), FamilyContentFilter.filter(listOf(item), enabled = false))
        assertFalse(FamilyContentFilter.isAllowed(item))
    }

    @Test
    fun doesNotFlagOrdinaryTitlesWithoutMatureMarkers() {
        assertTrue(FamilyContentFilter.isAllowed(SubjectItemDto(title = "The Parent Trap")))
        assertTrue(FamilyContentFilter.isAllowed(SubjectItemDto(title = "Hot Fuzz")))
        assertTrue(FamilyContentFilter.isAllowed(SubjectItemDto(title = "Sexy Beast")))
    }
}
