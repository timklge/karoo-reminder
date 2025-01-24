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
import io.hammerhead.karooext.models.UserProfile
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ClosedReceiveChannelException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

enum class ReminderTrigger(val id: String, val label: String) {
    ELAPSED_TIME("elapsed_time", "Elapsed Time"),
    DISTANCE("distance", "Distance"),
    ENERGY_OUTPUT("energy_output", "Energy Output"),
    HR_LIMIT_MAXIMUM_EXCEEDED("hr_limit_max", "HR above value"),
    HR_LIMIT_MINIMUM_EXCEEDED("hr_limit_min", "HR below value"),
    POWER_LIMIT_MAXIMUM_EXCEEDED("power_limit_min", "Power above value"),
    POWER_LIMIT_MINIMUM_EXCEEDED("power_limit_min", "Power below value"),
    SPEED_LIMIT_MAXIMUM_EXCEEDED("speed_limit_max", "Speed above value"),
    SPEED_LIMIT_MINIMUM_EXCEEDED("speed_limit_min", "Speed below value"),
    CADENCE_LIMIT_MAXIMUM_EXCEEDED("cadence_limit_max", "Cadence above value"),
    CADENCE_LIMIT_MINIMUM_EXCEEDED("cadence_limit_min", "Cadence below value");

    fun getPrefix(): String {
        return when (this) {
            HR_LIMIT_MINIMUM_EXCEEDED, POWER_LIMIT_MINIMUM_EXCEEDED, SPEED_LIMIT_MINIMUM_EXCEEDED, CADENCE_LIMIT_MINIMUM_EXCEEDED -> "<"
            HR_LIMIT_MAXIMUM_EXCEEDED, POWER_LIMIT_MAXIMUM_EXCEEDED, SPEED_LIMIT_MAXIMUM_EXCEEDED, CADENCE_LIMIT_MAXIMUM_EXCEEDED -> ">"
            ELAPSED_TIME, DISTANCE -> ""
            ENERGY_OUTPUT -> ""
        }
    }

    fun getSuffix(imperial: Boolean): String {
        return when (this) {
            ELAPSED_TIME -> "min"
            DISTANCE -> if(imperial) "mi" else "km"
            HR_LIMIT_MAXIMUM_EXCEEDED, HR_LIMIT_MINIMUM_EXCEEDED -> "bpm"
            POWER_LIMIT_MAXIMUM_EXCEEDED, POWER_LIMIT_MINIMUM_EXCEEDED -> "W"
            SPEED_LIMIT_MAXIMUM_EXCEEDED, SPEED_LIMIT_MINIMUM_EXCEEDED -> if(imperial) "mph" else "km/h"
            CADENCE_LIMIT_MAXIMUM_EXCEEDED, CADENCE_LIMIT_MINIMUM_EXCEEDED -> "rpm"
            ENERGY_OUTPUT -> "kJ"
        }
    }
}

fun Flow<Int>.allIntermediateInts(): Flow<Int> = flow {
    var lastValue: Int? = null

    collect { value ->
        if (lastValue != null){
            val start = (lastValue!! + 1).coerceAtMost(value)

            for (i in start..value) {
                emit(i)
            }
        } else {
            emit(value)
        }

        lastValue = value
    }
}

class KarooReminderExtension : KarooExtension("karoo-reminder", "1.1.3") {

    companion object {
        const val TAG = "karoo-reminder"
    }

    private lateinit var karooSystem: KarooSystemService

    private var jobs: MutableSet<Job> = mutableSetOf()

    data class DisplayedReminder(val beepPattern: ReminderBeepPattern, val trigger: ReminderTrigger, val alert: InRideAlert)

    private var reminderChannel = Channel<DisplayedReminder>(2, BufferOverflow.DROP_OLDEST)

    private var mediaPlayer: MediaPlayer? = null

