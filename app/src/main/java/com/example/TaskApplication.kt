package com.example

import android.app.Application
import com.example.db.TaskDatabase
import com.example.notification.NotificationHelper
import com.example.repository.TaskRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class TaskApplication : Application() {

    lateinit var database: TaskDatabase
        private set

    lateinit var repository: TaskRepository
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this

        NotificationHelper.createNotificationChannel(this)

        database = TaskDatabase.getInstance(this)
        repository = TaskRepository(database.taskDao(), this)

        // Seed initial sample tasks in the background if database is fresh
        CoroutineScope(Dispatchers.IO).launch {
            try {
                repository.seedSampleTasksIfEmpty()
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    companion object {
        lateinit var instance: TaskApplication
            private set
    }
}
