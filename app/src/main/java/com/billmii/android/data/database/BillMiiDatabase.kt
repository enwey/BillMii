package com.billmii.android.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.billmii.android.data.model.ClassificationRule
import com.billmii.android.data.model.OperationLog
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.database.converter.Converters
import net.sqlcipher.database.SQLiteDatabase
import net.sqlcipher.database.SupportFactory

/**
 * BillMii Database - Encrypted Room Database
 * Encrypted database with SQLCipher for secure storage
 */
@Database(
    entities = [
        Receipt::class,
        Reimbursement::class,
        ClassificationRule::class,
        OperationLog::class
    ],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class BillMiiDatabase : RoomDatabase() {
    
    abstract fun receiptDao(): ReceiptDao
    abstract fun reimbursementDao(): ReimbursementDao
    abstract fun classificationRuleDao(): ClassificationRuleDao
    abstract fun operationLogDao(): OperationLogDao
    
    companion object {
        private const val DATABASE_NAME = "billmii.db"
        private const val DATABASE_PASSPHRASE = "BillMiiSecureKey2024!" // In production, use secure storage
        
        @Volatile
        private var INSTANCE: BillMiiDatabase? = null
        
        /**
         * Get database instance with encryption
         */
        fun getDatabase(context: Context): BillMiiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = createDatabase(context)
                INSTANCE = instance
                instance
            }
        }
        
        /**
         * Create encrypted database instance
         */
        private fun createDatabase(context: Context): BillMiiDatabase {
            // Prepare SQLCipher passphrase
            val passphrase: ByteArray = SQLiteDatabase.getBytes(DATABASE_PASSPHRASE.toCharArray())
            val factory = SupportFactory(passphrase)
            
            return Room.databaseBuilder(
                context.applicationContext,
                BillMiiDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .addMigrations(MIGRATION_1_2)
                .fallbackToDestructiveMigration() // Only for development
                .build()
        }
        
        /**
         * Migration from version 1 to 2
         */
        private val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(database: SupportSQLiteDatabase) {
                // Add new columns or tables here
                // Example:
                // database.execSQL("ALTER TABLE receipts ADD COLUMN new_column TEXT")
            }
        }
        
        /**
         * Change database passphrase (for password change functionality)
         */
        fun changePassphrase(context: Context, newPassphrase: String) {
            // Close existing database
            INSTANCE?.close()
            INSTANCE = null
            
            // Update passphrase and recreate
            val passphrase: ByteArray = SQLiteDatabase.getBytes(newPassphrase.toCharArray())
            val factory = SupportFactory(passphrase)
            
            Room.databaseBuilder(
                context.applicationContext,
                BillMiiDatabase::class.java,
                DATABASE_NAME
            )
                .openHelperFactory(factory)
                .build()
        }
    }
}