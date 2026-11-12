package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.AppCustomization
import com.example.model.HabitItem
import com.example.model.NoteItem
import com.example.model.TaskItem
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [NoteItem::class, TaskItem::class, HabitItem::class, AppCustomization::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun launcherDao(): LauncherDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "aura_launcher_database"
                )
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class DatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateInitialData(database.launcherDao())
                    }
                }
            }

            private suspend fun populateInitialData(dao: LauncherDao) {
                // Pre-populate some starter notes
                dao.insertNote(
                    NoteItem(
                        content = "💡 Swipe up for App Drawer • Swipe down for Smart Search • Long press icons for shortcuts!",
                        isPinned = true
                    )
                )
                dao.insertNote(
                    NoteItem(
                        content = "📋 Project goals: Ship Material You launcher with fluid gesture transitions."
                    )
                )

                // Starter tasks
                dao.insertTask(
                    TaskItem(
                        title = "Explore icon pack styles in Launcher Settings",
                        isCompleted = false,
                        category = "Personal"
                    )
                )
                dao.insertTask(
                    TaskItem(
                        title = "Configure daily habit streak goals",
                        isCompleted = false,
                        category = "Routines"
                    )
                )
                dao.insertTask(
                    TaskItem(
                        title = "Try math expressions in Search (e.g. 48 * 2.5)",
                        isCompleted = true,
                        category = "Launcher"
                    )
                )

                // Starter habits
                dao.insertHabit(
                    HabitItem(
                        title = "Hydrate (2.5L)",
                        iconName = "water",
                        currentStreak = 4,
                        isCompletedToday = true
                    )
                )
                dao.insertHabit(
                    HabitItem(
                        title = "Read 20 mins",
                        iconName = "book",
                        currentStreak = 7,
                        isCompletedToday = false
                    )
                )
                dao.insertHabit(
                    HabitItem(
                        title = "30m Workout",
                        iconName = "fitness",
                        currentStreak = 3,
                        isCompletedToday = false
                    )
                )
                dao.insertHabit(
                    HabitItem(
                        title = "Meditation",
                        iconName = "mindfulness",
                        currentStreak = 12,
                        isCompletedToday = true
                    )
                )
            }
        }
    }
}
