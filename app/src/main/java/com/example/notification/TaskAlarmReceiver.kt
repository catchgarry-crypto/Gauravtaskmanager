package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.db.TaskDatabase
import com.example.model.TaskPriority
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        val fallbackTitle = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
        val fallbackDesc = intent.getStringExtra("EXTRA_TASK_DESC") ?: ""
        val fallbackPriorityStr = intent.getStringExtra("EXTRA_TASK_PRIORITY") ?: "MEDIUM"
        val fallbackPriority = try {
            TaskPriority.valueOf(fallbackPriorityStr)
        } catch (e: Exception) {
            TaskPriority.MEDIUM
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = TaskDatabase.getInstance(context)
                val task = db.taskDao().getTaskById(taskId)

                if (task != null) {
                    if (!task.isCompleted) {
                        NotificationHelper.showTaskDueNotification(
                            context = context,
                            taskId = task.id,
                            taskTitle = task.title,
                            taskDescription = task.description,
                            priority = task.priority
                        )
                    }
                } else {
                    NotificationHelper.showTaskDueNotification(
                        context = context,
                        taskId = taskId,
                        taskTitle = fallbackTitle,
                        taskDescription = fallbackDesc,
                        priority = fallbackPriority
                    )
                }
            } catch (e: Exception) {
                NotificationHelper.showTaskDueNotification(
                    context = context,
                    taskId = taskId,
                    taskTitle = fallbackTitle,
                    taskDescription = fallbackDesc,
                    priority = fallbackPriority
                )
            }
        }
    }
}
