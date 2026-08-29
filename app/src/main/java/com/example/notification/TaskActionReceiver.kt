package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.db.TaskDatabase
import com.example.model.TaskStatus
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getLongExtra(NotificationHelper.EXTRA_TASK_ID, -1L)
        if (taskId == -1L) return

        when (intent.action) {
            NotificationHelper.ACTION_MARK_DONE -> {
                NotificationHelper.cancelNotification(context, taskId)
                CoroutineScope(Dispatchers.IO).launch {
                    val db = TaskDatabase.getInstance(context)
                    val task = db.taskDao().getTaskById(taskId)
                    if (task != null) {
                        val updated = task.copy(
                            status = TaskStatus.COMPLETED,
                            completedAt = System.currentTimeMillis(),
                            updatedAt = System.currentTimeMillis()
                        )
                        db.taskDao().updateTask(updated)
                    }
                }
            }

            NotificationHelper.ACTION_SNOOZE_15M -> {
                NotificationHelper.cancelNotification(context, taskId)
                val title = intent.getStringExtra(NotificationHelper.EXTRA_TASK_TITLE) ?: "Task Reminder"
                val scheduler = TaskAlarmScheduler(context)
                scheduler.scheduleSnoozeReminder(taskId, title, minutes = 15)
            }
        }
    }
}
