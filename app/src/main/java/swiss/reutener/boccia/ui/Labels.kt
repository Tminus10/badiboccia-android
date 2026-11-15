package swiss.reutener.boccia.ui

import android.content.res.Resources
import android.text.format.DateFormat
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalResources
import swiss.reutener.boccia.R
import swiss.reutener.boccia.model.Game
import swiss.reutener.boccia.model.Phase
import swiss.reutener.boccia.model.Placeholder
import swiss.reutener.boccia.model.startDate
import swiss.reutener.boccia.net.ApiException
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

// Texts derived from the model, in the app's language, like the String(localized:) parts of the
// iOS app's Domain.swift. Composables get the resources with `res()`.

@Composable
fun res(): Resources = LocalResources.current

private val Resources.locale: Locale get() = configuration.locales[0]

fun Phase.title(res: Resources): String = res.getString(
    when (this) {
        Phase.GROUP -> R.string.phase_group
        Phase.QF -> R.string.phase_qf
        Phase.SF -> R.string.phase_sf
        Phase.FINAL -> R.string.phase_final
        Phase.THIRD -> R.string.phase_third
    },
)

/** "Gruppe A", "Viertelfinal 2", "Final", ... */
fun Game.title(res: Resources): String = when (phase) {
    Phase.GROUP -> groupName?.let { res.getString(R.string.group_name, it) } ?: phase.title(res)
    Phase.QF, Phase.SF -> slotIndex?.let { "${phase.title(res)} $it" } ?: phase.title(res)
    Phase.FINAL, Phase.THIRD -> phase.title(res)
}

/** "Sieger Gruppe A", "Zweiter Gruppe B", "Sieger Viertelfinal 1", "Verlierer Halbfinal 2". */
fun Placeholder.label(res: Resources): String = when (type) {
    Placeholder.Kind.GROUP_WINNER -> res.getString(R.string.placeholder_group_winner, group ?: "?")
    Placeholder.Kind.GROUP_RUNNER_UP -> res.getString(R.string.placeholder_group_runner_up, group ?: "?")
    Placeholder.Kind.WINNER -> res.getString(R.string.placeholder_winner, phase?.title(res) ?: "", slot ?: 0)
    Placeholder.Kind.LOSER -> res.getString(R.string.placeholder_loser, phase?.title(res) ?: "", slot ?: 0)
}

/** A date in Swiss time, in the device language's usual form for these fields (an ICU skeleton like "EEEMMMMd"). */
fun ZonedDateTime.format(res: Resources, skeleton: String): String {
    val locale = res.locale
    return format(DateTimeFormatter.ofPattern(DateFormat.getBestDateTimePattern(locale, skeleton), locale))
}

/** "18:00", always 24 hours, like everything else at the league. */
fun ZonedDateTime.timeText(): String = format(DateTimeFormatter.ofPattern("HH:mm"))

/** "Sa., 4. Juli · 18:00", "Sa., 4. Juli" or null when no date is set. */
fun Game.dateLabel(res: Resources): String? {
    val start = startDate ?: return null
    val day = start.format(res, "EEEMMMMd")
    return if (gameTime == null) day else "$day · ${start.timeText()}"
}

/** "Samstag, 4. Juli 2026" */
fun ZonedDateTime.longDate(res: Resources): String = format(res, "EEEEMMMMdy")

fun ApiException.message(res: Resources): String = when (code) {
    "wrong_pin" -> res.getString(R.string.error_wrong_pin)
    "locked_out" -> res.getString(R.string.error_locked_out)
    "unauthorized" -> res.getString(R.string.error_unauthorized)
    "season_archived" -> res.getString(R.string.error_season_archived)
    "not_participant" -> res.getString(R.string.error_not_participant)
    "opponent_unknown" -> res.getString(R.string.error_opponent_unknown)
    "invalid_score" -> res.getString(R.string.error_invalid_score)
    "draw_not_allowed" -> res.getString(R.string.error_draw_not_allowed)
    "invalid_date" -> res.getString(R.string.error_invalid_date)
    "invalid_time" -> res.getString(R.string.error_invalid_time)
    "not_found" -> res.getString(R.string.error_not_found)
    "no_current_season" -> res.getString(R.string.error_no_current_season)
    "not_own_team" -> res.getString(R.string.error_not_own_team)
    "invalid_photo" -> res.getString(R.string.error_invalid_photo)
    "schedule_conflict" -> res.getString(R.string.error_schedule_conflict)
    ApiException.NETWORK -> res.getString(R.string.error_network)
    ApiException.NOT_SUPPORTED -> res.getString(R.string.error_api_missing)
    else -> serverMessage ?: res.getString(R.string.error_unexpected)
}
