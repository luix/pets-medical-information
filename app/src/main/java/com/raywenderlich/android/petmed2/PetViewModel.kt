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

import android.arch.lifecycle.ViewModel
import com.raywenderlich.android.petmed2.model.Pet
import com.raywenderlich.android.petmed2.model.Pets
import org.simpleframework.xml.core.Persister
import java.io.File

class PetViewModel : ViewModel() {

  private var pets: ArrayList<Pet>? = null

  fun getPets(file: File, password: CharArray) : ArrayList<Pet> {
    if (pets == null) {
      loadPets(file, password)
    }

    return pets ?: arrayListOf()
  }

  private fun loadPets(file: File, password: CharArray) {

    /*
    var decrypted: ByteArray? = null
    ObjectInputStream(FileInputStream(file)).use { it ->
      val data = it.readObject()

      when(data) {
        is Map<*, *> -> {

          if (data.containsKey("iv") && data.containsKey("salt") && data.containsKey("encrypted")) {
            val iv = data["iv"]
            val salt = data["salt"]
            val encrypted = data["encrypted"]
            if (iv is ByteArray && salt is ByteArray && encrypted is ByteArray) {
              //TODO: Add decrypt call here
            }
          }
        }
      }
    }

    if (decrypted != null) {
    */
      val serializer = Persister()
      val inputStream = file.inputStream() //TODO: Replace me
      val pets = try { serializer.read(Pets::class.java, inputStream) } catch (e: Exception) {null}
      pets?.list?.let {
        this.pets = ArrayList(it)
      }
    /*} */
  }
}