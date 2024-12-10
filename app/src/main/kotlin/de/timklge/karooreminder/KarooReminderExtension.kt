package de.timklge.karooreminder

import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import de.timklge.karooreminder.screens.Reminder
import de.timklge.karooreminder.screens.ReminderBeepPattern
import de.timklge.karooreminder.screens.defaultReminders
import de.timklge.karooreminder.screens.preferencesKey
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.StreamState
import io.hammerhead.karooext.models.TurnScreenOn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

class KarooReminderExtension : KarooExtension("karoo-reminder", "1.0.5") {

    companion object {
        const val TAG = "karoo-reminder"
    }

    private lateinit var karooSystem: KarooSystemService

    private var serviceJob: Job? = null

    override fun onCreate() {
        super.onCreate()

        karooSystem = KarooSystemService(applicationContext)

        val mediaPlayer = MediaPlayer.create(this, R.raw.reminder)

        serviceJob = CoroutineScope(Dispatchers.IO).launch {
            karooSystem.connect { connected ->
                if (connected) {
                    Log.i(TAG, "Connected")
                }
            }

            val preferences = applicationContext.dataStore.data.map { remindersJson ->
                try {
                    Json.decodeFromString<MutableList<Reminder>>(
                        remindersJson[preferencesKey] ?: defaultReminders
                    )
                } catch(e: Throwable){
                    Log.e(TAG,"Failed to read preferences", e)
                    mutableListOf()
                }
            }

            karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue  }
                .map { (it / 1000 / 60).toInt() }
                .distinctUntilChanged()
                .filterNot { it == 0 }
                .combine(preferences) { elapsedMinutes, reminders -> elapsedMinutes to reminders}
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collect { (elapsedMinutes, reminders) ->
                    reminders
                        .filter { reminder -> reminder.isActive && elapsedMinutes % reminder.interval == 0 }
                        .forEach { reminder ->
                            karooSystem.dispatch(TurnScreenOn)

                            val intent = Intent("de.timklge.HIDE_POWERBAR").apply {
                                putExtra("duration", (if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else 15_000L) + 1000L)
                                putExtra("location", "top")
                            }

                            delay(1_000)
                            applicationContext.sendBroadcast(intent)

                            if (reminder.tone != ReminderBeepPattern.NO_TONES){
                                karooSystem.dispatch(PlayBeepPattern(reminder.tone.tones))
                                mediaPlayer.start()
                            }
                            karooSystem.dispatch(
                                InRideAlert(
                                    id = "reminder-${reminder.id}-${elapsedMinutes}",
                                    detail = reminder.text,
                                    title = reminder.name,
                                    autoDismissMs = if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                                    icon = R.drawable.ic_launcher,
                                    textColor = R.color.white,
                                    backgroundColor = reminder.getResourceColor(applicationContext)
                                ),
                            )
                    }
                }
        }
    }

    override fun onDestroy() {
        serviceJob?.cancel()
        serviceJob = null
        karooSystem.disconnect()
        super.onDestroy()
    }
}
