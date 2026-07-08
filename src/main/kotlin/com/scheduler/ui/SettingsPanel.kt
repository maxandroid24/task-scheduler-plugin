package com.scheduler.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBTextField
import com.scheduler.persistence.TaskStorage
import java.awt.GridLayout
import javax.swing.*

class SettingsPanel(private val project: Project) : JPanel() {
    private val maxHistoryField = JBTextField()
    private val autoStartAllCheck = JCheckBox("Enable Scheduler on startup", true)
    
    init {
        layout = BoxLayout(this, BoxLayout.Y_AXIS)
        border = BorderFactory.createTitledBorder("General Preferences")
        
        val gridPanel = JPanel(GridLayout(2, 2, 5, 5))
        gridPanel.add(JLabel("Max History Log Size:"))
        maxHistoryField.text = TaskStorage.getInstance(project).getState().maxHistorySize.toString()
        gridPanel.add(maxHistoryField)
        
        gridPanel.add(JLabel("Auto Start Enabled:"))
        autoStartAllCheck.isSelected = TaskStorage.getInstance(project).getState().autoStartAll
        gridPanel.add(autoStartAllCheck)
        
        add(gridPanel)
        
        val saveButton = JButton("Save Configuration")
        saveButton.addActionListener {
            val maxHistory = maxHistoryField.text.toIntOrNull() ?: 100
            TaskStorage.getInstance(project).setSettings(maxHistory, autoStartAllCheck.isSelected)
            JOptionPane.showMessageDialog(this, "Settings Saved Successfully.")
        }
        add(Box.createRigidArea(java.awt.Dimension(0, 10)))
        add(saveButton)
    }
}
