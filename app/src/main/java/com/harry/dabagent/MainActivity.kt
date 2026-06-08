package com.harry.dabagent

import android.app.Activity
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import com.harry.dabagent.service.DabAgentService

class MainActivity : Activity() {
    private lateinit var status: TextView
    private lateinit var logs: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val config = AgentConfig.load(this)
        val root = ScrollView(this)
        val form = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(24, 24, 24, 24)
        }

        val broker = form.field("Broker host or URI", config.brokerHost)
        val port = form.field("Broker port", config.brokerPort.toString()).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
        }
        val username = form.field("Username", config.username)
        val password = form.field("Password", config.password).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val bridge = form.field("Bridge ID", config.bridgeId)
        val device = form.field("Device ID", config.deviceId)
        val packages = form.field("Allowed packages (comma-separated, blank = any valid)", config.allowedPackages)
        val methods = form.field("Allowed methods (comma-separated, blank = defaults)", config.allowedMethods)
        val spinner = Spinner(this).apply {
            adapter = ArrayAdapter(
                this@MainActivity,
                android.R.layout.simple_spinner_dropdown_item,
                listOf("NO_PRIVILEGE", "SELF_ADB_PLACEHOLDER"),
            )
            setSelection(if (config.executorMode == "SELF_ADB_PLACEHOLDER") 1 else 0)
        }

        form.addView(TextView(this).apply { text = "Executor mode" })
        form.addView(spinner)
        status = TextView(this).apply {
            text = "Service status: stopped or unknown"
            textSize = 16f
        }
        logs = TextView(this).apply { text = logText() }

        val connect = Button(this).apply {
            text = "Save & Connect"
            setOnClickListener {
                AgentConfig.save(
                    this@MainActivity,
                    AgentConfig(
                        brokerHost = broker.text.toString().trim(),
                        brokerPort = port.text.toString().toIntOrNull() ?: 1883,
                        username = username.text.toString(),
                        password = password.text.toString(),
                        bridgeId = bridge.text.toString().trim(),
                        deviceId = device.text.toString().trim(),
                        allowedPackages = packages.text.toString(),
                        allowedMethods = methods.text.toString(),
                        executorMode = spinner.selectedItem.toString(),
                    ),
                )
                startAgentService()
                status.text = "Service status: connect requested"
                refreshLog()
            }
        }
        val disconnect = Button(this).apply {
            text = "Disconnect Service"
            setOnClickListener {
                stopService(Intent(this@MainActivity, DabAgentService::class.java))
                status.text = "Service status: stopped"
                refreshLog()
            }
        }
        val refresh = Button(this).apply {
            text = "Refresh Logs"
            setOnClickListener { refreshLog() }
        }

        form.addView(connect)
        form.addView(disconnect)
        form.addView(refresh)
        form.addView(status)
        form.addView(TextView(this).apply { text = "Last request / Last response / logs" })
        form.addView(logs)
        root.addView(form)
        setContentView(root)
        requestNotificationPermissionIfNeeded()
    }

    private fun LinearLayout.field(label: String, value: String): EditText {
        addView(TextView(this@MainActivity).apply { text = label })
        val input = EditText(this@MainActivity).apply { setText(value) }
        addView(input)
        return input
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 33) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), NOTIFICATION_PERMISSION_REQUEST)
        }
    }

    private fun startAgentService() {
        val serviceIntent = Intent(this, DabAgentService::class.java)
        if (Build.VERSION.SDK_INT >= 26) {
            startForegroundService(serviceIntent)
        } else {
            startService(serviceIntent)
        }
    }

    private fun refreshLog() {
        logs.text = logText()
    }

    private fun logText(): String {
        val prefs = getSharedPreferences("dab_agent_logs", MODE_PRIVATE)
        return "Last request: ${prefs.getString("lastRequest", "none")}\n" +
            "Last response: ${prefs.getString("lastResponse", "none")}\n" +
            "Log: ${prefs.getString("lastLog", "none")}"
    }

    companion object {
        private const val NOTIFICATION_PERMISSION_REQUEST = 1001
    }
}
