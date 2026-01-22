package com.billmii.android.data.service

import android.content.Context
import android.net.Uri
import com.billmii.android.data.database.dao.ReceiptDao
import com.billmii.android.data.database.dao.ReimbursementDao
import com.billmii.android.data.model.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.poi.ss.usermodel.*
import org.apache.poi.ss.util.CellRangeAddress
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Export Service
 * Handles data export to Excel and PDF formats
 */
@Singleton
class ExportService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receiptDao: ReceiptDao,
    private val reimbursementDao: ReimbursementDao
) {
    
    companion object {
        private const val TAG = "ExportService"
        private const val DATE_FORMAT = "yyyy-MM-dd HH:mm:ss"
        private const val EXCEL_EXTENSION = ".xlsx"
    }
    
    /**
     * Export receipts to Excel
     */
    suspend fun exportReceiptsToExcel(
        receiptIds: List<Long>,
        outputFile: File
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("票据明细")
            
            // Create header style
            val headerStyle = createHeaderStyle(workbook)
            val dataStyle = createDataStyle(workbook)
            
            // Create headers
            val headers = listOf(
                "序号", "归档编号", "票据类型", "票据分类", "发票代码", "发票号码",
                "开票日期", "购买方", "销售方", "总金额", "不含税金额", "税额",
                "税率", "报销单ID", "创建时间", "备注"
            )
            
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            // Get receipts
            val receipts = receiptIds.mapNotNull { receiptDao.getById(it) }
            
            // Write data
            receipts.forEachIndexed { rowIndex, receipt ->
                val row = sheet.createRow(rowIndex + 1)
                
                row.createCell(0).setCellValue(rowIndex + 1)
                row.createCell(1).setCellValue(receipt.archiveNumber ?: "")
                row.createCell(2).setCellValue(receipt.receiptType.displayName)
                row.createCell(3).setCellValue(receipt.receiptCategory.displayName)
                row.createCell(4).setCellValue(receipt.invoiceCode ?: "")
                row.createCell(5).setCellValue(receipt.invoiceNumber ?: "")
                row.createCell(6).setCellValue(formatDate(receipt.invoiceDate))
                row.createCell(7).setCellValue(receipt.buyerName ?: "")
                row.createCell(8).setCellValue(receipt.sellerName ?: "")
                row.createCell(9).setCellValue(receipt.totalAmount ?: 0.0)
                row.createCell(10).setCellValue(receipt.amountWithoutTax ?: 0.0)
                row.createCell(11).setCellValue(receipt.taxAmount ?: 0.0)
                row.createCell(12).setCellValue(receipt.taxRate ?: 0.0)
                row.createCell(13).setCellValue(receipt.reimbursementId ?: 0)
                row.createCell(14).setCellValue(formatDate(receipt.createdAt))
                row.createCell(15).setCellValue(receipt.remarks ?: "")
                
                // Apply data style
                for (i in 0..15) {
                    row.getCell(i).cellStyle = dataStyle
                }
            }
            
            // Auto-size columns
            for (i in 0..15) {
                sheet.autoSizeColumn(i)
            }
            
            // Write to file
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            
            Result.success(Uri.fromFile(outputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export reimbursements to Excel
     */
    suspend fun exportReimbursementsToExcel(
        reimbursementIds: List<Long>,
        outputFile: File
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("报销单")
            
            // Create styles
            val headerStyle = createHeaderStyle(workbook)
            val dataStyle = createDataStyle(workbook)
            
            // Create headers
            val headers = listOf(
                "序号", "报销单号", "标题", "申请人", "部门", "项目", "预算代码",
                "总金额", "不含税金额", "税额", "票据数量", "状态", "提交时间",
                "审批时间", "审批人", "备注"
            )
            
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            // Get reimbursements
            val reimbursements = reimbursementIds.mapNotNull { reimbursementDao.getById(it) }
            
            // Write data
            reimbursements.forEachIndexed { rowIndex, reimbursement ->
                val row = sheet.createRow(rowIndex + 1)
                
                row.createCell(0).setCellValue(rowIndex + 1)
                row.createCell(1).setCellValue(reimbursement.id)
                row.createCell(2).setCellValue(reimbursement.title)
                row.createCell(3).setCellValue(reimbursement.applicant)
                row.createCell(4).setCellValue(reimbursement.department ?: "")
                row.createCell(5).setCellValue(reimbursement.project ?: "")
                row.createCell(6).setCellValue(reimbursement.budgetCode ?: "")
                row.createCell(7).setCellValue(reimbursement.totalAmount ?: 0.0)
                row.createCell(8).setCellValue(reimbursement.amountWithoutTax ?: 0.0)
                row.createCell(9).setCellValue(reimbursement.taxAmount ?: 0.0)
                row.createCell(10).setCellValue(reimbursement.receiptCount)
                row.createCell(11).setCellValue(reimbursement.status.displayName)
                row.createCell(12).setCellValue(formatDate(reimbursement.submittedAt))
                row.createCell(13).setCellValue(formatDate(reimbursement.approvedAt))
                row.createCell(14).setCellValue(reimbursement.currentApprover ?: "")
                row.createCell(15).setCellValue(reimbursement.description ?: "")
                
                // Apply data style
                for (i in 0..15) {
                    row.getCell(i).cellStyle = dataStyle
                }
            }
            
            // Auto-size columns
            for (i in 0..15) {
                sheet.autoSizeColumn(i)
            }
            
            // Write to file
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            
            Result.success(Uri.fromFile(outputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export to Kingdee format
     */
    suspend fun exportToKingdeeFormat(
        reimbursementIds: List<Long>,
        outputFile: File
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("金蝶导入数据")
            
            val headerStyle = createHeaderStyle(workbook)
            val dataStyle = createDataStyle(workbook)
            
            // Kingdee specific headers
            val headers = listOf(
                "单据编号", "业务日期", "报销人", "部门", "项目", "费用类别",
                "金额", "币别", "摘要", "凭证字号", "核算项目"
            )
            
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            val reimbursements = reimbursementIds.mapNotNull { reimbursementDao.getById(it) }
            
            reimbursements.forEachIndexed { rowIndex, reimbursement ->
                val row = sheet.createRow(rowIndex + 1)
                
                row.createCell(0).setCellValue("") // 单据编号
                row.createCell(1).setCellValue(formatDate(reimbursement.createdAt))
                row.createCell(2).setCellValue(reimbursement.applicant)
                row.createCell(3).setCellValue(reimbursement.department ?: "")
                row.createCell(4).setCellValue(reimbursement.project ?: "")
                row.createCell(5).setCellValue(getExpenseCategory(reimbursement))
                row.createCell(6).setCellValue(reimbursement.totalAmount ?: 0.0)
                row.createCell(7).setCellValue("CNY")
                row.createCell(8).setCellValue(reimbursement.description ?: "")
                row.createCell(9).setCellValue("") // 凭证字号
                row.createCell(10).setCellValue("") // 核算项目
                
                for (i in 0..10) {
                    row.getCell(i).cellStyle = dataStyle
                }
            }
            
            for (i in 0..10) {
                sheet.autoSizeColumn(i)
            }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            
            Result.success(Uri.fromFile(outputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Export to Yonyou format
     */
    suspend fun exportToYonyouFormat(
        reimbursementIds: List<Long>,
        outputFile: File
    ): Result<Uri> = withContext(Dispatchers.IO) {
        try {
            val workbook = XSSFWorkbook()
            val sheet = workbook.createSheet("用友导入数据")
            
            val headerStyle = createHeaderStyle(workbook)
            val dataStyle = createDataStyle(workbook)
            
            // Yonyou specific headers
            val headers = listOf(
                "单据类型", "单据日期", "报销人", "部门", "项目", "费用项目",
                "原币金额", "币种", "汇率", "本币金额", "摘要", "附件张数"
            )
            
            val headerRow = sheet.createRow(0)
            headers.forEachIndexed { index, header ->
                val cell = headerRow.createCell(index)
                cell.setCellValue(header)
                cell.cellStyle = headerStyle
            }
            
            val reimbursements = reimbursementIds.mapNotNull { reimbursementDao.getById(it) }
            
            reimbursements.forEachIndexed { rowIndex, reimbursement ->
                val row = sheet.createRow(rowIndex + 1)
                
                row.createCell(0).setCellValue("费用报销")
                row.createCell(1).setCellValue(formatDate(reimbursement.createdAt))
                row.createCell(2).setCellValue(reimbursement.applicant)
                row.createCell(3).setCellValue(reimbursement.department ?: "")
                row.createCell(4).setCellValue(reimbursement.project ?: "")
                row.createCell(5).setCellValue(getExpenseCategory(reimbursement))
                row.createCell(6).setCellValue(reimbursement.totalAmount ?: 0.0)
                row.createCell(7).setCellValue("CNY")
                row.createCell(8).setCellValue(1.0)
                row.createCell(9).setCellValue(reimbursement.totalAmount ?: 0.0)
                row.createCell(10).setCellValue(reimbursement.description ?: "")
                row.createCell(11).setCellValue(reimbursement.receiptCount)
                
                for (i in 0..11) {
                    row.getCell(i).cellStyle = dataStyle
                }
            }
            
            for (i in 0..11) {
                sheet.autoSizeColumn(i)
            }
            
            FileOutputStream(outputFile).use { workbook.write(it) }
            workbook.close()
            
            Result.success(Uri.fromFile(outputFile))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Create header style
     */
    private fun createHeaderStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        style.fillPattern = FillPatternType.SOLID_FOREGROUND
        style.alignment = HorizontalAlignment.CENTER
        style.verticalAlignment = VerticalAlignment.CENTER
        
        val font = workbook.createFont()
        font.bold = true
        font.fontHeightInPoints = 11
        style.setFont(font)
        
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        
        return style
    }
    
    /**
     * Create data style
     */
    private fun createDataStyle(workbook: Workbook): CellStyle {
        val style = workbook.createCellStyle()
        style.alignment = HorizontalAlignment.LEFT
        style.verticalAlignment = VerticalAlignment.CENTER
        
        val font = workbook.createFont()
        font.fontHeightInPoints = 10
        style.setFont(font)
        
        style.borderTop = BorderStyle.THIN
        style.borderBottom = BorderStyle.THIN
        style.borderLeft = BorderStyle.THIN
        style.borderRight = BorderStyle.THIN
        
        // Number format for amount columns
        val amountFormat = workbook.createDataFormat().getFormat("#,##0.00")
        style.dataFormat = amountFormat
        
        return style
    }
    
    /**
     * Format date
     */
    private fun formatDate(date: Date?): String {
        if (date == null) return ""
        val formatter = SimpleDateFormat(DATE_FORMAT, Locale.getDefault())
        return formatter.format(date)
    }
    
    /**
     * Get expense category from reimbursement
     */
    private fun getExpenseCategory(reimbursement: Reimbursement): String {
        // Get receipts for this reimbursement
        val receipts = reimbursementDao.getReceiptsForReimbursement(reimbursement.id)
        
        // Determine category based on first receipt
        return receipts.firstOrNull()?.expenseSubCategory?.displayName ?: "其他费用"
    }
}

/**
 * Export result
 */
data class ExportResult(
    val success: Boolean,
    val fileUri: Uri? = null,
    val recordCount: Int = 0,
    val error: String? = null
)