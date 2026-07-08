package com.scheduler.ui

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.Project
import javax.swing.JComponent

class SettingsConfigurable(private val project: Project) : Configurable {
    private var panel: SettingsPanel? = null

    override fun getDisplayName(): String = "Task Scheduler"

    override fun createComponent(): JComponent? {
        panel = SettingsPanel(project)
        return panel
    }

    override fun isModified(): Boolean {
        return false
    }

    override fun apply() {
        // Logic is currently handled by the "Save" button in SettingsPanel
    }

    override fun disposeUIResources() {
        panel = null
    }
}
