package com.example.repository

import android.content.Context
import com.example.db.TaskDao
import com.example.model.SubTask
import com.example.model.TaskItem
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import com.example.notification.TaskAlarmScheduler
import kotlinx.coroutines.flow.Flow
import java.util.Calendar

class TaskRepository(
    private val taskDao: TaskDao,
    private val context: Context
) {
    private val alarmScheduler = TaskAlarmScheduler(context)

    val allTasks: Flow<List<TaskItem>> = taskDao.getAllTasks()
    val completedTasks: Flow<List<TaskItem>> = taskDao.getCompletedTasks()

    suspend fun insertTask(task: TaskItem): Long {
        val id = taskDao.insertTask(task)
        val inserted = task.copy(id = id)
        if (inserted.reminderEnabled && inserted.dueDate != null && !inserted.isCompleted) {
            alarmScheduler.scheduleTaskReminder(inserted)
        }
        return id
    }

    suspend fun updateTask(task: TaskItem) {
        val updated = task.copy(updatedAt = System.currentTimeMillis())
        taskDao.updateTask(updated)
        if (updated.isCompleted || !updated.reminderEnabled || updated.dueDate == null) {
            alarmScheduler.cancelTaskReminder(updated.id)
        } else {
            alarmScheduler.scheduleTaskReminder(updated)
        }
    }

    suspend fun deleteTask(task: TaskItem) {
        alarmScheduler.cancelTaskReminder(task.id)
        taskDao.deleteTask(task)
    }

    suspend fun toggleTaskCompletion(task: TaskItem) {
        val newStatus = if (task.isCompleted) TaskStatus.PENDING else TaskStatus.COMPLETED
        val completedTime = if (newStatus == TaskStatus.COMPLETED) System.currentTimeMillis() else null
        
        // Also toggle all subtasks if completing the task
        val updatedSubtasks = if (newStatus == TaskStatus.COMPLETED) {
            task.subtasks.map { it.copy(isCompleted = true) }
        } else {
            task.subtasks
        }

        val updated = task.copy(
            status = newStatus,
            completedAt = completedTime,
            subtasks = updatedSubtasks,
            updatedAt = System.currentTimeMillis()
        )
        updateTask(updated)
    }

    suspend fun toggleSubtask(task: TaskItem, subtaskId: String) {
        val updatedList = task.subtasks.map { sub ->
            if (sub.id == subtaskId) sub.copy(isCompleted = !sub.isCompleted) else sub
        }
        val allDone = updatedList.isNotEmpty() && updatedList.all { it.isCompleted }
        val newStatus = if (allDone) TaskStatus.COMPLETED else if (updatedList.any { it.isCompleted }) TaskStatus.IN_PROGRESS else task.status
        val completedTime = if (newStatus == TaskStatus.COMPLETED && !task.isCompleted) System.currentTimeMillis() else task.completedAt

        val updated = task.copy(
            subtasks = updatedList,
            status = newStatus,
            completedAt = completedTime,
            updatedAt = System.currentTimeMillis()
        )
        updateTask(updated)
    }

    suspend fun seedSampleTasksIfEmpty() {
        val count = taskDao.getTaskCount()
        if (count > 0) return

        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()

        // 1. Overdue urgent assignment
        calendar.timeInMillis = now
        calendar.add(Calendar.HOUR_OF_DAY, -3)
        val overdueDue = calendar.timeInMillis
        val sample1 = TaskItem(
            title = "Finalize UX Architecture Wireframes",
            description = "Complete user journey flows and verify high-density prototype with stakeholders.",
            priority = TaskPriority.URGENT,
            status = TaskStatus.IN_PROGRESS,
            category = "Project",
            dueDate = overdueDue,
            reminderEnabled = true,
            subtasks = listOf(
                SubTask(title = "Review persona requirements", isCompleted = true),
                SubTask(title = "Export high-resolution specs", isCompleted = false),
                SubTask(title = "Schedule design review call", isCompleted = false)
            )
        )

        // 2. High priority task due today
        calendar.timeInMillis = now
        calendar.add(Calendar.HOUR_OF_DAY, 4)
        val todayDue = calendar.timeInMillis
        val sample2 = TaskItem(
            title = "Submit Machine Learning Assignment",
            description = "Implement gradient descent optimization algorithm and prepare final benchmark report.",
            priority = TaskPriority.HIGH,
            status = TaskStatus.PENDING,
            category = "Study",
            dueDate = todayDue,
            reminderEnabled = true,
            subtasks = listOf(
                SubTask(title = "Run convergence tests", isCompleted = true),
                SubTask(title = "Compile Jupyter notebook to PDF", isCompleted = false)
            )
        )

        // 3. Medium priority task due tomorrow
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 17)
        calendar.set(Calendar.MINUTE, 0)
        val tomorrowDue = calendar.timeInMillis
        val sample3 = TaskItem(
            title = "Sprint Planning & Timeline Estimation",
            description = "Organize backlog tickets and align milestone deliverables for the upcoming release.",
            priority = TaskPriority.MEDIUM,
            status = TaskStatus.PENDING,
            category = "Work",
            dueDate = tomorrowDue,
            reminderEnabled = true,
            subtasks = listOf(
                SubTask(title = "Review incoming feature requests", isCompleted = false),
                SubTask(title = "Assign story point estimates", isCompleted = false)
            )
        )

        // 4. Low priority task later this week
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, 4)
        val laterDue = calendar.timeInMillis
        val sample4 = TaskItem(
            title = "Research Cloud Infrastructure Scalability",
            description = "Explore database sharding strategies and caching layers for peak traffic spikes.",
            priority = TaskPriority.LOW,
            status = TaskStatus.PENDING,
            category = "Research",
            dueDate = laterDue,
            reminderEnabled = true
        )

        // 5. Completed task from yesterday
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -1)
        val completedDue = calendar.timeInMillis
        val sample5 = TaskItem(
            title = "Review Security & Privacy Compliance Checklist",
            description = "Verified authentication tokens, encryption at rest, and audit trail logs.",
            priority = TaskPriority.HIGH,
            status = TaskStatus.COMPLETED,
            category = "Work",
            dueDate = completedDue,
            completedAt = now - (18 * 60 * 60 * 1000L),
            subtasks = listOf(
                SubTask(title = "Check SSL certs", isCompleted = true),
                SubTask(title = "Audit API keys", isCompleted = true)
            )
        )

        // 6. Completed task from 3 days ago
        calendar.timeInMillis = now
        calendar.add(Calendar.DAY_OF_YEAR, -3)
        val sample6 = TaskItem(
            title = "Setup Database Schema & Migration Scripts",
            description = "Configured Room entities, DAOs, and automated indexing strategies.",
            priority = TaskPriority.URGENT,
            status = TaskStatus.COMPLETED,
            category = "Project",
            dueDate = calendar.timeInMillis,
            completedAt = now - (3 * 24 * 60 * 60 * 1000L),
            subtasks = listOf(
                SubTask(title = "Define entity relations", isCompleted = true),
                SubTask(title = "Run migration verification tests", isCompleted = true)
            )
        )

        insertTask(sample1)
        insertTask(sample2)
        insertTask(sample3)
        insertTask(sample4)
        insertTask(sample5)
        insertTask(sample6)
    }
}
