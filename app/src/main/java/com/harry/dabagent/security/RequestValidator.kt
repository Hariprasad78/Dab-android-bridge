package com.harry.dabagent.security

import com.harry.dabagent.protocol.DabMethod
import com.harry.dabagent.protocol.DabRequest

class RequestValidator(private val policy: CommandPolicy) {
    fun validate(request: DabRequest): String? {
        if (request.requestId.isBlank()) return "requestId is required"
        if (DabMethod.fromWireName(request.method) == null) return "Unsupported method: ${request.method}"
        if (!policy.isMethodAllowed(request.method)) return "Method denied by command policy: ${request.method}"
        val pkg = request.params.optString("packageName", "")
        if (pkg.isNotBlank() && !policy.isPackageAllowed(pkg)) return "Package denied or invalid: $pkg"
        val apkPath = request.params.optString("apkPath", "")
        if (apkPath.isNotBlank() && !CommandPolicy.isValidApkPath(apkPath)) return "Invalid APK path"
        val key = request.params.optString("keyCode", request.params.optString("keyevent", ""))
        if (key.isNotBlank() && !CommandPolicy.isValidDabKeyEvent(key) && !CommandPolicy.isValidAndroidKeyEvent(key)) {
            return "Invalid key event format: $key"
        }
        return null
    }
}
