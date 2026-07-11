package com.scheduler.scheduler

import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory
import com.scheduler.persistence.TaskStorage

class CombinedTaskExecutor(private val schedulerManager: SchedulerManager) : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val steps = task.target.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { part ->
            val subParts = part.split(":")
            val id = subParts[0]
            val reps = if (subParts.size > 1) subParts[1].toIntOrNull() ?: 1 else 1
            Pair(id, reps)
        }
        val storage = TaskStorage.getInstance(project)
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        
        outputBuilder.append("Executing Combined Task Sequence: ${task.name}\n")
        outputBuilder.append("Total tasks in sequence: ${steps.size}\n")
        outputBuilder.append("--------------------------------------------------\n\n")

        var exitCode = 0
        var successCount = 0
        var executedCount = 0

        for ((index, step) in steps.withIndex()) {
            val (subTaskId, reps) = step
            val subTask = storage.getTask(subTaskId)
            if (subTask == null) {
                outputBuilder.append("Step ${index + 1}: ERROR - Task with ID '$subTaskId' not found.\n")
                exitCode = -1
                break
            }

            val executor = schedulerManager.getExecutor(subTask.type)
            if (executor == null) {
                outputBuilder.append("Step ${index + 1}: ERROR - No executor registered for type '${subTask.type}'.\n")
                exitCode = -1
                break
            }

            var stepFailed = false
            for (r in 1..reps) {
                val repText = if (reps > 1) " (Run $r of $reps)" else ""
                outputBuilder.append("Step ${index + 1}$repText: Running task '${subTask.name}' (${subTask.type.displayName})...\n")
                
                try {
                    val history = executor.execute(project, subTask)
                    
                    outputBuilder.append("--- Output for '${subTask.name}'$repText ---\n")
                    outputBuilder.append(history.output.trimEnd()).append("\n")
                    outputBuilder.append("--- End of output (exit code: ${history.exitCode}) ---\n\n")

                    if (history.exitCode != 0) {
                        outputBuilder.append("Step ${index + 1}$repText failed with exit code ${history.exitCode}. Halting sequence.\n")
                        exitCode = history.exitCode
                        stepFailed = true
                        break
                    }
                    successCount++
                } catch (e: Exception) {
                    outputBuilder.append("Step ${index + 1}$repText threw an exception: ${e.message}\n")
                    outputBuilder.append(e.stackTraceToString()).append("\n")
                    exitCode = -1
                    stepFailed = true
                    break
                }
            }
            if (stepFailed) {
                break
            }
            executedCount++
        }

        val duration = System.currentTimeMillis() - startTime
        val totalReps = steps.map { it.second }.sum()
        val resultText = if (exitCode == 0) {
            "Successfully completed all $successCount sub-task runs"
        } else {
            "Failed at step ${executedCount + 1} (executed $successCount of $totalReps runs successfully)"
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
