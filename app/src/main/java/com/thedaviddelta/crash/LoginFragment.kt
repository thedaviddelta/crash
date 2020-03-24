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
import android.content.Context
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.thedaviddelta.crash.api.Api
import com.thedaviddelta.crash.model.MastodonAccessToken
import com.thedaviddelta.crash.model.MastodonAppCredentials
import kotlinx.android.synthetic.main.dialog_instance.view.*
import kotlinx.android.synthetic.main.fragment_login.*
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.URLEncoder

class LoginFragment : Fragment() {

    companion object {
        private const val TW_TEMP_TOKEN = "twTempToken"
        private const val MASTO_DOMAIN = "mastoDomain"
        private const val MASTO_CLIENT_ID = "mastoClientId"
        private const val MASTO_CLIENT_SECRET = "mastoClientSecret"
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val activity = activity!!
        val sharedPreferences = activity.getSharedPreferences(
            "${BuildConfig.APPLICATION_ID}.sharedprefs.login", Context.MODE_PRIVATE
        )

        arguments?.run {
            val platform = getString("platform") ?: return@run
            when(platform) {
                "twitter" -> {
                    val tempToken = getString("tw_oauth_token") ?: return@run
                    val verifier = getString("tw_oauth_verifier") ?: return@run

                    val storedTempToken = sharedPreferences.getString(TW_TEMP_TOKEN, "")
                    sharedPreferences.edit().clear().apply()
                    if (tempToken != storedTempToken)
                        return@run Toast.makeText(activity, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()

                    loading = true
                    Api.Twitter.authClient.accessToken(tempToken, verifier).enqueue(object : Callback<ResponseBody> {
                        override fun onResponse(call: Call<ResponseBody>, response: Response<ResponseBody>) {
                            loading = false
                            val body = response.body()
                                ?: return Toast.makeText(
                                    activity,
                                    "${getString(R.string.login_error_unexpected)} (${response.code()})",
                                    Toast.LENGTH_LONG
                                ).show()

                            val (token, secret, userId, screenName) = body.string().split('&').map { it.split('=')[1] }
                            Log.i("Login", screenName)
                        }
                        override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                            loading = false
                            Toast.makeText(activity, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()
                        }
                    })
                }
                "mastodon" -> {
                    val code = getString("masto_oauth_code") ?: return@run

                    val domain = sharedPreferences.getString(MASTO_DOMAIN, null) ?: return@run
                    val clientId = sharedPreferences.getString(MASTO_CLIENT_ID, null) ?: return@run
                    val clientSecret = sharedPreferences.getString(MASTO_CLIENT_SECRET, null) ?: return@run
                    sharedPreferences.edit().clear().apply()

                    loading = true
                    Api.Mastodon.authClient.requestToken(domain, clientId, clientSecret, code).enqueue(object : Callback<MastodonAccessToken> {
                        override fun onResponse(call: Call<MastodonAccessToken>, response: Response<MastodonAccessToken>) {
                            loading = false
                            val accessToken = response.body()?.accessToken
                                ?: return Toast.makeText(
                                    activity,
                                    "${getString(R.string.login_error_unexpected)} (${response.code()})",
                                    Toast.LENGTH_LONG
                                ).show()
                            Log.i("Login", accessToken)
                        }
                        override fun onFailure(call: Call<MastodonAccessToken>, t: Throwable) {
                            loading = false
                            Toast.makeText(activity, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()
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
                            activity,
                            "${getString(R.string.login_error_unexpected)} (${response.code()})",
                            Toast.LENGTH_LONG
                        ).show()

                    val (tempToken, tempSecret, callbackConfirmed) = body.string().split('&').map { it.split('=')[1] }
                    if (callbackConfirmed.toBoolean().not())
                        Log.w("Login", "Callback not confirmed")

                    sharedPreferences.edit()
                        .putString(TW_TEMP_TOKEN, tempToken)
                        .apply()
                    val uri = "https://api.twitter.com/oauth/authenticate?oauth_token=${tempToken}".toUri()
                    launchOnBrowser(uri)
                }
                override fun onFailure(call: Call<ResponseBody>, t: Throwable) {
                    loading = false
                    Toast.makeText(activity, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()
                }
            })
        }

        button_login_mastodon.setOnClickListener {
            val parentViewGroup: ViewGroup? = null
            val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_instance, parentViewGroup)

            val dialog = MaterialAlertDialogBuilder(activity)
                .setView(dialogView)
                .create()
            dialog.show()

            val layout = dialogView.layout_login_dialog
            val editText = dialogView.edittext_login_dialog_domain
            val button = dialogView.button_login_dialog_ok

            button.setOnClickListener okBtn@{
                val domain = editText.text.toString()
                    .replace(Regex("https?://"), "")

                if(domain.isEmpty()){
                    layout.error = getString(R.string.login_dialog_domain_invalid)
                    return@okBtn
                }

                Api.Mastodon.authClient.createApp(domain, getString(R.string.app_name)).enqueue(object : Callback<MastodonAppCredentials> {
                    override fun onResponse(call: Call<MastodonAppCredentials>, response: Response<MastodonAppCredentials>) {
                        val body = response.body()
                            ?: if(response.code() == 404) {
                                layout.error = getString(R.string.login_dialog_domain_invalid)
                                return
                            } else {
                                Toast.makeText(
                                    activity,
                                    "${getString(R.string.login_error_unexpected)} (${response.code()})",
                                    Toast.LENGTH_LONG
                                ).show()
                                return dialog.dismiss()
                            }

                        sharedPreferences.edit()
                            .putString(MASTO_DOMAIN, domain)
                            .putString(MASTO_CLIENT_ID, body.clientId)
                            .putString(MASTO_CLIENT_SECRET, body.clientSecret)
                            .apply()

                        val params = mapOf(
                            "client_id" to body.clientId,
                            "redirect_uri" to Api.Mastodon.CALLBACK,
                            "response_type" to "code"
                        ).map { (k,v) ->
                            "$k=${URLEncoder.encode(v, "UTF-8")}"
                        }.joinToString("&")

                        val uri = "https://$domain/oauth/authorize?${params}".toUri()
                        dialog.dismiss()
                        launchOnBrowser(uri)
                    }
                    override fun onFailure(call: Call<MastodonAppCredentials>, t: Throwable) {
                        layout.error = getString(R.string.login_dialog_domain_invalid)
                    }
                })
            }
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
