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

import android.content.ActivityNotFoundException
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.net.toUri
import com.thedaviddelta.crash.api.Api
import kotlinx.android.synthetic.main.fragment_login.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class LoginFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        arguments?.run {
            val platform = getString("platform") ?: return@run
            loading = true
            when(platform) {
                "twitter" -> {
                    val tempToken = getString("tw_oauth_token") ?: return@run
                    val verifier = getString("tw_oauth_verifier") ?: return@run

                    Api.Twitter.authClient.accessToken(tempToken, verifier).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            loading = false
                            val body = response.body()
                                ?: return Toast.makeText(
                                    activity!!,
                                    "${getString(R.string.login_error_unexpected)} (${response.code()})",
                                    Toast.LENGTH_LONG
                                ).show()

                            val (token, secret, userId, screenName) = body.string().split('&').map { it.split('=')[1] }
                            Log.i("Login", screenName)
                        }
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            loading = false
                            Toast.makeText(activity!!, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()
                        }
                    })
                }
            }
        }

        button_login_twitter.setOnClickListener {
            loading = true
            Api.Twitter.authClient.requestToken().enqueue(object : Callback<ResponseBody> {
                override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                    loading = false
                    val body = response.body()
                        ?: return Toast.makeText(
                            activity!!,
                            "${getString(R.string.login_error_unexpected)} (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()

                    val (tempToken, tempSecret, callbackConfirmed) = body.string().split('&').map { it.split('=')[1] }
                    if (callbackConfirmed.toBoolean().not())
                        Log.w("Login", "Callback not confirmed")

                    val uri = "https://api.twitter.com/oauth/authenticate?oauth_token=${tempToken}".toUri()
                    launchOnBrowser(uri)
                }

                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    loading = false
                    Toast.makeText(activity!!, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()
                }
            })
        }
    }

    private fun launchOnBrowser(uri: Uri) {
        val activity = activity!!
        val color = resources.getColor(R.color.red200, null)
        try {
            CustomTabsIntent.Builder()
                .setToolbarColor(color)
                .build()
                .launchUrl(activity, uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(activity, R.string.login_error_browser, Toast.LENGTH_LONG).show()
        }
    }

    private var loading = false
        set(value) {
            linearlayout_login_buttons.visibility = if (value) View.GONE else View.VISIBLE
            linearlayout_login_loading.visibility = if (value) View.VISIBLE else View.GONE
            field = value
        }
}
