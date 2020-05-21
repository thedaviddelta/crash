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
import com.thedaviddelta.crash.model.MastodonAccount
import com.thedaviddelta.crash.model.TwitterAccount
import com.thedaviddelta.crash.repository.MastodonRepository
import com.thedaviddelta.crash.repository.TwitterRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/**
 * Controls the whole local [Account]s logic
 */
object Accounts {
    private const val FILE_NAME = "${BuildConfig.APPLICATION_ID}.accounts.list.out"
    private const val SHARED_PREFS_NAME = "${BuildConfig.APPLICATION_ID}.accounts.current"

    private lateinit var secureFile: SecureFile

    private var list: MutableList<Account>? = null
    private var sharedPrefs: SharedPreferences? = null

    private var currentIndex: Int = -1

    /** Instance of the [Account] currently active at the app, or `null` if none is active */
    var current: Account?
        get() = list?.getOrNull(currentIndex)
        set(value) {
            list?.indexOf(value)?.takeIf { it != -1 }?.let {
                currentIndex = it
                sharedPrefs?.edit()?.putInt("index", currentIndex)?.apply()
            }
        }

    /** Immutable instance of all local [Account]s registered on the app */
    val readOnlyList: List<Account>?
        get() = list

    /**
     * Initializes class' [SecureFile] and makes initial read
     *
     * @param context for creating [SecureFile] object
     * @return `true` if read correctly, or `false` if an error occurred
     */
    suspend fun initialize(context: Context): Boolean {
        secureFile = SecureFile.with(context)

        list = secureFile.readFile(FILE_NAME, mutableListOf()) ?: return false
        sharedPrefs = secureFile.sharedPreferences(SHARED_PREFS_NAME) ?: return false

        currentIndex = sharedPrefs?.getInt("index", -1) ?: -1
        return true
    }

    /**
     * Updates information of [all registered Accounts][readOnlyList] from the network services
     *
     * @return `true` if updated correctly, or `false` if an error occurred
     */
    suspend fun update(): Boolean = coroutineScope {
        list?.map {
            when(it) {
                is TwitterAccount -> async {
                    TwitterRepository.getUsers(
                        userIds = it.id.toString()
                    )?.body()?.firstOrNull()?.let(it::updateFrom)
                }
                is MastodonAccount -> async {
                    MastodonRepository.verifyCredentials(
                        domain = it.domain,
                        bearer = "Bearer ${it.bearer}"
                    )?.body()?.let(it::updateFrom)
                }
                else -> null
            }
        }?.all {
            it?.await() != null
        }?.let {
            secureFile.writeFile(FILE_NAME, list!!) && it
        } ?: false
    }

    /**
     * Adds the [given Account][account]
     *
     * @param account to be added
     * @return `true` if added correctly, or `false` if an error occurred
     */
    suspend fun add(account: Account): Boolean {
        return list?.takeIf {
            !it.contains(account) && it.add(account)
        }?.let {
            current = account
            secureFile.writeFile(FILE_NAME, it)
        } ?: false
    }

    /**
     * Removes the [given Account][account]
     *
     * @param account to be removed
     * @return `true` if removed correctly, or `false` if an error occurred
     */
    suspend fun remove(account: Account): Boolean {
        return current?.let { tempCurrent ->
            list?.takeIf {
                it.remove(account)
            }?.let {
                current = tempCurrent
                secureFile.writeFile(FILE_NAME, it)
            }
        } ?: false
    }
}
