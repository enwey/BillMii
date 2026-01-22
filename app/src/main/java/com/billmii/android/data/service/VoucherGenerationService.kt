package com.billmii.android.data.service

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.Reimbursement
import com.billmii.android.data.model.ReimbursementStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 报销凭证生成服务
 */
@Singleton
class VoucherGenerationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    companion object {
        private const val PAGE_WIDTH = 595 // A4 width in points
        private const val PAGE_HEIGHT = 842 // A4 height in points
        private const val MARGIN = 50
        private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
    }
    
    private val dateFormat = SimpleDateFormat("yyyy年MM月dd日", Locale.CHINA)
    private val dateTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.CHINA)
    
    /**
     * 生成报销凭证PDF
     */
    suspend fun generateVoucherPdf(
        reimbursement: Reimbursement,
        receipts: List<Receipt>,
        outputDir: File? = null
    ): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val pdfDocument = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas
            val paint = Paint()
            
            var yPosition = MARGIN.toFloat()
            
            // 绘制标题
            paint.textSize = 24f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("报销凭证", PAGE_WIDTH / 2f, yPosition, paint)
            yPosition += 50f
            
            // 绘制分隔线
            paint.textSize = 12f
            paint.typeface = Typeface.DEFAULT
            yPosition += 20f
            canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, paint)
            yPosition += 30f
            
            // 绘制基本信息
            paint.textAlign = Paint.Align.LEFT
            yPosition = drawBasicInfo(canvas, paint, reimbursement, yPosition)
            yPosition += 20f
            
            // 绘制分隔线
            canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, paint)
            yPosition += 30f
            
            // 绘制票据列表
            yPosition = drawReceiptsTable(canvas, paint, receipts, yPosition)
            yPosition += 20f
            
            // 绘制分隔线
            canvas.drawLine(MARGIN.toFloat(), yPosition, (PAGE_WIDTH - MARGIN).toFloat(), yPosition, paint)
            yPosition += 30f
            
            // 绘制汇总
            yPosition = drawSummary(canvas, paint, reimbursement, receipts, yPosition)
            yPosition += 30f
            
            // 绘制签名区域
            yPosition = drawSignatureArea(canvas, paint, yPosition)
            
            // 绘制底部信息
            yPosition = PAGE_HEIGHT - MARGIN.toFloat() - 40f
            drawFooter(canvas, paint, yPosition)
            
            pdfDocument.finishPage(page)
            
            // 保存PDF文件
            val fileName = "报销凭证_${reimbursement.id}_${System.currentTimeMillis()}.pdf"
            val outputDirectory = outputDir ?: getOutputDirectory()
            val outputFile = File(outputDirectory, fileName)
            
            val outputStream = FileOutputStream(outputFile)
            pdfDocument.writeTo(outputStream)
            outputStream.close()
            pdfDocument.close()
            
            Result.success(outputFile)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 绘制基本信息
     */
    private fun drawBasicInfo(
        canvas: Canvas,
        paint: Paint,
        reimbursement: Reimbursement,
        yPosition: Float
    ): Float {
        var y = yPosition
        val lineHeight = 25f
        val labelWidth = 100f
        
        paint.textAlign = Paint.Align.LEFT
        paint.textSize = 12f
        
        // 报销单号
        canvas.drawText("报销单号: ${reimbursement.id}", MARGIN.toFloat(), y, paint)
        y += lineHeight
        
        // 报销标题
        canvas.drawText("报销标题: ${reimbursement.title}", MARGIN.toFloat(), y, paint)
        y += lineHeight
        
        // 申请人
        canvas.drawText("申请人: ${reimbursement.applicant}", MARGIN.toFloat(), y, paint)
        y += lineHeight
        
        // 部门
        if (reimbursement.department.isNotEmpty()) {
            canvas.drawText("所属部门: ${reimbursement.department}", MARGIN.toFloat(), y, paint)
            y += lineHeight
        }
        
        // 项目
        if (reimbursement.project.isNotEmpty()) {
            canvas.drawText("项目名称: ${reimbursement.project}", MARGIN.toFloat(), y, paint)
            y += lineHeight
        }
        
        // 预算代码
        if (reimbursement.budgetCode.isNotEmpty()) {
            canvas.drawText("预算代码: ${reimbursement.budgetCode}", MARGIN.toFloat(), y, paint)
            y += lineHeight
        }
        
        // 创建日期
        val createdDate = parseDateTime(reimbursement.createdAt)
        canvas.drawText("创建日期: ${dateFormat.format(createdDate)}", MARGIN.toFloat(), y, paint)
        y += lineHeight
        
        // 状态
        val statusText = when (reimbursement.status) {
            ReimbursementStatus.DRAFT -> "草稿"
            ReimbursementStatus.PENDING -> "待审批"
            ReimbursementStatus.APPROVED -> "已通过"
            ReimbursementStatus.REJECTED -> "已拒绝"
        }
        canvas.drawText("审批状态: $statusText", MARGIN.toFloat(), y, paint)
        y += lineHeight
        
        return y
    }
    
    /**
     * 绘制票据列表表格
     */
    private fun drawReceiptsTable(
        canvas: Canvas,
        paint: Paint,
        receipts: List<Receipt>,
        yPosition: Float
    ): Float {
        var y = yPosition
        val tableStartX = MARGIN.toFloat()
        val tableWidth = CONTENT_WIDTH.toFloat()
        
        // 表头
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        
        val col1Width = tableWidth * 0.15f // 序号
        val col2Width = tableWidth * 0.20f // 类型
        val col3Width = tableWidth * 0.25f // 商户
        val col4Width = tableWidth * 0.25f // 金额
        val col5Width = tableWidth * 0.15f // 日期
        
        val headerY = y
        canvas.drawText("序号", tableStartX, y, paint)
        canvas.drawText("票据类型", tableStartX + col1Width, y, paint)
        canvas.drawText("商户名称", tableStartX + col1Width + col2Width, y, paint)
        canvas.drawText("金额(元)", tableStartX + col1Width + col2Width + col3Width, y, paint)
        canvas.drawText("日期", tableStartX + col1Width + col2Width + col3Width + col4Width, y, paint)
        
        y += 20f
        
        // 绘制表头横线
        paint.typeface = Typeface.DEFAULT
        canvas.drawLine(tableStartX, y, tableStartX + tableWidth, y, paint)
        y += 5f
        
        // 绘制票据数据
        receipts.forEachIndexed { index, receipt ->
            canvas.drawText("${index + 1}", tableStartX, y, paint)
            canvas.drawText(receipt.type.displayName, tableStartX + col1Width, y, paint)
            canvas.drawText(receipt.merchant ?: "-", tableStartX + col1Width + col2Width, y, paint)
            canvas.drawText(String.format("%.2f", receipt.amount), tableStartX + col1Width + col2Width + col3Width, y, paint)
            
            val receiptDate = receipt.date?.let { dateFormat.format(it) } ?: "-"
            canvas.drawText(receiptDate, tableStartX + col1Width + col2Width + col3Width + col4Width, y, paint)
            
            y += 20f
        }
        
        // 绘制表格底部横线
        canvas.drawLine(tableStartX, y, tableStartX + tableWidth, y, paint)
        
        return y + 10f
    }
    
    /**
     * 绘制汇总信息
     */
    private fun drawSummary(
        canvas: Canvas,
        paint: Paint,
        reimbursement: Reimbursement,
        receipts: List<Receipt>,
        yPosition: Float
    ): Float {
        var y = yPosition
        val tableStartX = MARGIN.toFloat()
        
        paint.textSize = 12f
        paint.textAlign = Paint.Align.LEFT
        
        // 票据数量
        canvas.drawText("票据数量: ${receipts.size} 张", tableStartX, y, paint)
        y += 25f
        
        // 总金额
        paint.textSize = 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("报销总金额: ¥${String.format("%.2f", reimbursement.totalAmount)}", tableStartX, y, paint)
        y += 25f
        
        // 税额（假设6%）
        val taxAmount = reimbursement.totalAmount * 0.06
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        canvas.drawText("税额(6%): ¥${String.format("%.2f", taxAmount)}", tableStartX, y, paint)
        y += 25f
        
        // 不含税金额
        val amountWithoutTax = reimbursement.totalAmount - taxAmount
        canvas.drawText("不含税金额: ¥${String.format("%.2f", amountWithoutTax)}", tableStartX, y, paint)
        y += 25f
        
        // 大写金额
        val uppercaseAmount = convertToUppercase(reimbursement.totalAmount)
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("大写金额: $uppercaseAmount", tableStartX, y, paint)
        
        return y + 25f
    }
    
    /**
     * 绘制签名区域
     */
    private fun drawSignatureArea(
        canvas: Canvas,
        paint: Paint,
        yPosition: Float
    ): Float {
        var y = yPosition
        val tableStartX = MARGIN.toFloat()
        val tableWidth = CONTENT_WIDTH.toFloat()
        val signatureWidth = tableWidth / 3f
        
        paint.textSize = 12f
        paint.typeface = Typeface.DEFAULT
        paint.textAlign = Paint.Align.CENTER
        
        // 申请人签名
        canvas.drawText("申请人签名:", tableStartX + signatureWidth / 2f, y, paint)
        y += 40f
        canvas.drawLine(tableStartX, y, tableStartX + signatureWidth, y, paint)
        
        // 审批人签名
        canvas.drawText("审批人签名:", tableStartX + signatureWidth + signatureWidth / 2f, y, paint)
        canvas.drawLine(tableStartX + signatureWidth, y, tableStartX + signatureWidth * 2f, y, paint)
        
        // 财务签名
        canvas.drawText("财务签名:", tableStartX + signatureWidth * 2f + signatureWidth / 2f, y, paint)
        canvas.drawLine(tableStartX + signatureWidth * 2f, y, tableStartX + tableWidth, y, paint)
        
        return y + 30f
    }
    
    /**
     * 绘制底部信息
     */
    private fun drawFooter(
        canvas: Canvas,
        paint: Paint,
        yPosition: Float
    ) {
        paint.textSize = 10f
        paint.color = Color.GRAY
        paint.textAlign = Paint.Align.CENTER
        
        canvas.drawText("票小秘(BillMii) - 自动生成", PAGE_WIDTH / 2f, yPosition, paint)
        canvas.drawText("生成时间: ${dateTimeFormat.format(Date())}", PAGE_WIDTH / 2f, yPosition + 15f, paint)
    }
    
    /**
     * 数字转大写金额
     */
    private fun convertToUppercase(amount: Double): String {
        val units = arrayOf("元", "拾", "佰", "仟", "万", "拾", "佰", "仟", "亿")
        val digits = arrayOf("零", "壹", "贰", "叁", "肆", "伍", "陆", "柒", "捌", "玖")
        
        if (amount == 0.0) return "零元整"
        
        val integerPart = amount.toLong()
        val decimalPart = ((amount - integerPart) * 100).toInt()
        
        var result = ""
        var temp = integerPart
        
        // 处理整数部分
        var unitIndex = 0
        while (temp > 0) {
            val digit = (temp % 10).toInt()
            result = digits[digit] + units[unitIndex] + result
            temp /= 10
            unitIndex++
        }
        
        // 处理小数部分
        if (decimalPart > 0) {
            result += digits[decimalPart / 10] + "角"
            if (decimalPart % 10 > 0) {
                result += digits[decimalPart % 10] + "分"
            }
        } else {
            result += "整"
        }
        
        return result
    }
    
    /**
     * 解析日期时间
     */
    private fun parseDateTime(dateTimeStr: String): Date {
        return try {
            dateTimeFormat.parse(dateTimeStr) ?: Date()
        } catch (e: Exception) {
            Date()
        }
    }
    
    /**
     * 获取输出目录
     */
    private fun getOutputDirectory(): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val billMiiDir = File(downloadsDir, "BillMii/vouchers")
        if (!billMiiDir.exists()) {
            billMiiDir.mkdirs()
        }
        return billMiiDir
    }
    
    /**
     * 生成图片凭证
     */
    suspend fun generateVoucherImage(
        reimbursement: Reimbursement,
        receipts: List<Receipt>
    ): Result<Bitmap> = withContext(Dispatchers.Default) {
        return@withContext try {
            val width = 800
            val height = 1200
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            val paint = Paint()
            
            // 绘制白色背景
            canvas.drawColor(Color.WHITE)
            
            // 使用类似的绘制逻辑（简化版）
            var yPosition = 50f
            
            // 绘制标题
            paint.textSize = 36f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            paint.color = Color.BLACK
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText("报销凭证", width / 2f, yPosition, paint)
            yPosition += 60f
            
            // 绘制基本信息
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = 18f
            paint.typeface = Typeface.DEFAULT
            canvas.drawText("报销单号: ${reimbursement.id}", 50f, yPosition, paint)
            yPosition += 35f
            canvas.drawText("申请人: ${reimbursement.applicant}", 50f, yPosition, paint)
            yPosition += 35f
            canvas.drawText("报销金额: ¥${String.format("%.2f", reimbursement.totalAmount)}", 50f, yPosition, paint)
            yPosition += 35f
            
            // 绘制票据列表
            receipts.take(5).forEachIndexed { index, receipt ->
                canvas.drawText("${index + 1}. ${receipt.type.displayName} - ¥${String.format("%.2f", receipt.amount)}", 50f, yPosition, paint)
                yPosition += 30f
            }
            
            if (receipts.size > 5) {
                canvas.drawText("... 还有 ${receipts.size - 5} 张票据", 50f, yPosition, paint)
            }
            
            Result.success(bitmap)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}