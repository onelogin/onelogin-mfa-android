package com.onelogin.mfa.data.encryption

import android.content.Context
import android.content.SharedPreferences
import android.os.Build
import android.security.KeyPairGeneratorSpec
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import timber.log.Timber
import java.math.BigInteger
import java.security.*
import java.util.*
import javax.crypto.Cipher
import javax.crypto.NoSuchPaddingException
import javax.security.auth.x500.X500Principal

internal class EncryptionManagerImpl(private val context: Context): EncryptionManager {

    private var decryptCipher: Cipher? = null
    private var encryptCipher: Cipher? = null
    private var masterKey: KeyPair? = null
    private var sharedPreferences: SharedPreferences = context.getSharedPreferences(SHARED_PREFERENCES, Context.MODE_PRIVATE)

    init {
        try {
            if (isSupported())
                generateKeys()
        } catch (e: Exception) {
            Timber.e(e, "Error in generating keys")
            setIsEncryptionSupported(false)
        }
    }

    @Synchronized
    fun initialize() {
        if (masterKey == null)
            masterKey = getAndroidKeyStoreAsymmetricKeyPair()

        if (encryptCipher == null) {
            encryptCipher = prepareCipher()
            encryptCipher?.init(Cipher.ENCRYPT_MODE, masterKey?.public)
        }

        if (decryptCipher == null) {
            decryptCipher = prepareCipher()
            decryptCipher?.init(Cipher.DECRYPT_MODE, masterKey?.private)
        }
    }

    @Synchronized
    override fun encrypt(plainData: String): String {
        initialize()
        val bytes = encryptCipher?.doFinal(plainData.toByteArray())
        return Base64.encodeToString(bytes, Base64.DEFAULT)
    }

    @Synchronized
    override fun decrypt(encryptedData: String): String {
        initialize()
        val encryptedDataDecoded = Base64.decode(encryptedData, Base64.DEFAULT)
        val decodedData = decryptCipher?.doFinal(encryptedDataDecoded)
        return String(decodedData!!)
    }

    override fun isSupported(): Boolean {
        if (isEncryptionSupportChecked())
            return isEncryptionSupported()

        val result = try {
            generateKeys()
            setIsEncryptionSupported(true)
            true
        } catch (e: Exception) {
            setIsEncryptionSupported(false)
            false
        }

        setIsEncryptionSupportChecked(true)
        return result
    }

    @Suppress("DEPRECATION")
    private fun generateKeys() {
        if (getAndroidKeyStoreAsymmetricKeyPair() != null)
            return

        val generator = KeyPairGenerator.getInstance(RSA_ENCRYPTION, PROVIDER)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val builder = KeyGenParameterSpec.Builder(ALIAS, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(KeyProperties.BLOCK_MODE_ECB)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_RSA_PKCS1)

            generator.initialize(builder.build())
        } else {
            val start = Calendar.getInstance()
            val end = Calendar.getInstance()
            end.add(Calendar.YEAR, 20)

            val builder = KeyPairGeneratorSpec.Builder(context)
                .setAlias(ALIAS)
                .setSubject(X500Principal("CN=" + ALIAS + "CA Certificate"))
                .setSerialNumber(BigInteger.ONE)
                .setStartDate(start.time)
                .setEndDate(end.time)

            generator.initialize(builder.build())
        }

        generator.generateKeyPair()
    }

    private fun prepareKeyStore(): KeyStore? {
        return try {
            val keyStore = KeyStore.getInstance(PROVIDER)
            keyStore.load(null)
            keyStore
        } catch (e: Exception) {
            Timber.e(e, "KeyStore not supported")
            null
        }
    }

    private fun prepareCipher(): Cipher {
        val cipher: Cipher
        try {
            cipher = Cipher.getInstance(TRANSFORMATION)
        } catch (e: NoSuchAlgorithmException) {
            throw RuntimeException(e)
        } catch (e: NoSuchPaddingException) {
            throw RuntimeException(e)
        }

        return cipher
    }

    private fun getAndroidKeyStoreAsymmetricKeyPair(): KeyPair? {
        val keyStore: KeyStore? = prepareKeyStore()
        val privateKey = keyStore?.getKey(ALIAS, null) as PrivateKey?
        val publicKey = keyStore?.getCertificate(ALIAS)?.publicKey

        return if (privateKey != null && publicKey != null) {
            KeyPair(publicKey, privateKey)
        } else {
            null
        }
    }

    private fun isEncryptionSupported() = sharedPreferences.getBoolean(ENCRYPTION_SUPPORTED, false)

    private fun setIsEncryptionSupported(isSupported: Boolean) = with(sharedPreferences.edit()) {
        putBoolean(ENCRYPTION_SUPPORTED, isSupported)
        commit()
    }

    private fun isEncryptionSupportChecked() = sharedPreferences.getBoolean(ENCRYPTION_SUPPORT_CHECKED, false)

    private fun setIsEncryptionSupportChecked(isChecked: Boolean) = with(sharedPreferences.edit()) {
        putBoolean(ENCRYPTION_SUPPORT_CHECKED, isChecked)
        commit()
    }

    companion object {
        private const val TRANSFORMATION = "RSA/ECB/PKCS1Padding"
        private const val PROVIDER = "AndroidKeyStore"
        private const val RSA_ENCRYPTION = "RSA"
        private const val ALIAS = "ONELOGIN"
        private const val SHARED_PREFERENCES = "oneloginMfaPreferences"
        private const val ENCRYPTION_SUPPORTED = "oneloginMfaEncryptionSupported"
        private const val ENCRYPTION_SUPPORT_CHECKED = "oneloginMfaEncryptionSupportChecked"
    }
}
