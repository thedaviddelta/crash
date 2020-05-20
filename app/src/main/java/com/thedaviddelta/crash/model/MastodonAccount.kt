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
 * Local [Account] model for a registered [Mastodon User][MastodonUser]
 *
 * @property domain Instance location
 * @property bearer OAuth2 user token
 */
data class MastodonAccount(
    override val id: Long,
    override var username: String,
    override var fullName: String,
    override var avatarUrl: String,
    override var bannerUrl: String?,
    val domain: String,
    val bearer: String
) : Account {
    companion object {
        /**
         * Creates a new [MastodonAccount] from [MastodonUser] info and OAuth2 user token
         *
         * @param user the [MastodonUser] to take info from
         * @param bearer OAuth2 user token
         * @return a new [MastodonAccount] instance
         */
        fun from(
            user: MastodonUser,
            bearer: String
        ): MastodonAccount {
            return MastodonAccount(
                id = user.id,
                username = user.username,
                fullName = user.fullName,
                avatarUrl = user.avatarUrl,
                bannerUrl = user.bannerUrl,
                domain = user.domain,
                bearer = bearer
            )
        }
    }

    /**
     * Updates account's info from [MastodonUser]
     *
     * @param user the [MastodonUser] to take info from
     * @return the current [account][MastodonAccount] instance, or `null` if [id] or [domain] don't match
     */
    fun updateFrom(user: MastodonUser): MastodonAccount? {
        return this.takeIf {
            it.id == user.id
            && it.domain == user.domain
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
        if (other !is MastodonAccount)
            return false
        return this.id == other.id
            && this.domain == other.domain
    }

    override fun hashCode(): Int = 31 * id.hashCode() + domain.hashCode()
}
