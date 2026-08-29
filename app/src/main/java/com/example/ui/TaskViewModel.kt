package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.model.SubTask
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.repository.TaskRepository
import com.example.ui.model.CategoryStat
import com.example.ui.model.DashboardStats
import com.example.ui.model.DayCompletionStat
import com.example.ui.model.PriorityStat
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class AppTab(val title: String) {
    TASKS("Tasks"),
    TIMELINE("Timeline"),
    DASHBOARD("Dashboard")
}

enum class TaskFilter(val label: String) {
    ALL("All"),
    ACTIVE("Active"),
    IN_PROGRESS("In Progress"),
    COMPLETED("Completed"),
    OVERDUE("Overdue")
}

class TaskViewModel(
    private val repository: TaskRepository
) : ViewModel() {

    val currentTab = MutableStateFlow(AppTab.TASKS)
    val selectedFilter = MutableStateFlow(TaskFilter.ALL)
    val selectedCategory = MutableStateFlow<String?>(null)
    val selectedPriority = MutableStateFlow<TaskPriority?>(null)
    val searchQuery = MutableStateFlow("")

    val isAddEditSheetOpen = MutableStateFlow(false)
    val editingTask = MutableStateFlow<TaskItem?>(null)
    val userNotificationMessage = MutableStateFlow<String?>(null)

    // Master stream of all tasks
    val rawTasks: StateFlow<List<TaskItem>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Overdue tasks needing immediate attention
    val overdueTasks: StateFlow<List<TaskItem>> = rawTasks
        .combine(MutableStateFlow(Unit)) { tasks, _ ->
            tasks.filter { it.isOverdue }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Due Today tasks
    val dueTodayTasks: StateFlow<List<TaskItem>> = rawTasks
        .combine(MutableStateFlow(Unit)) { tasks, _ ->
            tasks.filter { it.isDueToday }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Available unique categories
    val categories: StateFlow<List<String>> = rawTasks
        .combine(MutableStateFlow(Unit)) { tasks, _ ->
            val set = mutableSetOf("Work", "Study", "Project", "Personal", "Health")
            tasks.forEach { if (it.category.isNotBlank()) set.add(it.category) }
            set.sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Work", "Study", "Project", "Personal", "Health")
        )

    // Filtered tasks for the Tasks list view
    val filteredTasks: StateFlow<List<TaskItem>> = combine(
        rawTasks,
        selectedFilter,
        selectedCategory,
        selectedPriority,
        searchQuery
    ) { tasks, filter, category, priority, query ->
        tasks.filter { task ->
            // Filter by search query
            val matchesQuery = query.isBlank() ||
                    task.title.contains(query, ignoreCase = true) ||
                    task.description.contains(query, ignoreCase = true) ||
                    task.category.contains(query, ignoreCase = true)

            // Filter by status/tab filter
            val matchesFilter = when (filter) {
                TaskFilter.ALL -> true
                TaskFilter.ACTIVE -> !task.isCompleted
                TaskFilter.IN_PROGRESS -> task.status == TaskStatus.IN_PROGRESS
                TaskFilter.COMPLETED -> task.isCompleted
                TaskFilter.OVERDUE -> task.isOverdue
            }

            // Filter by category
            val matchesCategory = category == null || task.category.equals(category, ignoreCase = true)

            // Filter by priority
            val matchesPriority = priority == null || task.priority == priority

            matchesQuery && matchesFilter && matchesCategory && matchesPriority
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Comprehensive dashboard analytics
    val dashboardStats: StateFlow<DashboardStats> = rawTasks
        .combine(MutableStateFlow(Unit)) { tasks, _ ->
            computeDashboardStats(tasks)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = DashboardStats()
        )

    private fun computeDashboardStats(tasks: List<TaskItem>): DashboardStats {
        if (tasks.isEmpty()) return DashboardStats()

        val total = tasks.size
        val completed = tasks.count { it.isCompleted }
        val inProgress = tasks.count { it.status == TaskStatus.IN_PROGRESS }
        val pending = tasks.count { it.status == TaskStatus.PENDING && !it.isOverdue }
        val overdue = tasks.count { it.isOverdue }

        val rate = if (total > 0) completed.toFloat() / total else 0f

        // Calculate 7-day velocity
        val velocity = mutableListOf<DayCompletionStat>()
        val cal = Calendar.getInstance()
        val todayDayOfYear = cal.get(Calendar.DAY_OF_YEAR)
        val todayYear = cal.get(Calendar.YEAR)

        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 6 downTo 0) {
            val dayCal = Calendar.getInstance().apply {
                add(Calendar.DAY_OF_YEAR, -i)
            }
            val targetDay = dayCal.get(Calendar.DAY_OF_YEAR)
            val targetYear = dayCal.get(Calendar.YEAR)
            val dayOfMonth = dayCal.get(Calendar.DAY_OF_MONTH)
            val dayLabel = dayFormat.format(dayCal.time)
            val isToday = (targetDay == todayDayOfYear && targetYear == todayYear)

            val countOnDay = tasks.count { task ->
                if (task.completedAt != null) {
                    val compCal = Calendar.getInstance().apply { timeInMillis = task.completedAt }
                    compCal.get(Calendar.DAY_OF_YEAR) == targetDay && compCal.get(Calendar.YEAR) == targetYear
                } else false
            }

            velocity.add(
                DayCompletionStat(
                    dayLabel = dayLabel,
                    dayOfMonth = dayOfMonth,
                    dateMillis = dayCal.timeInMillis,
                    completedCount = countOnDay,
                    isToday = isToday
                )
            )
        }

        // Priority breakdown stats
        val priorityStats = TaskPriority.values().map { p ->
            val pTasks = tasks.filter { it.priority == p }
            val pTotal = pTasks.size
            val pCompleted = pTasks.count { it.isCompleted }
            val pRate = if (pTotal > 0) pCompleted.toFloat() / pTotal else 0f
            PriorityStat(priority = p, totalCount = pTotal, completedCount = pCompleted, completionRate = pRate)
        }

        // Category breakdown stats
        val allCategories = tasks.map { it.category }.distinct()
        val categoryStats = allCategories.map { cat ->
            val cTasks = tasks.filter { it.category == cat }
            val cTotal = cTasks.size
            val cCompleted = cTasks.count { it.isCompleted }
            val cRate = if (cTotal > 0) cCompleted.toFloat() / cTotal else 0f
            CategoryStat(category = cat, totalCount = cTotal, completedCount = cCompleted, completionRate = cRate)
        }

        // Estimate completion streak
        var streak = 0
        var checkCal = Calendar.getInstance()
        for (dayBack in 0..30) {
            val dCal = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -dayBack) }
            val dDay = dCal.get(Calendar.DAY_OF_YEAR)
            val dYear = dCal.get(Calendar.YEAR)
            val hasCompleted = tasks.any {
                if (it.completedAt != null) {
                    val cCal = Calendar.getInstance().apply { timeInMillis = it.completedAt }
                    cCal.get(Calendar.DAY_OF_YEAR) == dDay && cCal.get(Calendar.YEAR) == dYear
                } else false
            }
            if (hasCompleted) {
                streak++
            } else if (dayBack > 0) {
                break
            }
        }

        return DashboardStats(
            totalTasks = total,
            completedCount = completed,
            pendingCount = pending,
            inProgressCount = inProgress,
            overdueCount = overdue,
            completionRate = rate,
            streakDays = streak,
            velocityLast7Days = velocity,
            priorityStats = priorityStats,
            categoryStats = categoryStats
        )
    }

    // Actions
    fun openCreateTaskSheet() {
        editingTask.value = null
        isAddEditSheetOpen.value = true
    }

    fun openEditTaskSheet(task: TaskItem) {
        editingTask.value = task
        isAddEditSheetOpen.value = true
    }

    fun closeAddEditSheet() {
        editingTask.value = null
        isAddEditSheetOpen.value = false
    }

    fun saveTask(
        title: String,
        description: String,
        priority: TaskPriority,
        category: String,
        dueDate: Long?,
        startDate: Long?,
        reminderEnabled: Boolean,
        subtasks: List<SubTask>,
        status: TaskStatus
    ) {
        viewModelScope.launch {
            val existing = editingTask.value
            if (existing != null) {
                val updated = existing.copy(
                    title = title,
                    description = description,
                    priority = priority,
                    category = category,
                    dueDate = dueDate,
                    startDate = startDate,
                    reminderEnabled = reminderEnabled,
                    subtasks = subtasks,
                    status = status,
                    completedAt = if (status == TaskStatus.COMPLETED && !existing.isCompleted) System.currentTimeMillis() else existing.completedAt
                )
                repository.updateTask(updated)
            } else {
                val newTask = TaskItem(
                    title = title,
                    description = description,
                    priority = priority,
                    category = category,
                    dueDate = dueDate,
                    startDate = startDate,
                    reminderEnabled = reminderEnabled,
                    subtasks = subtasks,
                    status = status,
                    completedAt = if (status == TaskStatus.COMPLETED) System.currentTimeMillis() else null
                )
                repository.insertTask(newTask)
            }
            closeAddEditSheet()
        }
    }

    fun toggleTaskCompletion(task: TaskItem) {
        viewModelScope.launch {
            repository.toggleTaskCompletion(task)
        }
    }

    fun toggleSubtask(task: TaskItem, subtaskId: String) {
        viewModelScope.launch {
            repository.toggleSubtask(task, subtaskId)
        }
    }

    fun deleteTask(task: TaskItem) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearNotificationMessage() {
        userNotificationMessage.value = null
    }

    fun selectTab(tab: AppTab) {
        currentTab.value = tab
    }
}

class TaskViewModelFactory(
    private val repository: TaskRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TaskViewModel::class.java)) {
            return TaskViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
