package dev.ambr.ar

import java.io.File

/** Where an ARCore session gets its camera and sensor data from. */
sealed interface SessionSource {
    /** The phone's camera and sensors. */
    data object Live : SessionSource

    /** A recorded ARCore session MP4, replayed deterministically. */
    data class Playback(val mp4: File) : SessionSource
}
