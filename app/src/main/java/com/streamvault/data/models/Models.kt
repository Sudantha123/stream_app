package com.streamvault.data.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class Video(
    val id: String,
    val title: String,
    val thumbnail: String = "",
    val duration: Double = 0.0,
    @SerialName("size_bytes") val sizeBytes: Long = 0L,
    @SerialName("created_at") val createdAt: String = "",
    @SerialName("m3u8_path") val m3u8Path: String = ""
)

@Serializable
data class Job(
    val id: String,
    val url: String,
    val title: String,
    val status: String,
    @SerialName("segment_length") val segmentLength: Int = 6,
    @SerialName("downloaded_bytes") val downloadedBytes: Long = 0L,
    @SerialName("total_bytes") val totalBytes: Long = 0L,
    @SerialName("download_speed") val downloadSpeed: Double = 0.0,
    @SerialName("download_eta") val downloadEta: Double = 0.0,
    @SerialName("download_pct") val downloadPct: Double = 0.0,
    @SerialName("transcode_segments") val transcodeSegments: Int = 0,
    @SerialName("transcode_done") val transcodeDone: Int = 0,
    @SerialName("transcode_pct") val transcodePct: Double = 0.0,
    val error: String? = null,
    @SerialName("created_at") val createdAt: String = ""
)

@Serializable
data class AddJobRequest(
    val url: String,
    val title: String = "",
    @SerialName("segment_length") val segmentLength: Int = 6
)

@Serializable
data class GenericResponse(val ok: Boolean = false, val error: String? = null)

// WebSocket messages
@Serializable
data class WsMessage(
    val type: String,
    val data: kotlinx.serialization.json.JsonElement? = null
)

enum class JobStatus(val value: String) {
    PENDING("pending"),
    DOWNLOADING("downloading"),
    TRANSCODING("transcoding"),
    THUMBNAILING("thumbnailing"),
    DONE("done"),
    FAILED("failed"),
    CANCELLED("cancelled");

    companion object {
        fun from(v: String) = values().firstOrNull { it.value == v } ?: PENDING
    }
}
