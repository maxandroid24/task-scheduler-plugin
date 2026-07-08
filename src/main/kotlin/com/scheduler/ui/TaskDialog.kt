package com.scheduler.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.scheduler.model.ScheduledTask
import com.scheduler.model.TaskType
import java.awt.GridLayout
import javax.swing.*

class TaskDialog(private val project: Project, private val existingTask: ScheduledTask?) : DialogWrapper(project) {
    private val nameField = JBTextField()
    private val typeBox = JComboBox(TaskType.values())
    private val targetField = JBTextField()
    private val delayField = JTextField("5")
    private val intervalField = JTextField("60")
    private val repeatCheck = JCheckBox("Repeat continuously")
    private val autoStartCheck = JCheckBox("Auto-start on launch", true)
    private val descField = JBTextField()

    init {
        title = if (existingTask == null) "New Scheduled Task" else "Edit Task"
        if (existingTask != null) {
            nameField.text = existingTask.name
            typeBox.selectedItem = existingTask.type
            targetField.text = existingTask.target
            delayField.text = existingTask.delaySeconds.toString()
            intervalField.text = existingTask.repeatIntervalSeconds.toString()
            repeatCheck.isSelected = existingTask.isRepeat
            autoStartCheck.isSelected = existingTask.autoStart
            descField.text = existingTask.description
        }
        init()
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridLayout(8, 2, 10, 10))
        panel.add(JLabel("Task Name:"))
        panel.add(nameField)
        
        panel.add(JLabel("Type:"))
        panel.add(typeBox)
        
        panel.add(JLabel("Target (ID / Command / Path):"))
        panel.add(targetField)
        
        panel.add(JLabel("Initial Delay (seconds):"))
        panel.add(delayField)
        
        panel.add(JLabel("Repeat Interval (seconds):"))
        panel.add(intervalField)
        
        panel.add(JLabel("Options:"))
        val checkboxPanel = JPanel(GridLayout(1, 2))
        checkboxPanel.add(repeatCheck)
        checkboxPanel.add(autoStartCheck)
        panel.add(checkboxPanel)
        
        panel.add(JLabel("Description:"))
        panel.add(descField)
        
        return panel
    }

    fun getTask(): ScheduledTask {
        val task = existingTask ?: ScheduledTask()
        task.name = nameField.text.trim()
        task.type = typeBox.selectedItem as TaskType
        task.target = targetField.text.trim()
        task.delaySeconds = delayField.text.toLongOrNull() ?: 0L
        task.repeatIntervalSeconds = intervalField.text.toLongOrNull() ?: 0L
        task.isRepeat = repeatCheck.isSelected
        task.autoStart = autoStartCheck.isSelected
        task.description = descField.text.trim()
        return task
    }
}
