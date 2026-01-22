package com.billmii.android.security

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.IvParameterSpec

/**
 * Encryption Key Manager
 * Manages cryptographic keys using Android Keystore
 */
class EncryptionKeyManager {
    
    companion object {
        private const val ANDROID_KEYSTORE = "AndroidKeyStore"
        private const val KEY_ALIAS = "BillMiiEncryptionKey"
        private const val KEY_SIZE = 256
        private const val TRANSFORMATION = "AES/CBC/PKCS7Padding"
    }
    
    private val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply {
        load(null)
    }
    
    /**
     * Get or create encryption key
     */
    fun getOrCreateKey(): SecretKey {
        val existingKey = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
        
        return existingKey?.secretKey ?: createKey()
    }
    
    /**
     * Create new encryption key
     */
    private fun createKey(): SecretKey {
        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            ANDROID_KEYSTORE
        )
        
        val keyGenSpec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_CBC)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_PKCS7)
            .setKeySize(KEY_SIZE)
            .setUserAuthenticationRequired(false)
            .build()
        
        keyGenerator.init(keyGenSpec)
        return keyGenerator.generateKey()
    }
    
    /**
     * Encrypt data
     * @param data Data to encrypt
     * @return Pair of encrypted bytes and IV
     */
    fun encrypt(data: ByteArray): Pair<ByteArray, ByteArray> {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        
        val iv = cipher.iv
        val encryptedData = cipher.doFinal(data)
        
        return Pair(encryptedData, iv)
    }
    
    /**
     * Decrypt data
     * @param encryptedData Encrypted data
     * @param iv Initialization vector
     * @return Decrypted bytes
     */
    fun decrypt(encryptedData: ByteArray, iv: ByteArray): ByteArray {
        val key = getOrCreateKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        val ivSpec = IvParameterSpec(iv)
        cipher.init(Cipher.DECRYPT_MODE, key, ivSpec)
        
        return cipher.doFinal(encryptedData)
    }
    
    /**
     * Encrypt string
     * @param plaintext String to encrypt
     * @return Pair of encrypted bytes and IV
     */
    fun encryptString(plaintext: String): Pair<ByteArray, ByteArray> {
        return encrypt(plaintext.toByteArray(Charsets.UTF_8))
    }
    
    /**
     * Decrypt to string
     * @param encryptedData Encrypted data
     * @param iv Initialization vector
     * @return Decrypted string
     */
    fun decryptToString(encryptedData: ByteArray, iv: ByteArray): String {
        val decryptedBytes = decrypt(encryptedData, iv)
        return String(decryptedBytes, Charsets.UTF_8)
    }
    
    /**
     * Check if key exists
     */
    fun keyExists(): Boolean {
        return keyStore.containsAlias(KEY_ALIAS)
    }
    
    /**
     * Delete encryption key
     * WARNING: This will permanently delete the key and all encrypted data will be lost
     */
    fun deleteKey() {
        keyStore.deleteEntry(KEY_ALIAS)
    }
}