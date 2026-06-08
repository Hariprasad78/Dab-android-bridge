package com.harry.dabagent.commands

import android.util.Log
import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.input.AndroidKeyMapper
import com.harry.dabagent.input.KeyMappingResult
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse
import org.json.JSONObject

class InputKeyPressHandler : CommandHandler {
    override val method = DabMethod.INPUT_KEY_PRESS

    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse {
        val keyCode = request.params.optString("keyCode").takeIf { it.isNotBlank() }
            ?: return DabResponse.fail(request, 400, "BAD_REQUEST", "keyCode is required")
        val androidKeyCode = when (val mapped = AndroidKeyMapper.toAndroidKeyCode(keyCode)) {
            is KeyMappingResult.Mapped -> mapped.androidKeyCode
            is KeyMappingResult.Invalid -> return DabResponse.fail(request, 400, "BAD_REQUEST", mapped.message)
            is KeyMappingResult.CustomUnsupported -> return DabResponse.fail(request, 501, "NOT_IMPLEMENTED", mapped.message)
        }
        val result = executor.pressKey(androidKeyCode)
        if (result.durationMs > KEY_PRESS_TIMEOUT_MS) {
            Log.w("DabAgent", "input/key-press exceeded ${KEY_PRESS_TIMEOUT_MS}ms: ${result.durationMs}ms")
        }
        return if (result.success) {
            DabResponse.ok(request, JSONObject().put("status", 200), 200)
        } else {
            DabResponse.fail(request, result.status, if (result.status == 501) "NOT_IMPLEMENTED" else "EXECUTION_FAILED", result.error ?: "input/key-press failed")
        }
    }

    companion object {
        const val KEY_PRESS_TIMEOUT_MS = 2_000L
    }
}
