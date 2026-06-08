package com.harry.dabagent.executor

import org.json.JSONObject

interface DeviceExecutor {
    val mode: String
    suspend fun getDeviceInfo(): ExecutorResult
    suspend fun listApplications(): ExecutorResult
    suspend fun launchApplication(packageName: String): ExecutorResult
    suspend fun exitApplication(packageName: String): ExecutorResult
    suspend fun getApplicationState(packageName: String): ExecutorResult
    suspend fun clearApplicationData(packageName: String): ExecutorResult
    suspend fun installApplication(apkPath: String): ExecutorResult
    suspend fun uninstallApplication(packageName: String): ExecutorResult
    suspend fun pressKey(androidKeyCode: String): ExecutorResult
    suspend fun longPressKey(androidKeyCode: String, durationMs: Long): ExecutorResult
    suspend fun captureImage(): ExecutorResult

    fun resultJson(result: ExecutorResult): JSONObject = JSONObject()
        .put("stdout", result.stdout ?: "")
        .put("stderr", result.stderr ?: "")
        .put("status", result.status)
        .put("durationMs", result.durationMs)
        .apply { result.error?.let { put("error", it) } }
}
