package com.reflex.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.reflex.app.data.ReflexDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class DatabaseMigrationsFullTest {

    private fun createDbMock(executedStatements: MutableList<String>): SupportSQLiteDatabase {
        return Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase
    }

    @Test
    fun testAllMigrationVersions() {
        assertEquals(4, ReflexDatabase.MIGRATION_4_5.startVersion)
        assertEquals(5, ReflexDatabase.MIGRATION_4_5.endVersion)

        assertEquals(5, ReflexDatabase.MIGRATION_5_6.startVersion)
        assertEquals(6, ReflexDatabase.MIGRATION_5_6.endVersion)

        assertEquals(6, ReflexDatabase.MIGRATION_6_7.startVersion)
        assertEquals(7, ReflexDatabase.MIGRATION_6_7.endVersion)

        assertEquals(7, ReflexDatabase.MIGRATION_7_8.startVersion)
        assertEquals(8, ReflexDatabase.MIGRATION_7_8.endVersion)

        assertEquals(8, ReflexDatabase.MIGRATION_8_9.startVersion)
        assertEquals(9, ReflexDatabase.MIGRATION_8_9.endVersion)

        assertEquals(9, ReflexDatabase.MIGRATION_9_10.startVersion)
        assertEquals(10, ReflexDatabase.MIGRATION_9_10.endVersion)

        assertEquals(10, ReflexDatabase.MIGRATION_10_11.startVersion)
        assertEquals(11, ReflexDatabase.MIGRATION_10_11.endVersion)

        assertEquals(11, ReflexDatabase.MIGRATION_11_12.startVersion)
        assertEquals(12, ReflexDatabase.MIGRATION_11_12.endVersion)

        assertEquals(12, ReflexDatabase.MIGRATION_12_13.startVersion)
        assertEquals(13, ReflexDatabase.MIGRATION_12_13.endVersion)

        assertEquals(13, ReflexDatabase.MIGRATION_13_14.startVersion)
        assertEquals(14, ReflexDatabase.MIGRATION_13_14.endVersion)
    }

    @Test
    fun testMigration4To5() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_4_5.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE routines ADD COLUMN restBetweenStepsEnabled") })
        assertTrue(stmts.any { it.contains("ALTER TABLE routines ADD COLUMN restDurationSeconds") })
    }

    @Test
    fun testMigration5To6() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_5_6.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE routines ADD COLUMN scheduledDays") })
        assertTrue(stmts.any { it.contains("ALTER TABLE routines ADD COLUMN reminderTime") })
        assertTrue(stmts.any { it.contains("ALTER TABLE routines ADD COLUMN reminderEnabled") })
        assertTrue(stmts.any { it.contains("ALTER TABLE completion_logs ADD COLUMN isSkipped") })
    }

    @Test
    fun testMigration6To7() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_6_7.migrate(db)
        assertTrue(stmts.any { it.contains("CREATE TABLE IF NOT EXISTS `focus_settings`") })
        assertTrue(stmts.any { it.contains("CREATE TABLE IF NOT EXISTS `focus_sessions`") })
    }

    @Test
    fun testMigration7To8() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_7_8.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_settings ADD COLUMN blockingMode") })
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_settings ADD COLUMN selectedPackages") })
    }

    @Test
    fun testMigration8To9() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_8_9.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_sessions ADD COLUMN blockedAttemptCount") })
    }

    @Test
    fun testMigration9To10() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_9_10.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE steps ADD COLUMN emoji") })
    }

    @Test
    fun testMigration10To11() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_10_11.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_sessions ADD COLUMN sessionTitle") })
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_sessions ADD COLUMN checklistJson") })
    }

    @Test
    fun testMigration11To12() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_11_12.migrate(db)
        assertTrue(stmts.any { it.contains("CREATE TABLE IF NOT EXISTS `habits`") })
        assertTrue(stmts.any { it.contains("CREATE TABLE IF NOT EXISTS `habit_logs`") })
        assertTrue(stmts.any { it.contains("CREATE UNIQUE INDEX IF NOT EXISTS `index_habit_logs_habitId_epochDay`") })
    }

    @Test
    fun testMigration12To13() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_12_13.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE steps ADD COLUMN restDurationSeconds") })
        assertTrue(stmts.any { it.contains("ALTER TABLE completion_logs ADD COLUMN totalStepsCount") })
        assertTrue(stmts.any { it.contains("ALTER TABLE completion_logs ADD COLUMN stepLogsJson") })
        assertTrue(stmts.any { it.contains("CREATE TABLE IF NOT EXISTS `focus_tags`") })
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_sessions ADD COLUMN tagId") })
        assertTrue(stmts.any { it.contains("CREATE TABLE IF NOT EXISTS `blocked_app_events`") })
    }

    @Test
    fun testMigration13To14() {
        val stmts = mutableListOf<String>()
        val db = createDbMock(stmts)
        ReflexDatabase.MIGRATION_13_14.migrate(db)
        assertTrue(stmts.any { it.contains("ALTER TABLE focus_sessions ADD COLUMN endReason") })
        assertTrue(stmts.any { it.contains("ALTER TABLE habits ADD COLUMN frequencyType") })
        assertTrue(stmts.any { it.contains("ALTER TABLE habits ADD COLUMN reminderEnabled") })
        assertTrue(stmts.any { it.contains("ALTER TABLE habits ADD COLUMN colorHex") })
    }
}
