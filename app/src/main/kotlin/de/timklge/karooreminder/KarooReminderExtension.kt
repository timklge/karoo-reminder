package de.timklge.karooreminder

import android.content.Intent
import android.media.MediaPlayer
import android.util.Log
import de.timklge.karooreminder.screens.Reminder
import de.timklge.karooreminder.screens.ReminderBeepPattern
import io.hammerhead.karooext.KarooSystemService
import io.hammerhead.karooext.extension.KarooExtension
import io.hammerhead.karooext.models.DataType
import io.hammerhead.karooext.models.HardwareType
import io.hammerhead.karooext.models.InRideAlert
import io.hammerhead.karooext.models.PlayBeepPattern
import io.hammerhead.karooext.models.RideState
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
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch

enum class SmoothSetting(val label: String) {
    NONE("None"),
    SMOOTH_3S("3 seconds"),
    SMOOTH_10S("10 seconds"),
    SMOOTH_30S("30 seconds"),
    SMOOTH_20M("20 minutes"),
    SMOOTH_60M("60 minutes"),
    SMOOTH_LAP("Lap"),
    SMOOTH_RIDE("Ride");
}

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
    CADENCE_LIMIT_MINIMUM_EXCEEDED("cadence_limit_min", "Cadence below value"),
    AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED("ambient_temperature_limit_max", "Ambient temperature above value"),
    AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED("ambient_temperature_limit_min", "Ambient temperature below value"),
    GRADIENT_LIMIT_MAXIMUM_EXCEEDED("gradient_limit_max", "Gradient above value"),
    GRADIENT_LIMIT_MINIMUM_EXCEEDED("gradient_limit_min", "Gradient below value"),
    CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED("core_temperature_limit_max", "Core temperature above value"),
    CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED("core_temperature_limit_min", "Core temperature below value"),
    FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED("front_tire_pressure_limit_max", "Front tire pressure above value"),
    FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED("front_tire_pressure_limit_min", "Front tire pressure below value"),
    REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED("rear_tire_pressure_limit_max", "Rear tire pressure above value"),
    REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED("rear_tire_pressure_limit_min", "Rear tire pressure below value");

    fun getPrefix(): String {
        return when (this) {
            HR_LIMIT_MINIMUM_EXCEEDED, POWER_LIMIT_MINIMUM_EXCEEDED, SPEED_LIMIT_MINIMUM_EXCEEDED, CADENCE_LIMIT_MINIMUM_EXCEEDED, CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED,
            AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, GRADIENT_LIMIT_MINIMUM_EXCEEDED -> "<"

            HR_LIMIT_MAXIMUM_EXCEEDED, POWER_LIMIT_MAXIMUM_EXCEEDED, SPEED_LIMIT_MAXIMUM_EXCEEDED, CADENCE_LIMIT_MAXIMUM_EXCEEDED, CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
            AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, GRADIENT_LIMIT_MAXIMUM_EXCEEDED -> ">"

            ELAPSED_TIME, DISTANCE, ENERGY_OUTPUT -> ""
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
            CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED -> if(imperial) "°F" else "°C"
            FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED -> if(imperial) "psi" else "bar"
            ENERGY_OUTPUT -> "kJ"
            GRADIENT_LIMIT_MAXIMUM_EXCEEDED, GRADIENT_LIMIT_MINIMUM_EXCEEDED -> "%"
        }
    }

    fun isDecimalValue() = this == CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED || this == CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED ||
            this == FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED || this == FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED ||
            this == REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED || this == REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED ||
            this == AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED || this == AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED ||
            this == GRADIENT_LIMIT_MAXIMUM_EXCEEDED || this == GRADIENT_LIMIT_MINIMUM_EXCEEDED

    fun getSmoothedDataType(smoothSetting: SmoothSetting): String {
        return when(this) {
            POWER_LIMIT_MAXIMUM_EXCEEDED, POWER_LIMIT_MINIMUM_EXCEEDED -> {
                when (smoothSetting) {
                    SmoothSetting.NONE -> DataType.Type.POWER
                    SmoothSetting.SMOOTH_3S -> DataType.Type.SMOOTHED_3S_AVERAGE_POWER
                    SmoothSetting.SMOOTH_10S -> DataType.Type.SMOOTHED_10S_AVERAGE_POWER
                    SmoothSetting.SMOOTH_30S -> DataType.Type.SMOOTHED_30S_AVERAGE_POWER
                    SmoothSetting.SMOOTH_20M -> DataType.Type.SMOOTHED_20M_AVERAGE_POWER
                    SmoothSetting.SMOOTH_60M -> DataType.Type.SMOOTHED_1HR_AVERAGE_POWER
                    SmoothSetting.SMOOTH_LAP -> DataType.Type.POWER_LAP
                    SmoothSetting.SMOOTH_RIDE -> DataType.Type.AVERAGE_POWER
                }
            }

            else -> getDataType()
        }
    }

    fun hasSmoothedDataTypes(): Boolean {
        return this == POWER_LIMIT_MAXIMUM_EXCEEDED || this == POWER_LIMIT_MINIMUM_EXCEEDED
    }

    fun getDataType(): String {
        return when (this) {
            ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.HEART_RATE
            POWER_LIMIT_MAXIMUM_EXCEEDED, POWER_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.SMOOTHED_3S_AVERAGE_POWER
            ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.SMOOTHED_3S_AVERAGE_SPEED
            ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE
            ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.CORE_TEMP
            ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.TIRE_PRESSURE_FRONT
            ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.TIRE_PRESSURE_REAR
            ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.ELEVATION_GRADE
            ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED -> DataType.Type.TEMPERATURE

            ReminderTrigger.DISTANCE, ReminderTrigger.ELAPSED_TIME, ReminderTrigger.ENERGY_OUTPUT -> error("Unsupported trigger type: $this")
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

class KarooReminderExtension : KarooExtension("karoo-reminder", BuildConfig.VERSION_NAME) {
    companion object {
        const val TAG = "karoo-reminder"
    }

    private lateinit var karooSystem: KarooSystemService

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
                    val tones = if (karooSystem.hardwareType == HardwareType.K2) displayedReminder.beepPattern.tonesKaroo2 else displayedReminder.beepPattern.tonesKaroo3
                    karooSystem.dispatch(PlayBeepPattern(tones))
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

    private lateinit var receiveJob: Job
    private lateinit var triggerStreamJob: Job

    override fun onCreate() {
        super.onCreate()

        mediaPlayer = MediaPlayer.create(this, R.raw.reminder)

        karooSystem = KarooSystemService(applicationContext)

        receiveJob = CoroutineScope(Dispatchers.IO).launch {
            receiverWorker()
        }

        karooSystem.connect { connected ->
            if (connected) {
                Log.i(TAG, "Connected")
            }
        }

        val triggerJobs = mutableSetOf<Job>()

        triggerStreamJob = CoroutineScope(Dispatchers.IO).launch {
            streamPreferences().collect { reminders ->
                triggerJobs.forEach { it.cancel() }
                triggerJobs.clear()

                if (reminders.any { it.trigger == ReminderTrigger.DISTANCE }){
                    val distanceJob = startIntervalJob(ReminderTrigger.DISTANCE) {
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
                    }
                    triggerJobs.add(distanceJob)
                }

                if (reminders.any { it.trigger == ReminderTrigger.ELAPSED_TIME }){
                    val elapsedTimeJob = startIntervalJob(ReminderTrigger.ELAPSED_TIME) {
                        karooSystem.streamDataFlow(DataType.Type.ELAPSED_TIME)
                            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue  }
                            .map { (it / 1000 / 60).toInt() }
                            .distinctUntilChanged()
                            .filterNot { it == 0 }
                    }
                    triggerJobs.add(elapsedTimeJob)
                }

                if (reminders.any { it.trigger == ReminderTrigger.ENERGY_OUTPUT }){
                    val energyOutputJob = startIntervalJob(ReminderTrigger.ENERGY_OUTPUT) {
                        karooSystem.streamDataFlow(DataType.Type.ENERGY_OUTPUT)
                            .mapNotNull { (it as? StreamState.Streaming)?.dataPoint?.singleValue  }
                            .map { it.toInt() }
                            .distinctUntilChanged()
                            .filterNot { it == 0 }
                            .allIntermediateInts()
                    }
                    triggerJobs.add(energyOutputJob)
                }

                val intervalTriggers = setOf(
                    ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED,
                    ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
                    ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED
                )

                intervalTriggers.forEach { trigger ->
                    SmoothSetting.entries.forEach { smoothSetting ->
                        if (reminders.any { it.trigger == trigger && it.smoothSetting == smoothSetting }){
                            val job = startRangeExceededJob(trigger, smoothSetting)
                            triggerJobs.add(job)
                        }
                    }
                }
            }
        }
    }

    private fun startIntervalJob(trigger: ReminderTrigger, flow: () -> Flow<Int>): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val preferences = streamPreferences()

            flow()
                .filterNot { it == 0 }
                .combine(preferences) { elapsedMinutes, reminders -> elapsedMinutes to reminders}
                .distinctUntilChanged { old, new -> old.first == new.first }
                .collectLatest { (elapsedMinutes, reminders) ->
                    val rs = reminders
                        .filter { reminder ->
                            val interval = reminder.interval
                            reminder.trigger == trigger && reminder.isActive && interval != null && elapsedMinutes % interval == 0
                        }

                    for (reminder in rs){
                        Log.i(TAG, "$trigger reminder: ${reminder.name}")
                        reminderChannel.send(DisplayedReminder(reminder.tone, trigger, InRideAlert(
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
    }

    private fun startRangeExceededJob(triggerType: ReminderTrigger, smoothSetting: SmoothSetting): Job {
        return CoroutineScope(Dispatchers.IO).launch {
            val preferences = streamPreferences()

            Log.i(TAG, "Starting range exceeded job for trigger $triggerType with smooth setting $smoothSetting")

            val valueStream = karooSystem.streamDataFlow(triggerType.getSmoothedDataType(smoothSetting))
                .mapNotNull {
                    val dataPoint = (it as? StreamState.Streaming)?.dataPoint

                    @Suppress("KotlinConstantConditions")
                    when (triggerType) {
                        ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED -> dataPoint?.values?.get(DataType.Field.CORE_TEMP)
                        ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED,
                        ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED -> dataPoint?.values?.get(DataType.Field.TIRE_PRESSURE)

                        ReminderTrigger.ELAPSED_TIME, ReminderTrigger.DISTANCE, ReminderTrigger.ENERGY_OUTPUT,
                        ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED,
                        ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED,
                        ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED,
                        ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED,
                        ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED,
                        ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED -> dataPoint?.singleValue
                    }
                }
                .filter { it != 0.0 }

            data class StreamData(val value: Double, val reminders: MutableList<Reminder>, val distanceImperial: Boolean, val temperatureImperial: Boolean, val rideState: RideState)

            combine(valueStream, preferences, karooSystem.streamUserProfile(), karooSystem.streamRideState()) { value, reminders, profile, rideState ->
                StreamData(distanceImperial = profile.preferredUnit.distance == UserProfile.PreferredUnit.UnitType.IMPERIAL, temperatureImperial = profile.preferredUnit.temperature == UserProfile.PreferredUnit.UnitType.IMPERIAL,
                    value = value, reminders = reminders, rideState = rideState)
            }.filter {
                it.rideState is RideState.Recording
            }.let {
                    @Suppress("KotlinConstantConditions")
                    when (triggerType){
                        // Tire pressure, gradient and temperature triggers do not require ongoing measurements, as measurement rate is unknown
                        ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED -> it

                        ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED,
                        ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED -> it.onlyIfNValuesReceivedWithinTimeframe(5, 1000 * 10) // At least 5 values have been received over the last 10 seconds

                        ReminderTrigger.ELAPSED_TIME, ReminderTrigger.DISTANCE, ReminderTrigger.ENERGY_OUTPUT -> error("Unsupported trigger type: $triggerType")
                    }
                }
                .map { (actualValue, reminders, distanceImperial, temperatureImperial) ->
                    val triggered = reminders.filter { reminder ->
                        val triggerThreshold = when(triggerType) {
                            ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED -> {
                                if (distanceImperial) reminder.interval?.times(0.44704) else reminder.interval?.times(0.277778)
                            }
                            ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED -> {
                                if (temperatureImperial) (reminder.intervalFloat?.minus(32))?.div(1.8) else reminder.intervalFloat
                            }
                            ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED -> {
                                if(distanceImperial) reminder.intervalFloat?.times(68.9476) /* psi to mbar */ else reminder.intervalFloat?.times(1000.0) /* bar to mbar */
                            }

                            ReminderTrigger.ELAPSED_TIME, ReminderTrigger.DISTANCE,
                            ReminderTrigger.ENERGY_OUTPUT, ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED -> reminder.interval?.toDouble()
                        }

                        val triggerIsMet = when (triggerType){
                            ReminderTrigger.HR_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.CADENCE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED, ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
                            ReminderTrigger.GRADIENT_LIMIT_MAXIMUM_EXCEEDED -> triggerThreshold != null && actualValue > triggerThreshold

                            ReminderTrigger.HR_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.POWER_LIMIT_MINIMUM_EXCEEDED,
                            ReminderTrigger.CADENCE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.SPEED_LIMIT_MINIMUM_EXCEEDED,
                            ReminderTrigger.CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED,
                            ReminderTrigger.REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED, ReminderTrigger.AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED,
                            ReminderTrigger.GRADIENT_LIMIT_MINIMUM_EXCEEDED -> triggerThreshold != null && actualValue < triggerThreshold

                            ReminderTrigger.ELAPSED_TIME, ReminderTrigger.DISTANCE, ReminderTrigger.ENERGY_OUTPUT -> error("Unsupported trigger type: $triggerType")
                        }

                        reminder.isActive && reminder.trigger == triggerType && triggerIsMet
                    }

                    triggered
                }
                .filterNotNull()
                .filter { it.isNotEmpty() }
                .throttle(1_000 * 60) // At most once every minute
                .onCompletion {
                    Log.i(TAG, "Range exceeded job for trigger $triggerType with smooth setting $smoothSetting completed")
                }
                .collectLatest { reminders ->
                    reminders.forEach { reminder ->
                        Log.d(TAG, "Dispatching reminder: ${reminder.name}")
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
        receiveJob.cancel()
        triggerStreamJob.cancel()

        karooSystem.disconnect()
        super.onDestroy()
    }
}
