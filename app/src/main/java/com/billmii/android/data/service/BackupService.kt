package com.billmii.android.data.service

import android.content.Context
import android.net.Uri
import com.billmii.android.data.database.BillMiiDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.*
import java.text.SimpleDateFormat
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Backup Service
 * Handles data backup and restore operations
 */
@Singleton
class BackupService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: BillMiiDatabase,
    private val fileStorageService: FileStorageService
) {
    
    companion object {
        private const val TAG = "BackupService"
        private const val BACKUP_DIR = "backups"
        private const val DATABASE_FILE = "billmii.db"
        private const val DATABASE_WAL_FILE = "billmii.db-wal"
        private const val DATABASE_SHM_FILE = "billmii.db-shm"
        private const val BACKUP_VERSION = 1
        private const val DATE_FORMAT = "yyyyMMdd_HHmmss"
    }
    
    /**
     * Create backup
     */
    suspend fun createBackup(
        includeImages: Boolean = true,
        description: String? = null
    ): Result<BackupInfo> = withContext(Dispatchers.IO) {
        try {
            // Create backup directory
            val backupDir = File(context.filesDir, BACKUP_DIR)
            if (!backupDir.exists()) {
                backupDir.mkdirs()
            }
            
            // Generate backup file name
            val timestamp = SimpleDateFormat(DATE_FORMAT, Locale.getDefault()).format(Date())
            val backupFileName = "billmii_backup_$timestamp.zip"
            val backupFile = File(backupDir, backupFileName)
            
            // Create zip file
            ZipOutputStream(FileOutputStream(backupFile)).use { zipOut ->
                // Add database
                addDatabaseToZip(zipOut)
                
                // Add images if requested
                if (includeImages) {
                    addImagesToZip(zipOut)
                }
                
                // Add backup metadata
                addBackupMetadata(zipOut, includeImages, description)
            }
            
            // Calculate file size
            val fileSize = backupFile.length()
            
            val backupInfo = BackupInfo(
                id = timestamp.toLong(),
                fileName = backupFileName,
                filePath = backupFile.absolutePath,
                fileSize = fileSize,
                createdAt = Date(),
                includesImages = includeImages,
                description = description,
                version = BACKUP_VERSION
            )
            
            Result.success(backupInfo)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Restore from backup
     */
    suspend fun restoreFromBackup(
        backupFile: File,
        restoreImages: Boolean = true
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            // Validate backup
            val validation = validateBackup(backupFile)
            if (!validation.isValid) {
                return@withContext Result.failure(Exception(validation.error))
            }
            
            // Close database before restore
            database.close()
            
            // Extract backup
            var restoredImages = 0
            var restoredDatabase = false
            
            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var entry = zipIn.nextEntry
                
                while (entry != null) {
                    when {
                        entry.name == "database/$DATABASE_FILE" -> {
                            // Restore database
                            val dbFile = context.getDatabasePath(DATABASE_FILE)
                            extractFile(zipIn, dbFile)
                            restoredDatabase = true
                        }
                        entry.name.startsWith("images/") && restoreImages -> {
                            // Restore image
                            val imageFile = File(context.filesDir, entry.name.substringAfter("images/"))
                            extractFile(zipIn, imageFile)
                            restoredImages++
                        }
                    }
                    
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            
            // Reopen database
            database.openHelper.readableDatabase
            
            val restoreResult = RestoreResult(
                success = true,
                databaseRestored = restoredDatabase,
                imagesRestored = restoredImages,
                timestamp = Date()
            )
            
            Result.success(restoreResult)
        } catch (e: Exception) {
            // Try to reopen database on error
            try {
                database.openHelper.readableDatabase
            } catch (ex: Exception) {
                ex.printStackTrace()
            }
            Result.failure(e)
        }
    }
    
    /**
     * Get all backups
     */
    fun getBackups(): Flow<List<BackupInfo>> {
        val backupDir = File(context.filesDir, BACKUP_DIR)
        val backups = mutableListOf<BackupInfo>()
        
        if (backupDir.exists()) {
            backupDir.listFiles { file ->
                file.extension == "zip" && file.name.startsWith("billmii_backup_")
            }?.sortedByDescending { it.lastModified() }?.forEach { file ->
                val info = BackupInfo(
                    id = file.lastModified(),
                    fileName = file.name,
                    filePath = file.absolutePath,
                    fileSize = file.length(),
                    createdAt = Date(file.lastModified()),
                    version = BACKUP_VERSION
                )
                backups.add(info)
            }
        }
        
        return kotlinx.coroutines.flow.flow { emit(backups) }
    }
    
    /**
     * Delete backup
     */
    suspend fun deleteBackup(backupFile: File): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            if (backupFile.exists()) {
                backupFile.delete()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export backup to external storage
     */
    suspend fun exportBackup(
        backupFile: File,
        destinationUri: Uri
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openOutputStream(destinationUri)?.use { output ->
                FileInputStream(backupFile).use { input ->
                    input.copyTo(output)
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Import backup from external storage
     */
    suspend fun importBackup(
        sourceUri: Uri,
        restoreImages: Boolean = true
    ): Result<RestoreResult> = withContext(Dispatchers.IO) {
        try {
            // Copy to temp file
            val tempFile = File(context.cacheDir, "temp_backup_${System.currentTimeMillis()}.zip")
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Restore from temp file
            val result = restoreFromBackup(tempFile, restoreImages)
            
            // Delete temp file
            tempFile.delete()
            
            result
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Add database to zip
     */
    private fun addDatabaseToZip(zipOut: ZipOutputStream) {
        val dbFile = context.getDatabasePath(DATABASE_FILE)
        if (dbFile.exists()) {
            val entry = ZipEntry("database/$DATABASE_FILE")
            zipOut.putNextEntry(entry)
            FileInputStream(dbFile).use { input ->
                input.copyTo(zipOut)
            }
            zipOut.closeEntry()
        }
    }
    
    /**
     * Add images to zip
     */
    private fun addImagesToZip(zipOut: ZipOutputStream) {
        val imagesDir = File(context.filesDir, "receipts")
        if (imagesDir.exists()) {
            imagesDir.walkTopDown()
                .filter { it.isFile }
                .forEach { file ->
                    val relativePath = imagesDir.toPath().relativize(file.toPath()).toString()
                    val entry = ZipEntry("images/$relativePath")
                    zipOut.putNextEntry(entry)
                    FileInputStream(file).use { input ->
                        input.copyTo(zipOut)
                    }
                    zipOut.closeEntry()
                }
        }
    }
    
    /**
     * Add backup metadata to zip
     */
    private fun addBackupMetadata(
        zipOut: ZipOutputStream,
        includesImages: Boolean,
        description: String?
    ) {
        val metadata = mapOf(
            "version" to BACKUP_VERSION,
            "timestamp" to Date().time,
            "includesImages" to includesImages,
            "description" to (description ?: ""),
            "appVersion" to getAppVersion()
        )
        
        val metadataJson = com.google.gson.Gson().toJson(metadata)
        val entry = ZipEntry("metadata.json")
        zipOut.putNextEntry(entry)
        zipOut.write(metadataJson.toByteArray())
        zipOut.closeEntry()
    }
    
    /**
     * Extract file from zip
     */
    private fun extractFile(zipIn: ZipInputStream, outputFile: File) {
        outputFile.parentFile?.mkdirs()
        FileOutputStream(outputFile).use { output ->
            zipIn.copyTo(output)
        }
    }
    
    /**
     * Validate backup
     */
    private fun validateBackup(backupFile: File): BackupValidation {
        if (!backupFile.exists()) {
            return BackupValidation(false, "Backup file not found")
        }
        
        try {
            ZipInputStream(FileInputStream(backupFile)).use { zipIn ->
                var hasDatabase = false
                var hasMetadata = false
                
                var entry = zipIn.nextEntry
                while (entry != null) {
                    when {
                        entry.name == "database/$DATABASE_FILE" -> hasDatabase = true
                        entry.name == "metadata.json" -> hasMetadata = true
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
                
                if (!hasDatabase) {
                    return BackupValidation(false, "Backup does not contain database")
                }
                
                if (!hasMetadata) {
                    return BackupValidation(false, "Backup metadata is missing")
                }
                
                return BackupValidation(true, null)
            }
        } catch (e: Exception) {
            return BackupValidation(false, "Invalid backup file: ${e.message}")
        }
    }
    
    /**
     * Get app version
     */
    private fun getAppVersion(): String {
        return try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            "${packageInfo.versionName} (${packageInfo.longVersionCode})"
        } catch (e: Exception) {
            "Unknown"
        }
    }
    
    /**
     * Get backup statistics
     */
    suspend fun getBackupStatistics(): BackupStatistics = withContext(Dispatchers.IO) {
        val backups = getBackups().first()
        val totalSize = backups.sumOf { it.fileSize }
        val totalBackups = backups.size
        
        // Get database size
        val dbFile = context.getDatabasePath(DATABASE_FILE)
        val databaseSize = if (dbFile.exists()) dbFile.length() else 0L
        
        // Get images size
        val imagesDir = File(context.filesDir, "receipts")
        val imagesSize = if (imagesDir.exists()) {
            imagesDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        } else {
            0L
        }
        
        BackupStatistics(
            totalBackups = totalBackups,
            totalBackupSize = totalSize,
            databaseSize = databaseSize,
            imagesSize = imagesSize,
            lastBackupDate = backups.firstOrNull()?.createdAt
        )
    }
    
    /**
     * Schedule automatic backup
     */
    suspend fun scheduleAutoBackup(intervalHours: Int): Result<Unit> {
        // TODO: Implement WorkManager for scheduled backups
        return Result.success(Unit)
    }
    
    /**
     * Cancel automatic backup
     */
    suspend fun cancelAutoBackup(): Result<Unit> {
        // TODO: Cancel WorkManager job
        return Result.success(Unit)
    }
}

/**
 * Backup information
 */
data class BackupInfo(
    val id: Long,
    val fileName: String,
    val filePath: String,
    val fileSize: Long,
    val createdAt: Date,
    val includesImages: Boolean = true,
    val description: String? = null,
    val version: Int = 1
)

/**
 * Restore result
 */
data class RestoreResult(
    val success: Boolean,
    val databaseRestored: Boolean,
    val imagesRestored: Int,
    val timestamp: Date,
    val error: String? = null
)

/**
 * Backup validation
 */
data class BackupValidation(
    val isValid: Boolean,
    val error: String? = null
)

/**
 * Backup statistics
 */
data class BackupStatistics(
    val totalBackups: Int,
    val totalBackupSize: Long,
    val databaseSize: Long,
    val imagesSize: Long,
    val lastBackupDate: Date?
)