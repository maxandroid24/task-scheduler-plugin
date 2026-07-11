package com.scheduler.ui

import com.intellij.icons.AllIcons
import com.intellij.openapi.actionSystem.*
import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.table.JBTable
import com.scheduler.model.ScheduledTask
import com.scheduler.persistence.TaskStorage
import com.scheduler.scheduler.SchedulerManager
import java.awt.BorderLayout
import javax.swing.*
import javax.swing.table.AbstractTableModel

class TaskDashboardPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val taskModel = TaskTableModel()
    private val taskTable = JBTable(taskModel)
    
    init {
        taskTable.selectionModel.selectionMode = ListSelectionModel.SINGLE_SELECTION
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
        add(JBScrollPane(taskTable), BorderLayout.CENTER)
    }

    fun refreshTaskList() {
        val tasks = TaskStorage.getInstance(project).getTasks()
        taskModel.setTasks(tasks)
    }

    private fun getSelectedTask(): ScheduledTask? {
        val selectedRow = taskTable.selectedRow
        if (selectedRow == -1) return null
        val modelRow = taskTable.convertRowIndexToModel(selectedRow)
        return taskModel.getTaskAt(modelRow)
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
            val selected = getSelectedTask() ?: return
            val dialog = TaskDialog(project, selected)
            if (dialog.showAndGet()) {
                val updatedTask = dialog.getTask()
                taskModel.fireTableDataChanged()
                project.getService(SchedulerManager::class.java).scheduleTask(updatedTask)
            }
        }
    }

    private inner class DeleteTaskAction : AnAction("Delete", "Delete selected task", AllIcons.General.Remove) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = getSelectedTask() ?: return
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
            val selected = getSelectedTask() ?: return
            selected.isEnabled = true
            project.getService(SchedulerManager::class.java).scheduleTask(selected)
            taskModel.fireTableDataChanged()
        }
    }

    private inner class PauseTaskAction : AnAction("Pause", "Pause task", AllIcons.Actions.Pause) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = getSelectedTask() ?: return
            project.getService(SchedulerManager::class.java).pauseTask(selected.id)
            taskModel.fireTableDataChanged()
        }
    }

    private inner class RunNowAction : AnAction("Run Now", "Force run task immediately", AllIcons.Actions.Play_first) {
        override fun actionPerformed(e: AnActionEvent) {
            val selected = getSelectedTask() ?: return
            project.getService(SchedulerManager::class.java).executeTaskNow(selected)
            taskModel.fireTableDataChanged()
        }
    }

    private inner class RefreshAction : AnAction("Refresh", "Refresh list", AllIcons.Actions.Refresh) {
        override fun actionPerformed(e: AnActionEvent) {
            refreshTaskList()
        }
    }
}

class TaskTableModel : AbstractTableModel() {
    private val columnNames = arrayOf("Task Name", "Continuous", "Status")
    private val tasks = mutableListOf<ScheduledTask>()

    override fun getRowCount(): Int = tasks.size
    override fun getColumnCount(): Int = columnNames.size
    override fun getColumnName(column: Int): String = columnNames[column]

    override fun getValueAt(rowIndex: Int, columnIndex: Int): Any {
        val task = tasks[rowIndex]
        return when (columnIndex) {
            0 -> task.name
            1 -> if (task.isRepeat) "Yes" else "No"
            2 -> task.status.displayName
            else -> ""
        }
    }

    override fun getColumnClass(columnIndex: Int): Class<*> {
        return String::class.java
    }

    fun getTaskAt(rowIndex: Int): ScheduledTask? {
        return tasks.getOrNull(rowIndex)
    }

    fun setTasks(newTasks: List<ScheduledTask>) {
        tasks.clear()
        tasks.addAll(newTasks)
        fireTableDataChanged()
    }
}
