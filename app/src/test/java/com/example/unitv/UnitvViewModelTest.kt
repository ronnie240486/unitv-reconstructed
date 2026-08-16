package com.example.unitv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UnitvViewModelTest {
    @Test
    fun filtersVodByTitle() {
        val viewModel = UnitvViewModel()
        viewModel.searchQuery = "aurora"
        assertEquals(listOf("Código da Aurora"), viewModel.filteredVod.map { it.title })
    }

    @Test
    fun selectingSectionChangesScreen() {
        val viewModel = UnitvViewModel()
        viewModel.selectSection(AppSection.SPORTS)
        assertEquals(AppScreen.SPORTS, viewModel.currentScreen)
    }

    @Test
    fun highlightsIsSeparateFromHome() {
        val viewModel = UnitvViewModel()
        viewModel.selectCategory("Destaques")
        assertEquals(AppScreen.HIGHLIGHTS, viewModel.currentScreen)
        viewModel.selectCategory("Home")
        assertEquals(AppScreen.HOME, viewModel.currentScreen)
    }

    @Test
    fun channelsCategoryOpensLiveScreen() {
        val viewModel = UnitvViewModel()
        viewModel.selectCategory("Canais")
        assertEquals(AppScreen.LIVE, viewModel.currentScreen)
    }

    @Test
    fun catalogStartsNotReady() {
        val viewModel = UnitvViewModel()
        assertTrue(!viewModel.catalogReady)
        assertTrue(!viewModel.catalogLoading)
    }

    @Test
    fun deviceIdentityAlwaysReturnsTwelveHexCharacters() {
        assertEquals("001122AABBCC", DeviceIdentity.normalize12("00:11:22:aa:bb:cc"))
        assertEquals("00:11:22:AA:BB:CC", DeviceIdentity.toMac("001122AABBCC"))
        assertEquals(12, DeviceIdentity.normalize12("abc").length)
    }

    @Test
    fun demoPlaylistContractReturnsAtMostFourLists() = kotlinx.coroutines.runBlocking {
        val playlists = DemoPlaylistRepository().fetchByDeviceId("001122AABBCC")
        assertEquals(4, playlists.size)
        assertTrue(playlists.all { it.name.isNotBlank() && it.url.isNotBlank() })
    }

    @Test
    fun dnsConfigAllowsAtMostFiveServers() {
        val config = DnsConfig(listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "208.67.222.222", "8.8.4.4"))
        assertTrue(config.servers.size <= 5)
    }
}
