package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.model.AppCustomization
import com.example.model.HabitItem
import com.example.model.NoteItem
import com.example.model.TaskItem
import kotlinx.coroutines.flow.Flow

@Dao
interface LauncherDao {
    // Notes
    @Query("SELECT * FROM quick_notes ORDER BY isPinned DESC, timestamp DESC")
    fun getAllNotes(): Flow<List<NoteItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertNote(note: NoteItem): Long

    @Delete
    suspend fun deleteNote(note: NoteItem)

    // Tasks
    @Query("SELECT * FROM todo_tasks ORDER BY isCompleted ASC, timestamp DESC")
    fun getAllTasks(): Flow<List<TaskItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: TaskItem): Long

    @Update
    suspend fun updateTask(task: TaskItem)

    @Delete
    suspend fun deleteTask(task: TaskItem)

    // Habits
    @Query("SELECT * FROM daily_habits ORDER BY id ASC")
    fun getAllHabits(): Flow<List<HabitItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitItem): Long

    @Update
    suspend fun updateHabit(habit: HabitItem)

    @Delete
    suspend fun deleteHabit(habit: HabitItem)

    // App Customizations
    @Query("SELECT * FROM app_customization")
    fun getAllCustomizations(): Flow<List<AppCustomization>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveCustomization(customization: AppCustomization)

    @Query("SELECT * FROM app_customization WHERE packageName = :pkg LIMIT 1")
    suspend fun getCustomization(pkg: String): AppCustomization?

    @Query("UPDATE app_customization SET launchCount = launchCount + 1, lastLaunchTime = :time WHERE packageName = :pkg")
    suspend fun recordLaunch(pkg: String, time: Long = System.currentTimeMillis())
}
