package swiss.reutener.boccia.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.SportsBaseball
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.EmojiEvents
import androidx.compose.material.icons.outlined.SportsBaseball
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import kotlinx.coroutines.delay
import swiss.reutener.boccia.R
import swiss.reutener.boccia.ui.bracket.BracketScreen
import swiss.reutener.boccia.ui.components.ServerIndicatorLine
import swiss.reutener.boccia.ui.game.GameDetailScreen
import swiss.reutener.boccia.ui.overview.OverviewScreen
import swiss.reutener.boccia.ui.schedule.ScheduleScreen
import swiss.reutener.boccia.ui.team.TeamDetailScreen
import swiss.reutener.boccia.ui.theme.Theme
import swiss.reutener.boccia.ui.theme.ThemedScreen

/** The tabs, plus the game and team pages pushed over them. */
@Composable
fun BocciaApp() {
    val model = LocalAppModel.current
    val navController = rememberNavController()
    val navigator = remember(navController) {
        object : Navigator {
            override fun openGame(id: Int) = navController.navigate(GameRoute(id))
            override fun openTeam(id: Int, seasonId: Int?) = navController.navigate(TeamRoute(id, seasonId))
        }
    }

    // Keep scores fresh while the app is open: results come in from other teams' phones.
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            while (true) {
                model.refresh().join()
                delay(60_000)
            }
        }
    }

    CompositionLocalProvider(LocalNavigator provides navigator) {
        ThemedScreen {
            NavHost(navController, startDestination = TabsRoute) {
                composable<TabsRoute> { Tabs() }
                composable<GameRoute> { entry ->
                    GameDetailScreen(entry.toRoute<GameRoute>().id, onBack = { navController.popBackStack() })
                }
                composable<TeamRoute> { entry ->
                    val route = entry.toRoute<TeamRoute>()
                    TeamDetailScreen(route.id, route.seasonId, onBack = { navController.popBackStack() })
                }
            }
            ServerIndicatorLine()
        }
    }
}

@Composable
private fun Tabs() {
    var tab by rememberSaveable { mutableStateOf(AppTab.OVERVIEW) }
    // Each tab keeps its scroll position while another one is shown.
    val states = rememberSaveableStateHolder()
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            NavigationBar(containerColor = Theme.backgroundBottom.copy(alpha = 0.94f)) {
                for (item in AppTab.entries) {
                    val (label, selectedIcon, icon) = item.appearance()
                    NavigationBarItem(
                        selected = tab == item,
                        onClick = { tab = item },
                        icon = { Icon(if (tab == item) selectedIcon else icon, null) },
                        label = { Text(stringResource(label)) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Theme.qualifier,
                            selectedTextColor = Theme.qualifier,
                            unselectedIconColor = Color.White,
                            unselectedTextColor = Color.White,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box {
            states.SaveableStateProvider(tab.name) {
                TabContent(tab, padding)
            }
        }
    }
}

@Composable
private fun TabContent(tab: AppTab, padding: PaddingValues) {
    when (tab) {
        AppTab.OVERVIEW -> OverviewScreen(padding)
        AppTab.BRACKET -> BracketScreen(padding)
        AppTab.SCHEDULE -> ScheduleScreen(padding)
    }
}

private data class TabAppearance(val label: Int, val selectedIcon: ImageVector, val icon: ImageVector)

private fun AppTab.appearance() = when (this) {
    AppTab.OVERVIEW -> TabAppearance(R.string.tab_overview, Icons.Filled.SportsBaseball, Icons.Outlined.SportsBaseball)
    AppTab.BRACKET -> TabAppearance(R.string.tab_bracket, Icons.Filled.EmojiEvents, Icons.Outlined.EmojiEvents)
    AppTab.SCHEDULE -> TabAppearance(R.string.tab_schedule, Icons.Filled.CalendarMonth, Icons.Outlined.CalendarMonth)
}
