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
import com.thedaviddelta.crash.model.MastodonUser
import com.thedaviddelta.crash.util.Accounts
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service structure for Mastodon API
 */
interface MastodonApi {
    /**
     * Creates new application to obtain OAuth2 credentials
     *
     * [Mastodon documentation](https://docs.joinmastodon.org/methods/apps/)
     *
     * @param domain instance of the logging user
     * @param clientName name of the application
     * @param redirectUris list of callback URLs (parsed as comma-separated string)
     * @return Retrofit response with all three [client_id][MastodonAppCredentials.clientId],
     * [client_secret][MastodonAppCredentials.clientSecret]
     * & [redirect_uri][MastodonAppCredentials.redirectUri]
     * wrapped inside a [Data Class][MastodonAppCredentials]
     */
    @FormUrlEncoded
    @POST("/api/v1/apps")
    suspend fun createApp(
        @Header("domain") domain: String,
        @Field("client_name") clientName: String,
        @Field("redirect_uris") redirectUris: String
    ): Response<MastodonAppCredentials>

    /**
     * Requests final OAuth2 user token
     *
     * [Mastodon documentation](https://docs.joinmastodon.org/methods/apps/oauth/)
     *
     * @param domain instance of the logging user
     * @param clientId application ID obtained on app creation
     * @param clientSecret application secret obtained on app creation
     * @param code temporary authorization code obtained during browser authentication
     * @param grantType type of app permissions (`authorization_code` for user-level, `client_credentials` for app-level)
     * @param redirectUris callback URL (must match one of the declared on app registration)
     * @return Retrofit response with the user Bearer token ([access_token][MastodonAccessToken.accessToken])
     * wrapped inside a [Data Class][MastodonAccessToken]
     */
    @FormUrlEncoded
    @POST("/oauth/token")
    suspend fun requestToken(
        @Header("domain") domain: String,
        @Field("client_id") clientId: String,
        @Field("client_secret") clientSecret: String,
        @Field("code") code: String,
        @Field("grant_type") grantType: String,
        @Field("redirect_uri") redirectUris: String
    ): Response<MastodonAccessToken>

    /**
     * Tests the access_token & retrieves [User][MastodonUser]'s info without knowing the ID
     *
     * This is needed because *Mastodon*'s OAuth2 flow doesn't retrieve `userId` at all
     *
     * [Mastodon documentation](https://docs.joinmastodon.org/methods/accounts/)
     *
     * @param domain instance of the logging user
     * @param bearer OAuth2 user token
     * @return Retrofit response with [MastodonUser] object
     */
    @GET("/api/v1/accounts/verify_credentials")
    suspend fun verifyCredentials(
        @Header("domain") domain: String,
        @Header("Authorization") bearer: String
    ): Response<MastodonUser>

    /**
     * Retrieves [MastodonUser] object from given [ID][userId] and [domain]
     *
     * [Mastodon documentation](https://docs.joinmastodon.org/methods/accounts/)
     *
     * @param domain instance of the desired user
     * @param userId ID of the desired user
     * @return Retrofit response with [MastodonUser] object
     */
    @GET("/api/v1/accounts/{id}")
    suspend fun getUser(
        @Header("domain") domain: String,
        @Path("id") userId: Long
    ): Response<MastodonUser>

    /**
     * Retrieves list of [MastodonUser] objects from [current Account][Accounts.current]'s followers/following
     *
     * [Mastodon documentation](https://docs.joinmastodon.org/methods/accounts/)
     *
     * @param type retrieve [followers][ContactType.FOLLOWERS] or [following][ContactType.FOLLOWING]
     * @param userId [current Account][Accounts.current]'s userId
     * @param limit max of [MastodonUser] objects from request
     * @param cursor last relationship ID retrieved on previous request (leave empty if first), because of limit of 80 objects per request
     * @return Retrofit response with list of [MastodonUser]
     */
    @GET("/api/v1/accounts/{id}/{type}")
    suspend fun getFollowersFollowing(
        @Path("type") type: ContactType,
        @Path("id") userId: Long,
        @Query("limit") limit: Int,
        @Query("max_id") cursor: Long?
    ): Response<List<MastodonUser>>
}
