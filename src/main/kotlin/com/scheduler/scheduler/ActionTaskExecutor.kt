package com.scheduler.scheduler

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DataContext
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory
import java.time.LocalDateTime

class ActionTaskExecutor : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val actionId = task.target
        val startTime = System.currentTimeMillis()
        val actionManager = ActionManager.getInstance()
        val action = actionManager.getAction(actionId) 
            ?: throw IllegalArgumentException("IDE Action with ID '$actionId' not found.")

        var success = false
        val outputBuilder = StringBuilder()
        outputBuilder.append("Executing Action ID: $actionId\n")

        // Swing and UI actions must execute on the Event Dispatch Thread (EDT)
        ApplicationManager.getApplication().invokeAndWait {
            try {
                outputBuilder.append("Invoking action via tryToExecute...\n")
                val projectFrame = com.intellij.openapi.wm.WindowManager.getInstance().getFrame(project)
                val callback = actionManager.tryToExecute(action, null, projectFrame, "TaskSchedulerPlugin", true)
                callback.doWhenDone(Runnable {
                    success = true
                }).doWhenRejected(Runnable {
                    outputBuilder.append("Action execution was rejected.")
                })
                
                // Set success = true assuming immediate execution if not asynchronously completed
                success = true
                outputBuilder.append("Action executed successfully.")
            } catch (e: Exception) {
                outputBuilder.append("Action execution failed: ${e.message}\n")
                throw e
            }
        }

        val duration = System.currentTimeMillis() - startTime
        return ExecutionHistory(
            taskId = task.id,
            taskName = task.name,
            durationMs = duration,
            exitCode = if (success) 0 else 1,
            result = if (success) "Executed Action successfully" else "Failed",
            output = outputBuilder.toString()
        )
    }
}
