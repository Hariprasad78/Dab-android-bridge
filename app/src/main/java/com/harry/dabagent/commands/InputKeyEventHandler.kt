package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse

class InputKeyEventHandler : CommandHandler {
    override val method = DabMethod.INPUT_KEYEVENT
    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse =
        InputKeyPressHandler().handle(
            DabRequest(request.requestId, DabMethod.INPUT_KEY_PRESS.wireName, request.params, request.timeoutMs),
            executor,
        )
}
