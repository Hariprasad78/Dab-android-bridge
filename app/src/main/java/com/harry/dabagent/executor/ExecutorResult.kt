package com.harry.dabagent.executor

data class ExecutorResult(
    val success: Boolean,
    val status: Int,
    val stdout: String? = null,
    val stderr: String? = null,
    val error: String? = null,
    val durationMs: Long = 0,
)
