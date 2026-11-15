package swiss.reutener.boccia.app

import android.app.Application
import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.SeasonSummary
import swiss.reutener.boccia.model.SeasonsResponse
import swiss.reutener.boccia.model.TeamDetail
import swiss.reutener.boccia.net.ApiClient
import swiss.reutener.boccia.net.ApiException
import swiss.reutener.boccia.net.ResponseCache
import swiss.reutener.boccia.net.ServerEnvironment
import java.time.Instant

/**
 * App-wide state, like the iOS app's AppModel: which server and season are shown and the loaded
 * season data. One instance for the whole app; screens read it through `LocalAppModel`.
 * Its state is Compose state, so screens update by themselves when it changes.
 */
class AppModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val cache = ResponseCache(application.cacheDir)

    var server by mutableStateOf(ServerEnvironment.default)
        private set
    var seasons by mutableStateOf<List<SeasonSummary>>(emptyList())
        private set
    val details = mutableStateMapOf<Int, SeasonDetail>()
    var isLoading by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<ApiException?>(null)
        private set
    var lastUpdated by mutableStateOf<Instant?>(null)
        private set

    /** The season shown in all tabs; null until the season list has loaded. */
    var selectedSeasonId by mutableStateOf<Int?>(null)
        private set

    private val client get() = ApiClient(server.apiBaseUrl)
    private var refreshJob: Job? = null

    init {
        val saved = ServerEnvironment.fromKey(prefs.getString(KEY_SERVER, null))
        server = saved?.takeIf { it in ServerEnvironment.selectable } ?: ServerEnvironment.default
        selectedSeasonId = prefs.getInt(KEY_SELECTED_SEASON, -1).takeIf { it >= 0 }
        loadCache()
    }

    // MARK: Derived state

    /** The season the league plays right now. */
    val currentSeason: SeasonSummary? get() = seasons.firstOrNull { it.isCurrent }
    val selectedSeason: SeasonSummary? get() = seasons.firstOrNull { it.id == selectedSeasonId }
    val selectedDetail: SeasonDetail? get() = selectedSeasonId?.let { details[it] }

    fun selectSeason(id: Int) {
        if (id == selectedSeasonId) return
        selectedSeasonId = id
        prefs.edit { putInt(KEY_SELECTED_SEASON, id) }
        viewModelScope.launch { loadSelectedSeason() }
    }

    // MARK: Loading

    /** Reloads the season list and the selected season. Safe to call often (pull to refresh, timer, foreground). */
    fun refresh(): Job {
        refreshJob?.takeIf { it.isActive }?.let { return it }
        return viewModelScope.launch { refreshNow() }.also { refreshJob = it }
    }

    private suspend fun refreshNow() {
        isLoading = true
        try {
            val seasons = client.seasons()
            this.seasons = seasons
            cache.write(ApiClient.json.encodeToString(SeasonsResponse(seasons)), "seasons", server)
            if (selectedSeasonId == null || seasons.none { it.id == selectedSeasonId }) {
                (currentSeason ?: seasons.firstOrNull())?.let { selectSeason(it.id) }
            } else {
                loadSelectedSeason()
            }
            val current = currentSeason
            if (current != null && current.id != selectedSeasonId && details[current.id] == null) {
                runCatching { loadSeason(current.id) }
            }
            lastError = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: ApiException) {
            lastError = e
        } finally {
            isLoading = false
        }
    }

    private suspend fun loadSelectedSeason() {
        val id = selectedSeasonId ?: return
        try {
            loadSeason(id)
            lastError = null
        } catch (e: ApiException) {
            lastError = e
        }
    }

    suspend fun loadSeason(id: Int) {
        val (detail, body) = client.season(id)
        details[id] = detail
        lastUpdated = Instant.now()
        cache.write(body, "season-$id", server)
    }

    suspend fun teamDetail(id: Int): TeamDetail = client.team(id)

    private fun loadCache() {
        cache.read("seasons", server)?.let { body ->
            runCatching { ApiClient.json.decodeFromString<SeasonsResponse>(body) }.getOrNull()?.let {
                seasons = it.seasons.filter { season -> !season.isDemo }
            }
        }
        for (season in seasons) {
            cache.read("season-${season.id}", server)?.let { body ->
                runCatching { ApiClient.json.decodeFromString<SeasonDetail>(body) }.getOrNull()?.let { details[season.id] = it }
            }
        }
    }

    // MARK: Server

    fun switchServer(to: ServerEnvironment) {
        if (to == server) return
        refreshJob?.cancel()
        server = to
        prefs.edit { putString(KEY_SERVER, to.key) }
        seasons = emptyList()
        details.clear()
        lastError = null
        selectedSeasonId = null
        prefs.edit { remove(KEY_SELECTED_SEASON) }
        loadCache()
        refresh()
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_SELECTED_SEASON = "selectedSeason"
    }
}
