package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabOperationRegistry
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse

class OperationsListHandler : CommandHandler {
    override val method = DabMethod.OPERATIONS_LIST
    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse =
        DabResponse.ok(request, DabOperationRegistry.operationsListResult())
}
