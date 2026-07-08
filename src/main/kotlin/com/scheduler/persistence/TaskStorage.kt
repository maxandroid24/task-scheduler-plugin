package com.scheduler.persistence

import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage
import com.intellij.openapi.project.Project
import com.intellij.util.xmlb.XmlSerializerUtil
import com.scheduler.model.ScheduledTask
import com.scheduler.model.ExecutionHistory
import java.util.concurrent.CopyOnWriteArrayList

@State(
    name = "TaskSchedulerStorage",
    storages = [Storage("task_scheduler_config.xml")]
)
class TaskStorage : PersistentStateComponent<TaskStorage.State> {
    
    class State {
        var tasks: MutableList<ScheduledTask> = CopyOnWriteArrayList()
        var histories: MutableList<ExecutionHistory> = CopyOnWriteArrayList()
        var maxHistorySize: Int = 100
        var autoStartAll: Boolean = true
    }

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        XmlSerializerUtil.copyBean(state, myState)
    }

    fun getTasks(): List<ScheduledTask> = myState.tasks
    
    fun getTask(id: String): ScheduledTask? = myState.tasks.firstOrNull { it.id == id }

    fun addTask(task: ScheduledTask) {
        myState.tasks.add(task)
    }

    fun removeTask(id: String) {
        myState.tasks.removeIf { it.id == id }
    }

    fun getHistories(): List<ExecutionHistory> = myState.histories

    fun addHistory(history: ExecutionHistory) {
        myState.histories.add(0, history) // Add to top (descending order)
        // Trim history list if it exceeds maximum size
        while (myState.histories.size > myState.maxHistorySize) {
            myState.histories.removeLast()
        }
    }

    fun clearHistory() {
        myState.histories.clear()
    }

    fun setSettings(maxHistorySize: Int, autoStartAll: Boolean) {
        myState.maxHistorySize = maxHistorySize
        myState.autoStartAll = autoStartAll
    }

    companion object {
        fun getInstance(project: Project): TaskStorage {
            return project.getService(TaskStorage::class.java)
        }
    }
}
