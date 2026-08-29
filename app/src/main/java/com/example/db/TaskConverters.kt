package com.example.db

import androidx.room.TypeConverter
import com.example.model.SubTask
import com.example.model.TaskPriority
import com.example.model.TaskStatus
import org.json.JSONArray
import org.json.JSONObject

class TaskConverters {

    @TypeConverter
    fun fromPriority(priority: TaskPriority?): String {
        return priority?.name ?: TaskPriority.MEDIUM.name
    }

    @TypeConverter
    fun toPriority(value: String?): TaskPriority {
        return try {
            if (value != null) TaskPriority.valueOf(value) else TaskPriority.MEDIUM
        } catch (e: Exception) {
            TaskPriority.MEDIUM
        }
    }

    @TypeConverter
    fun fromStatus(status: TaskStatus?): String {
        return status?.name ?: TaskStatus.PENDING.name
    }

    @TypeConverter
    fun toStatus(value: String?): TaskStatus {
        return try {
            if (value != null) TaskStatus.valueOf(value) else TaskStatus.PENDING
        } catch (e: Exception) {
            TaskStatus.PENDING
        }
    }

    @TypeConverter
    fun fromSubTaskList(subtasks: List<SubTask>?): String {
        if (subtasks.isNullOrEmpty()) return "[]"
        val array = JSONArray()
        for (item in subtasks) {
            val obj = JSONObject()
            obj.put("id", item.id)
            obj.put("title", item.title)
            obj.put("isCompleted", item.isCompleted)
            array.put(obj)
        }
        return array.toString()
    }

    @TypeConverter
    fun toSubTaskList(value: String?): List<SubTask> {
        if (value.isNullOrEmpty()) return emptyList()
        val list = mutableListOf<SubTask>()
        try {
            val array = JSONArray(value)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id", java.util.UUID.randomUUID().toString())
                val title = obj.optString("title", "")
                val isCompleted = obj.optBoolean("isCompleted", false)
                list.add(SubTask(id = id, title = title, isCompleted = isCompleted))
            }
        } catch (e: Exception) {
            // fallback
        }
        return list
    }
}
