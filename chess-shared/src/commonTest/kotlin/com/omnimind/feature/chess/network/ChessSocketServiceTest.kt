package com.creativeblisslabs.omnimind.feature.chess.network

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ChessSocketServiceTest {

    @Test
    fun testParseInviteLink_FullUrl() {
        val url = "https://omnimind.app/chess/invite?gameId=ABC-123&time=5%2B3&variant=CLASSIC&rated=true&color=white&host=Manpreet"
        val info = ChessSocketService.parseInviteLink(url)
        
        assertNotNull(info)
        assertEquals("ABC-123", info.gameId)
        assertEquals("5+3", info.timeFormat)
        assertEquals("CLASSIC", info.variant)
        assertEquals(true, info.isRated)
        assertEquals("white", info.colorChoice)
        assertEquals("Manpreet", info.hostName)
    }

    @Test
    fun testParseInviteLink_DeepLink() {
        val url = "omnimind://chess/invite?gameId=XYZ-789&time=10+0&variant=BLITZ&rated=false&color=black&host=Guest"
        val info = ChessSocketService.parseInviteLink(url)
        
        assertNotNull(info)
        assertEquals("XYZ-789", info.gameId)
        assertEquals("10+0", info.timeFormat)
        assertEquals("BLITZ", info.variant)
        assertEquals(false, info.isRated)
        assertEquals("black", info.colorChoice)
        assertEquals("Guest", info.hostName)
    }

    @Test
    fun testParseInviteLink_ShortCode() {
        val code = "aB12c3D4"
        val info = ChessSocketService.parseInviteLink(code)
        
        assertNotNull(info)
        assertEquals("OMNI_aB12c3D4", info.gameId)
        // Defaults
        assertEquals("5+3", info.timeFormat)
        assertEquals("CLASSIC", info.variant)
        assertEquals(false, info.isRated)
    }

    @Test
    fun testParseInviteLink_OmniGameId() {
        val gameId = "OMNI_12345678"
        val info = ChessSocketService.parseInviteLink(gameId)
        
        assertNotNull(info)
        assertEquals("OMNI_12345678", info.gameId)
    }

    @Test
    fun testParseInviteLink_Malformed() {
        assertNull(ChessSocketService.parseInviteLink(""))
        assertNull(ChessSocketService.parseInviteLink("   "))
    }
    
    @Test
    fun testParseInviteLink_PathOnly() {
        val url = "https://omnimind.app/chess/invite/SPECIAL_ID"
        val info = ChessSocketService.parseInviteLink(url)
        
        assertNotNull(info)
        assertEquals("SPECIAL_ID", info.gameId)
    }
}
