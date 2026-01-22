package com.billmii.android.ui.settings.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.billmii.android.data.service.BackupInterval as ServiceBackupInterval
import com.billmii.android.data.service.BackupSchedulerService
import com.billmii.android.ui.settings.BackupInterval
import com.billmii.android.ui.settings.OCRMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Settings ViewModel
 * Manages application settings
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val backupSchedulerService: BackupSchedulerService
) : ViewModel() {
    
    // OCR Settings
    private val _ocrMode = MutableStateFlow(OCRMode.PRECISE)
    val ocrMode: StateFlow<OCRMode> = _ocrMode.asStateFlow()
    
    // Backup Settings
    private val _autoBackup = MutableStateFlow(false)
    val autoBackup: StateFlow<Boolean> = _autoBackup.asStateFlow()
    
    private val _backupInterval = MutableStateFlow(BackupInterval.WEEKLY)
    val backupInterval: StateFlow<BackupInterval> = _backupInterval.asStateFlow()
    
    init {
        loadSettings()
    }
    
    /**
     * Load settings from preferences
     */
    private fun loadSettings() {
        viewModelScope.launch {
            val schedule = backupSchedulerService.scheduleState.value
            _autoBackup.value = schedule.enabled
            _backupInterval.value = when (schedule.interval) {
                ServiceBackupInterval.DAILY -> BackupInterval.DAILY
                ServiceBackupInterval.WEEKLY -> BackupInterval.WEEKLY
                ServiceBackupInterval.MONTHLY -> BackupInterval.MONTHLY
            }
        }
    }
    
    /**
     * Update OCR mode
     */
    fun updateOcrMode(mode: OCRMode) {
        _ocrMode.value = mode
        viewModelScope.launch {
            // TODO: Save to preferences
        }
    }
    
    /**
     * Update auto backup setting
     */
    fun updateAutoBackup(enabled: Boolean) {
        _autoBackup.value = enabled
        viewModelScope.launch {
            if (enabled) {
                val interval = when (_backupInterval.value) {
                    BackupInterval.DAILY -> ServiceBackupInterval.DAILY
                    BackupInterval.WEEKLY -> ServiceBackupInterval.WEEKLY
                    BackupInterval.MONTHLY -> ServiceBackupInterval.MONTHLY
                }
                backupSchedulerService.enableAutoBackup(interval)
            } else {
                backupSchedulerService.disableAutoBackup()
            }
        }
    }
    
    /**
     * Update backup interval
     */
    fun updateBackupInterval(interval: BackupInterval) {
        _backupInterval.value = interval
        viewModelScope.launch {
            if (_autoBackup.value) {
                val serviceInterval = when (interval) {
                    BackupInterval.DAILY -> ServiceBackupInterval.DAILY
                    BackupInterval.WEEKLY -> ServiceBackupInterval.WEEKLY
                    BackupInterval.MONTHLY -> ServiceBackupInterval.MONTHLY
                }
                backupSchedulerService.enableAutoBackup(serviceInterval)
            }
        }
    }
}