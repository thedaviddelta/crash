/*
 * cr@sh - Secret crush matcher for social networks
 * Copyright (C) 2020 TheDavidDelta
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */

package com.thedaviddelta.crash.util

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

/**
 * I/O operations with new [Jetpack Security library](https://developer.android.com/jetpack/androidx/releases/security)
 */
class SecureFile private constructor(private val context: Context) {
    companion object {
        /**
         * Create an instance with a certain [context]
         *
         * @param context needed for I/O operations
         */
        fun with(context: Context) = SecureFile(context)
    }

    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)

    /**
     * Serializes [content] on a file with the [given name][fileName]
     *
     * @param T type of the content
     * @param fileName name of the file
     * @param content object to serialize
     * @return `true` if wrote correctly, or `false` if an error occurred
     */
    suspend fun <T> writeFile(fileName: String, content: T): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (file.exists())
                file.delete()

            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            ObjectOutputStream(encryptedFile.openFileOutput()).use {
                it.writeObject(content)
            }
            true
        } catch (e: Exception) {
            Log.e("SecureFile", "write-error ${e.localizedMessage}")
            false
        }
    }

    /**
     * Deserializes content of a file with the [given name][fileName]
     *
     * @param T type of the deserialized content
     * @param fileName name of the file
     * @param default value to return if file doesn't exist
     * @return file's content if read correctly, [default] if file doesn't exist, or `null` if an error occurred
     */
    @Suppress("UNCHECKED_CAST")
    suspend fun <T> readFile(fileName: String, default: T? = null): T? = withContext(Dispatchers.IO) {
        try {
            val file = File(context.filesDir, fileName)
            if (!file.exists())
                return@withContext default

            val encryptedFile = EncryptedFile.Builder(
                file,
                context,
                masterKeyAlias,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            ObjectInputStream(encryptedFile.openFileInput()).use {
                it.readObject() as? T
            }
        } catch (e: Exception) {
            Log.e("SecureFile", "read-error ${e.localizedMessage}")
            null
        }
    }

    /**
     * Returns secure shared preferences instance
     *
     * @param fileName name of the shared preferences file
     * @return a shared preferences instance, or `null` if an error occurred
     */
    suspend fun sharedPreferences(fileName: String): SharedPreferences? = withContext(Dispatchers.IO) {
        try {
            EncryptedSharedPreferences.create(
                fileName,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e("SecureFile", "shared-prefs-error ${e.localizedMessage}")
            null
        }
    }
}
