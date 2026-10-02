package com.reflex.app.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [Routine::class, Step::class, CompletionLog::class, Task::class, FocusSettings::class, FocusSession::class, Habit::class, HabitLog::class, FocusTag::class, BlockedAppEvent::class],
    version = 13,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class ReflexDatabase : RoomDatabase() {

    abstract fun routineDao(): RoutineDao
    abstract fun stepDao(): StepDao
    abstract fun completionLogDao(): CompletionLogDao
    abstract fun taskDao(): TaskDao
    abstract fun focusSettingsDao(): FocusSettingsDao
    abstract fun focusSessionDao(): FocusSessionDao
    abstract fun habitDao(): HabitDao
    abstract fun focusTagDao(): FocusTagDao
    abstract fun blockedAppEventDao(): BlockedAppEventDao

    companion object {
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN restBetweenStepsEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE routines ADD COLUMN restDurationSeconds INTEGER NOT NULL DEFAULT 15")
            }
        }

        val MIGRATION_5_6 = object : Migration(5, 6) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE routines ADD COLUMN scheduledDays TEXT NOT NULL DEFAULT ''")
                db.execSQL("ALTER TABLE routines ADD COLUMN reminderTime TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE routines ADD COLUMN reminderEnabled INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE completion_logs ADD COLUMN isSkipped INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_settings` (
                        `id` INTEGER NOT NULL,
                        `workDurationMin` INTEGER NOT NULL,
                        `shortBreakMin` INTEGER NOT NULL,
                        `longBreakMin` INTEGER NOT NULL,
                        `sessionsBeforeLongBreak` INTEGER NOT NULL,
                        `autoStartNextPhase` INTEGER NOT NULL,
                        `soundEnabled` INTEGER NOT NULL,
                        `vibrationEnabled` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_sessions` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `mode` TEXT NOT NULL,
                        `startTime` INTEGER NOT NULL,
                        `endTime` INTEGER NOT NULL,
                        `plannedDurationSeconds` INTEGER,
                        `actualDurationSeconds` INTEGER NOT NULL,
                        `completedCycles` INTEGER NOT NULL,
                        `completed` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_settings ADD COLUMN blockingMode TEXT NOT NULL DEFAULT 'OFF'")
                db.execSQL("ALTER TABLE focus_settings ADD COLUMN selectedPackages TEXT NOT NULL DEFAULT ''")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN blockedAttemptCount INTEGER NOT NULL DEFAULT 0")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE steps ADD COLUMN emoji TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN sessionTitle TEXT DEFAULT NULL")
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN checklistJson TEXT DEFAULT NULL")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habits` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `type` TEXT NOT NULL,
                        `target` REAL NOT NULL,
                        `unit` TEXT NOT NULL,
                        `step` REAL NOT NULL,
                        `startEpochDay` INTEGER NOT NULL,
                        `sortOrder` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `habit_logs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `habitId` INTEGER NOT NULL,
                        `epochDay` INTEGER NOT NULL,
                        `value` REAL NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_epochDay` ON `habit_logs` (`habitId`, `epochDay`)
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE steps ADD COLUMN restDurationSeconds INTEGER DEFAULT NULL")
                db.execSQL("ALTER TABLE completion_logs ADD COLUMN totalStepsCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE completion_logs ADD COLUMN stepLogsJson TEXT DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `focus_tags` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL("ALTER TABLE focus_sessions ADD COLUMN tagId INTEGER DEFAULT NULL")
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `blocked_app_events` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `packageName` TEXT NOT NULL,
                        `timestamp` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
            }
        }

        @Volatile
        private var INSTANCE: ReflexDatabase? = null

        fun getDatabase(context: Context): ReflexDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    ReflexDatabase::class.java,
                    "reflex_database"
                )
                .addMigrations(MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8, MIGRATION_8_9, MIGRATION_9_10, MIGRATION_10_11, MIGRATION_11_12, MIGRATION_12_13)
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
