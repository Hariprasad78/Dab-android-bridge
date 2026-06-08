package com.harry.dabagent

import com.harry.dabagent.commands.CommandRouter
import com.harry.dabagent.commands.DeviceInfoHandler
import com.harry.dabagent.commands.InputKeyListHandler
import com.harry.dabagent.commands.InputKeyPressHandler
import com.harry.dabagent.commands.InputLongKeyPressHandler
import com.harry.dabagent.commands.OperationsListHandler
import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.executor.ExecutorResult
import com.harry.dabagent.input.AndroidKeyMapper
import com.harry.dabagent.input.DabKeyCode
import com.harry.dabagent.input.KeyMappingResult
import com.harry.dabagent.mqtt.MqttConnectionManager
import com.harry.dabagent.mqtt.MqttTopicRouter
import com.harry.dabagent.mqtt.TopicMode
import com.harry.dabagent.protocol.DabOperationRegistry
import com.harry.dabagent.protocol.DabRequest
import com.harry.dabagent.protocol.DabResponse
import com.harry.dabagent.protocol.JsonCodec
import com.harry.dabagent.security.CommandPolicy
import com.harry.dabagent.security.PackageAllowlist
import com.harry.dabagent.security.RequestTracker
import com.harry.dabagent.security.RequestValidator
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProtocolSecurityTest {
    @Test
    fun parsesJsonRequest() {
        val request = JsonCodec.parseRequest(
            """{"requestId":"req-001","method":"applications/launch","params":{"packageName":"com.example.app"},"timeoutMs":10000}""",
        )

        assertEquals("req-001", request.requestId)
        assertEquals("applications/launch", request.method)
        assertEquals("com.example.app", request.params.getString("packageName"))
    }

    @Test
    fun generatesJsonResponse() {
        val json = JsonCodec.encodeResponse(
            DabResponse.ok(DabRequest("req-1", "device/info"), JSONObject().put("ok", true)),
        )
        val obj = JSONObject(json)

        assertTrue(obj.getBoolean("success"))
        assertEquals(200, obj.getInt("status"))
        assertTrue(obj.getJSONObject("result").getBoolean("ok"))
    }

    @Test
    fun validatesDabKeys() {
        assertTrue(DabKeyCode.validateFormat("KEY_HOME"))
        assertTrue(DabKeyCode.validateFormat("KEY_CUSTOM_VENDOR"))
        assertFalse(DabKeyCode.validateFormat("KEY_bad"))
        assertFalse(DabKeyCode.validateFormat("HOME"))
    }

    @Test
    fun mapsDabKeysToAndroidKeyCodes() {
        assertEquals(KeyMappingResult.Mapped("KEYCODE_HOME"), AndroidKeyMapper.toAndroidKeyCode("KEY_HOME"))
        assertEquals(KeyMappingResult.Mapped("KEYCODE_BACK"), AndroidKeyMapper.toAndroidKeyCode("KEY_BACK"))
        assertEquals(KeyMappingResult.Mapped("KEYCODE_DPAD_UP"), AndroidKeyMapper.toAndroidKeyCode("KEY_UP"))
        assertEquals(KeyMappingResult.Mapped("KEYCODE_DPAD_DOWN"), AndroidKeyMapper.toAndroidKeyCode("KEY_DOWN"))
        assertEquals(KeyMappingResult.Mapped("KEYCODE_DPAD_LEFT"), AndroidKeyMapper.toAndroidKeyCode("KEY_LEFT"))
        assertEquals(KeyMappingResult.Mapped("KEYCODE_DPAD_RIGHT"), AndroidKeyMapper.toAndroidKeyCode("KEY_RIGHT"))
        assertEquals(KeyMappingResult.Mapped("KEYCODE_DPAD_CENTER"), AndroidKeyMapper.toAndroidKeyCode("KEY_ENTER"))
    }

    @Test
    fun inputKeyListResponseShape() = runBlocking {
        val response = InputKeyListHandler().handle(DabRequest("req", "input/key/list"), FakeExecutor())
        val official = JSONObject(JsonCodec.encodeOfficialResponse(response))

        assertEquals(200, official.getInt("status"))
        assertTrue(official.getJSONArray("keyCodes").toString().contains("KEY_HOME"))
        assertTrue(official.getJSONArray("keyCodes").toString().contains("KEY_ENTER"))
    }

    @Test
    fun inputKeyPressValidKeyReturnsSuccessWhenExecutorSucceeds() = runBlocking {
        val executor = FakeExecutor(pressResult = ExecutorResult(success = true, status = 200))
        val response = InputKeyPressHandler().handle(
            DabRequest("req", "input/key-press", JSONObject().put("keyCode", "KEY_LEFT")),
            executor,
        )

        assertTrue(response.success)
        assertEquals(200, response.status)
        assertEquals("KEYCODE_DPAD_LEFT", executor.lastPressedKey)
    }

    @Test
    fun inputKeyPressInvalidKeyReturns400() = runBlocking {
        val response = InputKeyPressHandler().handle(
            DabRequest("req", "input/key-press", JSONObject().put("keyCode", "KEY_UNKNOWN")),
            FakeExecutor(),
        )

        assertFalse(response.success)
        assertEquals(400, response.status)
        assertEquals("Unsupported keyCode: KEY_UNKNOWN", response.error?.message)
    }

    @Test
    fun inputKeyPressUnsupportedExecutorReturns501() = runBlocking {
        val response = InputKeyPressHandler().handle(
            DabRequest("req", "input/key-press", JSONObject().put("keyCode", "KEY_HOME")),
            FakeExecutor(pressResult = ExecutorResult(success = false, status = 501, error = "input/key-press is not implemented for current executor mode")),
        )

        assertFalse(response.success)
        assertEquals(501, response.status)
    }

    @Test
    fun inputLongKeyPressInvalidDurationReturns400() = runBlocking {
        val response = InputLongKeyPressHandler().handle(
            DabRequest("req", "input/long-key-press", JSONObject().put("keyCode", "KEY_ENTER").put("durationMs", 100)),
            FakeExecutor(),
        )

        assertFalse(response.success)
        assertEquals(400, response.status)
    }

    @Test
    fun operationsListDoesNotIncludeItself() = runBlocking {
        val response = OperationsListHandler().handle(DabRequest("req", "operations/list"), FakeExecutor())
        val operations = response.result!!.getJSONArray("operations").toString()

        assertEquals(200, response.result!!.getInt("status"))
        assertFalse(operations.contains("operations/list"))
        assertTrue(operations.contains("input/key-press"))
    }

    @Test
    fun validatesPackageNamesAndPolicy() {
        val policy = CommandPolicy(setOf("device/info"), PackageAllowlist.parse("com.allowed.app"))

        assertTrue(PackageAllowlist.isValidPackageName("com.google.android.youtube.tv"))
        assertFalse(PackageAllowlist.isValidPackageName("com.bad;rm -rf"))
        assertFalse(PackageAllowlist.isValidPackageName("bad"))
        assertTrue(policy.isMethodAllowed("device/info"))
        assertFalse(policy.isMethodAllowed("applications/launch"))
        assertTrue(policy.isPackageAllowed("com.allowed.app"))
        assertFalse(policy.isPackageAllowed("com.other.app"))
        assertTrue(CommandPolicy.isValidApkPath("/data/local/tmp/app.apk"))
        assertFalse(CommandPolicy.isValidApkPath("/data/local/tmp/../app.apk"))
        assertTrue(CommandPolicy.isValidOutputPath("/sdcard/capture.png"))
    }

    @Test
    fun methodRouting() {
        val router = CommandRouter(
            listOf(DeviceInfoHandler()),
            RequestValidator(CommandPolicy(setOf("device/info"), PackageAllowlist.parse(""))),
        )

        assertTrue(router.canRoute("device/info"))
        assertFalse(router.canRoute("applications/launch"))
    }

    @Test
    fun tracksDuplicateRequestIds() {
        val tracker = RequestTracker(maxTrackedIds = 2)

        assertTrue(tracker.markSeen("req-1"))
        assertFalse(tracker.markSeen("req-1"))
        assertTrue(tracker.markSeen("req-2"))
        assertTrue(tracker.markSeen("req-3"))
        assertTrue("Old IDs are evicted when capacity is exceeded", tracker.markSeen("req-1"))
    }

    @Test
    fun bridgeTopicRequestRoutesByMethod() = runBlocking {
        val resolved = MqttTopicRouter.resolve("dab/bridge/harry/device/adt4-ack/request", "harry", "adt4-ack")!!
        val request = JsonCodec.parseRequest(
            """{"requestId":"req-001","method":"input/key-press","params":{"keyCode":"KEY_HOME"},"timeoutMs":2000}""",
        )
        val router = keyRouter()
        val executor = FakeExecutor(pressResult = ExecutorResult(success = true, status = 200))
        val response = router.route(request, executor)

        assertEquals(TopicMode.BRIDGE, resolved.mode)
        assertTrue(response.success)
        assertEquals("KEYCODE_HOME", executor.lastPressedKey)
    }

    @Test
    fun officialTopicRoutesByMqttTopic() = runBlocking {
        val resolved = MqttTopicRouter.resolve("dab/adt4-ack/input/key-press", "harry", "adt4-ack")!!
        val request = JsonCodec.parseOfficialRequest(resolved.method, """{"keyCode":"KEY_ENTER"}""")
        val router = keyRouter()
        val executor = FakeExecutor(pressResult = ExecutorResult(success = true, status = 200))
        val response = router.route(request, executor)

        assertEquals(TopicMode.OFFICIAL, resolved.mode)
        assertEquals("input/key-press", request.method)
        assertTrue(response.success)
        assertEquals("KEYCODE_DPAD_CENTER", executor.lastPressedKey)
    }

    @Test
    fun routesTopicsAndBrokerUris() {
        val topics = MqttTopicRouter.topics("bridge/one", "device one")

        assertEquals("dab/bridge/bridge_one/device/device_one/request", topics.request)
        assertEquals("dab/bridge/bridge_one/device/device_one/response", topics.response)
        assertEquals("dab/bridge/bridge_one/device/device_one/status", topics.status)
        assertTrue(topics.officialSubscriptions.contains("dab/device_one/input/key/list"))
        assertEquals("tcp://example.com:1883", MqttConnectionManager.buildServerUri("example.com", 1883))
        assertEquals("ssl://example.com:8883", MqttConnectionManager.buildServerUri("ssl://example.com:8883", 1883))
    }

    private fun keyRouter(): CommandRouter = CommandRouter(
        listOf(InputKeyPressHandler(), InputLongKeyPressHandler(), InputKeyListHandler()),
        RequestValidator(CommandPolicy(DabOperationRegistry.supportedOperations.toSet(), PackageAllowlist.parse(""))),
    )

    private class FakeExecutor(
        private val pressResult: ExecutorResult = ExecutorResult(success = false, status = 501, error = "unsupported"),
    ) : DeviceExecutor {
        var lastPressedKey: String? = null
        override val mode: String = "FAKE"
        override suspend fun getDeviceInfo(): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun listApplications(): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun launchApplication(packageName: String): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun exitApplication(packageName: String): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun getApplicationState(packageName: String): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun clearApplicationData(packageName: String): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun installApplication(apkPath: String): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun uninstallApplication(packageName: String): ExecutorResult = ExecutorResult(success = true, status = 200)
        override suspend fun pressKey(androidKeyCode: String): ExecutorResult {
            lastPressedKey = androidKeyCode
            return pressResult
        }
        override suspend fun longPressKey(androidKeyCode: String, durationMs: Long): ExecutorResult = ExecutorResult(success = false, status = 501)
        override suspend fun captureImage(): ExecutorResult = ExecutorResult(success = true, status = 200)
    }
}
