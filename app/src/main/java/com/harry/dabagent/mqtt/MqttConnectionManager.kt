package com.harry.dabagent.mqtt

import android.util.Log
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended
import org.eclipse.paho.client.mqttv3.MqttClient
import org.eclipse.paho.client.mqttv3.MqttConnectOptions
import org.eclipse.paho.client.mqttv3.MqttMessage
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence
import java.net.URI
import java.util.UUID

class MqttConnectionManager(
    private val onMessage: (String, String) -> Unit,
    private val onLog: (String) -> Unit,
) : MqttPublisher {
    private var client: MqttClient? = null
    private var requestTopic: String = ""

    val isConnected: Boolean
        get() = client?.isConnected == true

    fun connect(
        brokerUri: String,
        port: Int,
        username: String?,
        password: String?,
        topics: DabTopics,
    ) {
        disconnect()
        requestTopic = topics.request
        val serverUri = buildServerUri(brokerUri, port)
        val mqttClient = MqttClient(serverUri, "DabDeviceAgent-${UUID.randomUUID()}", MemoryPersistence())
        mqttClient.setCallback(
            object : MqttCallbackExtended {
                override fun connectComplete(reconnect: Boolean, serverURI: String?) {
                    onLog("MQTT connected: $serverURI")
                    mqttClient.subscribe(requestTopic, 1)
                    topics.officialSubscriptions.forEach { mqttClient.subscribe(it, 1) }
                }

                override fun connectionLost(cause: Throwable?) {
                    onLog("MQTT connection lost: ${cause?.message}")
                }

                override fun messageArrived(topic: String, message: MqttMessage) {
                    onMessage(topic, message.toString())
                }

                override fun deliveryComplete(token: IMqttDeliveryToken?) = Unit
            },
        )
        val options = MqttConnectOptions().apply {
            isAutomaticReconnect = true
            isCleanSession = true
            if (!username.isNullOrBlank()) userName = username
            if (!password.isNullOrBlank()) this.password = password.toCharArray()
        }
        mqttClient.connect(options)
        client = mqttClient
    }

    override fun publish(topic: String, payload: String, retained: Boolean) {
        val message = MqttMessage(payload.toByteArray()).apply {
            qos = 1
            isRetained = retained
        }
        client?.publish(topic, message) ?: Log.w("DabAgent", "Publish skipped while disconnected")
    }

    fun disconnect() {
        runCatching { client?.disconnect() }
        client = null
    }

    companion object {
        fun buildServerUri(brokerUri: String, port: Int): String {
            val trimmed = brokerUri.trim()
            val withScheme = if (trimmed.startsWith("tcp://") || trimmed.startsWith("ssl://")) {
                trimmed
            } else {
                "tcp://$trimmed"
            }
            val parsed = URI(withScheme)
            return if (parsed.port > 0) {
                withScheme
            } else {
                URI(parsed.scheme, parsed.userInfo, parsed.host, port, parsed.path, parsed.query, parsed.fragment).toString()
            }
        }
    }
}
