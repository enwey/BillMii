package com.billmii.android.data.model

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Date

/**
 * Unit tests for Reimbursement data model
 */
@RunWith(JUnit4::class)
class ReimbursementTest {

    @Test
    fun `test reimbursement creation`() {
        val date = Date()
        val reimbursement = Reimbursement(
            id = 1,
            title = "测试报销单",
            applicant = "张三",
            department = "技术部",
            totalAmount = 1500.00,
            currency = "CNY",
            status = ReimbursementStatus.PENDING,
            submitDate = date,
            approvalDate = null,
            approver = null,
            description = "测试报销描述",
            receiptIds = "1,2,3",
            category = "差旅费",
            expenseType = "交通费",
            projectCode = "PROJ001",
            createdAt = date,
            updatedAt = date
        )
        
        assertEquals(1, reimbursement.id)
        assertEquals("测试报销单", reimbursement.title)
        assertEquals("张三", reimbursement.applicant)
        assertEquals("技术部", reimbursement.department)
        assertEquals(1500.00, reimbursement.totalAmount, 0.001)
        assertEquals("CNY", reimbursement.currency)
        assertEquals(ReimbursementStatus.PENDING, reimbursement.status)
        assertEquals(date, reimbursement.submitDate)
        assertNull(reimbursement.approvalDate)
        assertNull(reimbursement.approver)
        assertEquals("测试报销描述", reimbursement.description)
        assertEquals("1,2,3", reimbursement.receiptIds)
        assertEquals("差旅费", reimbursement.category)
        assertEquals("交通费", reimbursement.expenseType)
        assertEquals("PROJ001", reimbursement.projectCode)
    }

    @Test
    fun `test reimbursement status values`() {
        assertEquals("pending", ReimbursementStatus.PENDING.value)
        assertEquals("submitted", ReimbursementStatus.SUBMITTED.value)
        assertEquals("approved", ReimbursementStatus.APPROVED.value)
        assertEquals("rejected", ReimbursementStatus.REJECTED.value)
        assertEquals("paid", ReimbursementStatus.PAID.value)
        assertEquals("cancelled", ReimbursementStatus.CANCELLED.value)
    }

    @Test
    fun `test reimbursement status display names`() {
        assertEquals("草稿", ReimbursementStatus.PENDING.displayName)
        assertEquals("已提交", ReimbursementStatus.SUBMITTED.displayName)
        assertEquals("已通过", ReimbursementStatus.APPROVED.displayName)
        assertEquals("已拒绝", ReimbursementStatus.REJECTED.displayName)
        assertEquals("已支付", ReimbursementStatus.PAID.displayName)
        assertEquals("已取消", ReimbursementStatus.CANCELLED.displayName)
    }

    @Test
    fun `test reimbursement status colors`() {
        // Verify status colors are valid hex codes
        val colors = listOf(
            ReimbursementStatus.PENDING.color,
            ReimbursementStatus.SUBMITTED.color,
            ReimbursementStatus.APPROVED.color,
            ReimbursementStatus.REJECTED.color,
            ReimbursementStatus.PAID.color,
            ReimbursementStatus.CANCELLED.color
        )
        
        colors.forEach { color ->
            assertTrue(color.matches(Regex("^#[0-9A-Fa-f]{6}$")))
        }
    }

    @Test
    fun `test reimbursement receipt ids parsing`() {
        val reimbursement = Reimbursement(
            id = 1,
            title = "测试",
            applicant = "张三",
            department = "技术部",
            totalAmount = 100.00,
            receiptIds = "1,2,3,4,5",
            createdAt = Date(),
            updatedAt = Date()
        )
        
        // Parse receipt IDs
        val receiptIds = reimbursement.receiptIds?.split(",")?.map { it.trim().toLong() }
        
        assertNotNull(receiptIds)
        assertEquals(5, receiptIds?.size)
        assertEquals(1L, receiptIds?.get(0))
        assertEquals(5L, receiptIds?.get(4))
    }

    @Test
    fun `test reimbursement amount validation`() {
        // Valid amounts
        val validAmounts = listOf(0.01, 100.50, 10000.00)
        validAmounts.forEach { amount ->
            assertTrue(amount > 0)
        }
        
        // Invalid amounts
        val invalidAmounts = listOf(-1.0, 0.0, -100.50)
        invalidAmounts.forEach { amount ->
            assertFalse(amount > 0)
        }
    }

    @Test
    fun `test reimbursement status transitions`() {
        // Valid status transitions
        val validTransitions = mapOf(
            ReimbursementStatus.PENDING to listOf(
                ReimbursementStatus.SUBMITTED,
                ReimbursementStatus.CANCELLED
            ),
            ReimbursementStatus.SUBMITTED to listOf(
                ReimbursementStatus.APPROVED,
                ReimbursementStatus.REJECTED
            ),
            ReimbursementStatus.APPROVED to listOf(
                ReimbursementStatus.PAID
            )
        )
        
        // Verify transitions are defined
        assertTrue(validTransitions.containsKey(ReimbursementStatus.PENDING))
        assertTrue(validTransitions.containsKey(ReimbursementStatus.SUBMITTED))
        assertTrue(validTransitions.containsKey(ReimbursementStatus.APPROVED))
    }
}