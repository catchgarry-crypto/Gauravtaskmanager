package com.example.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.example.db.TaskDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            val scheduler = TaskAlarmScheduler(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val db = TaskDatabase.getInstance(context)
                    val tasks = db.taskDao().getAllTasks().first()
                    val now = System.currentTimeMillis()
                    for (task in tasks) {
                        if (!task.isCompleted && task.reminderEnabled && task.dueDate != null && task.dueDate > now) {
                            scheduler.scheduleTaskReminder(task)
                        }
                    }
                } catch (e: Exception) {
                    // ignore
                }
            }
        }
    }
}
