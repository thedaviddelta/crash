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

import com.thedaviddelta.crash.model.*
import com.thedaviddelta.crash.util.Accounts
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.*

/**
 * Retrofit service structure for Twitter API
 */
interface TwitterApi {
    /**
     * Requests temporary token inside OAuth 1.0 flow (1st step)
     *
     * [Twitter Developers](https://developer.twitter.com/en/docs/basics/authentication/api-reference/request_token)
     *
     * @param callback URL to be redirected to after successful authentication in 2nd step
     * @return Retrofit response with all three `oauth_token`, `oauth_token_secret` & `callback_confirmed`
     * wrapped inside a [ResponseBody]
     */
    @FormUrlEncoded
    @POST("/oauth/request_token")
    suspend fun requestToken(
        @Field("oauth_callback") callback: String
    ): Response<ResponseBody>

    /**
     * Requests final user token inside OAuth 1.0 flow (3rd & last step)
     *
     * [Twitter Developers](https://developer.twitter.com/en/docs/basics/authentication/api-reference/access_token)
     *
     * @param token temporary token obtained in 1st step and verified in 2nd
     * @param verifier verifying code obtained in 2nd step
     * @return Retrofit response with all four `oauth_token`, `oauth_token_secret`, `user_id` & `screen_name`
     * wrapped inside a [ResponseBody]
     */
    @FormUrlEncoded
    @POST("/oauth/access_token")
    suspend fun accessToken(
        @Field("oauth_token") token: String,
        @Field("oauth_verifier") verifier: String
    ): Response<ResponseBody>

    /**
     * Retrieves full [TwitterUser] objects from [IDs][userIds]
     *
     * [Twitter Developers](https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-users-lookup)
     *
     * @param userIds list of userIds to query (parsed as comma-separated string)
     * @return Retrofit response with list of [TwitterUser]
     */
    @FormUrlEncoded
    @POST("/1.1/users/lookup.json")
    suspend fun getUsers(
        @Field("user_id") userIds: String
    ): Response<List<TwitterUser>>

    /**
     * Retrieves IDs from [current Account][Accounts.current]'s followers/following
     *
     * [Twitter Developers (followers)](https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-followers-ids)
     *
     * [Twitter Developers (following)](https://developer.twitter.com/en/docs/accounts-and-users/follow-search-get-users/api-reference/get-friends-ids)
     *
     * @param type retrieve [followers][ContactType.FOLLOWERS] or [following][ContactType.FRIENDS] (called *friends* in Twitter API)
     * @param cursor last ID retrieved on previous request (use `-1` if first), because of limit of 5000 IDs per request
     * @return Retrofit response with both list of [ids][TwitterFollowersFollowing.ids]
     * & [next_cursor][TwitterFollowersFollowing.nextCursor]
     * wrapped inside a [Data Class][TwitterFollowersFollowing]
     */
    @GET("/1.1/{type}/ids.json")
    suspend fun getFollowersFollowing(
        @Path("type") type: ContactType,
        @Query("cursor") cursor: Long
    ): Response<TwitterFollowersFollowing>
}
