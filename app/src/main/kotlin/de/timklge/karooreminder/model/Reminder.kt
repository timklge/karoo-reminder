package de.timklge.karooreminder.model

import android.graphics.Color
import io.hammerhead.karooext.models.RideProfile
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
class Reminder(val id: Int, var name: String,
               /** Trigger value used by all triggers except temperature, gradient, tire pressure */
               var interval: Int? = null,
               /** Trigger value used by temperature, gradient, tire pressure triggers */
               var intervalFloat: Double? = null,
               /** Smooth interval used by power, speed triggers */
               var smoothSetting: SmoothSetting = SmoothSetting.SMOOTH_3S,
               var text: String,
               var displayForegroundColor: ReminderColor? = null,
               @Deprecated("Use displayForegroundColor instead")
               var foregroundColor: Int = Color.parseColor("#FF6060"),
               val isActive: Boolean = true, val isAutoDismiss: Boolean = true,
               val tone: ReminderBeepPattern = ReminderBeepPattern.THREE_TONES_UP,
               val autoDismissSeconds: Int = 15,
               val enabledRideProfiles: Set<String> = emptySet(),
               @Serializable(with = ReminderTriggerSerializer::class)
               val trigger: ReminderTrigger = ReminderTrigger.ELAPSED_TIME,
               val oneShot: Boolean = false
)

val defaultReminders = Json.encodeToString(listOf(Reminder(0, "Drink", 30, text = "Take a sip!")))

fun reminderIsActive(reminder: Reminder, currentRideProfile: RideProfile?): Boolean {
    val enabledRideProfiles = reminder.enabledRideProfiles.map { it.lowercase().trim() }
    val currentProfileName = currentRideProfile?.name?.lowercase()?.trim()

    return reminder.isActive && (currentRideProfile == null || enabledRideProfiles.isEmpty() || enabledRideProfiles.contains(currentProfileName))
}