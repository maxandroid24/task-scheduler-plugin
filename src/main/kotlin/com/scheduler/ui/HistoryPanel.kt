package com.scheduler.ui

import com.intellij.openapi.project.Project
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.components.JBTextArea
import com.intellij.ui.table.JBTable
import com.scheduler.persistence.TaskStorage
import java.awt.BorderLayout
import java.awt.Dimension
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.swing.*
import javax.swing.table.DefaultTableModel

class HistoryPanel(private val project: Project) : JPanel(BorderLayout()) {
    private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault())
    private val tableModel = DefaultTableModel(arrayOf("Time", "Task Name", "Result", "Duration (ms)"), 0)
    private val table = JBTable(tableModel)
    private val outputArea = JBTextArea()

    init {
        val splitPane = JSplitPane(JSplitPane.VERTICAL_SPLIT)
        splitPane.dividerLocation = 150
        
        splitPane.leftComponent = JBScrollPane(table)
        
        outputArea.isEditable = false
        val outputScroll = JBScrollPane(outputArea)
        outputScroll.border = BorderFactory.createTitledBorder("Console Output Log")
        splitPane.rightComponent = outputScroll
        
        add(splitPane, BorderLayout.CENTER)
        
        val buttonPanel = JPanel()
        val clearButton = JButton("Clear Log")
        clearButton.addActionListener {
            TaskStorage.getInstance(project).clearHistory()
            refreshHistory()
        }
        val refreshButton = JButton("Refresh")
        refreshButton.addActionListener { refreshHistory() }
        
        buttonPanel.add(refreshButton)
        buttonPanel.add(clearButton)
        add(buttonPanel, BorderLayout.SOUTH)
        
        table.selectionModel.addListSelectionListener {
            val row = table.selectedRow
            if (row >= 0) {
                val history = TaskStorage.getInstance(project).getHistories().getOrNull(row)
                outputArea.text = history?.output ?: "No log available"
            }
        }
        
        refreshHistory()
    }

    fun refreshHistory() {
        tableModel.rowCount = 0
        val histories = TaskStorage.getInstance(project).getHistories()
        histories.forEach { hist ->
            tableModel.addRow(arrayOf(
                timeFormatter.format(Instant.ofEpochMilli(hist.timestamp)), 
                hist.taskName, 
                hist.result, 
                hist.durationMs
            ))
        }
        outputArea.text = ""
    }
}
