package de.timklge.karooreminder.screens

import android.content.Context
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import de.timklge.karooreminder.R
import de.timklge.karooreminder.ReminderTrigger
import io.hammerhead.karooext.models.PlayBeepPattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class ReminderBeepPattern(val displayName: String, val tonesKaroo2: List<PlayBeepPattern.Tone>, val tonesKaroo3: List<PlayBeepPattern.Tone>) {
    NO_TONES("No tones", emptyList(), emptyList()),
    THREE_TONES_UP("Three tones up",
        listOf(PlayBeepPattern.Tone(4_000, 500), PlayBeepPattern.Tone(5_000, 500), PlayBeepPattern.Tone(6_000, 500)),
        listOf(PlayBeepPattern.Tone(2_000, 250), PlayBeepPattern.Tone(2_500, 250), PlayBeepPattern.Tone(3_000, 250))),
    THREE_TONES_DOWN("Three tones down",
        listOf(PlayBeepPattern.Tone(6_000, 500), PlayBeepPattern.Tone(5_000, 500), PlayBeepPattern.Tone(4_000, 500)),
        listOf(PlayBeepPattern.Tone(3_000, 250), PlayBeepPattern.Tone(2_500, 250), PlayBeepPattern.Tone(2_000, 250))),
    DOUBLE_HIGH("Double high", listOf(
        PlayBeepPattern.Tone(4_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(5_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(5_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(4_000, 500)),
        listOf(
            PlayBeepPattern.Tone(2_000, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_500, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_500, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_000, 250)
        )
    ),
    DOUBLE_LOW("Double low", listOf(
        PlayBeepPattern.Tone(5_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(4_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(4_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(5_000, 500)),
        listOf(
            PlayBeepPattern.Tone(2_500, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_000, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_000, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_500, 250)
        )
    ),
    LONG_SHORT_SHORT("Long short short", listOf(
        PlayBeepPattern.Tone(4_000, 1_000),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(4_000, 500),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(4_000, 500)),
        listOf(
            PlayBeepPattern.Tone(2_500, 500),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_500, 250),
            PlayBeepPattern.Tone(0, 100),
            PlayBeepPattern.Tone(2_500, 250)
        )
    ),
    FIVE_TONES_UP("Five tones up", listOf(
        PlayBeepPattern.Tone(3_000, 300),
        PlayBeepPattern.Tone(3_500, 300),
        PlayBeepPattern.Tone(4_000, 300),
        PlayBeepPattern.Tone(4_500, 300),
        PlayBeepPattern.Tone(5_000, 300)),
        listOf(
            PlayBeepPattern.Tone(2_000, 200),
            PlayBeepPattern.Tone(2_500, 200),
            PlayBeepPattern.Tone(3_000, 200),
            PlayBeepPattern.Tone(3_500, 200),
            PlayBeepPattern.Tone(4_000, 200)
        )
    ),

}

/**
 * Colors for reminders
 *
 * @param colorRes The color resource for the color
 * @param whiteFont Whether the font should be white or black
 * @param mapFromColors The colors that should be mapped to this color (only used for migration of old colors that were stored as integers)
 */
@Serializable
enum class ReminderColor(@ColorRes val colorRes: Int, val whiteFont: Boolean, val mapFromColors: Set<Int> = emptySet()) {
    DARK_RED(R.color.bRed, true),
    DARK_PURPLE(R.color.bPurple, true),
    DARK_YELLOW(R.color.bYellow, true),
    DARK_GREEN(R.color.bGreen, true),
    DARK_BLUE(R.color.bBlue, true),
    DARK_CYAN(R.color.bCyan, true),

    LIGHT_RED(R.color.hRed, false, setOf(android.graphics.Color.parseColor("#FF6060"))),
    LIGHT_PURPLE(R.color.hPurple, false, setOf(android.graphics.Color.parseColor("#FF70FF"))),
    LIGHT_YELLOW(R.color.hYellow, false, setOf(android.graphics.Color.parseColor("#FFFF60"))),
    LIGHT_GREEN(R.color.hGreen, false, setOf(android.graphics.Color.parseColor("#50FF50"))),
    LIGHT_BLUE(R.color.hBlue, false, setOf(android.graphics.Color.parseColor("#7070FF"))),
    LIGHT_CYAN(R.color.hCyan, false, setOf(android.graphics.Color.parseColor("#60FFFF")));

    @ColorRes
    fun getTextColor(): Int {
        return if(whiteFont) R.color.white else R.color.black
    }

    companion object {
        fun getColor(context: Context, color: Int): ReminderColor {
            return entries.find { reminderColor ->
                val mapFromColors = reminderColor.mapFromColors + ContextCompat.getColor(context, reminderColor.colorRes)

                mapFromColors.any { it == color }
            } ?: error("Unknown color resource")
        }
    }
}

@Serializable
class Reminder(val id: Int, var name: String, var interval: Int, var text: String,
               var displayForegroundColor: ReminderColor? = null,
               @Deprecated("Use displayForegroundColor instead")
               var foregroundColor: Int = android.graphics.Color.parseColor("#FF6060"),
               val isActive: Boolean = true, val isAutoDismiss: Boolean = true,
               val tone: ReminderBeepPattern = ReminderBeepPattern.THREE_TONES_UP,
               var trigger: ReminderTrigger = ReminderTrigger.ELAPSED_TIME,
               val autoDismissSeconds: Int = 15)

val defaultReminders = Json.encodeToString(listOf(Reminder(0, "Drink", 30, "Take a sip!")))