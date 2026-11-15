package swiss.reutener.boccia.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import okhttp3.OkHttpClient
import okhttp3.Request
import swiss.reutener.boccia.model.ApiErrorBody
import swiss.reutener.boccia.model.ApiInfo
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.SeasonSummary
import swiss.reutener.boccia.model.SeasonsResponse
import swiss.reutener.boccia.model.TeamDetail
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * An API failure. `code` is the API's error code (`wrong_pin`, `schedule_conflict`, ...) or one of
 * the client-side codes below; ui/Labels.kt turns it into a message.
 */
class ApiException(
    val code: String,
    val status: Int? = null,
    val serverMessage: String? = null,
    val conflicts: List<Game> = emptyList(),
) : Exception(serverMessage ?: code) {
    val isUnauthorized: Boolean get() = code == "unauthorized"

    companion object {
        const val NETWORK = "network"
        const val NOT_SUPPORTED = "api_missing"
        const val INVALID_RESPONSE = "invalid_response"
    }
}

/** Thin coroutine wrapper around the website's JSON API, like the iOS app's APIClient. */
class ApiClient(private val baseUrl: String) {

    suspend fun info(): ApiInfo = decode(get(""))

    suspend fun seasons(): List<SeasonSummary> = decode<SeasonsResponse>(get("seasons")).seasons

    /** The season plus the raw response, so it can be cached as-is. */
    suspend fun season(id: Int): Pair<SeasonDetail, String> {
        val body = get("seasons/$id")
        return decode<SeasonDetail>(body) to body
    }

    suspend fun team(id: Int): TeamDetail = decode(get("teams/$id"))

    // MARK: Plumbing

    private suspend fun get(path: String): String = send(request(path).get().build())

    private fun request(path: String): Request.Builder =
        Request.Builder()
            .url(if (path.isEmpty()) baseUrl else "$baseUrl/$path")
            .header("Accept", "application/json")

    private suspend fun send(request: Request): String = withContext(Dispatchers.IO) {
        val response = try {
            http.newCall(request).execute()
        } catch (e: IOException) {
            throw ApiException(ApiException.NETWORK, serverMessage = e.localizedMessage)
        }
        response.use {
            val body = it.body.string()
            val isJson = it.header("Content-Type")?.contains("json") == true
            if (it.isSuccessful && isJson) return@withContext body
            val error = if (isJson) runCatching { json.decodeFromString<ApiErrorBody>(body) }.getOrNull() else null
            if (error != null) {
                throw ApiException(error.error, it.code, error.message, error.conflicts.orEmpty())
            }
            // An HTML page instead of JSON: the website exists but has no API yet (not deployed there).
            throw ApiException(ApiException.NOT_SUPPORTED, it.code)
        }
    }

    private inline fun <reified T> decode(body: String): T =
        try {
            json.decodeFromString<T>(body)
        } catch (e: IllegalArgumentException) {
            throw ApiException(ApiException.INVALID_RESPONSE)
        }

    companion object {
        @OptIn(ExperimentalSerializationApi::class)
        val json = Json {
            namingStrategy = JsonNamingStrategy.SnakeCase
            ignoreUnknownKeys = true
            explicitNulls = false
            coerceInputValues = true
        }

        private val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()
    }
}