    private suspend fun receiverWorker() {
        for(displayedReminder in reminderChannel) {
            Log.i(TAG, "Dispatching reminder: ${displayedReminder.alert.title}")

            try {
                karooSystem.dispatch(TurnScreenOn)

                val isAutoDismiss = displayedReminder.alert.autoDismissMs != null
                val autoDismissMs = (displayedReminder.alert.autoDismissMs ?: 0L)

                val intent = Intent("de.timklge.HIDE_POWERBAR").apply {
                    putExtra("duration", (if (isAutoDismiss) autoDismissMs else 15_000L) + 1000L)
                    putExtra("location", "top")
                }

                delay(1_000)
                applicationContext.sendBroadcast(intent)

                if (displayedReminder.beepPattern != ReminderBeepPattern.NO_TONES) {
                    karooSystem.dispatch(PlayBeepPattern(displayedReminder.beepPattern.tones))
                    mediaPlayer?.start()
                }
                karooSystem.dispatch(displayedReminder.alert)

                val delayMs = if (isAutoDismiss) autoDismissMs else 20000L
                delay(delayMs)
            } catch(e: ClosedReceiveChannelException){
                Log.w(TAG, "Dispatch channel closed, exiting")
                return
            } catch(e: Exception){
                Log.e(TAG, "Failed to dispatch reminder", e)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer.create(this, R.raw.reminder)

        karooSystem = KarooSystemService(applicationContext)

        val receiveJob = CoroutineScope(Dispatchers.IO).launch {
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
                .combine(karooSystem.streamUserProfile()) { distance, profile -> distance to profile }
                .map { (distance, profile) ->
                    if (profile.preferredUnit.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL){
                        (distance / 1609.344).toInt()
                    } else {
                        (distance / 1000.0).toInt()
                    }
                }
                .distinctUntilChanged()
                .filterNot { it == 0 }
                .combine(preferences) { distance, reminders -> distance to reminders}
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collectLatest { (distance, reminders) ->
                    val rs = reminders
                        .filter { reminder -> reminder.trigger == ReminderTrigger.DISTANCE && reminder.isActive && distance % reminder.interval == 0 }

                    for (reminder in rs){
                        Log.i(TAG, "Distance reminder: ${reminder.name}")
                        reminderChannel.send(DisplayedReminder(reminder.tone, ReminderTrigger.DISTANCE, InRideAlert(
                            id = "reminder-${reminder.id}-${distance}",
                            detail = reminder.text,
                            title = reminder.name,
                            autoDismissMs = if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                            icon = R.drawable.timer,
                            textColor = reminder.displayForegroundColor?.getTextColor() ?: R.color.black,
                            backgroundColor = reminder.displayForegroundColor?.colorRes ?: R.color.hRed
                        )))
                    }
                }
        }
        jobs.add(distanceJob)

        jobs.addAll(listOf(
            startRangeExceededJob(ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED),
            startRangeExceededJob(ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED)
        ))

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
                        .filter { reminder -> reminder.trigger == ReminderTrigger.ELAPSED_TIME && reminder.isActive && elapsedMinutes % reminder.interval == 0 }

                    for (reminder in rs){
                        Log.i(TAG, "Elapsed time reminder: ${reminder.name}")
                        reminderChannel.send(DisplayedReminder(reminder.tone, ReminderTrigger.ELAPSED_TIME, InRideAlert(
                            id = "reminder-${reminder.id}-${elapsedMinutes}",
                            detail = reminder.text,
                            title = reminder.name,
                            autoDismissMs = if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                            icon = R.drawable.timer,
                            textColor = reminder.displayForegroundColor?.getTextColor() ?: R.color.black,
                            backgroundColor = reminder.displayForegroundColor?.colorRes ?: R.color.hRed
                        )))
                    }
                }
        }
        jobs.add(elapsedTimeJob)

        val energyOutputJob = CoroutineScope(Dispatchers.IO).launch {
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

            karooSystem.streamDataFlow(DataType.Type.ENERGY_OUTPUT)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue  }
                .map { it.toInt() }
                .distinctUntilChanged()
                .filterNot { it == 0 }
                .allIntermediateInts()
                .combine(preferences) { energyOutput, reminders -> energyOutput to reminders}
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collectLatest { (energyOutput, reminders) ->
                    val rs = reminders
                        .filter { reminder -> reminder.trigger == ReminderTrigger.ENERGY_OUTPUT && reminder.isActive && energyOutput % reminder.interval == 0 }

                    for (reminder in rs){
                        Log.i(TAG, "Energy output reminder: ${reminder.name}")
                        reminderChannel.send(DisplayedReminder(reminder.tone, ReminderTrigger.ENERGY_OUTPUT, InRideAlert(
                            id = "reminder-${reminder.id}-${energyOutput}",
                            detail = reminder.text,
                            title = reminder.name,
                            autoDismissMs = if(reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                            icon = R.drawable.timer,
                            textColor = reminder.displayForegroundColor?.getTextColor() ?: R.color.black,
                            backgroundColor = reminder.displayForegroundColor?.colorRes ?: R.color.hRed
                        )))
                    }
                }
        }
        jobs.add(energyOutputJob)
    }

    data class StreamData(val value: Double, val reminders: MutableList<Reminder>? = null, val imperial: Boolean = false)

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
                ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.SMOOTHED_3S_AVERAGE_SPEED
                ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE
                ReminderTrigger.DISTANCE, ReminderTrigger.ELAPSED_TIME, ReminderTrigger.ENERGY_OUTPUT -> error("Unsupported trigger type: $triggerType")
            }

            karooSystem.streamDataFlow(dataType)
                .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue }
                .filter { it > 0.0 }
                .combine(preferences) { value, reminders -> StreamData(value, reminders) }
                .combine(karooSystem.streamUserProfile()) { streamData, profile -> streamData.copy(imperial = profile.preferredUnit.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL) }
                .onlyIfNValuesReceivedWithinTimeframe(5, 1000 * 10) // At least 5 values have been received over the last 10 seconds
                .map { (value, reminders, imperial) ->
                    val triggered = reminders?.filter { reminder ->
                        val isSpeedTrigger = triggerType == ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED || triggerType == ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED
                        val reminderValue = if (isSpeedTrigger){
                            // Convert m/s speed to km/h or mph
                            if (imperial) reminder.interval * 0.44704 else reminder.interval * 0.277778
                        } else {
                            reminder.interval.toDouble()
                        }

                        val triggerIsMet = when (triggerType){
                            ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED -> value > reminderValue

                            ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED,
                            ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED -> value < reminderValue

                            else -> error("Unsupported trigger type: $triggerType")
                        }

                        reminder.isActive && reminder.trigger == triggerType && triggerIsMet
                    }

                    triggered
                }
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .throttle(1_000 * 60) // At most once every minute
                .collectLatest { reminders ->
                    Log.i(TAG, "Triggered range reminder: ${reminders.size} reminders")

                    reminders.forEach { reminder ->
                        reminderChannel.send(
                            DisplayedReminder(
                                reminder.tone, triggerType, InRideAlert(
                                    id = "reminder-${reminder.id}-${System.currentTimeMillis()}",
                                    detail = reminder.text,
                                    title = reminder.name,
                                    autoDismissMs = if (reminder.isAutoDismiss) reminder.autoDismissSeconds * 1000L else null,
                                    icon = R.drawable.timer,
                                    textColor = reminder.displayForegroundColor?.getTextColor() ?: R.color.black,
                                    backgroundColor = reminder.displayForegroundColor?.colorRes ?: R.color.hRed
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
