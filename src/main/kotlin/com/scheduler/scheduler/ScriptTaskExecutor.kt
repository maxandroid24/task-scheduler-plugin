package com.scheduler.scheduler

import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.time.LocalDateTime

class ScriptTaskExecutor : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val scriptPath = task.target
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        
        val scriptFile = File(scriptPath)
        if (!scriptFile.exists()) {
            throw IllegalArgumentException("Script file not found at: $scriptPath")
        }

        // Auto-detect extension and set runner
        val extension = scriptFile.extension.lowercase()
        val commandList = mutableListOf<String>()

        when (extension) {
            "py" -> commandList.addAll(listOf("python", scriptPath))
            "sh" -> commandList.addAll(listOf("bash", scriptPath))
            "ps1" -> commandList.addAll(listOf("powershell", "-File", scriptPath))
            "bat", "cmd" -> commandList.addAll(listOf("cmd.exe", "/c", scriptPath))
            else -> {
                // If it's a binary or executable script, run directly
                if (scriptFile.canExecute()) {
                    commandList.add(scriptPath)
                } else {
                    // Try bash fallback
                    commandList.addAll(listOf("sh", scriptPath))
                }
            }
        }

        outputBuilder.append("Executing script: ${commandList.joinToString(" ")}\n\n")

        var exitCode = 0
        var resultText = "Success"

        try {
            val processBuilder = ProcessBuilder(commandList)
            processBuilder.directory(File(project.basePath ?: "."))
            processBuilder.redirectErrorStream(true)
            
            val process = processBuilder.start()
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                outputBuilder.append(line).append("\n")
            }
            
            exitCode = process.waitFor()
            if (exitCode != 0) {
                resultText = "Script exited with code $exitCode"
            }
        } catch (e: Exception) {
            exitCode = -1
            resultText = "Script failed: ${e.message}"
            outputBuilder.append("ERROR running script: ${e.message}\n")
            throw e
        }

        val duration = System.currentTimeMillis() - startTime
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
