package com.example.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.model.TaskItem

class TaskAlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleTaskReminder(task: TaskItem) {
        val dueTime = task.dueDate ?: return
        if (task.isCompleted || !task.reminderEnabled) return

        // If due time is already in the past, do not schedule
        if (dueTime <= System.currentTimeMillis()) return

        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, task.id)
            putExtra(NotificationHelper.EXTRA_TASK_TITLE, task.title)
            putExtra("EXTRA_TASK_DESC", task.description)
            putExtra("EXTRA_TASK_PRIORITY", task.priority.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            task.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    dueTime,
                    pendingIntent
                )
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    dueTime,
                    pendingIntent
                )
            }
            Log.d("TaskAlarmScheduler", "Alarm scheduled for task ${task.id} at $dueTime")
        } catch (e: SecurityException) {
            // If exact alarm permission is not granted on newer Android, fallback to standard set()
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                dueTime,
                pendingIntent
            )
            Log.w("TaskAlarmScheduler", "Exact alarm fallback to inexact for task ${task.id}")
        }
    }

    fun scheduleSnoozeReminder(taskId: Long, taskTitle: String, minutes: Int = 15) {
        val triggerTime = System.currentTimeMillis() + (minutes * 60 * 1000L)
        val intent = Intent(context, TaskAlarmReceiver::class.java).apply {
            putExtra(NotificationHelper.EXTRA_TASK_ID, taskId)
            putExtra(NotificationHelper.EXTRA_TASK_TITLE, taskTitle)
            putExtra("EXTRA_TASK_DESC", "Snoozed reminder")
            putExtra("EXTRA_TASK_PRIORITY", "HIGH")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            } else {
                alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    triggerTime,
                    pendingIntent
                )
            }
        } catch (e: Exception) {
            alarmManager.set(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                pendingIntent
            )
        }
    }

    fun cancelTaskReminder(taskId: Long) {
        val intent = Intent(context, TaskAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            taskId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }
}
