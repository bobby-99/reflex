package com.reflex.app.service

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.reflex.app.util.AppLog
import com.reflex.app.util.DataExportImportManager
import com.reflex.app.util.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class AutoBackupWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val context = applicationContext
        val treeUri = SettingsRepository.getAutoBackupUri(context)
        if (treeUri == null) {
            SettingsRepository.setLastBackupStatus(context, "No folder selected")
            return@withContext Result.failure()
        }

        try {
            val rootDir = DocumentFile.fromTreeUri(context, treeUri)
            if (rootDir == null || !rootDir.canWrite()) {
                SettingsRepository.setLastBackupStatus(context, "Cannot write to folder")
                return@withContext Result.failure()
            }

            val jsonContent = DataExportImportManager.exportDataToJson(context)
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
            val fileName = "reflex-backup-$dateStr.json"

            val existingFile = rootDir.findFile(fileName)
            val targetFile = existingFile ?: rootDir.createFile("application/json", fileName)
            if (targetFile == null) {
                SettingsRepository.setLastBackupStatus(context, "Failed to create backup file")
                return@withContext Result.failure()
            }

            context.contentResolver.openOutputStream(targetFile.uri, "wt")?.use { out ->
                out.write(jsonContent.toByteArray())
            } ?: run {
                SettingsRepository.setLastBackupStatus(context, "Failed to write backup data")
                return@withContext Result.failure()
            }

            // Prunes backups in that directory to retain only the newest 4 reflex-backup-*.json files
            pruneOldBackups(rootDir)

            SettingsRepository.setLastBackupTime(context, System.currentTimeMillis())
            SettingsRepository.setLastBackupStatus(context, null)
            Result.success()
        } catch (e: Exception) {
            AppLog.e("AutoBackupWorker", "Auto backup failed", e)
            SettingsRepository.setLastBackupStatus(context, e.message ?: "Backup failed")
            Result.retry()
        }
    }

    private fun pruneOldBackups(rootDir: DocumentFile) {
        try {
            val files = rootDir.listFiles()
            val backupFiles = files.filter { file ->
                val name = file.name ?: ""
                name.startsWith("reflex-backup-") && name.endsWith(".json")
            }.sortedByDescending { it.name ?: "" }

            if (backupFiles.size > 4) {
                for (i in 4 until backupFiles.size) {
                    backupFiles[i].delete()
                }
            }
        } catch (e: Exception) {
            AppLog.w("AutoBackupWorker", "Failed to prune old backups", e)
        }
    }

    companion object {
        const val UNIQUE_WORK_NAME = "reflex_weekly_auto_backup"
    }
}
