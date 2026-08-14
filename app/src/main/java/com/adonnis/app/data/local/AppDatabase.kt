package com.adonnis.app.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.adonnis.app.data.local.dao.*
import com.adonnis.app.data.local.entity.*

@Database(
    entities = [
        UserEntity::class,
        PlanEntity::class,
        DiaryEntryEntity::class,
        ReminderEntity::class,
        AlarmEntity::class,
        ChatMessageEntity::class,
        MemoryEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun planDao(): PlanDao
    abstract fun diaryEntryDao(): DiaryEntryDao
    abstract fun reminderDao(): ReminderDao
    abstract fun alarmDao(): AlarmDao
    abstract fun chatMessageDao(): ChatMessageDao
    abstract fun memoryDao(): MemoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        /**
         * v3 -> v4: add the memory_items table for the AI's long-term memory.
         * Written as a real migration so user data (timetable, goals, plans)
         * is NOT wiped on upgrade.
         */
        private val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """CREATE TABLE IF NOT EXISTS `memory_items` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `category` TEXT NOT NULL,
                        `content` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `created_at` INTEGER NOT NULL
                        )"""
                )
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_memory_items_created_at` ON `memory_items` (`created_at`)")
            }
        }

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "adonnis_database"
                )
                    .addMigrations(MIGRATION_3_4)
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
