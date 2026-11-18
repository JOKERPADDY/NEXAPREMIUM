package com.nexapremium.data.provider

import com.google.gson.Gson
import com.nexapremium.data.api.model.CDNChannelsResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CdnLiveTvProviderTest {
    @Test
    fun testParseChannelsResponseAndStatus() {
        val json = """
            {
              "total_channels": 2,
              "channels": [
                {
                  "name": "Live Channel",
                  "code": "live1",
                  "url": "https://example.com/live.m3u8",
                  "image": "https://example.com/logo.jpg",
                  "status": "online",
                  "viewers": 150
                },
                {
                  "name": "Offline Channel",
                  "code": "offline1",
                  "url": "https://example.com/offline.m3u8",
                  "image": "https://example.com/logo2.jpg",
                  "status": "offline",
                  "viewers": 0
                }
              ]
            }
        """.trimIndent()

        val response = Gson().fromJson(json, CDNChannelsResponse::class.java)
        assertEquals(2, response.channels.size)
        
        val liveChannel = response.channels[0]
        assertEquals("online", liveChannel.status)
        val isLive1 = liveChannel.status.lowercase() in setOf("online", "live", "active", "1")
        assertTrue(isLive1)

        val offlineChannel = response.channels[1]
        assertEquals("offline", offlineChannel.status)
        val isLive2 = offlineChannel.status.lowercase() in setOf("online", "live", "active", "1")
        assertTrue(!isLive2)
    }
}
