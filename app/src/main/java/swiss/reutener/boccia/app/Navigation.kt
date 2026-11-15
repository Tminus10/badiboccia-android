package swiss.reutener.boccia.app

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable

/** The app model, provided once at the top (see MainActivity). */
val LocalAppModel = staticCompositionLocalOf<AppModel> { error("No AppModel provided") }

/** Opens screens from anywhere, like the iOS app's Navigator. */
interface Navigator {
    fun openGame(id: Int) = open(GameRoute(id))
    fun openTeam(id: Int, seasonId: Int?) = open(TeamRoute(id, seasonId))

    /** Pushes one of the routes below over the tabs. */
    fun open(route: Any)
    fun back()
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator provided") }

// Screens pushed over the tabs.

@Serializable
data object TabsRoute

@Serializable
data class GameRoute(val id: Int)

@Serializable
data class TeamRoute(val id: Int, val seasonId: Int? = null)

@Serializable
data class ResultEntryRoute(val gameId: Int)

@Serializable
data class ScheduleGameRoute(val gameId: Int)

/** "Demo ausprobieren": the demo season's teams. */
@Serializable
data object DemoTeamsRoute

@Serializable
data object SettingsRoute

enum class AppTab { OVERVIEW, BRACKET, SCHEDULE, TEAM }
