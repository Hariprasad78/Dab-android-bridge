package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse

class ApplicationsListHandler : CommandHandler {
    override val method = DabMethod.APPLICATIONS_LIST
    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse =
        executorResponse(request, executor, executor.listApplications())
}
