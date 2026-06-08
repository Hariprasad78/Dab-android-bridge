package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.input.DabKeyCode
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse
import org.json.JSONArray
import org.json.JSONObject

class InputKeyListHandler : CommandHandler {
    override val method = DabMethod.INPUT_KEY_LIST
    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse = DabResponse.ok(
        request,
        JSONObject()
            .put("status", 200)
            .put("keyCodes", JSONArray(DabKeyCode.names())),
    )
}
