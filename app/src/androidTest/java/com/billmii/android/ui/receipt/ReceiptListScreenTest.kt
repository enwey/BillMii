package com.billmii.android.ui.receipt

import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.ReceiptStatus
import com.billmii.android.data.model.ReceiptType
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/**
 * UI tests for ReceiptListScreen
 * These tests verify the UI components and user interactions
 */
@RunWith(AndroidJUnit4::class)
class ReceiptListScreenTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val testReceipts = listOf(
        Receipt(
            id = 1,
            title = "餐饮发票",
            merchantName = "测试餐厅",
            amount = 100.50,
            currency = "CNY",
            date = Date(),
            category = "餐饮",
            type = ReceiptType.INVOICE,
            status = ReceiptStatus.PENDING,
            imagePath = "/path/to/image1.jpg",
            createdAt = Date(),
            updatedAt = Date()
        ),
        Receipt(
            id = 2,
            title = "交通收据",
            merchantName = "出租车公司",
            amount = 50.00,
            currency = "CNY",
            date = Date(),
            category = "交通",
            type = ReceiptType.RECEIPT,
            status = ReceiptStatus.APPROVED,
            imagePath = "/path/to/image2.jpg",
            createdAt = Date(),
            updatedAt = Date()
        )
    )

    private val receiptsFlow = MutableStateFlow(testReceipts)
    private val loadingFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    @Before
    fun setup() {
        composeTestRule.setContent {
            ReceiptListScreen(
                receipts = receiptsFlow.asStateFlow(),
                isLoading = loadingFlow.asStateFlow(),
                error = errorFlow.asStateFlow(),
                onReceiptClick = {},
                onAddReceipt = {},
                onSearchClick = {},
                onFilterClick = {},
                onDeleteReceipt = {},
                onBatchDelete = {},
                onBatchExport = {},
                onQrScanClick = {}
            )
        }
    }

    @Test
    fun `test receipt list displays correctly`() {
        // Verify receipt items are displayed
        composeTestRule.onNodeWithText("餐饮发票").assertIsDisplayed()
        composeTestRule.onNodeWithText("交通收据").assertIsDisplayed()
        composeTestRule.onNodeWithText("¥100.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("¥50.00").assertIsDisplayed()
    }

    @Test
    fun `test receipt categories are displayed`() {
        // Verify categories are shown
        composeTestRule.onNodeWithText("餐饮").assertIsDisplayed()
        composeTestRule.onNodeWithText("交通").assertIsDisplayed()
    }

    @Test
    fun `test loading state displays`() {
        loadingFlow.value = true
        
        // Verify loading indicator is shown
        composeTestRule.onNode(hasTestTag("loading_indicator"))
            .assertIsDisplayed()
    }

    @Test
    fun `test error state displays`() {
        errorFlow.value = "加载失败"
        
        // Verify error message is shown
        composeTestRule.onNodeWithText("加载失败").assertIsDisplayed()
    }

    @Test
    fun `test empty state displays`() {
        receiptsFlow.value = emptyList()
        
        // Verify empty state is shown
        composeTestRule.onNodeWithText("暂无票据").assertIsDisplayed()
    }

    @Test
    fun `test search button is displayed`() {
        composeTestRule.onNodeWithContentDescription("搜索").assertIsDisplayed()
    }

    @Test
    fun `test filter button is displayed`() {
        composeTestRule.onNodeWithContentDescription("筛选").assertIsDisplayed()
    }

    @Test
    fun `test add receipt FAB is displayed`() {
        composeTestRule.onNodeWithContentDescription("添加票据").assertIsDisplayed()
    }

    @Test
    fun `test receipt item click`() {
        var clickedReceipt: Receipt? = null
        
        composeTestRule.setContent {
            ReceiptListScreen(
                receipts = receiptsFlow.asStateFlow(),
                isLoading = loadingFlow.asStateFlow(),
                error = errorFlow.asStateFlow(),
                onReceiptClick = { receipt -> clickedReceipt = receipt },
                onAddReceipt = {},
                onSearchClick = {},
                onFilterClick = {},
                onDeleteReceipt = {},
                onBatchDelete = {},
                onBatchExport = {},
                onQrScanClick = {}
            )
        }
        
        // Click on first receipt
        composeTestRule.onNodeWithText("餐饮发票").performClick()
        
        // Verify callback was invoked
        assertEquals(1, clickedReceipt?.id)
    }

    @Test
    fun `test add receipt button click`() {
        var addClicked = false
        
        composeTestRule.setContent {
            ReceiptListScreen(
                receipts = receiptsFlow.asStateFlow(),
                isLoading = loadingFlow.asStateFlow(),
                error = errorFlow.asStateFlow(),
                onReceiptClick = {},
                onAddReceipt = { addClicked = true },
                onSearchClick = {},
                onFilterClick = {},
                onDeleteReceipt = {},
                onBatchDelete = {},
                onBatchExport = {},
                onQrScanClick = {}
            )
        }
        
        // Click on add button
        composeTestRule.onNodeWithContentDescription("添加票据").performClick()
        
        // Verify callback was invoked
        assertTrue(addClicked)
    }

    @Test
    fun `test search button click`() {
        var searchClicked = false
        
        composeTestRule.setContent {
            ReceiptListScreen(
                receipts = receiptsFlow.asStateFlow(),
                isLoading = loadingFlow.asStateFlow(),
                error = errorFlow.asStateFlow(),
                onReceiptClick = {},
                onAddReceipt = {},
                onSearchClick = { searchClicked = true },
                onFilterClick = {},
                onDeleteReceipt = {},
                onBatchDelete = {},
                onBatchExport = {},
                onQrScanClick = {}
            )
        }
        
        // Click on search button
        composeTestRule.onNodeWithContentDescription("搜索").performClick()
        
        // Verify callback was invoked
        assertTrue(searchClicked)
    }

    @Test
    fun `test filter button click`() {
        var filterClicked = false
        
        composeTestRule.setContent {
            ReceiptListScreen(
                receipts = receiptsFlow.asStateFlow(),
                isLoading = loadingFlow.asStateFlow(),
                error = errorFlow.asStateFlow(),
                onReceiptClick = {},
                onAddReceipt = {},
                onSearchClick = {},
                onFilterClick = { filterClicked = true },
                onDeleteReceipt = {},
                onBatchDelete = {},
                onBatchExport = {},
                onQrScanClick = {}
            )
        }
        
        // Click on filter button
        composeTestRule.onNodeWithContentDescription("筛选").performClick()
        
        // Verify callback was invoked
        assertTrue(filterClicked)
    }
}