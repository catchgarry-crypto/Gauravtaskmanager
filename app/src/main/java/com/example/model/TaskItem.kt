package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val description: String = "",
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val status: TaskStatus = TaskStatus.PENDING,
    val category: String = "General",
    val startDate: Long? = null,
    val dueDate: Long? = null,
    val reminderEnabled: Boolean = true,
    val subtasks: List<SubTask> = emptyList(),
    val completedAt: Long? = null,
    val estimatedHours: Double? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val isCompleted: Boolean
        get() = status == TaskStatus.COMPLETED

    val isOverdue: Boolean
        get() = !isCompleted && dueDate != null && dueDate < System.currentTimeMillis()

    val isDueToday: Boolean
        get() {
            if (dueDate == null || isCompleted) return false
            val cal1 = Calendar.getInstance().apply { timeInMillis = System.currentTimeMillis() }
            val cal2 = Calendar.getInstance().apply { timeInMillis = dueDate }
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
                    cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR)
        }

    val isDueSoon: Boolean
        get() {
            if (dueDate == null || isCompleted || isOverdue) return false
            val diffMillis = dueDate - System.currentTimeMillis()
            return diffMillis in 0..(24 * 60 * 60 * 1000) // Within 24 hours
        }

    val progressFraction: Float
        get() {
            if (isCompleted) return 1f
            if (subtasks.isEmpty()) {
                return if (status == TaskStatus.IN_PROGRESS) 0.5f else 0f
            }
            val done = subtasks.count { it.isCompleted }
            return done.toFloat() / subtasks.size
        }

    fun getFormattedDueDate(): String {
        if (dueDate == null) return "No due date"
        val date = Date(dueDate)
        val format = SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault())
        return format.format(date)
    }

    fun getShortFormattedDueDate(): String {
        if (dueDate == null) return "No deadline"
        val now = Calendar.getInstance()
        val due = Calendar.getInstance().apply { timeInMillis = dueDate }

        val isSameYear = now.get(Calendar.YEAR) == due.get(Calendar.YEAR)
        val isToday = isSameYear && now.get(Calendar.DAY_OF_YEAR) == due.get(Calendar.DAY_OF_YEAR)
        val isTomorrow = isSameYear && now.get(Calendar.DAY_OF_YEAR) + 1 == due.get(Calendar.DAY_OF_YEAR)
        val isYesterday = isSameYear && now.get(Calendar.DAY_OF_YEAR) - 1 == due.get(Calendar.DAY_OF_YEAR)

        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val timeStr = timeFormat.format(Date(dueDate))

        return when {
            isToday -> "Today, $timeStr"
            isTomorrow -> "Tomorrow, $timeStr"
            isYesterday -> "Yesterday, $timeStr"
            isSameYear -> SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(dueDate))
            else -> SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(Date(dueDate))
        }
    }

    fun getRelativeTimelineText(): String {
        if (isCompleted) {
            return if (completedAt != null) {
                val dateStr = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(Date(completedAt))
                "Completed on $dateStr"
            } else {
                "Completed"
            }
        }
        if (dueDate == null) return "No timeline set"

        val diff = dueDate - System.currentTimeMillis()
        val absDiff = Math.abs(diff)
        val hours = absDiff / (1000 * 60 * 60)
        val days = hours / 24

        return if (diff < 0) {
            when {
                hours < 1 -> "Overdue just now"
                hours < 24 -> "Overdue by ${hours}h"
                else -> "Overdue by ${days}d"
            }
        } else {
            when {
                hours < 1 -> "Due in ${absDiff / (1000 * 60)}m"
                hours < 24 -> "Due in ${hours}h"
                days == 1L -> "Due tomorrow"
                else -> "Due in ${days}d"
            }
        }
    }
}
