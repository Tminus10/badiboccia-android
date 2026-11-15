package swiss.reutener.boccia.app

import android.app.Application
import android.content.Context
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.edit
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Score
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.SeasonSummary
import swiss.reutener.boccia.model.SeasonsResponse
import swiss.reutener.boccia.model.TeamDetail
import swiss.reutener.boccia.model.hasBothTeams
import swiss.reutener.boccia.model.involves
import swiss.reutener.boccia.net.ApiClient
import swiss.reutener.boccia.net.ApiException
import swiss.reutener.boccia.net.ResponseCache
import swiss.reutener.boccia.net.ServerEnvironment
import swiss.reutener.boccia.net.SessionStore
import swiss.reutener.boccia.net.TeamSession
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** The server's demo season and its public PIN ("Demo ausprobieren"). */
data class DemoInfo(val seasonId: Int, val pin: String)

/**
 * App-wide state, like the iOS app's AppModel: which server and season are shown, the loaded
 * season data, and the team login; it also does all API calls. One instance for the whole app;
 * screens read it through `LocalAppModel`. Its state is Compose state, so screens update by
 * themselves when it changes.
 */
class AppModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("settings", Context.MODE_PRIVATE)
    private val cache = ResponseCache(application.cacheDir)
    private val sessions = SessionStore(application)

    var server by mutableStateOf(ServerEnvironment.default)
        private set
    var seasons by mutableStateOf<List<SeasonSummary>>(emptyList())
        private set
    val details = mutableStateMapOf<Int, SeasonDetail>()
    var session by mutableStateOf<TeamSession?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var lastError by mutableStateOf<ApiException?>(null)
        private set
    var lastUpdated by mutableStateOf<Instant?>(null)
        private set

    /** The server's demo season and its public PIN; null while unknown or if there's none. */
    var demo by mutableStateOf<DemoInfo?>(null)
        private set

    /** The season shown in all tabs; null until the season list has loaded. */
    var selectedSeasonId by mutableStateOf<Int?>(null)
        private set

    private val client get() = ApiClient(server.apiBaseUrl)
    private var refreshJob: Job? = null

    init {
        val saved = ServerEnvironment.fromKey(prefs.getString(KEY_SERVER, null))
        server = saved?.takeIf { it in ServerEnvironment.selectable } ?: ServerEnvironment.default
        session = sessions.load(server)
        selectedSeasonId = prefs.getInt(KEY_SELECTED_SEASON, -1).takeIf { it >= 0 }
        loadCache()
    }

    // MARK: Derived state

    /**
     * A demo team plays in the demo season, which isn't part of the league's season list: while
     * one is logged in, `seasons` also holds the demo season and it stands in for the current one.
     */
    val isDemoSession: Boolean get() = session?.team?.isDemo == true

    /** The season the user plays in: the running season, or the demo season in a demo session. */
    val currentSeason: SeasonSummary?
        get() = if (isDemoSession) seasons.firstOrNull { it.isDemo } else seasons.firstOrNull { it.isCurrent }

    val selectedSeason: SeasonSummary? get() = seasons.firstOrNull { it.id == selectedSeasonId }
    val selectedDetail: SeasonDetail? get() = selectedSeasonId?.let { details[it] }
    val currentDetail: SeasonDetail? get() = currentSeason?.let { details[it.id] }

    /** Whether the logged-in team may enter a result or a date for this game (same rules as the website). */
    fun canEdit(game: Game): Boolean {
        val session = session ?: return false
        if (!game.hasBothTeams || !game.involves(session.team.id)) return false
        val season = seasons.firstOrNull { it.id == game.seasonId } ?: return false
        return season.isCurrent || season.isDemo
    }

    fun selectSeason(id: Int?) {
        if (id == selectedSeasonId) return
        selectedSeasonId = id
        prefs.edit { if (id != null) putInt(KEY_SELECTED_SEASON, id) else remove(KEY_SELECTED_SEASON) }
        if (id != null) viewModelScope.launch { loadSelectedSeason() }
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
            val client = client
            val info = viewModelScope.async { runCatching { client.info() }.getOrNull() }
            val seasons = client.seasons().toMutableList()
            info.await()?.let { demo = it.demoSeasonId?.let { id -> DemoInfo(id, it.demoPin.orEmpty()) } }
            val demo = demo
            if (isDemoSession && demo != null) {
                runCatching { loadSeason(demo.seasonId) }
            }
            // Checked again after loading: the team may have logged out in the meantime.
            val demoSummary = demo?.let { details[it.seasonId]?.season }
            if (isDemoSession && demoSummary != null) {
                seasons.add(0, demoSummary)
            }
            this.seasons = seasons
            cache.write(ApiClient.json.encodeToString(SeasonsResponse(seasons)), "seasons", server)
            if (selectedSeasonId == null || seasons.none { it.id == selectedSeasonId }) {
                selectSeason((currentSeason ?: seasons.firstOrNull())?.id)
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

    /** Loads the demo season's teams for "Demo ausprobieren". */
    suspend fun loadDemoSeason() {
        val demo = demo ?: return
        loadSeason(demo.seasonId)
    }

    private fun loadCache() {
        cache.read("seasons", server)?.let { body ->
            runCatching { ApiClient.json.decodeFromString<SeasonsResponse>(body) }.getOrNull()?.let {
                seasons = if (isDemoSession) it.seasons else it.seasons.filter { season -> !season.isDemo }
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
        demo = null
        session = sessions.load(to)
        selectSeason(null)
        loadCache()
        refresh()
    }

    // MARK: Team login

    suspend fun login(teamId: Int, pin: String) {
        val response = client.login(teamId, pin, deviceName())
        val session = TeamSession(response.token, response.team)
        this.session = session
        sessions.save(session, server)
        if (isDemoSession) {
            refresh().join()
            selectSeason(currentSeason?.id)
        }
    }

    fun logout() {
        val token = session?.token
        endSession()
        if (token != null) {
            val client = client
            viewModelScope.launch { runCatching { client.logout(token) } }
        }
    }

    /** Checks the stored token is still valid (it's revoked when an admin resets the team's PIN). */
    suspend fun validateSession() {
        val token = session?.token ?: return
        try {
            val updated = TeamSession(token, client.me(token))
            if (updated != session) {
                session = updated
                sessions.save(updated, server)
            }
        } catch (e: ApiException) {
            if (e.isUnauthorized) endSession()
        }
    }

    private fun endSession() {
        val wasDemo = isDemoSession
        session = null
        sessions.save(null, server)
        if (wasDemo) {
            seasons = seasons.filter { !it.isDemo }
            if (selectedSeason == null) selectSeason((currentSeason ?: seasons.firstOrNull())?.id)
        }
    }

    /** The name the website's change log shows next to the team, like "Pixel 9". */
    private fun deviceName(): String =
        Settings.Global.getString(getApplication<Application>().contentResolver, Settings.Global.DEVICE_NAME)
            ?: "${Build.MANUFACTURER} ${Build.MODEL}"

    // MARK: Result entry

    suspend fun submitResult(game: Game, score: Score, date: LocalDate?, time: LocalTime?) {
        authorized { token ->
            client.submitResult(game.id, score.a, score.b, date?.toString(), time?.format(TIME), token)
        }
        runCatching { loadSeason(game.seasonId) }
    }

    /** Throws an ApiException with code `schedule_conflict` (and the clashing games) unless `force` is set. */
    suspend fun schedule(game: Game, date: LocalDate, time: LocalTime?, force: Boolean) {
        authorized { token -> client.schedule(game.id, date.toString(), time?.format(TIME), force, token) }
        runCatching { loadSeason(game.seasonId) }
    }

    /** Runs a call with the session token; a rejected token logs the team out. */
    private suspend fun <T> authorized(call: suspend (String) -> T): T {
        val token = session?.token ?: throw ApiException("unauthorized", 401)
        try {
            return call(token)
        } catch (e: ApiException) {
            if (e.isUnauthorized) endSession()
            throw e
        }
    }

    private companion object {
        const val KEY_SERVER = "server"
        const val KEY_SELECTED_SEASON = "selectedSeason"
        val TIME: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
