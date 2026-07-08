package com.scheduler.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTabbedPane
import com.intellij.util.ui.JBUI
import java.awt.BorderLayout
import javax.swing.JPanel

/**
 * Main split panel containing the toolbar, task list, 
 * execution history pane, and settings manager.
 */
class SchedulerMainPanel(private val project: Project) : JPanel(BorderLayout()) {
    init {
        border = JBUI.Borders.empty(5)
        
        val tabbedPane = JBTabbedPane()
        
        // Tab 1: Tasks Dashboard
        val dashboardPanel = TaskDashboardPanel(project)
        tabbedPane.addTab("Tasks", dashboardPanel)
        
        // Tab 2: Detailed Logs & Execution History
        val historyPanel = HistoryPanel(project)
        tabbedPane.addTab("History", historyPanel)
        
        // Tab 3: Configuration Settings
        val settingsPanel = SettingsPanel(project)
        tabbedPane.addTab("Settings", settingsPanel)
        
        add(tabbedPane, BorderLayout.CENTER)
    }
}
