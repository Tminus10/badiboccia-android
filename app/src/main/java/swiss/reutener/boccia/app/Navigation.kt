package swiss.reutener.boccia.app

import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.serialization.Serializable

/** The app model, provided once at the top (see MainActivity). */
val LocalAppModel = staticCompositionLocalOf<AppModel> { error("No AppModel provided") }

/** Opens screens from anywhere, like the iOS app's Navigator: a game, or a team's page. */
interface Navigator {
    fun openGame(id: Int)
    fun openTeam(id: Int, seasonId: Int?)
}

val LocalNavigator = staticCompositionLocalOf<Navigator> { error("No Navigator provided") }

/** Screens pushed over the tabs. */
@Serializable
data class GameRoute(val id: Int)

@Serializable
data class TeamRoute(val id: Int, val seasonId: Int? = null)

@Serializable
data object TabsRoute

enum class AppTab { OVERVIEW, BRACKET, SCHEDULE }
