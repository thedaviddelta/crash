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
}
