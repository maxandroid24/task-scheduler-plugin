package com.scheduler.scheduler

import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory

/**
 * Strategy interface following open-closed principle to easily support future
 * automation and integrations like AI CLI, REST APIs, Git etc.
 */
interface TaskExecutor {
    @Throws(Exception::class)
    fun execute(project: Project, task: ScheduledTask): ExecutionHistory
}
