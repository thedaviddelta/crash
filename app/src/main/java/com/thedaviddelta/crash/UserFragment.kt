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

package com.thedaviddelta.crash

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import com.thedaviddelta.crash.model.MastodonUser
import com.thedaviddelta.crash.model.User
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.SnackbarBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*

class UserFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val actWindow = requireActivity().window
        val statusColor = actWindow.statusBarColor

        toolbar_user.setNavigationOnClickListener {
            findNavController().navigateUp()
            actWindow.statusBarColor = statusColor
        }

        val user = arguments?.getSerializable("user")?.let {
            if (it is User) it else null
        } ?: return run {
            findNavController().navigateUp()
            SnackbarBuilder(requireActivity().nav_host_fragment.requireView())
                .error(R.string.user_error_empty)
                .buildAndShow()
        }

        textview_user_fullname.text = user.fullName
        textview_user_username.text = "@${user.username}"

        if (user is MastodonUser && user.fullName.isBlank())
            textview_user_fullname.text = user.username

        ImageRepository.apply {
            loadImage(user.avatarUrl, imageview_user_avatar::setImageBitmap)
            user.bannerUrl?.let { url ->
                loadImage(url) {
                    imageview_user_banner?.apply {
                        Palette.from(it).generate { palette ->
                            actWindow.statusBarColor = palette?.dominantSwatch?.rgb ?: return@generate
                        }
                        setImageBitmap(it)
                    }
                }
            }
        }
    }
}
