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
import androidx.core.net.toUri
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.BuildConfig
import com.thedaviddelta.crash.api.ContactType
import com.thedaviddelta.crash.api.MastodonApi
import com.thedaviddelta.crash.model.MastodonAccount
import com.thedaviddelta.crash.model.MastodonUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object MastodonRepository {
    private const val CALLBACK = "tdd-oauth://${BuildConfig.APPLICATION_ID}/mastodon"

    private val client: MastodonApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://mastodon.placeholder")
            .client(
                OkHttpClient().newBuilder().addInterceptor {
                    val req = it.request()

                    val domain = req.header("domain")
                        ?: Accounts.current?.let { acc ->
                            if (acc is MastodonAccount)
                                acc.domain
                            else null
                        } ?: return@addInterceptor it.proceed(req)

                    val url = req.url().newBuilder().host(domain).build()
                    val newReq = req
                        .newBuilder()
                        .url(url)
                        .removeHeader("domain")
                        .build()
                    it.proceed(newReq)
                }.addInterceptor {
                    val req = it.request()

                    val headerExists = req.header("Authorization") != null
                    val bearer = Accounts.current?.let { acc ->
                        if (acc is MastodonAccount)
                            "Bearer ${acc.bearer}"
                        else null
                    }
                    if (headerExists || bearer == null)
                        return@addInterceptor it.proceed(req)

                    val newReq = req
                        .newBuilder()
                        .header("Authorization", bearer)
                        .build()
                    it.proceed(newReq)
                }.build()
            )
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MastodonApi::class.java)
    }

    suspend fun createApp(domain: String, clientName: String, redirectUris: String = CALLBACK) = withContext(Dispatchers.IO) {
        try {
            client.createApp(domain, clientName, redirectUris)
        } catch (e: Exception) {
            Log.e("MastodonRepository", "create-app-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun requestToken(domain: String, clientId: String, clientSecret: String, code: String,
            grantType: String = "authorization_code", redirectUris: String = CALLBACK) = withContext(Dispatchers.IO) {
        try {
            client.requestToken(domain, clientId, clientSecret, code, grantType, redirectUris)
        } catch (e: Exception) {
            Log.e("MastodonRepository", "request-token-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun verifyCredentials(domain: String, bearer: String) = withContext(Dispatchers.IO) {
        try {
            client.verifyCredentials(domain, bearer)
        } catch (e: Exception) {
            Log.e("MastodonRepository", "verify-credentials-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun getFollowersFollowing(type: ContactType, id: Long, limit: Int, cursor: Long?) = withContext(Dispatchers.IO) {
        try {
            client.getFollowersFollowing(type, id, limit, cursor)
        } catch (e: Exception) {
            Log.e("MastodonRepository", "verify-credentials-error ${e.localizedMessage}")
            null
        }
    }

    suspend fun getAllFollowersFollowing(type: ContactType, id: Long): List<MastodonUser>? {
        suspend fun contacts(
            cursor: Long? = null
        ): List<MastodonUser>? {
            val res = getFollowersFollowing(type, id, Int.MAX_VALUE, cursor)
            val list = res?.body() ?: return null

            val link = res.headers().get("link")
            val nextUrl = link?.split(",")?.getOrNull(0)?.split(";")?.getOrNull(0)
            val newCursor = nextUrl?.trim('<', '>')?.toUri()?.getQueryParameter("max_id")?.toLongOrNull()
                ?: return list

            return contacts(newCursor)?.union(list)?.toList()
        }
        return contacts(null)
    }

    suspend fun getMutuals(): List<MastodonUser>? {
        val id = Accounts.current?.id ?: return null

        val followers = getAllFollowersFollowing(ContactType.FOLLOWERS, id)
            ?: return null
        val following = getAllFollowersFollowing(ContactType.FOLLOWING, id)
            ?: return null

        return followers.intersect(following).toList()
    }
}
