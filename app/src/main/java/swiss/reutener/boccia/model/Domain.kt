package swiss.reutener.boccia.model

import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

// Derived logic, like the iOS app's Domain.swift. Texts shown to the user (game titles,
// placeholders, dates) are in ui/Labels.kt, since they need the app's string resources.

/**
 * The league plays at a Swiss beach; dates and times from the API are local Swiss time and are
 * always shown that way, whatever time zone the device is in.
 */
object SwissTime {
    val zone: ZoneId = ZoneId.of("Europe/Zurich")

    /** "yyyy-MM-dd" of today in Switzerland. */
    val today: String get() = LocalDate.now(zone).toString()

    /** The day and time as a moment in Swiss time; midday if only the date is known. */
    fun dateTime(day: String, time: String? = null): ZonedDateTime? {
        val date = runCatching { LocalDate.parse(day) }.getOrNull() ?: return null
        val clock = time?.let { runCatching { LocalTime.parse(it.take(5)) }.getOrNull() } ?: LocalTime.NOON
        return ZonedDateTime.of(date, clock, zone)
    }
}

// MARK: Game

val Game.isComplete: Boolean get() = setsA != null && setsB != null
val Game.hasBothTeams: Boolean get() = teamAId != null && teamBId != null
val Game.isDraw: Boolean get() = isComplete && setsA == setsB
val Game.isEstimated: Boolean get() = resultEstimated

/** A real score is known; an estimated game has a winner but still lacks its result. */
val Game.hasScore: Boolean get() = isComplete && !isEstimated
val Game.shownSetsA: Int? get() = if (isEstimated) null else setsA
val Game.shownSetsB: Int? get() = if (isEstimated) null else setsB

/** Start of the game in Swiss time; midday if only the date is known. */
val Game.startDate: ZonedDateTime? get() = playedDate?.let { SwissTime.dateTime(it, gameTime) }

fun Game.involves(teamId: Int): Boolean = teamAId == teamId || teamBId == teamId

fun Game.opponent(teamId: Int): Int? = when (teamId) {
    teamAId -> teamBId
    teamBId -> teamAId
    else -> null
}

/** Sort order for lists of games: by date, timed games before untimed ones on the same day, undated games last. */
val chronological: Comparator<Game> = compareBy<Game>({ it.playedDate ?: "9999" }, { it.gameTime ?: "99" }, { it.id })

// MARK: Season

/** Same rule as `Season::isEditable` on the website: anything still open in a finished season stays open. */
val SeasonSummary.isRunning: Boolean get() = isCurrent || isDemo

val SeasonDetail.teamsById: Map<Int, Team> get() = teams.associateBy { it.id }
val SeasonDetail.groupGames: List<Game> get() = groups.flatMap { it.games }
val SeasonDetail.bracketGames: List<Game> get() = bracket.qf + bracket.sf + listOfNotNull(bracket.final, bracket.third)
val SeasonDetail.allGames: List<Game> get() = groupGames + bracketGames

/** The game with this id, wherever it is in the season. */
fun SeasonDetail.game(id: Int): Game? = allGames.firstOrNull { it.id == id }

/** Whether any group game has a result yet -- before that, standings are all-tied and alphabetical. */
val SeasonDetail.groupPhaseStarted: Boolean get() = groupGames.any { it.isComplete }

// MARK: Team

/** "Debi & Erika" unless the team name already is exactly that. */
val Team.playersSubtitle: String?
    get() {
        val players = "${player1.trim()} & ${player2.trim()}"
        return if (players.equals(name.trim(), ignoreCase = true)) null else players
    }

/** Same rule as the website's initials(): first letters of the first and last word, or the first two letters of a single word. */
val Team.initials: String
    get() {
        val parts = name.replace("&", " ").split(Regex("\\s+")).filter { it.isNotEmpty() }
        val first = parts.firstOrNull() ?: return "?"
        if (parts.size == 1) return first.take(2).uppercase()
        return (first.take(1) + parts.last().take(1)).uppercase()
    }
