package swiss.reutener.boccia.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNamingStrategy
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.Serializable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import swiss.reutener.boccia.model.ApiErrorBody
import swiss.reutener.boccia.model.ApiInfo
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.SeasonDetail
import swiss.reutener.boccia.model.SeasonSummary
import swiss.reutener.boccia.model.SeasonsResponse
import swiss.reutener.boccia.model.Team
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

    suspend fun login(teamId: Int, pin: String, device: String): LoginResponse =
        decode(post("auth/login", body("team_id" to teamId, "pin" to pin, "device" to device)))

    suspend fun logout(token: String) {
        post("auth/logout", body(), token)
    }

    suspend fun me(token: String): Team = decode<TeamResponse>(get("me", token)).team

    /** Always sends date and time, as explicit nulls when cleared -- the API treats a missing key as "keep". */
    suspend fun submitResult(gameId: Int, setsA: Int, setsB: Int, playedDate: String?, gameTime: String?, token: String): Game =
        decode<GameResponse>(
            post(
                "games/$gameId/result",
                body("sets_a" to setsA, "sets_b" to setsB, "played_date" to playedDate, "game_time" to gameTime),
                token,
            ),
        ).game

    /** Throws an ApiException with code `schedule_conflict` (and the clashing games) unless `force` is set. */
    suspend fun schedule(gameId: Int, playedDate: String, gameTime: String?, force: Boolean, token: String): Game =
        decode<GameResponse>(
            post("games/$gameId/schedule", body("played_date" to playedDate, "game_time" to gameTime, "force" to force), token),
        ).game

    /** Replaces the team's photo; `jpeg` should already be square (the server crops to the center). */
    suspend fun uploadPhoto(teamId: Int, jpeg: ByteArray, token: String): Team {
        val body = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("photo", "photo.jpg", jpeg.toRequestBody("image/jpeg".toMediaType()))
            .build()
        return decode<TeamResponse>(send(request("teams/$teamId/photo", token).post(body).build(), upload = true)).team
    }

    // MARK: Plumbing

    private suspend fun get(path: String, token: String? = null): String = send(request(path, token).get().build())

    private suspend fun post(path: String, body: JsonObject, token: String? = null): String =
        send(request(path, token).post(body.toString().toRequestBody(JSON_TYPE)).build())

    /** A JSON object with explicit nulls (the shared `json` config leaves nulls out). */
    private fun body(vararg fields: Pair<String, Any?>) = JsonObject(
        fields.associate { (key, value) ->
            key to when (value) {
                null -> JsonNull
                is Number -> JsonPrimitive(value)
                is Boolean -> JsonPrimitive(value)
                else -> JsonPrimitive(value.toString())
            }
        },
    )

    private fun request(path: String, token: String? = null): Request.Builder =
        Request.Builder()
            .url(if (path.isEmpty()) baseUrl else "$baseUrl/$path")
            .header("Accept", "application/json")
            .apply {
                if (token != null) {
                    header("Authorization", "Bearer $token")
                    // Some shared-hosting PHP setups drop the Authorization header; the API accepts this too.
                    header("X-Api-Token", token)
                }
            }

    private suspend fun send(request: Request, upload: Boolean = false): String = withContext(Dispatchers.IO) {
        val response = try {
            (if (upload) uploadHttp else http).newCall(request).execute()
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

        private val JSON_TYPE = "application/json".toMediaType()

        private val http = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .build()

        /** A photo takes longer on a slow beach connection. */
        private val uploadHttp = http.newBuilder()
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }
}

@Serializable
data class LoginResponse(val token: String, val team: Team)

@Serializable
private data class TeamResponse(val team: Team)

@Serializable
private data class GameResponse(val game: Game)
