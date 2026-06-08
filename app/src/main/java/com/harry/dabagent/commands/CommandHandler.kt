package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.executor.ExecutorResult
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse
import com.harry.dabagent.security.RequestValidator
import org.json.JSONObject

interface CommandHandler {
    val method: DabMethod
    suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse
}

class CommandRouter(
    private val handlers: List<CommandHandler>,
    private val validator: RequestValidator,
) {
    suspend fun route(request: DabRequest, executor: DeviceExecutor): DabResponse {
        validator.validate(request)?.let { reason ->
            return DabResponse.fail(request, 403, "POLICY_DENIED", reason)
        }
        val handler = handlers.firstOrNull { it.method.wireName == request.method }
            ?: return DabResponse.fail(request, 404, "UNSUPPORTED_METHOD", "Unsupported method")
        return handler.handle(request, executor)
    }

    fun canRoute(method: String): Boolean = handlers.any { it.method.wireName == method }
}

fun executorResponse(request: DabRequest, executor: DeviceExecutor, result: ExecutorResult): DabResponse =
    if (result.success) {
        DabResponse.ok(request, executor.resultJson(result), result.status)
    } else {
        DabResponse.fail(
            request = request,
            status = result.status,
            code = if (result.status == 501) "NOT_IMPLEMENTED" else "EXECUTION_FAILED",
            message = result.error ?: result.stderr ?: "Command failed",
        )
    }

fun requireParam(request: DabRequest, name: String): String =
    request.params.optString(name).takeIf { it.isNotBlank() }
        ?: throw IllegalArgumentException("Missing parameter: $name")

fun JSONObject.defaultPath(): String = optString("path", "/sdcard/dabagent-screenshot.png")
