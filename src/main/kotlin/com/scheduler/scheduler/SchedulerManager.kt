package com.scheduler.scheduler

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.concurrency.JobScheduler
import com.scheduler.model.*
import com.scheduler.persistence.TaskStorage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

@Service(Service.Level.PROJECT)
class SchedulerManager(private val project: Project) : Disposable {
    private val LOG = Logger.getInstance(SchedulerManager::class.java)
    private val activeFutures = ConcurrentHashMap<String, ScheduledFuture<*>>()
    
    // Core executors mapped by type following Strategy pattern
    private val executors = mapOf(
        TaskType.IDE_ACTION to ActionTaskExecutor(),
        TaskType.IDE_MACRO to MacroTaskExecutor(),
        TaskType.TERMINAL_COMMAND to TerminalTaskExecutor(),
        TaskType.EXTERNAL_SCRIPT to ScriptTaskExecutor()
    )

    init {
        LOG.info("Initializing Task Scheduler Engine...")
        // Automatically start tasks scheduled for autoStart
        ApplicationManager.getApplication().executeOnPooledThread {
            Thread.sleep(3000) // Small warm-up buffer
            val tasks = TaskStorage.getInstance(project).getTasks()
            tasks.forEach { task ->
                if (task.autoStart && task.isEnabled) {
                    scheduleTask(task)
                }
            }
        }
    }

    @Synchronized
    fun scheduleTask(task: ScheduledTask) {
        cancelScheduledFuture(task.id)
        
        if (!task.isEnabled) {
            task.status = TaskStatus.DISABLED
            return
        }

        val initialDelay = task.delaySeconds
        val interval = task.repeatIntervalSeconds

        val taskRunner = Runnable {
            executeTaskNow(task)
        }

        task.status = TaskStatus.SCHEDULED
        val future = if (task.isRepeat && interval > 0) {
            task.nextRunTimestamp = System.currentTimeMillis() + (initialDelay * 1000)
            JobScheduler.getScheduler().scheduleWithFixedDelay(
                {
                    taskRunner.run()
                    task.nextRunTimestamp = System.currentTimeMillis() + (interval * 1000)
                },
                initialDelay,
                interval,
                TimeUnit.SECONDS
            )
        } else {
            task.nextRunTimestamp = System.currentTimeMillis() + (initialDelay * 1000)
            JobScheduler.getScheduler().schedule(
                {
                    taskRunner.run()
                    task.status = TaskStatus.STOPPED
                    task.nextRunTimestamp = null
                },
                initialDelay,
                TimeUnit.SECONDS
            )
        }

        activeFutures[task.id] = future
        LOG.info("Scheduled Task '${task.name}' successfully. Next execution in $initialDelay seconds.")
    }

    fun executeTaskNow(task: ScheduledTask) {
        val originalStatus = task.status
        task.status = TaskStatus.RUNNING
        LOG.info("Running Scheduled Task: ${task.name} (${task.type})")
        
        val executor = executors[task.type]
        if (executor == null) {
            val errorMsg = "Executor not registered for type: ${task.type}"
            LOG.error(errorMsg)
            handleTaskFailure(task, Exception(errorMsg), originalStatus)
            return
        }

        val startTime = System.currentTimeMillis()
        try {
            // Execute the action/terminal/script in background
            val history = executor.execute(project, task)
            
            task.runCount++
            task.lastRunTimestamp = System.currentTimeMillis()
            task.status = if (task.isRepeat) TaskStatus.SCHEDULED else TaskStatus.STOPPED
            
            // Persist the history
            TaskStorage.getInstance(project).addHistory(history)
            LOG.info("Task Completed: ${task.name} in ${history.durationMs}ms with code ${history.exitCode}")

            // Post Notification
            notifyUser(
                "Task Completed",
                "'${task.name}' executed successfully in ${history.durationMs} ms.",
                NotificationType.INFORMATION
            )
        } catch (e: Exception) {
            handleTaskFailure(task, e, originalStatus)
        }
    }

    private fun handleTaskFailure(task: ScheduledTask, e: Exception, fallbackStatus: TaskStatus) {
        task.runCount++
        task.lastRunTimestamp = System.currentTimeMillis()
        task.status = if (task.isRepeat) TaskStatus.SCHEDULED else TaskStatus.STOPPED

        val history = ExecutionHistory(
            taskId = task.id,
            taskName = task.name,
            durationMs = 0,
            exitCode = -1,
            result = "Failed: ${e.localizedMessage}",
            output = "Exception stack trace: \n${e.stackTraceToString()}",
            error = e.localizedMessage
        )
        TaskStorage.getInstance(project).addHistory(history)

        notifyUser(
            "Task Failed",
            "'${task.name}' failed: ${e.localizedMessage}",
            NotificationType.ERROR
        )
    }

    @Synchronized
    fun stopTask(taskId: String) {
        cancelScheduledFuture(taskId)
        val task = TaskStorage.getInstance(project).getTask(taskId)
        if (task != null) {
            task.status = TaskStatus.STOPPED
            task.nextRunTimestamp = null
        }
    }

    @Synchronized
    fun pauseTask(taskId: String) {
        cancelScheduledFuture(taskId)
        val task = TaskStorage.getInstance(project).getTask(taskId)
        if (task != null) {
            task.status = TaskStatus.PAUSED
            task.nextRunTimestamp = null
        }
    }

    private fun cancelScheduledFuture(taskId: String) {
        activeFutures.remove(taskId)?.cancel(true)
    }

    private fun notifyUser(title: String, content: String, type: NotificationType) {
        NotificationGroupManager.getInstance()
            .getNotificationGroup("Task Scheduler Alerts")
            .createNotification(title, content, type)
            .notify(project)
    }

    override fun dispose() {
        LOG.info("Disposing Task Scheduler Engine. Cancelling active timers.")
        activeFutures.values.forEach { it.cancel(true) }
        activeFutures.clear()
    }
}
