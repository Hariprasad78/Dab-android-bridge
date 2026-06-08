package com.harry.dabagent.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import com.harry.dabagent.AgentConfig
import com.harry.dabagent.commands.ApplicationClearDataHandler
import com.harry.dabagent.commands.ApplicationExitHandler
import com.harry.dabagent.commands.ApplicationGetStateHandler
import com.harry.dabagent.commands.ApplicationInstallHandler
import com.harry.dabagent.commands.ApplicationLaunchHandler
import com.harry.dabagent.commands.ApplicationUninstallHandler
import com.harry.dabagent.commands.ApplicationsListHandler
import com.harry.dabagent.commands.CommandHandler
import com.harry.dabagent.commands.CommandRouter
import com.harry.dabagent.commands.DeviceInfoHandler
import com.harry.dabagent.commands.InputKeyEventHandler
import com.harry.dabagent.commands.InputKeyListHandler
import com.harry.dabagent.commands.InputKeyPressHandler
import com.harry.dabagent.commands.InputLongKeyPressHandler
import com.harry.dabagent.commands.OperationsListHandler
import com.harry.dabagent.commands.OutputImageHandler
import com.harry.dabagent.executor.DeviceExecutor
import com.harry.dabagent.executor.NoPrivilegeExecutor
import com.harry.dabagent.executor.SelfAdbExecutor
import com.harry.dabagent.mqtt.DabTopics
import com.harry.dabagent.mqtt.MqttConnectionManager
import com.harry.dabagent.mqtt.MqttTopicRouter
import com.harry.dabagent.mqtt.TopicMode
import com.harry.dabagent.protocol.DabResponse
import com.harry.dabagent.protocol.JsonCodec
import com.harry.dabagent.security.CommandPolicy
import com.harry.dabagent.security.PackageAllowlist
import com.harry.dabagent.security.RequestTracker
import com.harry.dabagent.security.RequestValidator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

class DabAgentService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val requestTracker = RequestTracker()
    private var mqtt: MqttConnectionManager? = null
    private var topics: DabTopics? = null

    override fun onCreate() {
        super.onCreate()
        startForeground(NOTIFICATION_ID, notification("Starting"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        scope.launch { startAgent() }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun startAgent() {
        val config = AgentConfig.load(this)
        val resolvedTopics = MqttTopicRouter.topics(config.bridgeId, config.deviceId)
        val executor = createExecutor(config)
        val router = createRouter(config)
        val manager = MqttConnectionManager(
            onMessage = { topic, payload -> handlePayload(topic, payload, config, router, executor) },
            onLog = ::log,
        )
        mqtt?.disconnect()
        mqtt = manager
        topics = resolvedTopics
        runCatching {
            withContext(Dispatchers.IO) {
                manager.connect(
                    brokerUri = config.brokerHost,
                    port = config.brokerPort,
                    username = config.username.ifBlank { null },
                    password = config.password.ifBlank { null },
                    topics = resolvedTopics,
                )
                val statusPayload = JSONObject()
                    .put("online", true)
                    .put("executorMode", executor.mode)
                    .put("deviceId", config.deviceId)
                    .toString()
                manager.publish(resolvedTopics.status, statusPayload, retained = true)
                manager.publish(resolvedTopics.officialStatus, statusPayload, retained = true)
            }
        }.onFailure { log("MQTT connect failed: ${it.message}") }
    }

    private fun handlePayload(
        topic: String,
        payload: String,
        config: AgentConfig,
        router: CommandRouter,
        executor: DeviceExecutor,
    ) {
        record("lastRequest", "[$topic] $payload")
        log("Last request on $topic: $payload")
        val resolved = MqttTopicRouter.resolve(topic, config.bridgeId, config.deviceId)
        if (resolved == null) {
            log("Ignored unsupported topic: $topic")
            return
        }
        scope.launch {
            val response = runCatching {
                val request = if (resolved.mode == TopicMode.BRIDGE) {
                    JsonCodec.parseRequest(payload)
                } else {
                    JsonCodec.parseOfficialRequest(resolved.method, payload)
                }
                if (!requestTracker.markSeen(request.requestId)) {
                    DabResponse.fail(request, 409, "DUPLICATE_REQUEST", "Request ID has already been processed")
                } else {
                    router.route(request, executor)
                }
            }.getOrElse { error ->
                DabResponse.fail("", resolved.method, 400, "BAD_REQUEST", error.message ?: "Invalid request")
            }
            val encoded = if (resolved.mode == TopicMode.BRIDGE) {
                JsonCodec.encodeResponse(response)
            } else {
                JsonCodec.encodeOfficialResponse(response)
            }
            mqtt?.publish(resolved.responseTopic, encoded)
            record("lastResponse", "[${resolved.responseTopic}] $encoded")
            log("Last response on ${resolved.responseTopic}: $encoded")
        }
    }

    private fun createExecutor(config: AgentConfig): DeviceExecutor =
        if (config.executorMode == "SELF_ADB_PLACEHOLDER") {
            SelfAdbExecutor()
        } else {
            NoPrivilegeExecutor(applicationContext)
        }

    private fun createRouter(config: AgentConfig): CommandRouter {
        val policy = CommandPolicy(
            allowedMethods = CommandPolicy.parseMethods(config.allowedMethods),
            packageAllowlist = PackageAllowlist.parse(config.allowedPackages),
        )
        return CommandRouter(handlers(), RequestValidator(policy))
    }

    private fun handlers(): List<CommandHandler> = listOf(
        OperationsListHandler(),
        DeviceInfoHandler(),
        ApplicationsListHandler(),
        ApplicationLaunchHandler(),
        ApplicationExitHandler(),
        ApplicationGetStateHandler(),
        ApplicationClearDataHandler(),
        ApplicationInstallHandler(),
        ApplicationUninstallHandler(),
        InputKeyListHandler(),
        InputKeyPressHandler(),
        InputLongKeyPressHandler(),
        InputKeyEventHandler(),
        OutputImageHandler(),
    )

    private fun record(key: String, value: String) {
        getSharedPreferences("dab_agent_logs", MODE_PRIVATE).edit().putString(key, value).apply()
    }

    private fun log(line: String) {
        record("lastLog", line)
    }

    private fun notification(text: String): Notification {
        val channelId = "dab_agent"
        if (Build.VERSION.SDK_INT >= 26) {
            getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(channelId, "DabDeviceAgent", NotificationManager.IMPORTANCE_LOW),
            )
        }
        val builder = if (Build.VERSION.SDK_INT >= 26) Notification.Builder(this, channelId) else Notification.Builder(this)
        return builder
            .setContentTitle("DabDeviceAgent")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .build()
    }

    override fun onDestroy() {
        val resolvedTopics = topics
        val manager = mqtt
        if (resolvedTopics != null && manager?.isConnected == true) {
            runCatching {
                val offlinePayload = JSONObject().put("online", false).toString()
                manager.publish(resolvedTopics.status, offlinePayload, retained = true)
                manager.publish(resolvedTopics.officialStatus, offlinePayload, retained = true)
            }
        }
        manager?.disconnect()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val NOTIFICATION_ID = 42
    }
}
