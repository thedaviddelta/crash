/*
 * cr@sh - Secret crush matcher for social networks
 * Copyright (C) 2020 theda
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

package com.thedaviddelta.crash.api

import android.util.Log
import com.thedaviddelta.crash.BuildConfig
import io.github.cdimascio.dotenv.dotenv
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object Api {
    private const val CALLBACK = "tdd-oauth://${BuildConfig.APPLICATION_ID}"

    val dotenv = dotenv {
        directory = "/assets"
        filename = "env"
    }

    object Twitter {
        val CONSUMER_KEY = dotenv["TWITTER_CONSUMER_KEY"] ?: ""
        val CONSUMER_SECRET = dotenv["TWITTER_CONSUMER_SECRET"] ?: ""

        const val CALLBACK = "${Api.CALLBACK}/twitter"

        val authClient by lazy {
            Retrofit.Builder()
                .baseUrl("https://api.twitter.com")
                .client(
                    OkHttpClient().newBuilder().addInterceptor {
                        val req = it.request()
                        val authHeader = twitterAuthorization(req)
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
    }
}