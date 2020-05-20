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

package com.thedaviddelta.crash.model

/**
 * Local [Account] model for a registered [Twitter User][TwitterUser]
 *
 * @property token OAuth 1.0 token
 * @property secret OAuth 1.0 secret
 */
data class TwitterAccount(
    override val id: Long,
    override var username: String,
    override var fullName: String,
    override var avatarUrl: String,
    override var bannerUrl: String?,
    val token: String,
    val secret: String
) : Account {
    companion object {
        /**
         * Creates a new [TwitterAccount] from [TwitterUser] info and OAuth 1.0 tokens
         *
         * @param user the [TwitterUser] to take info from
         * @param token OAuth 1.0 token
         * @param secret OAuth 1.0 secret
         * @return a new [TwitterAccount] instance
         */
        fun from(
            user: TwitterUser,
            token: String,
            secret: String
        ): TwitterAccount {
            return TwitterAccount(
                id = user.id,
                username = user.username,
                fullName = user.fullName,
                avatarUrl = user.avatarUrl,
                bannerUrl = user.bannerUrl,
                token = token,
                secret = secret
            )
        }
    }

    /**
     * Updates account's info from [TwitterUser]
     *
     * @param user the [TwitterUser] to take info from
     * @return the current [account][TwitterAccount] instance, or `null` if [id] doesn't match
     */
    fun updateFrom(user: TwitterUser): TwitterAccount? {
        return this.takeIf {
            it.id == user.id
        }?.apply {
            this.username = user.username
            this.fullName = user.fullName
            this.avatarUrl = user.avatarUrl
            this.bannerUrl = user.bannerUrl
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other)
            return true
        if (other !is TwitterAccount)
            return false
        return this.id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
