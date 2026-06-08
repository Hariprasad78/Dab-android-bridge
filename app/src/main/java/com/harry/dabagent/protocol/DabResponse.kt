package com.harry.dabagent.protocol

import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class DabResponse(
    val requestId: String,
    val method: String,
    val status: Int,
    val success: Boolean,
    val result: JSONObject?,
    val error: DabError?,
    val timestamp: String = isoNow(),
) {
    companion object {
        fun ok(request: DabRequest, result: JSONObject = JSONObject(), status: Int = 200): DabResponse = DabResponse(
            requestId = request.requestId,
            method = request.method,
            status = status,
            success = true,
            result = result,
            error = null,
        )

        fun fail(request: DabRequest, status: Int, code: String, message: String): DabResponse = DabResponse(
            requestId = request.requestId,
            method = request.method,
            status = status,
            success = false,
            result = null,
            error = DabError(code, message),
        )

        fun fail(requestId: String, method: String, status: Int, code: String, message: String): DabResponse = DabResponse(
            requestId = requestId,
            method = method,
            status = status,
            success = false,
            result = null,
            error = DabError(code, message),
        )
    }
}

private fun isoNow(): String = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
    .apply { timeZone = TimeZone.getTimeZone("UTC") }
    .format(Date())
