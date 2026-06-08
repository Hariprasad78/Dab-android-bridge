package com.harry.dabagent.mqtt

import com.harry.dabagent.protocol.DabOperationRegistry

data class DabTopics(
    val request: String,
    val response: String,
    val status: String,
    val officialStatus: String,
    val officialSubscriptions: List<String>,
)

data class ResolvedDabTopic(
    val method: String,
    val responseTopic: String,
    val mode: TopicMode,
)

enum class TopicMode { OFFICIAL, BRIDGE }

object MqttTopicRouter {
    fun topics(bridgeId: String, deviceId: String): DabTopics {
        val bridge = sanitize(bridgeId)
        val device = sanitize(deviceId)
        val official = DabOperationRegistry.supportedOperations.map { "dab/$device/$it" }
        return DabTopics(
            request = "dab/bridge/$bridge/device/$device/request",
            response = "dab/bridge/$bridge/device/$device/response",
            status = "dab/bridge/$bridge/device/$device/status",
            officialStatus = "dab/$device/status",
            officialSubscriptions = official,
        )
    }

    fun resolve(topic: String, bridgeId: String, deviceId: String): ResolvedDabTopic? {
        val topics = topics(bridgeId, deviceId)
        if (topic == topics.request) return ResolvedDabTopic("", topics.response, TopicMode.BRIDGE)
        val prefix = "dab/${sanitize(deviceId)}/"
        if (!topic.startsWith(prefix)) return null
        val method = topic.removePrefix(prefix)
        if (!DabOperationRegistry.supportedOperations.contains(method)) return null
        return ResolvedDabTopic(method, "$topic/response", TopicMode.OFFICIAL)
    }

    fun sanitize(value: String): String = value.trim().replace(Regex("[^A-Za-z0-9._-]"), "_").ifBlank { "default" }
}
