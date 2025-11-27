package de.timklge.karooreminder.model

import io.hammerhead.karooext.models.PlayBeepPattern
import kotlinx.serialization.Serializable

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