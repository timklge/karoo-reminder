package de.timklge.karooreminder.screens

import android.content.Context
import androidx.core.content.ContextCompat
import de.timklge.karooreminder.R
import de.timklge.karooreminder.ReminderTrigger
import io.hammerhead.karooext.models.PlayBeepPattern
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
enum class ReminderBeepPattern(val displayName: String, val tones: List<PlayBeepPattern.Tone>) {
    NO_TONES("No tones", emptyList()),
    THREE_TONES_UP("Three tones up", listOf(PlayBeepPattern.Tone(2_000, 200), PlayBeepPattern.Tone(2_500, 200), PlayBeepPattern.Tone(3_000, 200))),
    THREE_TONES_DOWN("Three tones down", listOf(PlayBeepPattern.Tone(3_000, 200), PlayBeepPattern.Tone(2_500, 200), PlayBeepPattern.Tone(2_000, 200))),
    DOUBLE_HIGH("Double high", listOf(
        PlayBeepPattern.Tone(2_200, 400),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(2_800, 200),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(2_800, 200),
        PlayBeepPattern.Tone(0, 200),
        PlayBeepPattern.Tone(2_200, 400))
    )
}

@Serializable
class Reminder(val id: Int, var name: String, var interval: Int, var text: String,
               var foregroundColor: Int = android.graphics.Color.parseColor("#FF6060"),
               val isActive: Boolean = true, val isAutoDismiss: Boolean = true,
               val tone: ReminderBeepPattern = ReminderBeepPattern.THREE_TONES_UP,
               var trigger: ReminderTrigger = ReminderTrigger.ELAPSED_TIME,
               val autoDismissSeconds: Int = 15){

    fun getTextColor(context: Context): Int {
        return when(foregroundColor){
            ContextCompat.getColor(context, R.color.bRed),
            ContextCompat.getColor(context, R.color.bPurple),
            ContextCompat.getColor(context, R.color.bYellow),
            ContextCompat.getColor(context, R.color.bGreen),
            ContextCompat.getColor(context, R.color.bBlue),
            ContextCompat.getColor(context, R.color.bCyan) -> R.color.white
            else -> R.color.black
        }
    }

    fun getResourceColor(context: Context): Int {
        return when(foregroundColor){
            ContextCompat.getColor(context, R.color.bRed) -> R.color.bRed
            ContextCompat.getColor(context, R.color.bPurple) -> R.color.bPurple
            ContextCompat.getColor(context, R.color.bYellow) -> R.color.bYellow
            ContextCompat.getColor(context, R.color.bGreen) -> R.color.bGreen
            ContextCompat.getColor(context, R.color.bBlue) -> R.color.bBlue
            ContextCompat.getColor(context, R.color.bCyan) -> R.color.bCyan

            ContextCompat.getColor(context, R.color.hRed) -> R.color.hRed
            ContextCompat.getColor(context, R.color.hPurple) -> R.color.hPurple
            ContextCompat.getColor(context, R.color.hYellow) -> R.color.hYellow
            ContextCompat.getColor(context, R.color.hGreen) -> R.color.hGreen
            ContextCompat.getColor(context, R.color.hBlue) -> R.color.hBlue
            ContextCompat.getColor(context, R.color.hCyan) -> R.color.hCyan

            else -> error("Unknown color")
        }
    }
}

val defaultReminders = Json.encodeToString(listOf(Reminder(0, "Drink", 30, "Take a sip!")))