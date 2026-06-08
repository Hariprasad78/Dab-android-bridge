package com.harry.dabagent

import android.content.Context
import android.os.Build

data class AgentConfig(
    val brokerHost: String,
    val brokerPort: Int,
    val username: String,
    val password: String,
    val bridgeId: String,
    val deviceId: String,
    val allowedPackages: String,
    val allowedMethods: String,
    val executorMode: String,
) {
    companion object {
        private const val PREFS = "dab_agent_config"

        fun load(context: Context): AgentConfig {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            return AgentConfig(
                brokerHost = prefs.getString("brokerHost", "test.mosquitto.org") ?: "test.mosquitto.org",
                brokerPort = prefs.getInt("brokerPort", 1883),
                username = prefs.getString("username", "") ?: "",
                password = prefs.getString("password", "") ?: "",
                bridgeId = prefs.getString("bridgeId", "lab-bridge") ?: "lab-bridge",
                deviceId = prefs.getString("deviceId", Build.MODEL.replace(' ', '-')) ?: "device",
                allowedPackages = prefs.getString("allowedPackages", "") ?: "",
                allowedMethods = prefs.getString("allowedMethods", "") ?: "",
                executorMode = prefs.getString("executorMode", "NO_PRIVILEGE") ?: "NO_PRIVILEGE",
            )
        }

        fun save(context: Context, config: AgentConfig) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString("brokerHost", config.brokerHost)
                .putInt("brokerPort", config.brokerPort)
                .putString("username", config.username)
                .putString("password", config.password)
                .putString("bridgeId", config.bridgeId)
                .putString("deviceId", config.deviceId)
                .putString("allowedPackages", config.allowedPackages)
                .putString("allowedMethods", config.allowedMethods)
                .putString("executorMode", config.executorMode)
                .apply()
        }
    }
}
