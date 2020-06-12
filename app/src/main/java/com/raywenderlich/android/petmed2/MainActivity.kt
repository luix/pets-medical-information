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
import android.view.View
import kotlinx.android.synthetic.main.activity_main.*
import org.jetbrains.anko.toast
import java.io.File
import java.io.FileOutputStream
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

  private fun lastLoggedIn(): String? {
    //Retrieve shared prefs data
    val preferences = getSharedPreferences("MyPrefs", Context.MODE_WORLD_WRITEABLE)
    return preferences.getString("l", "")
  }

  private fun saveLastLoggedInTime() {
    val currentDateTimeString = DateFormat.getDateTimeInstance().format(Date())

    //Save to shared prefs
    val editor = getSharedPreferences("MyPrefs", Context.MODE_WORLD_READABLE).edit()
    editor.putString("l", currentDateTimeString)
    editor.apply()
  }

  //This is just for demo data
  private fun createDataSource(filename: String, outFile: File) {
    val fileDescriptor = applicationContext.assets.openFd(filename)
    fileDescriptor.createInputStream().use { inputStream ->
      FileOutputStream(outFile).use { outputStream ->
        inputStream.channel.transferTo(fileDescriptor.startOffset,
            fileDescriptor.length,
            outputStream.channel)
      }
    }
  }

  companion object {
    private const val PWD_KEY = "PWD"
  }

  object FileConstants {
    const val DATA_SOURCE_FILE_NAME = "pets.xml"
  }
}
