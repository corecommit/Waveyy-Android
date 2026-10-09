package com.waveyy.app.domain.model

import kotlinx.serialization.Serializable

/** Origin of a catalog item. Defaults to JioSaavn (existing behavior). */
@Serializable
enum class MusicSource {
    /** JioSaavn deployment / proxy. */
    JIOSAAVN,

    /** YouTube Music InnerTube. */
    YTMUSIC
}
