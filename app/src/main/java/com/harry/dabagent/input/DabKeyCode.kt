package com.harry.dabagent.input

enum class DabKeyCode {
    KEY_POWER,
    KEY_HOME,
    KEY_MENU,
    KEY_EXIT,
    KEY_INFO,
    KEY_UP,
    KEY_RIGHT,
    KEY_DOWN,
    KEY_LEFT,
    KEY_ENTER,
    KEY_BACK,
    KEY_PLAY,
    KEY_PAUSE,
    KEY_PLAY_PAUSE,
    KEY_STOP,
    KEY_REWIND,
    KEY_FAST_FORWARD,
    KEY_VOLUME_UP,
    KEY_VOLUME_DOWN,
    KEY_MUTE,
    KEY_CHANNEL_UP,
    KEY_CHANNEL_DOWN,
    KEY_0,
    KEY_1,
    KEY_2,
    KEY_3,
    KEY_4,
    KEY_5,
    KEY_6,
    KEY_7,
    KEY_8,
    KEY_9;

    companion object {
        val KEY_REGEX = Regex("^KEY_[A-Z0-9_]{1,60}$")
        fun names(): List<String> = entries.map { it.name }
        fun validateFormat(keyCode: String): Boolean = KEY_REGEX.matches(keyCode)
        fun fromWireName(keyCode: String): DabKeyCode? = entries.firstOrNull { it.name == keyCode }
        fun isCustom(keyCode: String): Boolean = keyCode.startsWith("KEY_CUSTOM_") && validateFormat(keyCode)
    }
}
