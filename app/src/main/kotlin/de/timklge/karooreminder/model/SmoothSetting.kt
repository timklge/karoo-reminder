package de.timklge.karooreminder.model

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