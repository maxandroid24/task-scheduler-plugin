package com.scheduler.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.scheduler.model.ScheduledTask
import com.scheduler.model.TaskStatus
import com.scheduler.persistence.TaskStorage
import com.scheduler.scheduler.SchedulerManager
import java.awt.BorderLayout
import javax.swing.*

class TaskDashboardPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val taskListModel = DefaultListModel<ScheduledTask>()
    private val taskList = JBList(taskListModel)
    
    init {
        refreshTaskList()
        setupLayout()
    }

    private fun setupLayout() {
        val actionGroup = DefaultActionGroup().apply {
            add(CreateTaskAction())
            add(EditTaskAction())
            add(DeleteTaskAction())
            addSeparator()
            add(StartTaskAction())
            add(PauseTaskAction())
            add(RunNowAction())
            addSeparator()
            add(RefreshAction())
        }
        
        val toolbar = ActionManager.getInstance()
            .createActionToolbar("TaskSchedulerToolbar", actionGroup, true)
        toolbar.targetComponent = this
        
        add(toolbar.component, BorderLayout.NORTH)
        add(JBScrollPane(taskList), BorderLayout.CENTER)
    }

    fun refreshTaskList() {
        taskListModel.clear()
        val tasks = TaskStorage.getInstance(project).getTasks()
        tasks.forEach { taskListModel.addElement(it) }
    }

    // Swing Sub-actions
    private inner class CreateTaskAction : AnAction("New Task", "Create a scheduled task", AllIcons.General.Add) {
        override fun actionPerformed(e: AnActionEvent) {
            val dialog = TaskDialog(project, null)
            if (dialog.showAndGet()) {
                val newTask = dialog.getTask()
                TaskStorage.getInstance(project).addTask(newTask)
                project.getService(SchedulerManager::class.java).scheduleTask(newTask)
                refreshTaskList()
            }
        }
    }

    private inner class EditTaskAction : AnAction("Edit", "Edit selected task", AllIcons.Actions.Edit) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = taskList.selectedValue ?: return
            val dialog = TaskDialog(project, selected)
            if (dialog.showAndGet()) {
                val updatedTask = dialog.getTask()
                taskList.repaint()
                project.getService(SchedulerManager::class.java).scheduleTask(updatedTask)
            }
        }
    }

    private inner class DeleteTaskAction : AnAction("Delete", "Delete selected task", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = taskList.selectedValue ?: return
            val confirm = JOptionPane.showConfirmDialog(
                this@TaskDashboardPanel, 
                "Are you sure you want to delete task '${selected.name}'?", 
                "Delete Task", 
                JOptionPane.YES_NO_OPTION
            )
            if (confirm == JOptionPane.YES_OPTION) {
                project.getService(SchedulerManager::class.java).stopTask(selected.id)
                TaskStorage.getInstance(project).removeTask(selected.id)
                refreshTaskList()
            }
        }
    }

    private inner class StartTaskAction : AnAction("Start", "Start scheduling", AllIcons.Actions.Execute) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = taskList.selectedValue ?: return
            selected.isEnabled = true
            project.getService(SchedulerManager::class.java).scheduleTask(selected)
            taskList.repaint()
        }
    }

    private inner class PauseTaskAction : AnAction("Pause", "Pause task", AllIcons.Actions.Pause) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = taskList.selectedValue ?: return
            project.getService(SchedulerManager::class.java).pauseTask(selected.id)
            taskList.repaint()
        }
    }

    private inner class RunNowAction : AnAction("Run Now", "Force run task immediately", AllIcons.Actions.Play_first) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = taskList.selectedValue ?: return
            project.getService(SchedulerManager::class.java).executeTaskNow(selected)
            taskList.repaint()
        }
    }

    private inner class RefreshAction : AnAction("Refresh", "Refresh list", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            refreshTaskList()
        }
    }
}
