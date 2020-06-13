/*
 * Copyright (c) 2019 Razeware LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * Notwithstanding the foregoing, you may not use, copy, modify, merge, publish,
 * distribute, sublicense, create a derivative work, and/or sell copies of the
 * Software in any work that is designed, intended, or marketed for pedagogical or
 * instructional purposes related to programming, coding, application development,
 * or information technology.  Permission for such use, copying, modification,
 * merger, publication, distribution, sublicensing, creation of derivative works,
 * or sale is expressly withheld.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package com.raywenderlich.android.petmed2

import android.annotation.TargetApi
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Log
import java.security.KeyStore
import java.security.SecureRandom
import java.util.HashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

internal class Encryption {

  fun encrypt(dataToEncrypt: ByteArray,
              password: CharArray): HashMap<String, ByteArray> {

    val map = HashMap<String, ByteArray>()

    //TODO: Add code here
    val random = SecureRandom()
    val salt = ByteArray(256)
    random.nextBytes(salt)

    val pbKeySpec = PBEKeySpec(password, salt, 1324, 256) // 1
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1") // 2
    val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded // 3
    val keySpec = SecretKeySpec(keyBytes, "AES") // 4

    val ivRandom = SecureRandom() //not caching previous seeded instance of SecureRandom
    // 1
    val iv = ByteArray(16)
    ivRandom.nextBytes(iv)
    val ivSpec = IvParameterSpec(iv) // 2

    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding") // 1
    cipher.init(Cipher.ENCRYPT_MODE, keySpec, ivSpec)
    val encrypted = cipher.doFinal(dataToEncrypt) // 2

    map["salt"] = salt
    map["iv"] = iv
    map["encrypted"] = encrypted

    return map
  }

  fun decrypt(map: HashMap<String, ByteArray>, password: CharArray): ByteArray? {

    var decrypted: ByteArray? = null

    //TODO: Add code here
    // 1
    val salt = map["salt"]
    val iv = map["iv"]
    val encrypted = map["encrypted"]

    // 2
    //regenerate key from password
    val pbKeySpec = PBEKeySpec(password, salt, 1324, 256)
    val secretKeyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1")
    val keyBytes = secretKeyFactory.generateSecret(pbKeySpec).encoded
    val keySpec = SecretKeySpec(keyBytes, "AES")

    // 3
    //Decrypt
    val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
    val ivSpec = IvParameterSpec(iv)
    cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
    decrypted = cipher.doFinal(encrypted)

    return decrypted
  }

  fun keystoreEncrypt(dataToEncrypt: ByteArray): HashMap<String, ByteArray> {

    val map = HashMap<String, ByteArray>()

    //TODO: Add code here
    // 1
    //Get the key
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    val secretKeyEntry =
            keyStore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
    val secretKey = secretKeyEntry.secretKey

    // 2
    //Encrypt data
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey)
    val ivBytes = cipher.iv
    val encryptedBytes = cipher.doFinal(dataToEncrypt)

    // 3
    map["iv"] = ivBytes
    map["encrypted"] = encryptedBytes


    return map
  }

  fun keystoreDecrypt(map: HashMap<String, ByteArray>): ByteArray? {

    var decrypted: ByteArray? = null

    //TODO: Add code here
    // 1
    //Get the key
    val keyStore = KeyStore.getInstance("AndroidKeyStore")
    keyStore.load(null)

    val secretKeyEntry =
            keyStore.getEntry("MyKeyAlias", null) as KeyStore.SecretKeyEntry
    val secretKey = secretKeyEntry.secretKey

    // 2
    //Extract info from map
    val encryptedBytes = map["encrypted"]
    val ivBytes = map["iv"]

    // 3
    //Decrypt data
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    val spec = GCMParameterSpec(128, ivBytes)
    cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
    decrypted = cipher.doFinal(encryptedBytes)


    return decrypted
  }

  @TargetApi(23)
  fun keystoreTest() {

    //TODO: Add code here
    val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore") // 1
    val keyGenParameterSpec = KeyGenParameterSpec.Builder("MyKeyAlias",
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            //.setUserAuthenticationRequired(true) // 2 requires lock screen, invalidated if lock screen is disabled
            //.setUserAuthenticationValidityDurationSeconds(120) // 3 only available x seconds from password authentication. -1 requires finger print - every time
            .setRandomizedEncryptionRequired(true) // 4 different ciphertext for same plaintext on each call
            .build()
    keyGenerator.init(keyGenParameterSpec)
    keyGenerator.generateKey()

    // 1
    val map = keystoreEncrypt("My very sensitive string!".toByteArray(Charsets.UTF_8))
    // 2
    val decryptedBytes = keystoreDecrypt(map)
    decryptedBytes?.let {
      val decryptedString = String(it, Charsets.UTF_8)
      Log.e("MyApp", "The decrypted string is: $decryptedString")
    }
  }
}

