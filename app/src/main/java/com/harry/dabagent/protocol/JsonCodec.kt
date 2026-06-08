package com.harry.dabagent.protocol

import org.json.JSONObject
import java.util.UUID

object JsonCodec {
    fun parseRequest(json: String): DabRequest {
        val obj = if (json.isBlank()) JSONObject() else JSONObject(json)
        val requestId = obj.optString("requestId").takeIf { it.isNotBlank() } ?: error("requestId is required")
        val method = obj.optString("method").takeIf { it.isNotBlank() } ?: error("method is required")
        val params = obj.optJSONObject("params") ?: JSONObject()
        val timeoutMs = obj.optLong("timeoutMs", 10_000).coerceIn(1_000, 120_000)
        return DabRequest(requestId, method, params, timeoutMs)
    }

    fun parseOfficialRequest(topicMethod: String, json: String): DabRequest {
        val payload = if (json.isBlank()) JSONObject() else JSONObject(json)
        val requestId = payload.optString("requestId").takeIf { it.isNotBlank() } ?: "official-${UUID.randomUUID()}"
        val timeoutMs = payload.optLong("timeoutMs", 2_000).coerceIn(500, 120_000)
        return DabRequest(requestId, topicMethod, payload, timeoutMs)
    }

    fun encodeResponse(response: DabResponse): String {
        val obj = JSONObject()
            .put("requestId", response.requestId)
            .put("method", response.method)
            .put("status", response.status)
            .put("success", response.success)
            .put("timestamp", response.timestamp)
        if (response.result == null) obj.put("result", JSONObject.NULL) else obj.put("result", response.result)
        if (response.error == null) obj.put("error", JSONObject.NULL) else obj.put("error", JSONObject().put("code", response.error.code).put("message", response.error.message))
        return obj.toString()
    }

    fun encodeOfficialResponse(response: DabResponse): String {
        if (!response.success) {
            return JSONObject()
                .put("status", response.status)
                .put("error", response.error?.message ?: "Request failed")
                .toString()
        }
        val result = response.result ?: JSONObject()
        if (!result.has("status")) result.put("status", response.status)
        return result.toString()
    }
}
