package com.harry.dabagent.executor

import android.util.Log
import com.harry.dabagent.security.CommandPolicy
import com.harry.dabagent.security.PackageAllowlist
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SelfAdbExecutor : DeviceExecutor {
    override val mode = "SELF_ADB_PLACEHOLDER"

    override suspend fun getDeviceInfo(): ExecutorResult = runAdbShellCommand(listOf("getprop"), DEFAULT_TIMEOUT_MS)

    override suspend fun listApplications(): ExecutorResult = runAdbShellCommand(listOf("pm", "list", "packages"), DEFAULT_TIMEOUT_MS)

    override suspend fun launchApplication(packageName: String): ExecutorResult = runPackageCommand(
        packageName,
        listOf("monkey", "-p", packageName, "-c", "android.intent.category.LAUNCHER", "1"),
        DEFAULT_TIMEOUT_MS,
    )

    override suspend fun exitApplication(packageName: String): ExecutorResult = runPackageCommand(
        packageName,
        listOf("am", "force-stop", packageName),
        DEFAULT_TIMEOUT_MS,
    )

    override suspend fun getApplicationState(packageName: String): ExecutorResult {
        if (!PackageAllowlist.isValidPackageName(packageName)) return validationError("Invalid package name")
        val started = System.currentTimeMillis()
        val installed = runAdbShellCommand(listOf("pm", "path", packageName), DEFAULT_TIMEOUT_MS)
        val pid = runAdbShellCommand(listOf("pidof", packageName), DEFAULT_TIMEOUT_MS)
        val foreground = runAdbShellCommand(listOf("dumpsys", "window"), DEFAULT_TIMEOUT_MS)
        return ExecutorResult(
            success = true,
            status = 200,
            stdout = buildString {
                appendLine("installed=${installed.success}")
                appendLine("pid=${pid.stdout?.trim().orEmpty()}")
                appendLine("foreground=${foreground.stdout?.contains(packageName) == true}")
            },
            stderr = listOfNotNull(installed.stderr, pid.stderr, foreground.stderr).filter { it.isNotBlank() }.joinToString("\n"),
            durationMs = System.currentTimeMillis() - started,
        )
    }

    override suspend fun clearApplicationData(packageName: String): ExecutorResult = runPackageCommand(
        packageName,
        listOf("pm", "clear", packageName),
        DEFAULT_TIMEOUT_MS,
    )

    override suspend fun installApplication(apkPath: String): ExecutorResult =
        if (CommandPolicy.isValidApkPath(apkPath)) {
            runAdbShellCommand(listOf("pm", "install", "-r", apkPath), INSTALL_TIMEOUT_MS)
        } else {
            validationError("Invalid APK path")
        }

    override suspend fun uninstallApplication(packageName: String): ExecutorResult = runPackageCommand(
        packageName,
        listOf("pm", "uninstall", packageName),
        DEFAULT_TIMEOUT_MS,
    )

    override suspend fun pressKey(androidKeyCode: String): ExecutorResult {
        if (!CommandPolicy.isValidAndroidKeyEvent(androidKeyCode)) return validationError("Invalid Android keyCode")
        val result = runAdbShellCommand(listOf("input", "keyevent", androidKeyCode), KEY_TIMEOUT_MS)
        if (result.durationMs > KEY_TIMEOUT_MS) Log.w("DabAgent", "input/key-press exceeded ${KEY_TIMEOUT_MS}ms: ${result.durationMs}ms")
        return result
    }

    override suspend fun longPressKey(androidKeyCode: String, durationMs: Long): ExecutorResult = ExecutorResult(
        success = false,
        status = 501,
        error = "input/long-key-press is not implemented for current executor mode",
    )

    override suspend fun captureImage(): ExecutorResult =
        runAdbShellCommand(listOf("screencap", "-p", DEFAULT_SCREENSHOT_PATH), DEFAULT_TIMEOUT_MS)

    private suspend fun runPackageCommand(packageName: String, command: List<String>, timeoutMs: Long): ExecutorResult =
        if (PackageAllowlist.isValidPackageName(packageName)) {
            runAdbShellCommand(command, timeoutMs)
        } else {
            validationError("Invalid package name")
        }

    suspend fun runAdbShellCommand(args: List<String>, timeoutMs: Long): ExecutorResult = withContext(Dispatchers.IO) {
        val started = System.currentTimeMillis()
        try {
            // TODO: Replace direct command execution with a real authenticated self-ADB binary transport.
            // The args list is passed directly to ProcessBuilder and must only contain validated values.
            val process = ProcessBuilder(args).redirectErrorStream(false).start()
            val completed = process.waitFor(timeoutMs.coerceIn(500, 120_000), TimeUnit.MILLISECONDS)
            if (!completed) {
                process.destroyForcibly()
                return@withContext ExecutorResult(
                    success = false,
                    status = 504,
                    stderr = "Command timed out",
                    error = "Command timed out",
                    durationMs = System.currentTimeMillis() - started,
                )
            }
            val exitCode = process.exitValue()
            val stderr = process.errorStream.bufferedReader().readText()
            ExecutorResult(
                success = exitCode == 0,
                status = if (exitCode == 0) 200 else 500,
                stdout = process.inputStream.bufferedReader().readText(),
                stderr = stderr,
                error = stderr.takeIf { exitCode != 0 && it.isNotBlank() },
                durationMs = System.currentTimeMillis() - started,
            )
        } catch (e: Exception) {
            ExecutorResult(
                success = false,
                status = 500,
                stderr = e.message,
                error = e.message ?: e.javaClass.simpleName,
                durationMs = System.currentTimeMillis() - started,
            )
        }
    }

    private fun validationError(message: String): ExecutorResult = ExecutorResult(success = false, status = 400, error = message)

    companion object {
        private const val DEFAULT_TIMEOUT_MS = 10_000L
        private const val INSTALL_TIMEOUT_MS = 120_000L
        private const val KEY_TIMEOUT_MS = 2_000L
        private const val DEFAULT_SCREENSHOT_PATH = "/sdcard/dabagent-screenshot.png"
    }
}
