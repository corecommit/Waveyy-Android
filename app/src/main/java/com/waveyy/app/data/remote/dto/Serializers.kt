package com.waveyy.app.data.remote.dto

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.longOrNull

/**
 * Lenient long that accepts either a JSON number or a numeric string.
 * JioSaavn endpoints disagree on the type of `playCount`, `duration`, and
 * `followerCount` (`playCount` vs `play_count`, number vs string).
 */
object FlexibleLongSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("FlexibleLong", PrimitiveKind.LONG)

    override fun deserialize(decoder: Decoder): Long {
        val element = (decoder as JsonDecoder).decodeJsonElement()
        val primitive = element as? JsonPrimitive ?: return 0L
        return primitive.longOrNull
            ?: primitive.content.toLongOrNull()
            ?: 0L
    }

    override fun serialize(encoder: Encoder, value: Long) {
        encoder.encodeLong(value)
    }
}
