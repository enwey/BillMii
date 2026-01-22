package com.billmii.android.data.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Date

/**
 * Unit tests for Receipt data model
 */
@RunWith(JUnit4::class)
class ReceiptTest {

    @Test
    fun `test receipt creation`() {
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
            thumbnailPath = "/path/to/thumbnail.jpg",
            notes = "测试备注",
            tags = "测试,票据",
            ocrText = "OCR识别文本",
            confidenceScore = 0.95f,
            createdAt = Date(),
            updatedAt = Date()
        )
        
        assertEquals(1, receipt.id)
        assertEquals("测试票据", receipt.title)
        assertEquals("测试商户", receipt.merchantName)
        assertEquals(100.50, receipt.amount, 0.001)
        assertEquals("CNY", receipt.currency)
        assertEquals("餐饮", receipt.category)
        assertEquals(ReceiptType.INVOICE, receipt.type)
        assertEquals(ReceiptStatus.PENDING, receipt.status)
        assertEquals("/path/to/image.jpg", receipt.imagePath)
        assertEquals("/path/to/thumbnail.jpg", receipt.thumbnailPath)
        assertEquals("测试备注", receipt.notes)
        assertEquals("测试,票据", receipt.tags)
        assertEquals("OCR识别文本", receipt.ocrText)
        assertEquals(0.95f, receipt.confidenceScore, 0.001f)
    }

    @Test
    fun `test receipt type values`() {
        assertEquals("invoice", ReceiptType.INVOICE.value)
        assertEquals("receipt", ReceiptType.RECEIPT.value)
        assertEquals("voucher", ReceiptType.VOUCHER.value)
        assertEquals("contract", ReceiptType.CONTRACT.value)
        assertEquals("other", ReceiptType.OTHER.value)
    }

    @Test
    fun `test receipt type display names`() {
        assertEquals("发票", ReceiptType.INVOICE.displayName)
        assertEquals("收据", ReceiptType.RECEIPT.displayName)
        assertEquals("凭证", ReceiptType.VOUCHER.displayName)
        assertEquals("合同", ReceiptType.CONTRACT.displayName)
        assertEquals("其他", ReceiptType.OTHER.displayName)
    }

    @Test
    fun `test receipt status values`() {
        assertEquals("pending", ReceiptStatus.PENDING.value)
        assertEquals("processing", ReceiptStatus.PROCESSING.value)
        assertEquals("classified", ReceiptStatus.CLASSIFIED.value)
        assertEquals("approved", ReceiptStatus.APPROVED.value)
        assertEquals("rejected", ReceiptStatus.REJECTED.value)
    }

    @Test
    fun `test receipt status display names`() {
        assertEquals("待处理", ReceiptStatus.PENDING.displayName)
        assertEquals("处理中", ReceiptStatus.PROCESSING.displayName)
        assertEquals("已分类", ReceiptStatus.CLASSIFIED.displayName)
        assertEquals("已通过", ReceiptStatus.APPROVED.displayName)
        assertEquals("已拒绝", ReceiptStatus.REJECTED.displayName)
    }

    @Test
    fun `test receipt equality`() {
        val date = Date()
        val receipt1 = Receipt(
            id = 1,
            title = "测试票据",
            amount = 100.50,
            date = date,
            createdAt = date,
            updatedAt = date
        )
        
        val receipt2 = Receipt(
            id = 1,
            title = "测试票据",
            amount = 100.50,
            date = date,
            createdAt = date,
            updatedAt = date
        )
        
        val receipt3 = Receipt(
            id = 2,
            title = "测试票据2",
            amount = 200.50,
            date = date,
            createdAt = date,
            updatedAt = date
        )
        
        // Receipts with same ID should be considered equal
        assertEquals(receipt1.id, receipt2.id)
        assertNotEquals(receipt1.id, receipt3.id)
    }

    @Test
    fun `test receipt amount validation`() {
        // Valid amounts
        val validAmounts = listOf(0.01, 100.50, 999999.99)
        validAmounts.forEach { amount ->
            assertTrue(amount > 0)
        }
        
        // Invalid amounts
        val invalidAmounts = listOf(-1.0, 0.0, -100.50)
        invalidAmounts.forEach { amount ->
            assertFalse(amount > 0)
        }
    }
}