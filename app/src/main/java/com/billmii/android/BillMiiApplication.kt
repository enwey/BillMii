package com.billmii.android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import com.billmii.android.data.database.BillMiiDatabase

/**
 * BillMii Application class
 * Initializes application components and services
 */
class BillMiiApplication : Application() {
    
    companion object {
        lateinit var instance: BillMiiApplication
            private set
        
        lateinit var database: BillMiiDatabase
            private set
        
        const val NOTIFICATION_CHANNEL_ID = "billmii_notification_channel"
        const val NOTIFICATION_CHANNEL_NAME = "BillMii Notifications"
        const val NOTIFICATION_CHANNEL_DESCRIPTION = "Notifications for BillMii operations"
    }
    
    override fun onCreate() {
        super.onCreate()
        instance = this
        
        // Initialize database
        initializeDatabase()
        
        // Create notification channels
        createNotificationChannels()
        
        // Initialize other components
        initializeComponents()
    }
    
    /**
     * Initialize encrypted database
     */
    private fun initializeDatabase() {
        database = BillMiiDatabase.getDatabase(this)
    }
    
    /**
     * Create notification channels for Android O and above
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                NOTIFICATION_CHANNEL_ID,
                NOTIFICATION_CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = NOTIFICATION_CHANNEL_DESCRIPTION
                enableVibration(true)
                setShowBadge(true)
            }
            
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    /**
     * Initialize application components
     */
    private fun initializeComponents() {
        // Initialize preferences
        // Initialize security manager
        // Initialize OCR engine (lazy initialization)
        // Initialize backup manager
    }
}