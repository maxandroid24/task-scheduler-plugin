package com.scheduler.scheduler

import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory
import com.scheduler.persistence.TaskStorage

class CombinedTaskExecutor(private val schedulerManager: SchedulerManager) : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val subTaskIds = task.target.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        val storage = TaskStorage.getInstance(project)
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        
        outputBuilder.append("Executing Combined Task Sequence: ${task.name}\n")
        outputBuilder.append("Total tasks in sequence: ${subTaskIds.size}\n")
        outputBuilder.append("--------------------------------------------------\n\n")

        var exitCode = 0
        var successCount = 0

        for ((index, subTaskId) in subTaskIds.withIndex()) {
            val subTask = storage.getTask(subTaskId)
            if (subTask == null) {
                outputBuilder.append("Step ${index + 1}: ERROR - Task with ID '$subTaskId' not found.\n")
                exitCode = -1
                break
            }

            outputBuilder.append("Step ${index + 1}: Running task '${subTask.name}' (${subTask.type.displayName})...\n")
            val executor = schedulerManager.getExecutor(subTask.type)
            if (executor == null) {
                outputBuilder.append("Step ${index + 1}: ERROR - No executor registered for type '${subTask.type}'.\n")
                exitCode = -1
                break
            }

            try {
                val history = executor.execute(project, subTask)
                
                outputBuilder.append("--- Output for '${subTask.name}' ---\n")
                outputBuilder.append(history.output.trimEnd()).append("\n")
                outputBuilder.append("--- End of output (exit code: ${history.exitCode}) ---\n\n")

                if (history.exitCode != 0) {
                    outputBuilder.append("Step ${index + 1} failed with exit code ${history.exitCode}. Halting sequence.\n")
                    exitCode = history.exitCode
                    break
                }
                successCount++
            } catch (e: Exception) {
                outputBuilder.append("Step ${index + 1} threw an exception: ${e.message}\n")
                outputBuilder.append(e.stackTraceToString()).append("\n")
                exitCode = -1
                break
            }
        }

        val duration = System.currentTimeMillis() - startTime
        val resultText = if (exitCode == 0) {
            "Successfully completed all $successCount sub-tasks"
        } else {
            "Failed at step ${successCount + 1} (executed $successCount of ${subTaskIds.size} successfully)"
        }

        return ExecutionHistory(
            taskId = task.id,
            taskName = task.name,
            durationMs = duration,
            exitCode = exitCode,
            result = resultText,
            output = outputBuilder.toString()
        )
    }
}
