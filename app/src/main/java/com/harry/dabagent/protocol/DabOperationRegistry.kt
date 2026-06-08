package com.harry.dabagent.protocol

import org.json.JSONArray
import org.json.JSONObject

object DabOperationRegistry {
    val supportedOperations: List<String> = listOf(
        "operations/list",
        "device/info",
        "applications/list",
        "applications/launch",
        "applications/get-state",
        "applications/exit",
        "input/key/list",
        "input/key-press",
        "input/long-key-press",
        "output/image",
    )

    val advertisedOperations: List<String> = supportedOperations.filterNot { it == "operations/list" }

    fun operationsListResult(): JSONObject = JSONObject()
        .put("status", 200)
        .put("operations", JSONArray(advertisedOperations))
}
