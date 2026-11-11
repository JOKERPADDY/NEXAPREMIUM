package com.nexapremium.player

import java.io.InputStream
import java.util.*

data class SubtitleCue(
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String
)

object SRTSubtitleParser {

    fun parse(inputStream: InputStream): List<SubtitleCue> {
        val cues = mutableListOf<SubtitleCue>()
        val scanner = Scanner(inputStream, "UTF-8")

        var startTimeMs = 0L
        var endTimeMs = 0L
        val textBuilder = StringBuilder()

        while (scanner.hasNextLine()) {
            val line = scanner.nextLine().trim()
            if (line.isEmpty()) {
                if (textBuilder.isNotEmpty()) {
                    cues.add(SubtitleCue(startTimeMs, endTimeMs, textBuilder.toString().trim()))
                    textBuilder.clear()
                }
                continue
            }

            if (line.contains("-->")) {
                val times = line.split("-->")
                if (times.size >= 2) {
                    startTimeMs = parseTimestamp(times[0].trim())
                    endTimeMs = parseTimestamp(times[1].trim())
                }
            } else if (!line.matches(Regex("^\\d+$"))) {
                // Text line (ignoring pure index number lines)
                val cleanLine = line.replace(Regex("<[^>]*>"), "")
                if (textBuilder.isNotEmpty()) textBuilder.append("\n")
                textBuilder.append(cleanLine)
            }
        }
        if (textBuilder.isNotEmpty()) {
            cues.add(SubtitleCue(startTimeMs, endTimeMs, textBuilder.toString().trim()))
        }
        return cues
    }

    private fun parseTimestamp(timestamp: String): Long {
        return try {
            val cleanTs = timestamp.replace(',', '.')
            val parts = cleanTs.split(":")
            var hours = 0L
            var minutes = 0L
            var seconds = 0L
            var millis = 0L

            when (parts.size) {
                3 -> {
                    hours = parts[0].toLong()
                    minutes = parts[1].toLong()
                    val secParts = parts[2].split(".")
                    seconds = secParts[0].toLong()
                    if (secParts.size > 1) {
                        millis = secParts[1].padEnd(3, '0').take(3).toLong()
                    }
                }
                2 -> {
                    minutes = parts[0].toLong()
                    val secParts = parts[1].split(".")
                    seconds = secParts[0].toLong()
                    if (secParts.size > 1) {
                        millis = secParts[1].padEnd(3, '0').take(3).toLong()
                    }
                }
                1 -> {
                    val secParts = parts[0].split(".")
                    seconds = secParts[0].toLong()
                    if (secParts.size > 1) {
                        millis = secParts[1].padEnd(3, '0').take(3).toLong()
                    }
                }
            }

            (hours * 3600 + minutes * 60 + seconds) * 1000 + millis
        } catch (e: Exception) {
            0L
        }
    }
}
