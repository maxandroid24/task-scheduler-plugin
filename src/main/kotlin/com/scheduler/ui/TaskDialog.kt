package com.scheduler.ui

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.components.JBTextField
import com.intellij.ui.components.JBList
import com.intellij.ui.components.JBScrollPane
import com.intellij.ui.SimpleListCellRenderer
import com.scheduler.model.ScheduledTask
import com.scheduler.model.TaskType
import com.scheduler.persistence.TaskStorage
import java.awt.*
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

    private val availableListModel = DefaultListModel<ScheduledTask>()
    private val availableList = JBList(availableListModel)
    
    private val selectedListModel = DefaultListModel<ScheduledTask>()
    private val selectedList = JBList(selectedListModel)
    
    private val addButton = JButton(">")
    private val removeButton = JButton("<")
    private val upButton = JButton("▲ Move Up")
    private val downButton = JButton("▼ Move Down")

    init {
        title = if (existingTask == null) "New Scheduled Task" else "Edit Task"
        
        // Set up list selection listeners and initial states
        updateButtonStates()
        availableList.addListSelectionListener { updateButtonStates() }
        selectedList.addListSelectionListener { updateButtonStates() }
        
        addButton.addActionListener {
            val selectedValues = availableList.selectedValuesList
            for (value in selectedValues) {
                selectedListModel.addElement(value)
                availableListModel.removeElement(value)
            }
            updateButtonStates()
        }
        
        removeButton.addActionListener {
            val selectedValues = selectedList.selectedValuesList
            for (value in selectedValues) {
                availableListModel.addElement(value)
                selectedListModel.removeElement(value)
            }
            updateButtonStates()
        }
        
        upButton.addActionListener {
            val selectedIdx = selectedList.selectedIndex
            if (selectedIdx > 0) {
                val element = selectedListModel.getElementAt(selectedIdx)
                selectedListModel.remove(selectedIdx)
                selectedListModel.add(selectedIdx - 1, element)
                selectedList.selectedIndex = selectedIdx - 1
            }
        }
        
        downButton.addActionListener {
            val selectedIdx = selectedList.selectedIndex
            if (selectedIdx != -1 && selectedIdx < selectedListModel.size() - 1) {
                val element = selectedListModel.getElementAt(selectedIdx)
                selectedListModel.remove(selectedIdx)
                selectedListModel.add(selectedIdx + 1, element)
                selectedList.selectedIndex = selectedIdx + 1
            }
        }
        
        // Populate lists
        val currentTaskId = existingTask?.id ?: ""
        val allTasks = TaskStorage.getInstance(project).getTasks()
        
        val selectedIds = existingTask?.target?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        
        selectedIds.forEach { id ->
            val t = allTasks.firstOrNull { it.id == id }
            if (t != null) {
                selectedListModel.addElement(t)
            }
        }
        
        allTasks.forEach { task ->
            if (task.id != currentTaskId && 
                !selectedIds.contains(task.id) && 
                !reachesTask(task.id, currentTaskId)) {
                availableListModel.addElement(task)
            }
        }

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

    private fun updateButtonStates() {
        addButton.isEnabled = !availableList.isSelectionEmpty
        removeButton.isEnabled = !selectedList.isSelectionEmpty
        
        val selectedIdx = selectedList.selectedIndex
        upButton.isEnabled = selectedIdx > 0
        downButton.isEnabled = selectedIdx != -1 && selectedIdx < selectedListModel.size() - 1
    }

    private fun reachesTask(startTaskId: String, targetTaskId: String): Boolean {
        if (startTaskId.isEmpty() || targetTaskId.isEmpty()) return false
        if (startTaskId == targetTaskId) return true
        val storage = TaskStorage.getInstance(project)
        val visited = mutableSetOf<String>()
        val queue = java.util.ArrayDeque<String>()
        queue.add(startTaskId)
        while (queue.isNotEmpty()) {
            val curr = queue.removeFirst()
            if (curr == targetTaskId) return true
            if (visited.add(curr)) {
                val task = storage.getTask(curr)
                if (task != null && task.type == TaskType.COMBINED_SEQUENCE) {
                    val subIds = task.target.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    queue.addAll(subIds)
                }
            }
        }
        return false
    }

    override fun createCenterPanel(): JComponent {
        val panel = JPanel(GridBagLayout())
        val gbc = GridBagConstraints()
        gbc.fill = GridBagConstraints.HORIZONTAL
        gbc.insets = Insets(5, 5, 5, 5)
        gbc.weightx = 1.0
        
        // Row 0: Name
        gbc.gridx = 0
        gbc.gridy = 0
        panel.add(JLabel("Task Name:"), gbc)
        gbc.gridx = 1
        panel.add(nameField, gbc)
        
        // Row 1: Type
        gbc.gridx = 0
        gbc.gridy = 1
        panel.add(JLabel("Type:"), gbc)
        gbc.gridx = 1
        panel.add(typeBox, gbc)
        
        // Row 2: Card Panel for Target / Combined Sequence
        val cardLayout = CardLayout()
        val cardPanel = JPanel(cardLayout)
        
        // Card 1: Single Target
        val singleTargetPanel = JPanel(BorderLayout(5, 5))
        singleTargetPanel.add(JLabel("Target (ID / Command / Path):"), BorderLayout.NORTH)
        singleTargetPanel.add(targetField, BorderLayout.CENTER)
        cardPanel.add(singleTargetPanel, "SINGLE")
        
        // Card 2: Combined Task Dual List Box
        val combinedSeqPanel = JPanel(BorderLayout(5, 5))
        combinedSeqPanel.add(JLabel("Configure Task Sequence (Executed top to bottom):"), BorderLayout.NORTH)
        
        // Dual List Box Container
        val dualListBoxPanel = JPanel(GridBagLayout())
        val dlbc = GridBagConstraints()
        dlbc.fill = GridBagConstraints.BOTH
        dlbc.weighty = 1.0
        dlbc.insets = Insets(2, 2, 2, 2)
        
        // Left: Available
        dlbc.gridx = 0
        dlbc.gridy = 0
        dlbc.weightx = 0.4
        availableList.cellRenderer = SimpleListCellRenderer.create("") { task ->
            "${task.name} (${task.type.displayName})"
        }
        val availableScroll = JBScrollPane(availableList)
        availableScroll.preferredSize = Dimension(200, 150)
        dualListBoxPanel.add(JPanel(BorderLayout()).apply {
            add(JLabel("Available Tasks:"), BorderLayout.NORTH)
            add(availableScroll, BorderLayout.CENTER)
        }, dlbc)
        
        // Middle: Add/Remove Buttons
        dlbc.gridx = 1
        dlbc.weightx = 0.1
        val midButtons = JPanel(GridBagLayout())
        val mgbc = GridBagConstraints()
        mgbc.gridx = 0
        mgbc.gridy = 0
        mgbc.insets = Insets(5, 5, 5, 5)
        mgbc.fill = GridBagConstraints.HORIZONTAL
        midButtons.add(addButton, mgbc)
        mgbc.gridy = 1
        midButtons.add(removeButton, mgbc)
        dualListBoxPanel.add(midButtons, dlbc)
        
        // Right: Selected Sequence
        dlbc.gridx = 2
        dlbc.weightx = 0.4
        selectedList.cellRenderer = SimpleListCellRenderer.create("") { task ->
            "${task.name} (${task.type.displayName})"
        }
        val selectedScroll = JBScrollPane(selectedList)
        selectedScroll.preferredSize = Dimension(200, 150)
        dualListBoxPanel.add(JPanel(BorderLayout()).apply {
            add(JLabel("Tasks in Sequence:"), BorderLayout.NORTH)
            add(selectedScroll, BorderLayout.CENTER)
        }, dlbc)
        
        // Far Right: Up/Down Buttons
        dlbc.gridx = 3
        dlbc.weightx = 0.1
        val rightButtons = JPanel(GridBagLayout())
        val rgbc = GridBagConstraints()
        rgbc.gridx = 0
        rgbc.gridy = 0
        rgbc.insets = Insets(5, 5, 5, 5)
        rgbc.fill = GridBagConstraints.HORIZONTAL
        rightButtons.add(upButton, rgbc)
        rgbc.gridy = 1
        rightButtons.add(downButton, rgbc)
        dualListBoxPanel.add(rightButtons, dlbc)
        
        combinedSeqPanel.add(dualListBoxPanel, BorderLayout.CENTER)
        cardPanel.add(combinedSeqPanel, "COMBINED")
        
        // Add cardPanel to main grid bag layout spanning both columns
        gbc.gridx = 0
        gbc.gridy = 2
        gbc.gridwidth = 2
        gbc.weighty = 1.0
        gbc.fill = GridBagConstraints.BOTH
        panel.add(cardPanel, gbc)
        
        // Reset grid width and weight for standard components
        gbc.gridwidth = 1
        gbc.weighty = 0.0
        gbc.fill = GridBagConstraints.HORIZONTAL
        
        // Row 3: Initial Delay
        gbc.gridx = 0
        gbc.gridy = 3
        panel.add(JLabel("Initial Delay (seconds):"), gbc)
        gbc.gridx = 1
        panel.add(delayField, gbc)
        
        // Row 4: Repeat Interval
        gbc.gridx = 0
        gbc.gridy = 4
        panel.add(JLabel("Repeat Interval (seconds):"), gbc)
        gbc.gridx = 1
        panel.add(intervalField, gbc)
        
        // Row 5: Options Checkboxes
        gbc.gridx = 0
        gbc.gridy = 5
        panel.add(JLabel("Options:"), gbc)
        gbc.gridx = 1
        val checkboxPanel = JPanel(GridLayout(1, 2))
        checkboxPanel.add(repeatCheck)
        checkboxPanel.add(autoStartCheck)
        panel.add(checkboxPanel, gbc)
        
        // Row 6: Description
        gbc.gridx = 0
        gbc.gridy = 6
        panel.add(JLabel("Description:"), gbc)
        gbc.gridx = 1
        panel.add(descField, gbc)
        
        // Setup action listener to switch cards based on type selection
        val updateCard = {
            val selectedType = typeBox.selectedItem as? TaskType
            if (selectedType == TaskType.COMBINED_SEQUENCE) {
                cardLayout.show(cardPanel, "COMBINED")
            } else {
                cardLayout.show(cardPanel, "SINGLE")
            }
        }
        
        typeBox.addActionListener { updateCard() }
        
        // Trigger initial card layout view
        updateCard()
        
        return panel
    }

    fun getTask(): ScheduledTask {
        val task = existingTask ?: ScheduledTask()
        task.name = nameField.text.trim()
        task.type = typeBox.selectedItem as TaskType
        
        if (task.type == TaskType.COMBINED_SEQUENCE) {
            val selectedIds = mutableListOf<String>()
            for (i in 0 until selectedListModel.size()) {
                selectedIds.add(selectedListModel.getElementAt(i).id)
            }
            task.target = selectedIds.joinToString(",")
        } else {
            task.target = targetField.text.trim()
        }
        
        task.delaySeconds = delayField.text.toLongOrNull() ?: 0L
        task.repeatIntervalSeconds = intervalField.text.toLongOrNull() ?: 0L
        task.isRepeat = repeatCheck.isSelected
        task.autoStart = autoStartCheck.isSelected
        task.description = descField.text.trim()
        return task
    }
}
