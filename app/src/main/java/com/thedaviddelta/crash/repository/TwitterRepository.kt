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
import com.thedaviddelta.crash.model.CrushType
import com.thedaviddelta.crash.model.TwitterAccount
import com.thedaviddelta.crash.model.TwitterUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * Repository for Retrofit's Twitter service, following [Jetpack App Architecture](https://developer.android.com/jetpack/docs/guide)
 */
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

    /**
     * Repository wrapper for [TwitterApi.requestToken]
     *
     * @return [TwitterApi.requestToken]'s result, or `null` if a network error occurred
     */
    suspend fun requestToken(callback: String = CALLBACK) = withContext(Dispatchers.IO) {
        try {
            client.requestToken(callback)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "request-token-error ${e.localizedMessage}")
            null
        }
    }

    /**
     * Repository wrapper for [TwitterApi.accessToken]
     *
     * @return [TwitterApi.accessToken]'s result, or `null` if a network error occurred
     */
    suspend fun accessToken(token: String, verifier: String) = withContext(Dispatchers.IO) {
        try {
            client.accessToken(token, verifier)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "access-token-error ${e.localizedMessage}")
            null
        }
    }

    /**
     * Repository wrapper for [TwitterApi.getUsers]
     *
     * @return [TwitterApi.getUsers]'s result, or `null` if a network error occurred
     */
    suspend fun getUsers(userIds: String) = withContext(Dispatchers.IO) {
        try {
            client.getUsers(userIds)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "get-users-error ${e.localizedMessage}")
            null
        }
    }

    /**
     * Repository wrapper for [TwitterApi.getFollowersFollowing]
     *
     * @return [TwitterApi.getFollowersFollowing]'s result, or `null` if a network error occurred
     */
    suspend fun getFollowersFollowing(type: ContactType, cursor: Long) = withContext(Dispatchers.IO) {
        try {
            client.getFollowersFollowing(type, cursor)
        } catch (e: Exception) {
            Log.e("TwitterRepository", "get-followers-following-error ${e.localizedMessage}")
            null
        }
    }

    /**
     * Iterates over [getFollowersFollowing] and returns list of all followers/following
     *
     * @param type retrieve [followers][ContactType.FOLLOWERS] or [following][ContactType.FRIENDS] (called *friends* in Twitter API)
     * @return full list of contact's IDs, or `null` if an error occurred
     */
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

    /**
     * Retrieves info from [current Account][Accounts.current]'s `followers`, `following`, `crushes` & `crushedBy`,
     * filters by '`(followers ⋂ following) ⋃ crushes`', and get list of [TwitterUser] objects
     *
     * @return list of [TwitterUser], or `null` if an error occurred
     */
    suspend fun getMutuals(): List<TwitterUser>? = coroutineScope {
        val id = Accounts.current?.id ?: return@coroutineScope null

        val (followers, following, crushes, crushedBy) = listOf(
            async { getAllFollowersFollowing(ContactType.FOLLOWERS) },
            async { getAllFollowersFollowing(ContactType.FRIENDS) },
            async { FirestoreRepository.getTwitterCrushes(id) },
            async { FirestoreRepository.getTwitterCrushedBy(id) }
        ).map {
            it.await() ?: return@coroutineScope null
        }

        followers.intersect(following).union(crushes).chunked(100).map {
            async { getUsers(it.joinToString(","))?.body() }
        }.flatMap {
            it.await() ?: return@coroutineScope null
        }.map {
            if (it.id !in crushes)
                return@map it.apply { crush = CrushType.NONE }
            if (it.id !in crushedBy)
                return@map it.apply { crush = CrushType.CRUSH }
            it.apply { crush = CrushType.MUTUAL }
        }
    }
}
