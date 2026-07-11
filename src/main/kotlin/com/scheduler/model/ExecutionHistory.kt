package com.scheduler.model



data class ExecutionHistory(
    var id: String = java.util.UUID.randomUUID().toString(),
    var taskId: String = "",
    var taskName: String = "",
    var timestamp: Long = System.currentTimeMillis(),
    var durationMs: Long = 0,
    var exitCode: Int = 0, // 0 for success, non-zero for failure
    var result: String = "", // Short summary: "Success" or "Failed: Error Msg"
    var output: String = "", // Stderr / stdout execution output
    var error: String? = null // Detailed exception message
)
