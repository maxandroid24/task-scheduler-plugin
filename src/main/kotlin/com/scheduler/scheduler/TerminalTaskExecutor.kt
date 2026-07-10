package com.scheduler.scheduler

import com.intellij.openapi.project.Project
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory
import org.jetbrains.plugins.terminal.TerminalToolWindowManager
import java.io.BufferedReader
import java.io.InputStreamReader

class TerminalTaskExecutor : TaskExecutor {
    override fun execute(project: Project, task: ScheduledTask): ExecutionHistory {
        val command = task.target
        val startTime = System.currentTimeMillis()
        val outputBuilder = StringBuilder()
        
        outputBuilder.append("Command: $command\n")
        outputBuilder.append("Executing via IDE Terminal fall-back engine...\n\n")

        var exitCode = 0
        var resultText = "Success"

        try {
            if (task.arguments["use_ide_terminal"] == "true") {
                outputBuilder.append("Attempting integration with Terminal widget...\n")
                val terminalManager = TerminalToolWindowManager.getInstance(project)
                val widget = terminalManager.createLocalShellWidget(project.basePath, "Task Scheduler: ${task.name}")
                widget.executeCommand(command)
                outputBuilder.append("Sent command to IDE Terminal widget safely.")
                resultText = "Command sent to terminal widget"
            } else {
                // Fallback to local ProcessBuilder execution to capture real logs and exit codes
                val isWindows = System.getProperty("os.name").lowercase().contains("win")
                val processBuilder = if (isWindows) {
                    ProcessBuilder("cmd.exe", "/c", command)
                } else {
                    ProcessBuilder("sh", "-c", command)
                }
                
                processBuilder.directory(java.io.File(project.basePath ?: "."))
                processBuilder.redirectErrorStream(true)
                
                val process = processBuilder.start()
                val reader = BufferedReader(InputStreamReader(process.inputStream))
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    outputBuilder.append(line).append("\n")
                }
                
                exitCode = process.waitFor()
                if (exitCode != 0) {
                    resultText = "Process exited with code $exitCode"
                }
            }
        } catch (e: Exception) {
            exitCode = -1
            resultText = "Error: ${e.message}"
            outputBuilder.append("ERROR executing terminal: ${e.message}\n")
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
