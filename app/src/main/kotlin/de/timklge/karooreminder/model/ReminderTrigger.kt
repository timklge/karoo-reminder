package de.timklge.karooreminder.model
import io.hammerhead.karooext.models.DataType

sealed class ReminderTrigger(val id: String, val label: String) {
    abstract fun getPrefix(): String
    abstract fun getSuffix(imperial: Boolean): String
    abstract fun isDecimalValue(): Boolean
    abstract fun hasSmoothedDataTypes(): Boolean
    abstract fun getSmoothedDataType(smoothSetting: SmoothSetting): String
    abstract fun getDataType(): String
    abstract fun getFieldLabel(): String
    // Interval-based triggers
    data object ELAPSED_TIME : ReminderTrigger("elapsed_time", "Elapsed Time") {
        override fun getPrefix() = ""
        override fun getSuffix(imperial: Boolean) = "min"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = error("Unsupported trigger type: $this")
        override fun getFieldLabel() = "Interval"
    }
    data object DISTANCE : ReminderTrigger("distance", "Distance") {
        override fun getPrefix() = ""
        override fun getSuffix(imperial: Boolean) = if (imperial) "mi" else "km"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = error("Unsupported trigger type: $this")
        override fun getFieldLabel() = "Distance"
    }
    data object ENERGY_OUTPUT : ReminderTrigger("energy_output", "Energy Output") {
        override fun getPrefix() = ""
        override fun getSuffix(imperial: Boolean) = "kJ"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = error("Unsupported trigger type: $this")
        override fun getFieldLabel() = "Energy Output"
    }
    // Heart rate triggers
    data object HR_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("hr_limit_max", "HR above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = "bpm"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.HEART_RATE
        override fun getFieldLabel() = "Maximum heart rate"
    }
    data object HR_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("hr_limit_min", "HR below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = "bpm"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.HEART_RATE
        override fun getFieldLabel() = "Minimum heart rate"
    }
    // Power triggers
    data object POWER_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("power_limit_max", "Power above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = "W"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = true
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = when (smoothSetting) {
            SmoothSetting.NONE -> DataType.Type.POWER
            SmoothSetting.SMOOTH_3S -> DataType.Type.SMOOTHED_3S_AVERAGE_POWER
            SmoothSetting.SMOOTH_10S -> DataType.Type.SMOOTHED_10S_AVERAGE_POWER
            SmoothSetting.SMOOTH_30S -> DataType.Type.SMOOTHED_30S_AVERAGE_POWER
            SmoothSetting.SMOOTH_20M -> DataType.Type.SMOOTHED_20M_AVERAGE_POWER
            SmoothSetting.SMOOTH_60M -> DataType.Type.SMOOTHED_1HR_AVERAGE_POWER
            SmoothSetting.SMOOTH_LAP -> DataType.Type.POWER_LAP
            SmoothSetting.SMOOTH_RIDE -> DataType.Type.AVERAGE_POWER
        }
        override fun getDataType() = DataType.Type.SMOOTHED_3S_AVERAGE_POWER
        override fun getFieldLabel() = "Maximum power"
    }
    data object POWER_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("power_limit_min", "Power below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = "W"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = true
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = when (smoothSetting) {
            SmoothSetting.NONE -> DataType.Type.POWER
            SmoothSetting.SMOOTH_3S -> DataType.Type.SMOOTHED_3S_AVERAGE_POWER
            SmoothSetting.SMOOTH_10S -> DataType.Type.SMOOTHED_10S_AVERAGE_POWER
            SmoothSetting.SMOOTH_30S -> DataType.Type.SMOOTHED_30S_AVERAGE_POWER
            SmoothSetting.SMOOTH_20M -> DataType.Type.SMOOTHED_20M_AVERAGE_POWER
            SmoothSetting.SMOOTH_60M -> DataType.Type.SMOOTHED_1HR_AVERAGE_POWER
            SmoothSetting.SMOOTH_LAP -> DataType.Type.POWER_LAP
            SmoothSetting.SMOOTH_RIDE -> DataType.Type.AVERAGE_POWER
        }
        override fun getDataType() = DataType.Type.SMOOTHED_3S_AVERAGE_POWER
        override fun getFieldLabel() = "Minimum power"
    }
    // Speed triggers
    data object SPEED_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("speed_limit_max", "Speed above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = if (imperial) "mph" else "km/h"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.SMOOTHED_3S_AVERAGE_SPEED
        override fun getFieldLabel() = "Maximum speed"
    }
    data object SPEED_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("speed_limit_min", "Speed below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = if (imperial) "mph" else "km/h"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.SMOOTHED_3S_AVERAGE_SPEED
        override fun getFieldLabel() = "Minimum speed"
    }
    // Cadence triggers
    data object CADENCE_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("cadence_limit_max", "Cadence above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = "rpm"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE
        override fun getFieldLabel() = "Maximum cadence"
    }
    data object CADENCE_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("cadence_limit_min", "Cadence below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = "rpm"
        override fun isDecimalValue() = false
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.SMOOTHED_3S_AVERAGE_CADENCE
        override fun getFieldLabel() = "Minimum cadence"
    }
    // Ambient temperature triggers
    data object AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("ambient_temperature_limit_max", "Ambient temperature above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = if (imperial) "°F" else "°C"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.TEMPERATURE
        override fun getFieldLabel() = "Maximum temp"
    }
    data object AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("ambient_temperature_limit_min", "Ambient temperature below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = if (imperial) "°F" else "°C"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.TEMPERATURE
        override fun getFieldLabel() = "Minimum temp"
    }
    // Gradient triggers
    data object GRADIENT_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("gradient_limit_max", "Gradient above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = "%"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.ELEVATION_GRADE
        override fun getFieldLabel() = "Maximum gradient"
    }
    data object GRADIENT_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("gradient_limit_min", "Gradient below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = "%"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.ELEVATION_GRADE
        override fun getFieldLabel() = "Minimum gradient"
    }
    // Core temperature triggers
    data object CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("core_temperature_limit_max", "Core temperature above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = if (imperial) "°F" else "°C"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.CORE_TEMP
        override fun getFieldLabel() = "Maximum core temp"
    }
    data object CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("core_temperature_limit_min", "Core temperature below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = if (imperial) "°F" else "°C"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.CORE_TEMP
        override fun getFieldLabel() = "Minimum core temp"
    }
    // Front tire pressure triggers
    data object FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("front_tire_pressure_limit_max", "Front tire pressure above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = if (imperial) "psi" else "bar"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.TIRE_PRESSURE_FRONT
        override fun getFieldLabel() = "Max front tire pressure"
    }
    data object FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("front_tire_pressure_limit_min", "Front tire pressure below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = if (imperial) "psi" else "bar"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.TIRE_PRESSURE_FRONT
        override fun getFieldLabel() = "Min front tire pressure"
    }
    // Rear tire pressure triggers
    data object REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED : ReminderTrigger("rear_tire_pressure_limit_max", "Rear tire pressure above value") {
        override fun getPrefix() = ">"
        override fun getSuffix(imperial: Boolean) = if (imperial) "psi" else "bar"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.TIRE_PRESSURE_REAR
        override fun getFieldLabel() = "Max rear tire pressure"
    }
    data object REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED : ReminderTrigger("rear_tire_pressure_limit_min", "Rear tire pressure below value") {
        override fun getPrefix() = "<"
        override fun getSuffix(imperial: Boolean) = if (imperial) "psi" else "bar"
        override fun isDecimalValue() = true
        override fun hasSmoothedDataTypes() = false
        override fun getSmoothedDataType(smoothSetting: SmoothSetting) = getDataType()
        override fun getDataType() = DataType.Type.TIRE_PRESSURE_REAR
        override fun getFieldLabel() = "Min rear tire pressure"
    }
    companion object {
        val entries = listOf(
            ELAPSED_TIME,
            DISTANCE,
            ENERGY_OUTPUT,
            HR_LIMIT_MAXIMUM_EXCEEDED,
            HR_LIMIT_MINIMUM_EXCEEDED,
            POWER_LIMIT_MAXIMUM_EXCEEDED,
            POWER_LIMIT_MINIMUM_EXCEEDED,
            SPEED_LIMIT_MAXIMUM_EXCEEDED,
            SPEED_LIMIT_MINIMUM_EXCEEDED,
            CADENCE_LIMIT_MAXIMUM_EXCEEDED,
            CADENCE_LIMIT_MINIMUM_EXCEEDED,
            AMBIENT_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
            AMBIENT_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED,
            GRADIENT_LIMIT_MAXIMUM_EXCEEDED,
            GRADIENT_LIMIT_MINIMUM_EXCEEDED,
            CORE_TEMPERATURE_LIMIT_MAXIMUM_EXCEEDED,
            CORE_TEMPERATURE_LIMIT_MINIMUM_EXCEEDED,
            FRONT_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
            FRONT_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED,
            REAR_TIRE_PRESSURE_LIMIT_MAXIMUM_EXCEEDED,
            REAR_TIRE_PRESSURE_LIMIT_MINIMUM_EXCEEDED
        )
    }
}