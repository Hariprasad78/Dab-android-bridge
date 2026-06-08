package com.harry.dabagent.commands

import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse

class ApplicationUninstallHandler : CommandHandler {
    override val method = DabMethod.APPLICATIONS_UNINSTALL
    override suspend fun handle(request: DabRequest, executor: DeviceExecutor): DabResponse = runCatching {
        executorResponse(request, executor, executor.uninstallApplication(requireParam(request, "packageName")))
    }.getOrElse { DabResponse.fail(request, 400, "BAD_REQUEST", it.message ?: "bad request") }
}
