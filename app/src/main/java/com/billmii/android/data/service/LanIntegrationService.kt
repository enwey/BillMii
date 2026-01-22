package com.billmii.android.data.service

import android.content.Context
import android.net.Uri
import com.billmii.android.data.model.Receipt
import com.billmii.android.data.model.Reimbursement
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 局域网财务软件集成服务
 */
@Singleton
class LanIntegrationService @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    data class ServerConfig(
        val host: String,
        val port: Int = 8080,
        val protocol: String = "http",
        val apiKey: String? = null
    ) {
        val baseUrl: String
            get() = "$protocol://$host:$port"
    }
    
    data class SyncResult(
        val success: Boolean,
        val syncedCount: Int = 0,
        val failedCount: Int = 0,
        val errors: List<String> = emptyList()
    )
    
    data class IntegrationStatus(
        val isConnected: Boolean = false,
        val lastSyncTime: Long? = null,
        val pendingSyncCount: Int = 0
    )
    
    private val _integrationStatus = MutableStateFlow(IntegrationStatus())
    val integrationStatus: Flow<IntegrationStatus> = _integrationStatus.asStateFlow()
    
    private var serverConfig: ServerConfig? = null
    private var okHttpClient: OkHttpClient? = null
    
    companion object {
        private const val DEFAULT_TIMEOUT = 30L
        private const val JSON_MEDIA_TYPE = "application/json"
    }
    
    /**
     * 配置服务器连接
     */
    fun configureServer(config: ServerConfig) {
        serverConfig = config
        okHttpClient = OkHttpClient.Builder()
            .connectTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(DEFAULT_TIMEOUT, TimeUnit.SECONDS)
            .build()
        
        testConnection()
    }
    
    /**
     * 测试连接
     */
    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val config = serverConfig ?: return@withContext false
            val client = okHttpClient ?: return@withContext false
            
            val request = Request.Builder()
                .url("${config.baseUrl}/api/health")
                .get()
                .build()
            
            val response = client.newCall(request).execute()
            val isSuccessful = response.isSuccessful
            
            _integrationStatus.value = _integrationStatus.value.copy(
                isConnected = isSuccessful,
                lastSyncTime = if (isSuccessful) System.currentTimeMillis() else null
            )
            
            isSuccessful
        } catch (e: Exception) {
            _integrationStatus.value = _integrationStatus.value.copy(
                isConnected = false,
                lastSyncTime = null
            )
            false
        }
    }
    
    /**
     * 同步票据到财务软件
     */
    suspend fun syncReceipts(receipts: List<Receipt>): SyncResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val config = serverConfig ?: return@withContext SyncResult(
                success = false,
                errors = listOf("服务器未配置")
            )
            val client = okHttpClient ?: return@withContext SyncResult(
                success = false,
                errors = listOf("HTTP客户端未初始化")
            )
            
            var syncedCount = 0
            var failedCount = 0
            val errors = mutableListOf<String>()
            
            receipts.forEach { receipt ->
                try {
                    val receiptJson = convertReceiptToJson(receipt)
                    val requestBody = receiptJson.toRequestBody(JSON_MEDIA_TYPE.toMediaType())
                    
                    val request = Request.Builder()
                        .url("${config.baseUrl}/api/receipts")
                        .post(requestBody)
                        .addHeader("Content-Type", JSON_MEDIA_TYPE)
                        .apply {
                            config.apiKey?.let { 
                                addHeader("Authorization", "Bearer $it")
                            }
                        }
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        syncedCount++
                    } else {
                        failedCount++
                        errors.add("票据 ${receipt.id} 同步失败: ${response.code}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    errors.add("票据 ${receipt.id} 同步异常: ${e.message}")
                }
            }
            
            SyncResult(
                success = failedCount == 0,
                syncedCount = syncedCount,
                failedCount = failedCount,
                errors = errors
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                errors = listOf("同步失败: ${e.message}")
            )
        }
    }
    
    /**
     * 同步报销单到财务软件
     */
    suspend fun syncReimbursements(reimbursements: List<Reimbursement>): SyncResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val config = serverConfig ?: return@withContext SyncResult(
                success = false,
                errors = listOf("服务器未配置")
            )
            val client = okHttpClient ?: return@withContext SyncResult(
                success = false,
                errors = listOf("HTTP客户端未初始化")
            )
            
            var syncedCount = 0
            var failedCount = 0
            val errors = mutableListOf<String>()
            
            reimbursements.forEach { reimbursement ->
                try {
                    val reimbursementJson = convertReimbursementToJson(reimbursement)
                    val requestBody = reimbursementJson.toRequestBody(JSON_MEDIA_TYPE.toMediaType())
                    
                    val request = Request.Builder()
                        .url("${config.baseUrl}/api/reimbursements")
                        .post(requestBody)
                        .addHeader("Content-Type", JSON_MEDIA_TYPE)
                        .apply {
                            config.apiKey?.let { 
                                addHeader("Authorization", "Bearer $it")
                            }
                        }
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        syncedCount++
                    } else {
                        failedCount++
                        errors.add("报销单 ${reimbursement.id} 同步失败: ${response.code}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    errors.add("报销单 ${reimbursement.id} 同步异常: ${e.message}")
                }
            }
            
            SyncResult(
                success = failedCount == 0,
                syncedCount = syncedCount,
                failedCount = failedCount,
                errors = errors
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                errors = listOf("同步失败: ${e.message}")
            )
        }
    }
    
    /**
     * 从财务软件获取数据
     */
    suspend fun fetchFromFinancialSoftware(
        endpoint: String,
        params: Map<String, String> = emptyMap()
    ): Result<String> = withContext(Dispatchers.IO) {
        return@withContext try {
            val config = serverConfig ?: return@withContext Result.failure(
                Exception("服务器未配置")
            )
            val client = okHttpClient ?: return@withContext Result.failure(
                Exception("HTTP客户端未初始化")
            )
            
            val urlBuilder = HttpUrl.Builder()
                .scheme(config.protocol)
                .host(config.host)
                .port(config.port)
                .addPathSegments(endpoint.split("/"))
            
            params.forEach { (key, value) ->
                urlBuilder.addQueryParameter(key, value)
            }
            
            val request = Request.Builder()
                .url(urlBuilder.build())
                .get()
                .apply {
                    config.apiKey?.let { 
                        addHeader("Authorization", "Bearer $it")
                    }
                }
                .build()
            
            val response = client.newCall(request).execute()
            
            if (response.isSuccessful) {
                Result.success(response.body?.string() ?: "")
            } else {
                Result.failure(Exception("请求失败: ${response.code}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 转换票据为JSON
     */
    private fun convertReceiptToJson(receipt: Receipt): String {
        val json = JSONObject()
        json.put("id", receipt.id)
        json.put("receiptType", receipt.type.name)
        json.put("amount", receipt.amount)
        json.put("merchant", receipt.merchant)
        json.put("date", receipt.date?.toString())
        json.put("invoiceCode", receipt.invoiceCode)
        json.put("invoiceNumber", receipt.invoiceNumber)
        json.put("buyerName", receipt.buyerName)
        json.put("sellerName", receipt.sellerName)
        json.put("taxAmount", receipt.taxAmount)
        json.put("description", receipt.description)
        json.put("createdAt", receipt.createdAt.toString())
        json.put("filePath", receipt.filePath)
        
        return json.toString()
    }
    
    /**
     * 转换报销单为JSON
     */
    private fun convertReimbursementToJson(reimbursement: Reimbursement): String {
        val json = JSONObject()
        json.put("id", reimbursement.id)
        json.put("title", reimbursement.title)
        json.put("totalAmount", reimbursement.totalAmount)
        json.put("applicant", reimbursement.applicant)
        json.put("department", reimbursement.department)
        json.put("project", reimbursement.project)
        json.put("budgetCode", reimbursement.budgetCode)
        json.put("status", reimbursement.status.name)
        json.put("description", reimbursement.description)
        json.put("createdAt", reimbursement.createdAt)
        json.put("updatedAt", reimbursement.updatedAt)
        
        // 添加票据列表
        val receiptsArray = JSONArray()
        // Note: In a real implementation, you would fetch associated receipts
        json.put("receipts", receiptsArray)
        
        return json.toString()
    }
    
    /**
     * 批量上传文件
     */
    suspend fun uploadFiles(files: List<File>): SyncResult = withContext(Dispatchers.IO) {
        return@withContext try {
            val config = serverConfig ?: return@withContext SyncResult(
                success = false,
                errors = listOf("服务器未配置")
            )
            val client = okHttpClient ?: return@withContext SyncResult(
                success = false,
                errors = listOf("HTTP客户端未初始化")
            )
            
            var uploadedCount = 0
            var failedCount = 0
            val errors = mutableListOf<String>()
            
            files.forEach { file ->
                try {
                    val requestBody = MultipartBody.Builder()
                        .setType(MultipartBody.FORM)
                        .addFormDataPart("file", file.name, 
                            file.asRequestBody("application/octet-stream".toMediaType()))
                        .build()
                    
                    val request = Request.Builder()
                        .url("${config.baseUrl}/api/upload")
                        .post(requestBody)
                        .apply {
                            config.apiKey?.let { 
                                addHeader("Authorization", "Bearer $it")
                            }
                        }
                        .build()
                    
                    val response = client.newCall(request).execute()
                    
                    if (response.isSuccessful) {
                        uploadedCount++
                    } else {
                        failedCount++
                        errors.add("文件 ${file.name} 上传失败: ${response.code}")
                    }
                } catch (e: Exception) {
                    failedCount++
                    errors.add("文件 ${file.name} 上传异常: ${e.message}")
                }
            }
            
            SyncResult(
                success = failedCount == 0,
                syncedCount = uploadedCount,
                failedCount = failedCount,
                errors = errors
            )
        } catch (e: Exception) {
            SyncResult(
                success = false,
                errors = listOf("上传失败: ${e.message}")
            )
        }
    }
    
    /**
     * 数据格式转换
     */
    fun convertDataFormat(
        data: String,
        sourceFormat: String,
        targetFormat: String
    ): Result<String> {
        return try {
            // 简单的格式转换示例
            // 实际实现应根据具体的财务软件API文档进行调整
            val converted = when (targetFormat.lowercase()) {
                "json" -> convertToJson(data, sourceFormat)
                "xml" -> convertToXml(data, sourceFormat)
                "csv" -> convertToCsv(data, sourceFormat)
                else -> return Result.failure(Exception("不支持的目标格式: $targetFormat"))
            }
            Result.success(converted)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun convertToJson(data: String, sourceFormat: String): String {
        return when (sourceFormat.lowercase()) {
            "xml" -> xmlToJson(data)
            "csv" -> csvToJson(data)
            else -> data
        }
    }
    
    private fun convertToXml(data: String, sourceFormat: String): String {
        // 简化的XML转换实现
        return """<?xml version="1.0" encoding="UTF-8"?>
<root>
    <data>${data}</data>
</root>"""
    }
    
    private fun convertToCsv(data: String, sourceFormat: String): String {
        // 简化的CSV转换实现
        return "data\n$data"
    }
    
    private fun xmlToJson(xml: String): String {
        // 简化的XML到JSON转换
        return """{"converted": "$xml"}"""
    }
    
    private fun csvToJson(csv: String): String {
        // 简化的CSV到JSON转换
        val lines = csv.split("\n")
        val jsonArray = JSONArray()
        lines.forEach { line ->
            if (line.isNotEmpty()) {
                jsonArray.put(JSONObject().put("value", line))
            }
        }
        return jsonArray.toString()
    }
    
    /**
     * 清除配置
     */
    fun clearConfiguration() {
        serverConfig = null
        okHttpClient = null
        _integrationStatus.value = IntegrationStatus(
            isConnected = false,
            lastSyncTime = null,
            pendingSyncCount = 0
        )
    }
    
    /**
     * 获取当前配置
     */
    fun getCurrentConfig(): ServerConfig? = serverConfig
    
    /**
     * 检查是否已配置
     */
    fun isConfigured(): Boolean = serverConfig != null && okHttpClient != null
}