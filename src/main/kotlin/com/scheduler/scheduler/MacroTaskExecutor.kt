package com.scheduler.scheduler

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory

class MacroTaskExecutor : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val macroName = task.target
        val startTime = System.currentTimeMillis()
        
        var success = false
        val outputBuilder = StringBuilder()
        outputBuilder.append("Playing IDE Macro: $macroName\n")

        try {
            // Reflective access to ActionMacroManager to avoid compile-time dependency on internal APIs
            val actionMacroManagerClass = Class.forName("com.intellij.ide.actionMacro.ActionMacroManager")
            val macroManager = try {
                // Try getting via Companion object
                val companionField = actionMacroManagerClass.getField("Companion")
                val companionInstance = companionField.get(null)
                val getInstanceMethod = companionInstance.javaClass.getMethod("getInstance")
                getInstanceMethod.invoke(companionInstance)
            } catch (e: Exception) {
                // Fallback to static method on ActionMacroManager class
                val getInstanceMethod = actionMacroManagerClass.getMethod("getInstance")
                getInstanceMethod.invoke(null)
            }

            val allMacros = try {
                val getAllMacrosMethod = macroManager.javaClass.getMethod("getAllMacros")
                getAllMacrosMethod.invoke(macroManager) as Collection<*>
            } catch (e: Exception) {
                val allMacrosMethod = macroManager.javaClass.getMethod("allMacros")
                allMacrosMethod.invoke(macroManager) as Collection<*>
            }

            val macro = allMacros.firstOrNull { macroObj ->
                if (macroObj == null) return@firstOrNull false
                val name = try {
                    val getNameMethod = macroObj.javaClass.getMethod("getName")
                    getNameMethod.invoke(macroObj) as String
                } catch (e: Exception) {
                    val nameMethod = macroObj.javaClass.getMethod("name")
                    nameMethod.invoke(macroObj) as String
                }
                name == macroName
            } ?: throw IllegalArgumentException("Macro with name '$macroName' was not found.")

            val playMacroMethod = try {
                macroManager.javaClass.getMethod(
                    "playMacro", 
                    Class.forName("com.intellij.ide.actionMacro.ActionMacro")
                )
            } catch (e: Exception) {
                // Fallback searching for a method named playMacro with one parameter
                macroManager.javaClass.methods.first { 
                    it.name == "playMacro" && it.parameterCount == 1 
                }
            }

            ApplicationManager.getApplication().invokeAndWait {
                try {
                    playMacroMethod.invoke(macroManager, macro)
                    success = true
                    outputBuilder.append("Macro playback completed successfully.")
                } catch (e: Exception) {
                    val cause = e.cause ?: e
                    outputBuilder.append("Macro playback failed: ${cause.message}\n")
                    throw cause
                }
            }
        } catch (e: Exception) {
            outputBuilder.append("Macro execution error: ${e.message}\n")
            throw e
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
