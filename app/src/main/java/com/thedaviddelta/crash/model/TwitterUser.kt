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

import com.google.gson.annotations.SerializedName

data class TwitterUser(
    @SerializedName("id") override val id: Long,
    @SerializedName("screen_name") override val username: String,
    @SerializedName("name") override val fullName: String,
    @SerializedName("profile_image_url_https") override val avatarUrl: String,
    @SerializedName("profile_banner_url") override val bannerUrl: String
) : User