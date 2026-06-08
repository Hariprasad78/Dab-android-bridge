package com.harry.dabagent.executor

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class NoPrivilegeExecutor(private val context: Context) : DeviceExecutor {
    override val mode = "NO_PRIVILEGE"

    override suspend fun getDeviceInfo(): ExecutorResult = ok(
        "manufacturer=${android.os.Build.MANUFACTURER}\n" +
            "model=${android.os.Build.MODEL}\n" +
            "sdk=${android.os.Build.VERSION.SDK_INT}\n" +
            "executor=$mode",
    )

    override suspend fun listApplications(): ExecutorResult = withContext(Dispatchers.IO) {
        val apps = context.packageManager.getInstalledApplications(0)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .joinToString("\n") { it.packageName }
        ok(apps)
    }

    override suspend fun launchApplication(packageName: String): ExecutorResult = withContext(Dispatchers.Main) {
        val intent: Intent? = context.packageManager.getLaunchIntentForPackage(packageName)
        if (intent == null) {
            unsupported("No launchable activity for $packageName")
        } else {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            ok("launched $packageName")
        }
    }

    override suspend fun exitApplication(packageName: String): ExecutorResult =
        unsupported("applications/exit is not implemented for current executor mode")

    override suspend fun getApplicationState(packageName: String): ExecutorResult =
        ok("installed=${isInstalled(packageName)}")

    override suspend fun clearApplicationData(packageName: String): ExecutorResult =
        unsupported("applications/clear-data is not implemented for current executor mode")

    override suspend fun installApplication(apkPath: String): ExecutorResult =
        unsupported("applications/install is not implemented for current executor mode")

    override suspend fun uninstallApplication(packageName: String): ExecutorResult =
        unsupported("applications/uninstall is not implemented for current executor mode")

    override suspend fun pressKey(androidKeyCode: String): ExecutorResult =
        unsupported("input/key-press is not implemented for current executor mode")

    override suspend fun longPressKey(androidKeyCode: String, durationMs: Long): ExecutorResult =
        unsupported("input/long-key-press is not implemented for current executor mode")

    override suspend fun captureImage(): ExecutorResult =
        unsupported("output/image is not implemented for current executor mode")

    private fun isInstalled(packageName: String): Boolean = try {
        context.packageManager.getPackageInfo(packageName, 0)
        true
    } catch (_: Exception) {
        false
    }

    private fun ok(stdout: String): ExecutorResult = ExecutorResult(success = true, status = 200, stdout = stdout)

    private fun unsupported(message: String): ExecutorResult = ExecutorResult(success = false, status = 501, error = message)
}
