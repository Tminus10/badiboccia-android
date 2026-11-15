package swiss.reutener.boccia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Phase
import swiss.reutener.boccia.model.Placeholder
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.SwissTime
import swiss.reutener.boccia.model.Team
import swiss.reutener.boccia.model.TeamDetail
import swiss.reutener.boccia.model.allGames
import swiss.reutener.boccia.model.chronological
import swiss.reutener.boccia.model.hasScore
import swiss.reutener.boccia.model.initials
import swiss.reutener.boccia.model.involves
import swiss.reutener.boccia.model.isComplete
import swiss.reutener.boccia.model.isEstimated
import swiss.reutener.boccia.model.playersSubtitle
import swiss.reutener.boccia.model.shownSetsA
import swiss.reutener.boccia.model.shownSetsB
import swiss.reutener.boccia.net.ApiClient

/** Same checks as the iOS app's BocciaScoreTests, on the same fixtures (real API responses). */
class ModelTest {
    private inline fun <reified T> fixture(name: String): T {
        val text = javaClass.getResource("/fixtures/$name.json")!!.readText()
        return ApiClient.json.decodeFromString<T>(text)
    }

    @Test
    fun decodesCompleteSeason() {
        val detail = fixture<SeasonDetail>("season-complete")
        assertEquals(2018, detail.season.year)
        assertEquals(listOf("A", "B", "C"), detail.groups.map { it.name })
        assertEquals(4, detail.bracket.qf.size)
        assertEquals(2, detail.bracket.sf.size)
        assertNotNull(detail.bracket.final)
        assertNotNull(detail.podium?.championId)
        val teamIds = detail.teams.map { it.id }.toSet()
        for (game in detail.allGames) {
            listOfNotNull(game.teamAId, game.teamBId).forEach { assertTrue(it in teamIds) }
        }
    }

    @Test
    fun decodesBracketPlaceholders() {
        val detail = fixture<SeasonDetail>("season-unplayed-knockout")
        val sf1 = detail.bracket.sf.first()
        assertNull(sf1.teamAId)
        assertEquals(Placeholder(Placeholder.Kind.WINNER, phase = Phase.QF, slot = 1), sf1.placeholderA)
        assertEquals(Placeholder(Placeholder.Kind.WINNER, phase = Phase.QF, slot = 2), sf1.placeholderB)
        assertEquals(Placeholder.Kind.LOSER, detail.bracket.third?.placeholderA?.type)
        assertNull(detail.podium)
    }

    @Test
    fun decodesTeamDetail() {
        val detail = fixture<TeamDetail>("team")
        assertTrue(detail.enrollments.isNotEmpty())
        assertTrue(detail.games.all { it.involves(detail.team.id) })
    }

    private fun game(phase: Phase = Phase.GROUP, setsA: Int? = null, setsB: Int? = null, date: String? = null, time: String? = null, estimated: Boolean = false) =
        Game(
            id = 1, seasonId = 1, phase = phase, teamAId = 10, teamBId = 20, setsA = setsA, setsB = setsB,
            resultEstimated = estimated, playedDate = date, gameTime = time, slotMinutes = 90,
            winnerTeamId = if (setsA != null && setsB != null && setsA != setsB) (if (setsA > setsB) 10 else 20) else null,
        )

    @Test
    fun estimatedResultHidesItsPlaceholderScore() {
        val estimated = game(setsA = 0, setsB = 2, estimated = true)
        assertTrue(estimated.isEstimated)
        assertTrue(estimated.isComplete)
        assertFalse(estimated.hasScore)
        assertNull(estimated.shownSetsA)
        assertNull(estimated.shownSetsB)
        assertEquals(20, estimated.winnerTeamId)

        val plain = game(setsA = 2, setsB = 1)
        assertTrue(!plain.isEstimated && plain.hasScore)
        assertEquals(2, plain.shownSetsA)
        assertEquals(1, plain.shownSetsB)
    }

    @Test
    fun initialsMatchWebsite() {
        fun team(name: String) = Team(1, name, "a", "b", "#ffffff")
        assertEquals("DE", team("Debi & Erika").initials)
        assertEquals("KP", team("Kusi & Pädi").initials)
        assertEquals("ZI", team("Zimi").initials)
    }

    @Test
    fun playersSubtitleOnlyWhenDifferent() {
        assertNull(Team(1, "Debi & Erika", "Debi", "Erika", "#ffffff").playersSubtitle)
        assertEquals("Debi & Erika", Team(1, "Die Kugeln", "Debi", "Erika", "#ffffff").playersSubtitle)
    }

    @Test
    fun swissDatesUseSwissTime() {
        val start = SwissTime.dateTime("2026-07-04", "18:30")!!
        assertEquals("2026-07-04T18:30+02:00[Europe/Zurich]", start.toString())
        assertEquals(12, SwissTime.dateTime("2026-07-04")!!.hour)
    }

    @Test
    fun chronologicalOrderPutsUntimedGamesLast() {
        val early = game(date = "2026-07-04", time = "10:00")
        val untimed = game(date = "2026-07-04")
        val nextDay = game(date = "2026-07-05", time = "09:00")
        assertTrue(chronological.compare(early, untimed) < 0)
        assertTrue(chronological.compare(untimed, nextDay) < 0)
    }
}
