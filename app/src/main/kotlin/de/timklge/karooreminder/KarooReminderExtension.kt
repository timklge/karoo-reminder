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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class ReminderTrigger(val id: String, val label: String) {
    ELAPSED_TIME("elapsed_time", "Elapsed Time"),
    DISTANCE("distance", "Distance"),
    HR_LIMIT_MAXIMUM_EXCEEDED("hr_limit_max", "HR above value"),
    POWER_LIMIT_MAXIMUM_EXCEEDED("power_limit_min", "Power above value"),
    HR_LIMIT_MINIMUM_EXCEEDED("hr_limit_min", "HR below value"),
    POWER_LIMIT_MINIMUM_EXCEEDED("power_limit_min", "Power below value")
}

class KarooReminderExtension : KarooExtension("karoo-reminder", "1.1") {

    companion object {
        const val TAG = "karoo-reminder"
    }

    private lateinit var karooSystem: KarooSystemService

    private var jobs: MutableSet<Job> = mutableSetOf()

    data class DisplayedReminder(val tones: ReminderBeepPattern, val trigger: ReminderTrigger, val alert: InRideAlert)

    private var reminderChannel = Channel<DisplayedReminder>(Channel.UNLIMITED)

    val mediaPlayer = MediaPlayer.create(this, R.raw.reminder)

    private suspend fun receiverWorker() {
        for (displayedReminder in reminderChannel){
            karooSystem.dispatch(TurnScreenOn)

            val isAutoDismiss = displayedReminder.alert.autoDismissMs != null
            val autoDismissMs = (displayedReminder.alert.autoDismissMs ?: 0L)

            val intent = Intent("de.timklge.HIDE_POWERBAR").apply {
                putExtra("duration", (if(isAutoDismiss) autoDismissMs else 15_000L) + 1000L)
                putExtra("location", "top")
            }

            delay(1_000)
            applicationContext.sendBroadcast(intent)

            if (displayedReminder.tones != ReminderBeepPattern.NO_TONES){
                karooSystem.dispatch(PlayBeepPattern(displayedReminder.tones.tones))
                mediaPlayer.start()
            }
            karooSystem.dispatch(displayedReminder.alert)

            val delayMs = if(isAutoDismiss) autoDismissMs * 1000L else 20000L
            delay(delayMs)
        }
    }

    override fun onCreate() {
        super.onCreate()

        karooSystem = KarooSystemService(applicationContext)

        val receiveJob = CoroutineScope(Dispatchers.Default).launch {
            receiverWorker()
        }
        jobs.add(receiveJob)

        karooSystem.connect { connected ->
            if (connected) {
                Log.i(TAG, "Connected")
            }
        }

        val distanceJob = CoroutineScope(Dispatchers.IO).launch {
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

            karooSystem.streamDataFlow(DataType.Type.DISTANCE)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue  }
                .map { (it / 1000).toInt() }
                .distinctUntilChanged()
                .filterNot { it == 0 }
                .combine(preferences) { distance, reminders -> distance to reminders}
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collectLatest { (distance, reminders) ->
                    val rs = reminders
                        .filter { reminder -> reminder.isActive && distance % reminder.interval == 0 }

                    for (reminder in rs){
                        reminderChannel.send(DisplayedReminder(reminder.tone, ReminderTrigger.DISTANCE, InRideAlert(
                            id = "reminder-${reminder.id}-${distance}",
                            detail = reminder.text,
                            title = reminder.name,
                            autoDismissMs = if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                            icon = R.drawable.ic_launcher,
                            textColor = reminder.getTextColor(applicationContext),
                            backgroundColor = reminder.getResourceColor(applicationContext)
                        )))
                    }
                }
        }
        jobs.add(distanceJob)

        val powerRangeExceededJob = startRangeExceededJob(ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED)
        jobs.add(powerRangeExceededJob)

        val heartRangeExceededJob = startRangeExceededJob(ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED)
        jobs.add(heartRangeExceededJob)

        val powerRangeMinimumExceededJob = startRangeExceededJob(ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED)
        jobs.add(powerRangeMinimumExceededJob)

        val heartRangeMinimumExceededJob = startRangeExceededJob(ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED)
        jobs.add(heartRangeMinimumExceededJob)

        val elapsedTimeJob = CoroutineScope(Dispatchers.IO).launch {
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
                .collectLatest { (elapsedMinutes, reminders) ->
                    val rs = reminders
                        .filter { reminder -> reminder.isActive && elapsedMinutes % reminder.interval == 0 }

                    for (reminder in rs){
                        reminderChannel.send(DisplayedReminder(reminder.tone, ReminderTrigger.ELAPSED_TIME, InRideAlert(
                            id = "reminder-${reminder.id}-${elapsedMinutes}",
                            detail = reminder.text,
                            title = reminder.name,
                            autoDismissMs = if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                            icon = R.drawable.ic_launcher,
                            textColor = reminder.getTextColor(applicationContext),
                            backgroundColor = reminder.getResourceColor(applicationContext)
                        )))
                    }
                }
        }
        jobs.add(elapsedTimeJob)
    }

    private fun startRangeExceededJob(triggerType: ReminderTrigger): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val preferences = applicationContext.dataStore.data.map { remindersJson ->
                try {
                    Json.decodeFromString<MutableList<Reminder>>(
                        remindersJson[preferencesKey] ?: defaultReminders
                    )
                } catch (e: Throwable) {
                    Log.e(TAG, "Failed to read preferences", e)
                    mutableListOf()
                }
            }

            val dataType = when (triggerType) {
                ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.HEART_RATE
                ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.SMOOTHED_3S_AVERAGE_POWER
                else -> error("Unknown trigger type: $triggerType")
            }

            karooSystem.streamDataFlow(dataType)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                .distinctUntilChanged()

                .combine(preferences) { value, reminders -> value to reminders }
                .distinctUntilChanged { old, new -> old.first == new.first }
                .map { (value, reminders) ->
                    reminders.filter { reminder ->
                        val triggerIsMet = when (reminder.trigger){
                            ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED -> value > reminder.interval
                            ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED -> value < reminder.interval
                            else -> error("Unknown trigger type: $triggerType")
                        }

                        reminder.isActive && reminder.trigger == triggerType && triggerIsMet
                    }
                }
                .throttle(1_000 * 120) // At most once every two minutes
                .collectLatest { reminders ->
                    reminders.forEach { reminder ->
                        reminderChannel.send(
                            DisplayedReminder(
                                reminder.tone, triggerType, InRideAlert(
                                    id = "reminder-${reminder.id}-${System.currentTimeMillis()}",
                                    detail = reminder.text,
                                    title = reminder.name,
                                    autoDismissMs = if (reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                                    icon = R.drawable.ic_launcher,
                                    textColor = reminder.getTextColor(applicationContext),
                                    backgroundColor = reminder.getResourceColor(applicationContext)
                                )
                            )
                        )
                    }
                }
        }
    }

    override fun onDestroy() {
        jobs.forEach { job -> job.cancel() }
        jobs.clear()

        karooSystem.disconnect()
        super.onDestroy()
    }
}
