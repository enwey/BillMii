package com.billmii.android.data.work

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.billmii.android.data.repository.BackupRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 自动备份 Worker
 * 定期执行数据备份任务
 */
@HiltWorker
class BackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParams: WorkerParameters,
    private val backupRepository: BackupRepository
) : CoroutineWorker(context, workerParams) {
    
    companion object {
        private const val TAG = "BackupWorker"
        const val WORK_NAME = "backup_worker"
        const val KEY_BACKUP_TYPE = "backup_type"
        
        enum class BackupType {
            AUTO,
            MANUAL
        }
    }
    
    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting backup task")
            
            val backupType = inputData.getString(KEY_BACKUP_TYPE)?.let { 
                BackupType.valueOf(it) 
            } ?: BackupType.AUTO
            
            val backupFile = createBackupFile(backupType)
            val result = backupRepository.createBackup(backupFile.absolutePath)
            
            if (result.isSuccess) {
                Log.d(TAG, "Backup completed successfully: ${backupFile.absolutePath}")
                
                // Clean up old backups (keep last 10)
                cleanupOldBackups()
                
                Result.success()
            } else {
                Log.e(TAG, "Backup failed: ${result.exceptionOrNull()?.message}")
                Result.failure()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Backup error", e)
            Result.failure()
        }
    }
    
    /**
     * 创建备份文件
     */
    private fun createBackupFile(type: BackupType): File {
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "billmii_backup_${type.name.lowercase()}_$timestamp.zip"
        
        val backupDir = File(applicationContext.getExternalFilesDir(null), "backups")
        if (!backupDir.exists()) {
            backupDir.mkdirs()
        }
        
        return File(backupDir, fileName)
    }
    
    /**
     * 清理旧备份文件
     */
    private fun cleanupOldBackups() {
        try {
            val backupDir = File(applicationContext.getExternalFilesDir(null), "backups")
            if (!backupDir.exists()) return
            
            val backupFiles = backupDir.listFiles()
                ?.filter { it.extension == "zip" }
                ?.sortedByDescending { it.lastModified() }
                ?: return
            
            // Keep only the last 10 backups
            if (backupFiles.size > 10) {
                backupFiles.drop(10).forEach { file ->
                    if (file.delete()) {
                        Log.d(TAG, "Deleted old backup: ${file.name}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning up old backups", e)
        }
    }
}