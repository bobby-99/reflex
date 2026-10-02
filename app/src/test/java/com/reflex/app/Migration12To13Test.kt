package com.reflex.app

import androidx.sqlite.db.SupportSQLiteDatabase
import com.reflex.app.data.ReflexDatabase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.lang.reflect.Proxy

class Migration12To13Test {

    @Test
    fun testMigration12To13Versions() {
        assertEquals(12, ReflexDatabase.MIGRATION_12_13.startVersion)
        assertEquals(13, ReflexDatabase.MIGRATION_12_13.endVersion)
    }

    @Test
    fun testMigration12To13ExecutesRequiredStatements() {
        val executedStatements = mutableListOf<String>()

        val dbProxy = Proxy.newProxyInstance(
            SupportSQLiteDatabase::class.java.classLoader,
            arrayOf(SupportSQLiteDatabase::class.java)
        ) { _, method, args ->
            if (method.name == "execSQL" && args != null && args.isNotEmpty()) {
                executedStatements.add(args[0] as String)
            }
            null
        } as SupportSQLiteDatabase

        ReflexDatabase.MIGRATION_12_13.migrate(dbProxy)

        assertTrue(
            "Should add restDurationSeconds to steps",
            executedStatements.any { it.contains("ALTER TABLE steps ADD COLUMN restDurationSeconds") }
        )
        assertTrue(
            "Should add totalStepsCount to completion_logs",
            executedStatements.any { it.contains("ALTER TABLE completion_logs ADD COLUMN totalStepsCount") }
        )
        assertTrue(
            "Should add stepLogsJson to completion_logs",
            executedStatements.any { it.contains("ALTER TABLE completion_logs ADD COLUMN stepLogsJson") }
        )
        assertTrue(
            "Should create focus_tags table",
            executedStatements.any { it.contains("CREATE TABLE IF NOT EXISTS `focus_tags`") }
        )
        assertTrue(
            "Should add tagId to focus_sessions",
            executedStatements.any { it.contains("ALTER TABLE focus_sessions ADD COLUMN tagId") }
        )
        assertTrue(
            "Should create blocked_app_events table",
            executedStatements.any { it.contains("CREATE TABLE IF NOT EXISTS `blocked_app_events`") }
        )
    }
}
