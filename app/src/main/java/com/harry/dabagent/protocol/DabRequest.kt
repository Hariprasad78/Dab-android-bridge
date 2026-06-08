package com.harry.dabagent.protocol

import org.json.JSONObject

data class DabRequest(val requestId: String, val method: String, val params: JSONObject = JSONObject(), val timeoutMs: Long = 10_000)
