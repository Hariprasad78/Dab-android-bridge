package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.input.AndroidKeyMapper
import com.harry.dabagent.input.KeyMappingResult
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse
import org.json.JSONObject

class InputLongKeyPressHandler : CommandHandler {
    override val method = DabMethod.INPUT_LONG_KEY_PRESS

    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse {
        val keyCode = request.params.optString("keyCode").takeIf { it.isNotBlank() }
            ?: return DabResponse.fail(request, 400, "BAD_REQUEST", "keyCode is required")
        val durationMs = durationMsOrNull(request)
            ?: return DabResponse.fail(request, 400, "BAD_REQUEST", "durationMs must be a number")
        if (durationMs !in MIN_DURATION_MS..MAX_DURATION_MS) {
            return DabResponse.fail(request, 400, "BAD_REQUEST", "durationMs must be between $MIN_DURATION_MS and $MAX_DURATION_MS")
        }
        val androidKeyCode = when (val mapped = AndroidKeyMapper.toAndroidKeyCode(keyCode)) {
            is KeyMappingResult.Mapped -> mapped.androidKeyCode
            is KeyMappingResult.Invalid -> return DabResponse.fail(request, 400, "BAD_REQUEST", mapped.message)
            is KeyMappingResult.CustomUnsupported -> return DabResponse.fail(request, 501, "NOT_IMPLEMENTED", mapped.message)
        }
        val result = executor.longPressKey(androidKeyCode, durationMs)
        return if (result.success) {
            DabResponse.ok(request, JSONObject().put("status", 200), 200)
        } else {
            DabResponse.fail(request, result.status, if (result.status == 501) "NOT_IMPLEMENTED" else "EXECUTION_FAILED", result.error ?: "input/long-key-press failed")
        }
    }

    private fun durationMsOrNull(request: DabRequest): Long? {
        if (!request.params.has("durationMs")) return DEFAULT_DURATION_MS
        val value = request.params.opt("durationMs")
        return when (value) {
            is Number -> value.toLong()
            is String -> value.toLongOrNull()
            else -> null
        }
    }

    companion object {
        const val DEFAULT_DURATION_MS = 1_000L
        const val MIN_DURATION_MS = 500L
        const val MAX_DURATION_MS = 5_000L
    }
}
