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
import com.thedaviddelta.crash.BuildConfig
import com.thedaviddelta.crash.model.Account

object Accounts {
    private const val FILE_NAME = "${BuildConfig.APPLICATION_ID}.accounts.list.out"
    private const val SHARED_PREFS_NAME = "${BuildConfig.APPLICATION_ID}.accounts.current"

    private lateinit var secureFile: SecureFile
    private lateinit var sharedPrefs: SharedPreferences

    private lateinit var list: MutableList<Account>
    private var currentIndex = -1

    val current
        get() = list.getOrNull(currentIndex)

    suspend fun initialize(context: Context): Boolean {
        secureFile = SecureFile.with(context)

        list = secureFile.readFile(FILE_NAME, mutableListOf()) ?: return false
        sharedPrefs = secureFile.sharedPreferences(SHARED_PREFS_NAME) ?: return false
        currentIndex = sharedPrefs.getInt("index", -1)
        return true
    }

    suspend fun add(account: Account): Boolean {
        list.add(account)
        currentIndex = list.size - 1
        sharedPrefs.edit()?.putInt("index", currentIndex)?.apply()
        return secureFile.writeFile(FILE_NAME, list)
    }

    fun setCurrent(index: Int): Account? {
        if (index < list.size) currentIndex = index
        sharedPrefs.edit()?.putInt("index", currentIndex)?.apply()
        return current
    }
}
