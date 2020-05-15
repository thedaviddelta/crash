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
import android.content.pm.ActivityInfo
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.gms.tasks.Tasks
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.util.SnackbarBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class SplashFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_splash, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity()

        @SuppressLint("SourceLockedOrientationActivity")
        activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

        lifecycleScope.launch {
            Firebase.auth.signInAnonymously().runCatching {
                withContext(Dispatchers.IO) {
                    Tasks.await(this@runCatching)
                }
            }.onFailure {
                return@launch SnackbarBuilder(requireView())
                    .error(R.string.login_error_unexpected)
                    .during(Snackbar.LENGTH_INDEFINITE)
                    .buildAndShow()
            }

            ImageRepository.initializeCache(activity)
            Accounts.apply {
                initialize(activity).takeIf { !it }?.let {
                    return@launch SnackbarBuilder(requireView())
                        .error(R.string.splash_error_read)
                        .during(Snackbar.LENGTH_INDEFINITE)
                        .buildAndShow()
                }
                update().takeIf { !it }?.let {
                    SnackbarBuilder(requireView())
                        .error(R.string.splash_error_update)
                        .during(1250)
                        .buildAndShow()
                }
            }

            delay(1500L)

            findNavController().navigate(
                Accounts.current
                    ?.let { R.id.action_splash_to_main }
                    ?: R.id.action_splash_to_login
            ).also {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }
}
