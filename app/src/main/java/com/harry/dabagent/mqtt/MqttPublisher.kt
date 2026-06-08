package com.harry.dabagent.mqtt

interface MqttPublisher { fun publish(topic: String, payload: String, retained: Boolean = false) }
