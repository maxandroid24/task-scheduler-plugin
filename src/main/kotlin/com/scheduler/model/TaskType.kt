package com.scheduler.model

enum class TaskType(val displayName: String) {
    IDE_ACTION("IDE Action"),
    IDE_MACRO("Saved Macro"),
    TERMINAL_COMMAND("Terminal Command"),
    EXTERNAL_SCRIPT("External Script")
}
