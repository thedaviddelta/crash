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

package com.thedaviddelta.crash.repository

import android.util.Log
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.BuildConfig
import com.thedaviddelta.crash.api.ContactType
import com.thedaviddelta.crash.api.TwitterApi
import com.thedaviddelta.crash.api.twitterAuthorization
import com.thedaviddelta.crash.model.TwitterAccount
import com.thedaviddelta.crash.model.TwitterUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object TwitterRepository {
    private const val CALLBACK = "tdd-oauth://${BuildConfig.APPLICATION_ID}/twitter"

    private val client: TwitterApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.twitter.com")
            .client(
                OkHttpClient().newBuilder().addInterceptor {
                    val req = it.request()

                    if (req.url().encodedPath() == "/oauth/access_token")
                        return@addInterceptor it.proceed(req)

                    val (token, secret) = Accounts.current?.let { acc ->
                        if (acc is TwitterAccount)
                            Pair(acc.token, acc.secret)
                        else
                            null
                    } ?: Pair(null, null)

                    val authHeader = twitterAuthorization(req, token, secret)
                    val newReq = req
                        .newBuilder()
                        .header("Authorization", authHeader)
                        .build()
                    it.proceed(newReq)
                }.build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TwitterApi::class.java)
    }

    suspend fun requestToken(callback: String = CALLBACK) = withContext(Dispatchers.IO) {
        try {
            client.requestToken(callback)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "request-token-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun accessToken(token: String, verifier: String) = withContext(Dispatchers.IO) {
        try {
            client.accessToken(token, verifier)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "access-token-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun getUsers(userIds: String) = withContext(Dispatchers.IO) {
        try {
            client.getUsers(userIds)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "get-users-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun getFollowersFollowing(type: ContactType, cursor: Long) = withContext(Dispatchers.IO) {
        try {
            client.getFollowersFollowing(type, cursor)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "get-followers-following-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun getAllFollowersFollowing(type: ContactType): List<Long>? {
        val contacts = mutableListOf<Long>()
        var cursor = -1L
        while (cursor != 0L) {
            val (ids, nextCursor) = getFollowersFollowing(type, cursor)?.body()
                ?: return null
            contacts += ids
            cursor = nextCursor
        }
        return contacts
    }

    suspend fun getMutuals(): List<TwitterUser>? = coroutineScope {
        val (followers, following) = listOf(
            async { getAllFollowersFollowing(ContactType.FOLLOWERS) },
            async { getAllFollowersFollowing(ContactType.FRIENDS) }
        ).map {
            it.await() ?: return@coroutineScope null
        }

        followers.intersect(following).chunked(100).map {
            async { getUsers(it.joinToString(","))?.body() }
        }.flatMap {
            it.await() ?: return@coroutineScope null
        }
    }
}
