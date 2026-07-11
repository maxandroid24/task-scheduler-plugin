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
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.datatransfer.UnsupportedFlavorException
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
    
    private val selectedListModel = DefaultListModel<SequenceStep>()
    private val selectedList = JBList(selectedListModel)
    
    private val addButton = JButton(">")
    private val removeButton = JButton("<")
    private val upButton = JButton("▲ Move Up").apply {
        horizontalAlignment = SwingConstants.LEFT
        margin = Insets(4, 10, 4, 10)
    }
    private val downButton = JButton("▼ Move Down").apply {
        horizontalAlignment = SwingConstants.LEFT
        margin = Insets(4, 10, 4, 10)
    }

    private val repSpinnerModel = SpinnerNumberModel(1, 1, 100, 1)
    private val repSpinner = JSpinner(repSpinnerModel)
    private val repLabel = JLabel("Repetitions for selected task:")

    init {
        title = if (existingTask == null) "New Scheduled Task" else "Edit Task"
        
        // Set up list selection listeners and initial states
        updateButtonStates()
        availableList.addListSelectionListener { updateButtonStates() }
        selectedList.addListSelectionListener {
            updateButtonStates()
            val selectedStep = selectedList.selectedValue
            if (selectedStep != null) {
                repLabel.isEnabled = true
                repSpinner.isEnabled = true
                repSpinner.value = selectedStep.repetitions
            } else {
                repLabel.isEnabled = false
                repSpinner.isEnabled = false
                repSpinner.value = 1
            }
        }

        repSpinner.addChangeListener {
            val selectedStep = selectedList.selectedValue
            if (selectedStep != null) {
                val newVal = repSpinner.value as Int
                if (selectedStep.repetitions != newVal) {
                    selectedStep.repetitions = newVal
                    selectedList.repaint()
                }
            }
        }
        
        addButton.addActionListener {
            val selectedValues = availableList.selectedValuesList
            for (value in selectedValues) {
                selectedListModel.addElement(SequenceStep(value))
                availableListModel.removeElement(value)
            }
            updateButtonStates()
        }
        
        removeButton.addActionListener {
            val selectedValues = selectedList.selectedValuesList
            for (value in selectedValues) {
                availableListModel.addElement(value.task)
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

        // Drag and Drop implementation
        availableList.dragEnabled = true
        availableList.transferHandler = object : TransferHandler() {
            override fun getSourceActions(c: JComponent): Int = COPY_OR_MOVE
            
            override fun createTransferable(c: JComponent): Transferable? {
                val selected = availableList.selectedValuesList
                return if (selected.isNotEmpty()) TaskTransferable(selected) else null
            }
            
            override fun exportDone(source: JComponent, data: Transferable?, action: Int) {
                if (action == MOVE && data is TaskTransferable) {
                    for (task in data.tasks) {
                        availableListModel.removeElement(task)
                    }
                    updateButtonStates()
                }
            }
        }

        selectedList.dragEnabled = true
        selectedList.dropMode = DropMode.INSERT
        selectedList.transferHandler = object : TransferHandler() {
            override fun canImport(support: TransferSupport): Boolean {
                return support.isDataFlavorSupported(TaskTransferable.TASK_FLAVOR)
            }
            
            override fun importData(support: TransferSupport): Boolean {
                if (!canImport(support)) return false
                try {
                    val transferable = support.transferable
                    @Suppress("UNCHECKED_CAST")
                    val tasks = transferable.getTransferData(TaskTransferable.TASK_FLAVOR) as? List<ScheduledTask> ?: return false
                    
                    val dl = support.dropLocation as? JList.DropLocation
                    var index = dl?.index ?: selectedListModel.size()
                    if (index < 0) index = selectedListModel.size()
                    
                    for (task in tasks) {
                        selectedListModel.add(index++, SequenceStep(task))
                    }
                    return true
                } catch (e: Exception) {
                    return false
                }
            }
        }
        
        // Populate lists
        val currentTaskId = existingTask?.id ?: ""
        val allTasks = TaskStorage.getInstance(project).getTasks()
        
        val selectedParts = existingTask?.target?.split(",")?.map { it.trim() }?.filter { it.isNotEmpty() } ?: emptyList()
        
        selectedParts.forEach { part ->
            val subParts = part.split(":")
            val id = subParts[0]
            val reps = if (subParts.size > 1) subParts[1].toIntOrNull() ?: 1 else 1
            val t = allTasks.firstOrNull { it.id == id }
            if (t != null) {
                selectedListModel.addElement(SequenceStep(t, reps))
            }
        }

        val selectedIds = selectedParts.map { it.split(":")[0] }
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
                    val subIds = task.target.split(",").map { it.trim() }.filter { it.isNotEmpty() }.map { it.split(":")[0] }
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
        availableScroll.preferredSize = Dimension(240, 175)
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
        selectedList.cellRenderer = SimpleListCellRenderer.create("") { step ->
            val repStr = if (step.repetitions > 1) " [x${step.repetitions}]" else ""
            "${step.task.name} (${step.task.type.displayName})$repStr"
        }
        val selectedScroll = JBScrollPane(selectedList)
        selectedScroll.preferredSize = Dimension(240, 175)
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

        // Repetitions Spinner Panel at the bottom of combined config
        val repControlPanel = JPanel(FlowLayout(FlowLayout.LEFT, 5, 5))
        repControlPanel.add(repLabel)
        repSpinner.preferredSize = Dimension(60, 24)
        repControlPanel.add(repSpinner)
        repLabel.isEnabled = false
        repSpinner.isEnabled = false
        combinedSeqPanel.add(repControlPanel, BorderLayout.SOUTH)
        
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

    override fun getPreferredSize(): Dimension? {
        val size = super.getPreferredSize() ?: return null
        return Dimension((size.width * 1.20).toInt(), (size.height * 1.15).toInt())
    }

    fun getTask(): ScheduledTask {
        val task = existingTask ?: ScheduledTask()
        task.name = nameField.text.trim()
        task.type = typeBox.selectedItem as TaskType
        
        if (task.type == TaskType.COMBINED_SEQUENCE) {
            val selectedSteps = mutableListOf<String>()
            for (i in 0 until selectedListModel.size()) {
                val step = selectedListModel.getElementAt(i)
                selectedSteps.add("${step.task.id}:${step.repetitions}")
            }
            task.target = selectedSteps.joinToString(",")
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

    data class SequenceStep(val task: ScheduledTask, var repetitions: Int = 1)

    class TaskTransferable(val tasks: List<ScheduledTask>) : Transferable {
        companion object {
            val TASK_FLAVOR = DataFlavor(ScheduledTask::class.java, "ScheduledTask")
        }
        override fun getTransferDataFlavors(): Array<DataFlavor> = arrayOf(TASK_FLAVOR)
        override fun isDataFlavorSupported(flavor: DataFlavor): Boolean = flavor == TASK_FLAVOR
        override fun getTransferData(flavor: DataFlavor): Any {
            if (flavor == TASK_FLAVOR) return tasks
            throw UnsupportedFlavorException(flavor)
        }
    }
}
