package com.harry.dabagent.security

import com.harry.dabagent.input.DabKeyCode
import com.harry.dabagent.protocol.DabOperationRegistry

class CommandPolicy(
    private val allowedMethods: Set<String>,
    private val packageAllowlist: PackageAllowlist,
) {
    fun isMethodAllowed(method: String): Boolean = allowedMethods.isEmpty() || allowedMethods.contains(method)

    fun isPackageAllowed(packageName: String): Boolean =
        PackageAllowlist.isValidPackageName(packageName) && packageAllowlist.isAllowed(packageName)

    companion object {
        val DEFAULT_ALLOWED_METHODS: Set<String> = DabOperationRegistry.supportedOperations.toSet()
        private val ANDROID_KEY_REGEX = Regex("^KEYCODE_[A-Z0-9_]{1,60}$")

        fun parseMethods(csv: String): Set<String> = csv
            .split(',', '\n')
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        fun isValidApkPath(path: String): Boolean = path.endsWith(".apk") && isSafeDevicePath(path)

        fun isValidOutputPath(path: String): Boolean = path.endsWith(".png") && isSafeDevicePath(path)

        fun isValidDabKeyEvent(key: String): Boolean = DabKeyCode.validateFormat(key)

        fun isValidAndroidKeyEvent(key: String): Boolean = ANDROID_KEY_REGEX.matches(key)

        private fun isSafeDevicePath(path: String): Boolean = !path.contains("\u0000") &&
            !path.contains("..") &&
            (path.startsWith("/sdcard/") || path.startsWith("/storage/") || path.startsWith("/data/local/tmp/"))
    }
}
