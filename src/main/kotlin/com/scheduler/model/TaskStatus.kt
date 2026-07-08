package com.scheduler.model

enum class TaskStatus(val displayName: String) {
    RUNNING("Running"),
    WAITING("Waiting"),
    PAUSED("Paused"),
    STOPPED("Stopped"),
    DISABLED("Disabled"),
    SCHEDULED("Scheduled")
}
