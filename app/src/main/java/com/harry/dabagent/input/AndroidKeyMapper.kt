package com.harry.dabagent.input

object AndroidKeyMapper {
    private val mapping = mapOf(
        DabKeyCode.KEY_POWER to "KEYCODE_POWER",
        DabKeyCode.KEY_HOME to "KEYCODE_HOME",
        DabKeyCode.KEY_MENU to "KEYCODE_MENU",
        DabKeyCode.KEY_EXIT to "KEYCODE_ESCAPE",
        DabKeyCode.KEY_INFO to "KEYCODE_INFO",
        DabKeyCode.KEY_UP to "KEYCODE_DPAD_UP",
        DabKeyCode.KEY_RIGHT to "KEYCODE_DPAD_RIGHT",
        DabKeyCode.KEY_DOWN to "KEYCODE_DPAD_DOWN",
        DabKeyCode.KEY_LEFT to "KEYCODE_DPAD_LEFT",
        DabKeyCode.KEY_ENTER to "KEYCODE_DPAD_CENTER",
        DabKeyCode.KEY_BACK to "KEYCODE_BACK",
        DabKeyCode.KEY_PLAY to "KEYCODE_MEDIA_PLAY",
        DabKeyCode.KEY_PAUSE to "KEYCODE_MEDIA_PAUSE",
        DabKeyCode.KEY_PLAY_PAUSE to "KEYCODE_MEDIA_PLAY_PAUSE",
        DabKeyCode.KEY_STOP to "KEYCODE_MEDIA_STOP",
        DabKeyCode.KEY_REWIND to "KEYCODE_MEDIA_REWIND",
        DabKeyCode.KEY_FAST_FORWARD to "KEYCODE_MEDIA_FAST_FORWARD",
        DabKeyCode.KEY_VOLUME_UP to "KEYCODE_VOLUME_UP",
        DabKeyCode.KEY_VOLUME_DOWN to "KEYCODE_VOLUME_DOWN",
        DabKeyCode.KEY_MUTE to "KEYCODE_VOLUME_MUTE",
        DabKeyCode.KEY_CHANNEL_UP to "KEYCODE_CHANNEL_UP",
        DabKeyCode.KEY_CHANNEL_DOWN to "KEYCODE_CHANNEL_DOWN",
        DabKeyCode.KEY_0 to "KEYCODE_0",
        DabKeyCode.KEY_1 to "KEYCODE_1",
        DabKeyCode.KEY_2 to "KEYCODE_2",
        DabKeyCode.KEY_3 to "KEYCODE_3",
        DabKeyCode.KEY_4 to "KEYCODE_4",
        DabKeyCode.KEY_5 to "KEYCODE_5",
        DabKeyCode.KEY_6 to "KEYCODE_6",
        DabKeyCode.KEY_7 to "KEYCODE_7",
        DabKeyCode.KEY_8 to "KEYCODE_8",
        DabKeyCode.KEY_9 to "KEYCODE_9",
    )

    fun toAndroidKeyCode(keyCode: String): KeyMappingResult {
        if (!DabKeyCode.validateFormat(keyCode)) return KeyMappingResult.Invalid("Invalid keyCode: $keyCode")
        if (DabKeyCode.isCustom(keyCode)) return KeyMappingResult.CustomUnsupported("Custom keyCode mapping is not implemented: $keyCode")
        val dabKey = DabKeyCode.fromWireName(keyCode) ?: return KeyMappingResult.Invalid("Unsupported keyCode: $keyCode")
        return KeyMappingResult.Mapped(mapping.getValue(dabKey))
    }
}

sealed class KeyMappingResult {
    data class Mapped(val androidKeyCode: String) : KeyMappingResult()
    data class Invalid(val message: String) : KeyMappingResult()
    data class CustomUnsupported(val message: String) : KeyMappingResult()
}
