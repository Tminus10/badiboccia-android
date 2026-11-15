package swiss.reutener.boccia.ui.bracket

import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow

/**
 * Geometry of the tournament tree, Sports-app style (same as the iOS app's BracketLayout.swift):
 * the leftmost visible knockout round is packed tightly, and every later round centers each game
 * between the two games feeding it. Scrolling to a later round re-packs from there, so the
 * bracket never wastes vertical space.
 *
 * All values are in dp. `focusRound` may be fractional while the bracket animates from one
 * packing to the next.
 */
data class BracketLayout(
    /** Width of one column, including the gap on its right where the connector lines run. */
    val columnWidth: Float,
    val headerHeight: Float,
    val rowHeight: Float,
    val connectorGap: Float = 18f,
    val spacing: Float = 14f,
) {
    val cardWidth: Float get() = max(columnWidth - connectorGap, 0f)

    /** Date header + two team rows + the hairline between them. */
    val cardHeight: Float get() = headerHeight + 2 * rowHeight + 1
    val unit: Float get() = cardHeight + spacing

    /** Vertical distance between two neighboring games of `round` (0 = QF, 1 = SF, 2 = final). */
    fun step(round: Int, focusRound: Float): Float = unit * 2f.pow(max(round - focusRound, 0f))

    /** Center y of game `index` in `round`. */
    fun centerY(round: Int, index: Int, focusRound: Float): Float {
        val step = step(round, focusRound)
        return cardHeight / 2 + (step - unit) / 2 + index * step
    }

    fun topY(round: Int, index: Int, focusRound: Float): Float = centerY(round, index, focusRound) - cardHeight / 2

    /** Height taken by `count` games of `round`. */
    fun height(round: Int, count: Int, focusRound: Float): Float {
        if (count <= 0) return cardHeight
        return centerY(round, count - 1, focusRound) + cardHeight / 2
    }

    /**
     * Extra space the knockout columns need on top so a block of `height` (the champion badge) fits
     * above the final; zero when the gap above the final is already big enough.
     */
    fun insetAboveFinal(height: Float, focusRound: Float): Float = max(height - topY(2, 0, focusRound), 0f)

    companion object {
        /** How many columns fit side by side: two on phones, like the Sports app; more on tablets. */
        fun visibleColumns(width: Float, total: Int): Int {
            val fitting = if (width >= 1000) 4 else if (width >= 680) 3 else 2
            return min(fitting, total)
        }
    }
}
