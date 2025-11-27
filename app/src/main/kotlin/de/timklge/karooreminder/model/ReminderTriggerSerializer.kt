package de.timklge.karooreminder.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

object ReminderTriggerSerializer : KSerializer<ReminderTrigger> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ReminderTrigger", PrimitiveKind.STRING)
    override fun serialize(encoder: Encoder, value: ReminderTrigger) {
        encoder.encodeString(value.id)
    }
    override fun deserialize(decoder: Decoder): ReminderTrigger {
        val id = decoder.decodeString()
        return ReminderTrigger.entries.first { it.id == id }
    }
}