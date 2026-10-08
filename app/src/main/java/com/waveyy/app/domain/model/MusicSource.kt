package com.waveyy.app.domain.model

/** Origin of a catalog item. Defaults to JioSaavn (existing behavior). */
enum class MusicSource {
    /** JioSaavn deployment / proxy. */
    JIOSAAVN,

    /** YouTube Music InnerTube. */
    YTMUSIC
}
