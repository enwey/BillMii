package com.billmii.android.data.database

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.ReceiptStatus
import com.billmii.android.data.model.ReceiptType
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * Integration tests for ReceiptDao
 * These tests require Android instrumentation to run
 */
@RunWith(AndroidJUnit4::class)
class ReceiptDaoIntegrationTest {

    private lateinit var database: AppDatabase
    private lateinit var receiptDao: ReceiptDao

    @Before
    fun setup() {
        // Create in-memory database for testing
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AppDatabase::class.java
        ).build()
        receiptDao = database.receiptDao()
    }

    @After
    fun teardown() {
        database.close()
    }

    @Test
    fun `test insert and retrieve receipt`() = runTest {
        val receipt = Receipt(
            id = 1,
            title = "测试票据",
            merchantName = "测试商户",
            amount = 100.50,
            currency = "CNY",
            date = Date(),
            category = "餐饮",
            type = ReceiptType.INVOICE,
            status = ReceiptStatus.PENDING,
            imagePath = "/path/to/image.jpg",
            createdAt = Date(),
            updatedAt = Date()
        )

        receiptDao.insert(receipt)

        val retrieved = receiptDao.getById(1)
        assertNotNull(retrieved)
        assertEquals("测试票据", retrieved?.title)
        assertEquals(100.50, retrieved?.amount, 0.001)
    }

    @Test
    fun `test update receipt`() = runTest {
        val receipt = Receipt(
            id = 1,
            title = "原始标题",
            amount = 100.00,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        receiptDao.insert(receipt)

        val updated = receipt.copy(
            title = "更新标题",
            amount = 200.00
        )

        receiptDao.update(updated)

        val retrieved = receiptDao.getById(1)
        assertEquals("更新标题", retrieved?.title)
        assertEquals(200.00, retrieved?.amount, 0.001)
    }

    @Test
    fun `test delete receipt`() = runTest {
        val receipt = Receipt(
            id = 1,
            title = "测试票据",
            amount = 100.00,
            date = Date(),
            createdAt = Date(),
            updatedAt = Date()
        )

        receiptDao.insert(receipt)
        assertNotNull(receiptDao.getById(1))

        receiptDao.delete(receipt)
        assertNull(receiptDao.getById(1))
    }

    @Test
    fun `test get all receipts`() = runTest {
        val receipts = listOf(
            Receipt(
                id = 1,
                title = "票据1",
                amount = 100.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 2,
                title = "票据2",
                amount = 200.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 3,
                title = "票据3",
                amount = 300.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        receipts.forEach { receiptDao.insert(it) }

        val allReceipts = receiptDao.getAll().first()
        assertEquals(3, allReceipts.size)
    }

    @Test
    fun `test search receipts by title`() = runTest {
        val receipts = listOf(
            Receipt(
                id = 1,
                title = "餐饮发票",
                amount = 100.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 2,
                title = "交通收据",
                amount = 200.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 3,
                title = "餐饮发票2",
                amount = 150.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        receipts.forEach { receiptDao.insert(it) }

        val searchResults = receiptDao.searchByTitle("餐饮").first()
        assertEquals(2, searchResults.size)
        assertTrue(searchResults.all { it.title.contains("餐饮") })
    }

    @Test
    fun `test filter receipts by category`() = runTest {
        val receipts = listOf(
            Receipt(
                id = 1,
                title = "票据1",
                category = "餐饮",
                amount = 100.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 2,
                title = "票据2",
                category = "交通",
                amount = 200.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 3,
                title = "票据3",
                category = "餐饮",
                amount = 150.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        receipts.forEach { receiptDao.insert(it) }

        val filtered = receiptDao.getByCategory("餐饮").first()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.category == "餐饮" })
    }

    @Test
    fun `test filter receipts by status`() = runTest {
        val receipts = listOf(
            Receipt(
                id = 1,
                title = "票据1",
                status = ReceiptStatus.PENDING,
                amount = 100.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 2,
                title = "票据2",
                status = ReceiptStatus.APPROVED,
                amount = 200.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 3,
                title = "票据3",
                status = ReceiptStatus.PENDING,
                amount = 150.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        receipts.forEach { receiptDao.insert(it) }

        val filtered = receiptDao.getByStatus(ReceiptStatus.PENDING).first()
        assertEquals(2, filtered.size)
        assertTrue(filtered.all { it.status == ReceiptStatus.PENDING })
    }

    @Test
    fun `test get receipts by date range`() = runTest {
        val baseDate = Date()
        val receipts = listOf(
            Receipt(
                id = 1,
                title = "票据1",
                date = Date(baseDate.time - 86400000 * 5), // 5 days ago
                amount = 100.00,
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 2,
                title = "票据2",
                date = Date(baseDate.time - 86400000 * 2), // 2 days ago
                amount = 200.00,
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 3,
                title = "票据3",
                date = Date(baseDate.time - 86400000 * 1), // 1 day ago
                amount = 150.00,
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        receipts.forEach { receiptDao.insert(it) }

        val startDate = Date(baseDate.time - 86400000 * 3) // 3 days ago
        val endDate = Date(baseDate.time) // today

        val filtered = receiptDao.getByDateRange(startDate, endDate).first()
        assertEquals(2, filtered.size)
    }

    @Test
    fun `test get total amount by category`() = runTest {
        val receipts = listOf(
            Receipt(
                id = 1,
                title = "票据1",
                category = "餐饮",
                amount = 100.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 2,
                title = "票据2",
                category = "餐饮",
                amount = 200.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            ),
            Receipt(
                id = 3,
                title = "票据3",
                category = "交通",
                amount = 150.00,
                date = Date(),
                createdAt = Date(),
                updatedAt = Date()
            )
        )

        receipts.forEach { receiptDao.insert(it) }

        val total = receiptDao.getTotalAmountByCategory("餐饮")
        assertEquals(300.00, total, 0.001)
    }
}