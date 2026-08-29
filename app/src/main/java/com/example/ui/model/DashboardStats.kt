package com.example.ui.model

import com.example.model.TaskPriority

data class DayCompletionStat(
    val dayLabel: String,
    val dayOfMonth: Int,
    val dateMillis: Long,
    val completedCount: Int,
    val isToday: Boolean
)

data class PriorityStat(
    val priority: TaskPriority,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class CategoryStat(
    val category: String,
    val totalCount: Int,
    val completedCount: Int,
    val completionRate: Float
)

data class DashboardStats(
    val totalTasks: Int = 0,
    val completedCount: Int = 0,
    val pendingCount: Int = 0,
    val inProgressCount: Int = 0,
    val overdueCount: Int = 0,
    val completionRate: Float = 0f,
    val streakDays: Int = 0,
    val velocityLast7Days: List<DayCompletionStat> = emptyList(),
    val priorityStats: List<PriorityStat> = emptyList(),
    val categoryStats: List<CategoryStat> = emptyList(),
    val onTimeCompletionRate: Float = 1f
)
