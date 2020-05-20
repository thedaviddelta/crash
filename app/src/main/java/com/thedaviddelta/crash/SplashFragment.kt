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
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.tasks.Tasks
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.util.SecureFile
import com.thedaviddelta.crash.util.SnackbarBuilder
import kotlinx.coroutines.*
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

/**
 * [Fragment] to be shown while loading
 */
class SplashFragment : Fragment() {

    companion object {
        /** Cookies agreement [secure shared preferences file][SecureFile] name */
        private const val SHARED_PREFS_NAME = "${BuildConfig.APPLICATION_ID}.cookies"

        private const val COOKIES_AGREED = "agreed-v1"
    }

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
            val sharedPrefs = SecureFile.with(activity).sharedPreferences(SHARED_PREFS_NAME)
                ?: return@launch SnackbarBuilder(requireView())
                    .error(R.string.error_unexpected)
                    .during(Snackbar.LENGTH_INDEFINITE)
                    .buildAndShow()

            sharedPrefs.getBoolean(COOKIES_AGREED, false).takeIf { !it }?.let {
                suspendCoroutine<Unit> {
                    MaterialAlertDialogBuilder(activity)
                        .setTitle(R.string.splash_cookies_title)
                        .setMessage(R.string.splash_cookies_msg)
                        .setPositiveButton(R.string.splash_cookies_agree) { dialog, _ ->
                            dialog.dismiss()
                            it.resume(Unit)
                        }.create()
                        .also { it.setCancelable(false) }
                        .show()
                }.let {
                    sharedPrefs.edit().putBoolean(COOKIES_AGREED, true).apply()
                }
            }

            MobileAds.initialize(activity)

            Firebase.auth.signInAnonymously().runCatching {
                withContext(Dispatchers.IO) {
                    Tasks.await(this@runCatching)
                }
            }.onFailure {
                return@launch SnackbarBuilder(requireView())
                    .error(R.string.error_unexpected)
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
                        .buildAndShow()
                }
            }

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
