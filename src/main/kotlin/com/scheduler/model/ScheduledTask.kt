package com.scheduler.model

import java.util.UUID

data class ScheduledTask(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "",
    var type: TaskType = TaskType.TERMINAL_COMMAND,
    var target: String = "", // IDE Action ID, Macro Name, Command, or Script Path
    var delaySeconds: Long = 0,
    var repeatIntervalSeconds: Long = 0, // 0 means run once
    var isRepeat: Boolean = false,
    var isEnabled: Boolean = true,
    var status: TaskStatus = TaskStatus.WAITING,
    var lastRunTimestamp: Long? = null,
    var nextRunTimestamp: Long? = null,
    var runCount: Int = 0,
    var autoStart: Boolean = true,
    var description: String = "",
    var arguments: Map<String, String> = emptyMap()
)
