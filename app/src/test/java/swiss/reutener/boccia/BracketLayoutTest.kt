package swiss.reutener.boccia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import swiss.reutener.boccia.ui.bracket.BracketLayout
import kotlin.math.abs
import kotlin.math.ceil

/** Tournament tree geometry; same checks as the iOS app's BracketLayoutTests. */
class BracketLayoutTest {
    private val layout = BracketLayout(columnWidth = 185f, headerHeight = 28f, rowHeight = 38f)

    @Test
    fun laterRoundsCenterBetweenTheirFeeders() {
        // Also halfway through the animation from one packing to the next, for the rounds right of the focus.
        for (focus in listOf(0f, 0.5f, 1f)) {
            for (round in ceil(focus).toInt() until 2) {
                val games = 4 shr round
                for (pair in 0 until games / 2) {
                    val a = layout.centerY(round, 2 * pair, focus)
                    val b = layout.centerY(round, 2 * pair + 1, focus)
                    val next = layout.centerY(round + 1, pair, focus)
                    assertTrue(abs(next - (a + b) / 2) < 0.001f)
                }
            }
        }
    }

    @Test
    fun focusedRoundIsPackedTightly() {
        // Scrolled to the semi-finals: they sit directly below each other, like the quarter-finals at the start.
        val sfGap = layout.centerY(1, 1, 1f) - layout.centerY(1, 0, 1f)
        val qfGap = layout.centerY(0, 1, 0f) - layout.centerY(0, 0, 0f)
        assertEquals(layout.unit, sfGap, 0.001f)
        assertEquals(layout.unit, qfGap, 0.001f)
        assertEquals(0f, layout.topY(1, 0, 1f), 0.001f)
    }

    @Test
    fun championBadgeFitsAboveTheFinal() {
        for (focus in listOf(0f, 1f)) {
            val inset = layout.insetAboveFinal(110f, focus)
            assertTrue(inset >= 0)
            assertTrue(layout.topY(2, 0, focus) + inset >= 110f)
        }
        // From the quarter-finals there is room already, so nothing moves.
        assertEquals(0f, layout.insetAboveFinal(110f, 0f), 0f)
    }

    @Test
    fun phonesShowTwoColumnsLikeTheSportsApp() {
        assertEquals(2, BracketLayout.visibleColumns(412f, 4))
        assertEquals(3, BracketLayout.visibleColumns(834f, 4))
        assertEquals(4, BracketLayout.visibleColumns(1366f, 4))
    }
}
