package com.scheduler.model

import java.time.LocalDateTime

data class ExecutionHistory(
    val id: String = java.util.UUID.randomUUID().toString(),
    val taskId: String,
    val taskName: String,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val durationMs: Long,
    val exitCode: Int, // 0 for success, non-zero for failure
    val result: String, // Short summary: "Success" or "Failed: Error Msg"
    val output: String, // Stderr / stdout execution output
    val error: String? = null // Detailed exception message
)
