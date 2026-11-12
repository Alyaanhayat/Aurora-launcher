package com.example.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "quick_notes")
data class NoteItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false
)

@Entity(tableName = "todo_tasks")
data class TaskItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val priority: TaskPriority = TaskPriority.MEDIUM,
    val category: String = "General",
    val timestamp: Long = System.currentTimeMillis()
)

enum class TaskPriority(val label: String) {
    LOW("Low"),
    MEDIUM("Med"),
    HIGH("High")
}

@Entity(tableName = "daily_habits")
data class HabitItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val iconName: String,
    val currentStreak: Int = 1,
    val isCompletedToday: Boolean = false,
    val lastCompletedDate: String = ""
)

@Entity(tableName = "app_customization")
data class AppCustomization(
    @PrimaryKey val packageName: String,
    val customLabel: String? = null,
    val isHidden: Boolean = false,
    val isFavorite: Boolean = false,
    val isDocked: Boolean = false,
    val launchCount: Int = 0,
    val lastLaunchTime: Long = 0L
)
