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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import android.util.Base64.*
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream
import java.io.ObjectOutputStream
import java.text.DateFormat
import java.util.*

/**
 * Main Screen
 */
class MainActivity : AppCompatActivity() {

  private var isSignedUp = false
  private var workingFile: File? = null

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)

    workingFile = File(filesDir.absolutePath + java.io.File.separator +
        FileConstants.DATA_SOURCE_FILE_NAME)

    //Encryption().keystoreTest()

    updateLoggedInState()
  }

  fun loginPressed(view: android.view.View) {

    var success = false
    val password = login_password.text.toString()

    //Check if already signed up
    if (isSignedUp) {

      val lastLogin = lastLoggedIn()
      if (lastLogin != null) {
        success = true
        toast("Last login: $lastLogin")
      } else {
        toast("Please check your password and try again.")
      }

    } else {
      when {
        password.isEmpty() -> toast("Please enter a password!")
        password == login_confirm_password.text.toString() -> workingFile?.let {
          createDataSource("pets.xml", it)
          success = true
        }
        else -> toast("Passwords do not match!")
      }
    }

    if (success) {

      saveLastLoggedInTime()

      //Start next activity
      val context = view.context
      val petListIntent = Intent(context, PetListActivity::class.java)
      petListIntent.putExtra(PWD_KEY, password.toCharArray())
      context.startActivity(petListIntent)
    }
  }

  private fun updateLoggedInState() {
    val fileExists = workingFile?.exists() ?: false
    if (fileExists) {
      isSignedUp = true
      button.text = getString(R.string.login)
      login_confirm_password.visibility = View.INVISIBLE
    } else {
      button.text = getString(R.string.signup)
    }
  }

  private fun lastLoggedInOld(): String? {
    //Retrieve shared prefs data
    val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)  // Context.MODE_WORLD_WRITEABLE
    return preferences.getString("l", "")
  }

  private fun lastLoggedIn(): String? {
    //Get password
    val password = CharArray(login_password.length())
    login_password.text.getChars(0, login_password.length(), password, 0)

    //Retrieve shared prefs data
    // 1
    val preferences = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val base64Encrypted = preferences.getString("l", "")
    val base64Salt = preferences.getString("lsalt", "")
    val base64Iv = preferences.getString("liv", "")

    //Base64 decode
    // 2
    val encrypted = Base64.decode(base64Encrypted, Base64.NO_WRAP)
    val iv = Base64.decode(base64Iv, Base64.NO_WRAP)
    val salt = Base64.decode(base64Salt, Base64.NO_WRAP)

    //Decrypt
    // 3
    val decrypted = Encryption().decrypt(
            hashMapOf("iv" to iv, "salt" to salt, "encrypted" to encrypted), password)

    var lastLoggedIn: String? = null
    decrypted?.let {
      lastLoggedIn = String(it, Charsets.UTF_8)
    }
    return lastLoggedIn
  }

  private fun saveLastLoggedInTimeOld() {
    val currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())

    //Save to shared prefs
    val editor = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit()
    editor.putString("l", currentDateTimeString)
    editor.apply()
  }

  private fun saveLastLoggedInTime() {
    //Get password
    val password = CharArray(login_password.length())
    login_password.text.getChars(0, login_password.length(), password, 0)

    // Base64 the data
    val currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())
    // 1
    val map =
            Encryption().encrypt(currentDateTimeString.toByteArray(Charsets.UTF_8), password)
    // 2
    val valueBase64String = Base64.encodeToString(map["encrypted"], Base64.NO_WRAP)
    val saltBase64String = Base64.encodeToString(map["salt"], Base64.NO_WRAP)
    val ivBase64String = Base64.encodeToString(map["iv"], Base64.NO_WRAP)

    //Save to shared prefs
    val editor = getSharedPreferences("MyPrefs", Context.MODE_PRIVATE).edit()
    // 3
    editor.putString("l", valueBase64String)
    editor.putString("lsalt", saltBase64String)
    editor.putString("liv", ivBase64String)
    editor.apply()
  }

  //This is just for demo data
  private fun createDataSourceDemo(filename: String, outFile: File) {
    val fileDescriptor = applicationContext.assets.openFd(filename)
    fileDescriptor.createInputStream().use { inputStream ->
      FileOutputStream(outFile).use { outputStream ->
        inputStream.channel.transferTo(fileDescriptor.startOffset,
            fileDescriptor.length,
            outputStream.channel)
      }
    }
  }

  private fun createDataSource(filename: String, outFile: File) {
    val inputStream = applicationContext.assets.open(filename)
    val bytes = inputStream.readBytes()
    inputStream.close()

    val password = CharArray(login_password.length())
    login_password.text.getChars(0, login_password.length(), password, 0)
    val map = Encryption().encrypt(bytes, password)
    ObjectOutputStream(FileOutputStream(outFile)).use {
      it -> it.writeObject(map)
    }
  }

  companion object {
    private const val PWD_KEY = "PWD"
  }

  object FileConstants {
    const val DATA_SOURCE_FILE_NAME = "pets.xml"
  }
}
