package swiss.reutener.boccia.net

import swiss.reutener.boccia.BuildConfig
import swiss.reutener.boccia.R

/** Which BadiBoccia website the app talks to. */
enum class ServerEnvironment(val key: String, val websiteUrl: String) {
    PRODUCTION("production", "https://boccia.reutener.swiss"),
    DEVELOPMENT("development", "https://dev.boccia.reutener.swiss"),

    // The website's ./dev/serve.sh on this Mac, as the Android emulator sees it; not reachable from a phone.
    LOCAL("local", "http://10.0.2.2:8090");

    val apiBaseUrl: String get() = "$websiteUrl/api/v1"

    /** A website page in the app's language: the website switches (and stays) there via `?lang=`. */
    fun websitePage(path: String = "", language: String): String =
        (if (path.isEmpty()) websiteUrl else "$websiteUrl/$path") + "?lang=$language"

    val title: Int
        get() = when (this) {
            PRODUCTION -> R.string.server_production
            DEVELOPMENT -> R.string.server_development
            LOCAL -> R.string.server_local
        }

    /** Short tag shown in the app while it isn't on the live site (ServerIndicator); null for production. */
    val badge: String?
        get() = when (this) {
            PRODUCTION -> null
            DEVELOPMENT -> "DEV"
            LOCAL -> "LOCAL"
        }

    companion object {
        /** Release builds only offer the live site; debug builds can switch to DEV or the local dev server. */
        val selectable: List<ServerEnvironment>
            get() = if (BuildConfig.DEBUG) entries else listOf(PRODUCTION)

        val default: ServerEnvironment
            get() = if (BuildConfig.DEBUG) DEVELOPMENT else PRODUCTION

        fun fromKey(key: String?): ServerEnvironment? = entries.firstOrNull { it.key == key }
    }
}
