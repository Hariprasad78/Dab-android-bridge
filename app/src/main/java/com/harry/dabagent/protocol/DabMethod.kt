package com.harry.dabagent.protocol

enum class DabMethod(val wireName: String) {
    OPERATIONS_LIST("operations/list"),
    DEVICE_INFO("device/info"),
    APPLICATIONS_LIST("applications/list"),
    APPLICATIONS_LAUNCH("applications/launch"),
    APPLICATIONS_EXIT("applications/exit"),
    APPLICATIONS_GET_STATE("applications/get-state"),
    APPLICATIONS_CLEAR_DATA("applications/clear-data"),
    APPLICATIONS_INSTALL("applications/install"),
    APPLICATIONS_UNINSTALL("applications/uninstall"),
    INPUT_KEY_LIST("input/key/list"),
    INPUT_KEY_PRESS("input/key-press"),
    INPUT_LONG_KEY_PRESS("input/long-key-press"),
    INPUT_KEYEVENT("input/keyevent"),
    OUTPUT_IMAGE("output/image");

    companion object {
        fun fromWireName(value: String): DabMethod? = entries.firstOrNull { it.wireName == value }
    }
}
