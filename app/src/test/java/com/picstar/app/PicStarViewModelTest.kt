package com.picstar.app

import com.picstar.app.api.PixEzApiClient
import com.picstar.app.data.PicStarViewModel
import com.picstar.app.model.ArtistCard
import com.picstar.app.model.RankingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PicStarViewModelTest {

    @Test
    fun initialState_requiresLoginAndHasNoMockData() {
        val vm = PicStarViewModel()
        assertFalse(vm.userSettings.isPixivConnected)
        assertTrue(vm.highlights.isEmpty())
        assertTrue(vm.artists.isEmpty())
        assertTrue(vm.recommendedIllusts.isEmpty())
        assertTrue(vm.rankingIllusts.isEmpty())
        assertTrue(vm.favoriteIllusts.isEmpty())
    }

    @Test
    fun artistCarousel_isLimitedTo10() {
        val vm = PicStarViewModel()
        for (i in 1..15) {
            vm.artists.add(ArtistCard(id = i.toLong(), name = "Artist $i", handle = "@a$i"))
        }
        assertEquals(10, vm.getCarouselArtistsLimitedTo10().size)
    }

    @Test
    fun popularRankings_containsAll12PixivModes() {
        val modes = RankingMode.entries.map { it.displayName }
        val expected = listOf(
            "Daily", "For Male", "For Female", "Original", "Rookie",
            "Weekly", "Monthly", "AI", "XVIII AI", "XVIII", "XVIII weekly", "XVIII G"
        )
        assertEquals(expected, modes)
    }

    @Test
    fun pkceHeaders_generateValidClientHash() {
        val headers = PixEzApiClient.generateClientHeaders()
        assertNotNull(headers["X-Client-Time"])
        assertEquals(32, headers["X-Client-Hash"]?.length)
    }
}
