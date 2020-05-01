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

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import okhttp3.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Url
import java.util.concurrent.TimeUnit

object ImageRepository {
    private interface Api {
        @GET
        fun getImage(
            @Url url: String
        ): Call<ResponseBody>
    }

    private const val CACHE_SIZE: Long = 1024 * 1024 * 64

    private var cache: Cache? = null

    fun initializeCache(context: Context) {
        cache = Cache(context.cacheDir, CACHE_SIZE)
    }

    private val client: Api by lazy {
        Retrofit.Builder()
            .baseUrl("https://url.placeholder")
            .client(
                cache?.let { cache ->
                    OkHttpClient().newBuilder()
                        .cache(cache)
                        .addInterceptor {
                            val newReq = it.request()
                                .newBuilder()
                                .cacheControl(
                                    CacheControl.Builder()
                                        .maxAge(365, TimeUnit.DAYS)
                                        .immutable()
                                        .build()
                                ).build()
                            it.proceed(newReq)
                        }.build()
                } ?: OkHttpClient()
            )
            .build()
            .create(Api::class.java)
    }

    fun loadImage(url: String, onComplete: (Bitmap) -> Unit) {
        client.getImage(url).enqueue(object : Callback<ResponseBody> {
            override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                response.body()?.bytes()?.let {
                    onComplete(it.bitmap)
                }
            }
            override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                Log.e("ImageRepository", "get-image-error ${t.localizedMessage}")
            }
        })
    }

    private val ByteArray.bitmap
        get() = BitmapFactory.decodeByteArray(this, 0, this.size)
}
