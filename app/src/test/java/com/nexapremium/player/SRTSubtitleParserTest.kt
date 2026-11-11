package com.nexapremium.player

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.ByteArrayInputStream

class SRTSubtitleParserTest {

    @Test
    fun parse_validSrtContent_returnsParsedCues() {
        val srtContent = """
1
00:00:01,000 --> 00:00:04,000
Welcome to NEXAPREMIUM!

2
00:00:05,500 --> 00:00:09,000
Enjoy full HD movie streaming.
""".trimIndent()

        val inputStream = ByteArrayInputStream(srtContent.toByteArray(Charsets.UTF_8))
        val cues = SRTSubtitleParser.parse(inputStream)

        assertEquals(2, cues.size)
        assertEquals(1000L, cues[0].startTimeMs)
        assertEquals(4000L, cues[0].endTimeMs)
        assertEquals("Welcome to NEXAPREMIUM!", cues[0].text)

        assertEquals(5500L, cues[1].startTimeMs)
        assertEquals(9000L, cues[1].endTimeMs)
        assertEquals("Enjoy full HD movie streaming.", cues[1].text)
    }
}
