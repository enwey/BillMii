package com.billmii.android.data.service

import android.content.Context
import android.net.Uri
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * File Storage Service
 * Manages file storage operations including copying, hashing, and organization
 */
class FileStorageService(private val context: Context) {
    
    companion object {
        private const val RECEIPTS_DIR = "receipts"
        private const val TEMP_DIR = "temp"
        private const val BACKUP_DIR = "backup"
        private const val ARCHIVE_DIR = "archive"
    }
    
    /**
     * Get base storage directory
     */
    fun getBaseDirectory(): File {
        val dir = File(context.filesDir, "billmii")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Get receipts directory
     */
    fun getReceiptsDirectory(): File {
        val dir = File(getBaseDirectory(), RECEIPTS_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Get temp directory
     */
    fun getTempDirectory(): File {
        val dir = File(getBaseDirectory(), TEMP_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Get backup directory
     */
    fun getBackupDirectory(): File {
        val dir = File(getBaseDirectory(), BACKUP_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Get archive directory
     */
    fun getArchiveDirectory(): File {
        val dir = File(getBaseDirectory(), ARCHIVE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Create organized directory structure
     */
    fun createYearMonthDirectory(year: Int, month: Int): File {
        val dir = File(getReceiptsDirectory(), "$year/$month")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }
    
    /**
     * Copy file from URI to local storage
     */
    fun copyFromUri(uri: Uri, targetDir: File? = null): File {
        val inputStream = context.contentResolver.openInputStream(uri)
            ?: throw Exception("Unable to open input stream")
        
        val fileName = getFileName(uri) ?: generateFileName()
        val targetDirectory = targetDir ?: getReceiptsDirectory()
        val targetFile = File(targetDirectory, fileName)
        
        inputStream.use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        
        return targetFile
    }
    
    /**
     * Copy file to target location
     */
    fun copyFile(sourceFile: File, targetFile: File): File {
        FileInputStream(sourceFile).use { input ->
            FileOutputStream(targetFile).use { output ->
                input.copyTo(output)
            }
        }
        return targetFile
    }
    
    /**
     * Move file to target location
     */
    fun moveFile(sourceFile: File, targetFile: File): File {
        if (sourceFile.renameTo(targetFile)) {
            return targetFile
        } else {
            // If rename fails, copy and delete
            copyFile(sourceFile, targetFile)
            sourceFile.delete()
            return targetFile
        }
    }
    
    /**
     * Calculate file hash (SHA-256)
     */
    fun calculateFileHash(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        val hashBytes = digest.digest()
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    /**
     * Get file name from URI
     */
    private fun getFileName(uri: Uri): String? {
        var fileName: String? = null
        uri.path?.let { path ->
            val index = path.lastIndexOf('/')
            if (index != -1) {
                fileName = path.substring(index + 1)
            }
        }
        return fileName
    }
    
    /**
     * Generate unique file name
     */
    fun generateFileName(): String {
        val timestamp = System.currentTimeMillis()
        val random = (Math.random() * 1000).toInt()
        return "receipt_${timestamp}_$random.jpg"
    }
    
    /**
     * Generate file name with extension
     */
    fun generateFileName(extension: String): String {
        val timestamp = System.currentTimeMillis()
        val random = (Math.random() * 1000).toInt()
        return "receipt_${timestamp}_$random.$extension"
    }
    
    /**
     * Delete file
     */
    fun deleteFile(file: File): Boolean {
        return file.delete()
    }
    
    /**
     * Delete directory and its contents
     */
    fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            val files = directory.listFiles()
            if (files != null) {
                for (file in files) {
                    if (file.isDirectory) {
                        deleteDirectory(file)
                    } else {
                        file.delete()
                    }
                }
            }
        }
        return directory.delete()
    }
    
    /**
     * Get file size in human readable format
     */
    fun getFormattedFileSize(bytes: Long): String {
        val kb = bytes / 1024.0
        val mb = kb / 1024.0
        val gb = mb / 1024.0
        
        return when {
            gb >= 1 -> String.format("%.2f GB", gb)
            mb >= 1 -> String.format("%.2f MB", mb)
            kb >= 1 -> String.format("%.2f KB", kb)
            else -> "$bytes B"
        }
    }
    
    /**
     * Check if file exists
     */
    fun fileExists(filePath: String): Boolean {
        return File(filePath).exists()
    }
    
    /**
     * Get file by path
     */
    fun getFile(filePath: String): File {
        return File(filePath)
    }
    
    /**
     * Get file extension
     */
    fun getFileExtension(fileName: String): String {
        return fileName.substringAfterLast('.', "").lowercase()
    }
    
    /**
     * Validate file type
     */
    fun isValidFileType(fileName: String): Boolean {
        val extension = getFileExtension(fileName)
        return extension in listOf("jpg", "jpeg", "png", "tiff", "tif", "pdf")
    }
    
    /**
     * Create temp file
     */
    fun createTempFile(prefix: String = "billmii_", suffix: String = ".tmp"): File {
        return File.createTempFile(prefix, suffix, getTempDirectory())
    }
    
    /**
     * Clean up temp directory
     */
    fun cleanupTempDirectory() {
        val tempDir = getTempDirectory()
        if (tempDir.exists()) {
            val files = tempDir.listFiles()
            files?.forEach { file ->
                // Delete files older than 1 hour
                if (System.currentTimeMillis() - file.lastModified() > 3600000) {
                    file.delete()
                }
            }
        }
    }
    
    /**
     * Get total storage size
     */
    fun getTotalStorageSize(): Long {
        return getBaseDirectory().walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
    
    /**
     * Get storage size by directory
     */
    fun getStorageSizeByDirectory(directory: File): Long {
        return directory.walkTopDown()
            .filter { it.isFile }
            .map { it.length() }
            .sum()
    }
}