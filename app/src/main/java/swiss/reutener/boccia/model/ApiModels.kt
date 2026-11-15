package swiss.reutener.boccia.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Mirrors the JSON of the website's /api/v1 (badiboccia: src/Controllers/ApiController.php), like
// the iOS app's APIModels.swift. Decoded with snake_case naming (see ApiClient.json), so `team_a_id`
// arrives as `teamAId`. Fields newer than the first API version have defaults, so caches and servers
// from before them still decode.

@Serializable
data class SeasonSummary(
    val id: Int,
    val label: String,
    val year: Int,
    val isCurrent: Boolean,
    /** The demo season (badiboccia: scripts/seed-demo.php). */
    val isDemo: Boolean = false,
    /** Set up but not started yet (badiboccia: `Season::isUpcoming`), so not archived either. */
    val isUpcoming: Boolean = false,
)

@Serializable
data class Team(
    val id: Int,
    val name: String,
    val player1: String,
    val player2: String,
    val colorHex: String,
    val photoUrl: String? = null,
    /** The team's group in the season it was loaded for; null outside a season context. */
    val groupId: Int? = null,
    /** Plays only in the demo season. */
    val isDemo: Boolean = false,
)

@Serializable
enum class Phase {
    @SerialName("group") GROUP,
    @SerialName("qf") QF,
    @SerialName("sf") SF,
    @SerialName("final") FINAL,
    @SerialName("third") THIRD,
}

/** What will fill a bracket slot whose team isn't known yet, e.g. "winner of group A" or "winner of quarter-final 2". */
@Serializable
data class Placeholder(
    val type: Kind,
    val group: String? = null,
    val phase: Phase? = null,
    val slot: Int? = null,
) {
    @Serializable
    enum class Kind {
        @SerialName("group_winner") GROUP_WINNER,
        @SerialName("group_runner_up") GROUP_RUNNER_UP,
        @SerialName("winner") WINNER,
        @SerialName("loser") LOSER,
    }
}

@Serializable
data class Game(
    val id: Int,
    val seasonId: Int,
    val phase: Phase,
    val groupId: Int? = null,
    val groupName: String? = null,
    val slotIndex: Int? = null,
    val teamAId: Int? = null,
    val teamBId: Int? = null,
    val setsA: Int? = null,
    val setsB: Int? = null,
    /** Winner known but not the score: the sets are a placeholder 2:0, never show them (see `shownSetsA`). */
    val resultEstimated: Boolean = false,
    /** "yyyy-MM-dd", local Swiss date. */
    val playedDate: String? = null,
    /** "HH:mm", null means "all day" / no fixed time. */
    val gameTime: String? = null,
    val slotMinutes: Int,
    val winnerTeamId: Int? = null,
    val placeholderA: Placeholder? = null,
    val placeholderB: Placeholder? = null,
    val updatedAt: String? = null,
)

@Serializable
data class StandingRow(
    val teamId: Int,
    val rank: Int,
    val played: Int,
    val won: Int,
    val tied: Int,
    val lost: Int,
    val setsFor: Int,
    val setsAgainst: Int,
    val points: Int,
) {
    val setDifference: Int get() = setsFor - setsAgainst
}

@Serializable
data class TeamGroup(
    val id: Int,
    val name: String,
    val standings: List<StandingRow>,
    val games: List<Game>,
)

@Serializable
data class Bracket(
    val qf: List<Game>,
    val sf: List<Game>,
    val final: Game? = null,
    val third: Game? = null,
)

@Serializable
data class Podium(
    val championId: Int? = null,
    val runnerUpId: Int? = null,
    val thirdId: Int? = null,
)

@Serializable
data class CalendarEvent(
    val id: Int,
    val title: String,
    val location: String? = null,
    val date: String,
    val time: String? = null,
    val durationMinutes: Int? = null,
)

@Serializable
data class SeasonDetail(
    val season: SeasonSummary,
    val qualifiersPerGroup: Int,
    val teams: List<Team>,
    val groups: List<TeamGroup>,
    val bracket: Bracket,
    val podium: Podium? = null,
    val events: List<CalendarEvent> = emptyList(),
)

@Serializable
data class Enrollment(
    val seasonId: Int,
    val seasonLabel: String,
    val year: Int,
    val groupId: Int,
    val groupName: String,
)

@Serializable
data class TeamDetail(
    val team: Team,
    val enrollments: List<Enrollment>,
    val games: List<Game>,
    val opponents: List<Team>,
)

@Serializable
data class ApiInfo(
    val apiVersion: Int,
    val env: String,
    val currentSeasonId: Int? = null,
    /** Not part of /seasons; the app offers it separately as "Demo ausprobieren". */
    val demoSeasonId: Int? = null,
    /** Public: shown next to "Demo ausprobieren". */
    val demoPin: String? = null,
)

@Serializable
data class SeasonsResponse(val seasons: List<SeasonSummary>)

/** Every API error: `{"error": code, "message": German text}`; a schedule clash adds the conflicting games. */
@Serializable
data class ApiErrorBody(
    val error: String,
    val message: String? = null,
    val slotMinutes: Int? = null,
    val conflicts: List<Game>? = null,
)
