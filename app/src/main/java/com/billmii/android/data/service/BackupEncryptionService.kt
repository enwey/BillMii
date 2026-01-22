package com.billmii.android.data.service

import android.util.Base64
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 备份文件加密服务
 * 提供备份文件的加密和解密功能
 */
@Singleton
class BackupEncryptionService @Inject constructor() {
    
    companion object {
        private const val ALGORITHM = "AES"
        private const val TRANSFORMATION = "AES/CBC/PKCS5Padding"
        private const val KEY_SIZE = 256
        private const val IV_SIZE = 16
    }
    
    /**
     * 生成加密密钥
     */
    fun generateKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(ALGORITHM)
        keyGenerator.init(KEY_SIZE, SecureRandom())
        return keyGenerator.generateKey()
    }
    
    /**
     * 从密码生成密钥
     */
    fun generateKeyFromPassword(password: String, salt: ByteArray): SecretKey {
        // 使用PBKDF2从密码派生密钥
        val factory = javax.crypto.SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = javax.crypto.spec.PBEKeySpec(
            password.toCharArray(),
            salt,
            100000,
            256
        )
        val tmp = factory.generateSecret(spec)
        return SecretKeySpec(tmp.encoded, ALGORITHM)
    }
    
    /**
     * 生成随机IV
     */
    fun generateIv(): IvParameterSpec {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return IvParameterSpec(iv)
    }
    
    /**
     * 加密文件
     */
    fun encryptFile(
        inputFile: File,
        outputFile: File,
        key: SecretKey,
        iv: IvParameterSpec? = null
    ): Result<Unit> {
        return try {
            val actualIv = iv ?: generateIv()
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, actualIv)
            
            // 写入IV到输出文件开头
            FileOutputStream(outputFile).use { fos ->
                fos.write(actualIv.iv)
            }
            
            // 加密文件内容
            CipherInputStream(FileInputStream(inputFile), cipher).use { cis ->
                FileOutputStream(outputFile, true).use { fos ->
                    cis.copyTo(fos)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解密文件
     */
    fun decryptFile(
        inputFile: File,
        outputFile: File,
        key: SecretKey
    ): Result<Unit> {
        return try {
            // 从输入文件读取IV
            val iv = ByteArray(IV_SIZE)
            FileInputStream(inputFile).use { fis ->
                fis.read(iv)
            }
            
            val ivSpec = IvParameterSpec(iv)
            
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            
            // 解密文件内容
            CipherInputStream(FileInputStream(inputFile), cipher).use { cis ->
                // 跳过IV部分
                cis.skip(IV_SIZE.toLong())
                
                FileOutputStream(outputFile).use { fos ->
                    cis.copyTo(fos)
                }
            }
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 加密字节数组
     */
    fun encryptBytes(data: ByteArray, key: SecretKey, iv: IvParameterSpec? = null): Result<ByteArray> {
        return try {
            val actualIv = iv ?: generateIv()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, key, actualIv)
            
            val encryptedData = cipher.doFinal(data)
            
            // 将IV和加密数据合并
            val result = ByteArray(IV_SIZE + encryptedData.size)
            System.arraycopy(actualIv.iv, 0, result, 0, IV_SIZE)
            System.arraycopy(encryptedData, 0, result, IV_SIZE, encryptedData.size)
            
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 解密字节数组
     */
    fun decryptBytes(encryptedData: ByteArray, key: SecretKey): Result<ByteArray> {
        return try {
            // 提取IV
            val iv = ByteArray(IV_SIZE)
            System.arraycopy(encryptedData, 0, iv, 0, IV_SIZE)
            
            // 提取加密数据
            val cipherText = ByteArray(encryptedData.size - IV_SIZE)
            System.arraycopy(encryptedData, IV_SIZE, cipherText, 0, cipherText.size)
            
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
            
            val decryptedData = cipher.doFinal(cipherText)
            Result.success(decryptedData)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 将密钥转换为Base64字符串
     */
    fun keyToString(key: SecretKey): String {
        return Base64.encodeToString(key.encoded, Base64.NO_WRAP)
    }
    
    /**
     * 从Base64字符串创建密钥
     */
    fun stringToKey(keyString: String): SecretKey {
        val keyBytes = Base64.decode(keyString, Base64.NO_WRAP)
        return SecretKeySpec(keyBytes, ALGORITHM)
    }
    
    /**
     * 生成随机盐值
     */
    fun generateSalt(): ByteArray {
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        return salt
    }
    
    /**
     * 将盐值转换为Base64字符串
     */
    fun saltToString(salt: ByteArray): String {
        return Base64.encodeToString(salt, Base64.NO_WRAP)
    }
    
    /**
     * 从Base64字符串创建盐值
     */
    fun stringToSalt(saltString: String): ByteArray {
        return Base64.decode(saltString, Base64.NO_WRAP)
    }
    
    /**
     * 使用密码加密文件
     */
    fun encryptFileWithPassword(
        inputFile: File,
        outputFile: File,
        password: String
    ): Result<ByteArray> {
        return try {
            val salt = generateSalt()
            val key = generateKeyFromPassword(password, salt)
            val iv = generateIv()
            
            val result = encryptFile(inputFile, outputFile, key, iv)
            
            if (result.isSuccess) {
                // 返回salt和iv以便后续解密
                val combined = ByteArray(salt.size + iv.iv.size)
                System.arraycopy(salt, 0, combined, 0, salt.size)
                System.arraycopy(iv.iv, 0, combined, salt.size, iv.iv.size)
                Result.success(combined)
            } else {
                result
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * 使用密码解密文件
     */
    fun decryptFileWithPassword(
        inputFile: File,
        outputFile: File,
        password: String,
        saltAndIv: ByteArray
    ): Result<Unit> {
        return try {
            val salt = ByteArray(16)
            val iv = ByteArray(IV_SIZE)
            
            System.arraycopy(saltAndIv, 0, salt, 0, salt.size)
            System.arraycopy(saltAndIv, salt.size, iv, 0, iv.size)
            
            val key = generateKeyFromPassword(password, salt)
            val ivSpec = IvParameterSpec(iv)
            
            decryptFile(inputFile, outputFile, key)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}