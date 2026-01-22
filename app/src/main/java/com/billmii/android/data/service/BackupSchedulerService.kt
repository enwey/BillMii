package com.billmii.android.data.service

import android.content.Context
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.*
import com.billmii.android.data.work.BackupWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份调度服务
 * 管理自动备份任务的调度
 */
@Singleton
class BackupSchedulerService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val workManager = WorkManager.getInstance(context)
    
    data class BackupSchedule(
        val enabled: Boolean = false,
        val interval: BackupInterval = BackupInterval.WEEKLY
    )
    
    private val _scheduleState = MutableStateFlow(BackupSchedule())
    val scheduleState: StateFlow<BackupSchedule> = _scheduleState
    
    companion object {
        private const val AUTO_BACKUP_TAG = "auto_backup"
    }
    
    /**
     * 启用自动备份
     */
    fun enableAutoBackup(interval: BackupInterval) {
        val schedule = BackupSchedule(enabled = true, interval = interval)
        _scheduleState.value = schedule
        scheduleBackup(interval)
    }
    
    /**
     * 禁用自动备份
     */
    fun disableAutoBackup() {
        _scheduleState.value = BackupSchedule(enabled = false)
        cancelAutoBackup()
    }
    
    /**
     * 调度备份任务
     */
    private fun scheduleBackup(interval: BackupInterval) {
        // Cancel existing backup first
        cancelAutoBackup()
        
        val repeatInterval = when (interval) {
            BackupInterval.DAILY -> 1L
            BackupInterval.WEEKLY -> 7L
            BackupInterval.MONTHLY -> 30L
        }
        
        val timeUnit = when (interval) {
            BackupInterval.DAILY -> TimeUnit.DAYS
            BackupInterval.WEEKLY -> TimeUnit.DAYS
            BackupInterval.MONTHLY -> TimeUnit.DAYS
        }
        
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .setRequiresCharging(false)
            .build()
        
        val workRequest = PeriodicWorkRequestBuilder<BackupWorker>(
            repeatInterval = repeatInterval,
            repeatIntervalTimeUnit = timeUnit
        )
            .setConstraints(constraints)
            .addTag(AUTO_BACKUP_TAG)
            .setInputData(
                workDataOf(
                    BackupWorker.KEY_BACKUP_TYPE to BackupWorker.BackupType.AUTO.name
                )
            )
            .build()
        
        workManager.enqueueUniquePeriodicWork(
            AUTO_BACKUP_TAG,
            ExistingPeriodicWorkPolicy.REPLACE,
            workRequest
        )
    }
    
    /**
     * 取消自动备份
     */
    private fun cancelAutoBackup() {
        workManager.cancelAllWorkByTag(AUTO_BACKUP_TAG)
    }
    
    /**
     * 立即执行一次备份
     */
    fun scheduleOneTimeBackup() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()
        
        val workRequest = OneTimeWorkRequestBuilder<BackupWorker>()
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    BackupWorker.KEY_BACKUP_TYPE to BackupWorker.BackupType.MANUAL.name
                )
            )
            .build()
        
        workManager.enqueue(workRequest)
    }
    
    /**
     * 获取备份任务状态
     */
    fun getBackupWorkInfo(): StateFlow<WorkInfo?> {
        val workInfoFlow = MutableStateFlow<WorkInfo?>(null)
        
        workManager.getWorkInfosByTagFlow(AUTO_BACKUP_TAG).collect { workInfos ->
            workInfoFlow.value = workInfos.firstOrNull()
        }
        
        return workInfoFlow
    }
    
    /**
     * 检查是否已启用自动备份
     */
    suspend fun isAutoBackupEnabled(): Boolean {
        return scheduleState.value.enabled
    }
    
    /**
     * 获取当前备份间隔
     */
    suspend fun getCurrentBackupInterval(): BackupInterval {
        return scheduleState.value.interval
    }
}

/**
 * 备份间隔枚举
 */
enum class BackupInterval(val displayName: String) {
    DAILY("每天"),
    WEEKLY("每周"),
    MONTHLY("每月")
}