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

package com.thedaviddelta.crash.api

import com.thedaviddelta.crash.model.MastodonAccessToken
import com.thedaviddelta.crash.model.MastodonAppCredentials
import retrofit2.Call
import retrofit2.http.*

interface MastodonApi {
    @FormUrlEncoded
    @POST("/api/v1/apps")
    fun createApp(
        @Header("domain") domain: String,
        @Field("client_name") clientName: String,
        @Field("redirect_uris") redirectUris: String = Api.Mastodon.CALLBACK
    ): Call<MastodonAppCredentials>

    @FormUrlEncoded
    @POST("/oauth/token")
    fun requestToken(
        @Header("domain") domain: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String = "authorization_code",
        @Field("redirect_uri") redirectUris: String = Api.Mastodon.CALLBACK
    ): Call<MastodonAccessToken>
}
