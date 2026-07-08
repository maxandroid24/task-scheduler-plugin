package com.scheduler.scheduler

import com.intellij.ide.actionMacro.ActionMacroManager
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory

class MacroTaskExecutor : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val macroName = task.target
        val startTime = System.currentTimeMillis()
        val macroManager = ActionMacroManager.getInstance()
        
        val macro = macroManager.allMacros.firstOrNull { it.name == macroName }
            ?: throw IllegalArgumentException("Macro with name '$macroName' was not found.")

        var success = false
        val outputBuilder = StringBuilder()
        outputBuilder.append("Playing IDE Macro: $macroName\n")

        ApplicationManager.getApplication().invokeAndWait {
            try {
                macroManager.playMacro(macro)
                success = true
                outputBuilder.append("Macro playback completed successfully.")
            } catch (e: Exception) {
                outputBuilder.append("Macro playback failed: ${e.message}\n")
                throw e
            }
        }

        val duration = System.currentTimeMillis() - startTime
        return ExecutionHistory(
            taskId = task.id,
            taskName = task.name,
            durationMs = duration,
            exitCode = if (success) 0 else 1,
            result = if (success) "Macro Playback Completed" else "Failed",
            output = outputBuilder.toString(),
        )
    }
}
