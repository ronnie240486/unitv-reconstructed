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
    fun dnsConfigAllowsAtMostFiveServers() {
        val config = DnsConfig(listOf("1.1.1.1", "8.8.8.8", "9.9.9.9", "208.67.222.222", "8.8.4.4"))
        assertTrue(config.servers.size <= 5)
    }
}
