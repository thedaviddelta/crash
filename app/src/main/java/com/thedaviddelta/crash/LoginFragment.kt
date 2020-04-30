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
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputLayout
import com.thedaviddelta.crash.util.SecureFile
import com.thedaviddelta.crash.model.*
import com.thedaviddelta.crash.repository.*
import com.thedaviddelta.crash.util.Accounts
import kotlinx.android.synthetic.main.dialog_instance.view.*
import kotlinx.android.synthetic.main.fragment_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.URLEncoder
import java.util.*

class LoginFragment : Fragment() {

    companion object {
        private const val SHARED_PREFS_NAME = "${BuildConfig.APPLICATION_ID}.login"

        private const val TW_TEMP_TOKEN = "twTempToken"
        private const val MASTO_DOMAIN = "mastoDomain"
        private const val MASTO_CLIENT_ID = "mastoClientId"
        private const val MASTO_CLIENT_SECRET = "mastoClientSecret"
    }

    private lateinit var fragActivity: FragmentActivity

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_login, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        fragActivity = requireActivity()
        val secureFile = SecureFile.with(fragActivity)

        toolbar_login.setNavigationOnClickListener {
            findNavController().navigateUp()
        }

        arguments?.run {
            getBoolean("add").takeIf { !it }?.let {
                toolbar_login.navigationIcon = null
            }
            val platform = getString("platform") ?: return@run
            when(platform) {
                "twitter" -> {
                    val tempToken = getString("tw_oauth_token") ?: return@run
                    val verifier = getString("tw_oauth_verifier") ?: return@run

                    lifecycleScope.launch {
                        loading = true

                        val storedTempToken = withContext(Dispatchers.IO) {
                            secureFile.sharedPreferences(SHARED_PREFS_NAME)?.let {
                                val token = it.getString(TW_TEMP_TOKEN, "")
                                it.edit().clear().apply()
                                token
                            }
                        }

                        if (tempToken != storedTempToken)
                            return@launch error

                        val body = TwitterRepository.accessToken(tempToken, verifier)
                            ?.let { res ->
                                withContext(Dispatchers.IO) {
                                    res.body()?.string()
                                } ?: return@launch error
                            } ?: return@launch netError

                        val (token, secret, userId) = body.split('&').map { it.split('=')[1] }

                        val user = TwitterRepository.getUsers(userId)
                            ?.let { res ->
                                res.body()?.firstOrNull()
                                    ?: return@launch error
                            } ?: return@launch netError

                        Accounts.add(TwitterAccount.from(user, token, secret))
                            .let { if (!it) return@launch error }

                        loading = false
                        findNavController().navigate(R.id.action_login_to_main)
                    }
                }
                "mastodon" -> {
                    val code = getString("masto_oauth_code") ?: return@run

                    lifecycleScope.launch {
                        loading = true

                        val (domain, clientId, clientSecret) = withContext(Dispatchers.IO) {
                            secureFile.sharedPreferences(SHARED_PREFS_NAME)?.let {
                                val domain = it.getString(MASTO_DOMAIN, null) ?: return@withContext null
                                val clientId = it.getString(MASTO_CLIENT_ID, null) ?: return@withContext null
                                val clientSecret = it.getString(MASTO_CLIENT_SECRET, null) ?: return@withContext null
                                it.edit().clear().apply()
                                Triple(domain, clientId, clientSecret)
                            }
                        } ?: return@launch error

                        val bearer = MastodonRepository.requestToken(domain, clientId, clientSecret, code)
                            ?.let { res ->
                                res.body()?.accessToken
                                    ?: return@launch error
                            } ?: return@launch netError

                        val user = MastodonRepository.verifyCredentials(domain, "Bearer $bearer")
                            ?.let { res ->
                                res.body()
                                    ?: return@launch error
                            } ?: return@launch netError

                        Accounts.add(MastodonAccount.from(user, bearer))
                            .let { if (!it) return@launch error }

                        loading = false
                        findNavController().navigate(R.id.action_login_to_main)
                    }
                }
            }
        }

        button_login_twitter.setOnClickListener {
            lifecycleScope.launch {
                loading = true

                val body = TwitterRepository.requestToken()
                    ?.let { res ->
                        withContext(Dispatchers.IO) {
                            res.body()?.string()
                        } ?: return@launch error
                    } ?: return@launch netError

                val (tempToken, tempSecret, callbackConfirmed) = body.split('&').map { it.split('=')[1] }
                if (callbackConfirmed.toBoolean().not())
                    Log.w("Login", "Callback not confirmed")

                withContext(Dispatchers.IO) {
                    secureFile.sharedPreferences(SHARED_PREFS_NAME)
                        ?.edit()
                        ?.putString(TW_TEMP_TOKEN, tempToken)
                        ?.apply()
                }

                val uri = "https://api.twitter.com/oauth/authenticate?oauth_token=${tempToken}".toUri()
                launchOnBrowser(uri)

                loading = false
            }
        }

        button_login_mastodon.setOnClickListener {
            val parentViewGroup: ViewGroup? = null
            val dialogView = LayoutInflater.from(fragActivity).inflate(R.layout.dialog_instance, parentViewGroup)

            val dialog = MaterialAlertDialogBuilder(fragActivity)
                .setView(dialogView)
                .create()
            dialog.show()

            val layout = dialogView.layout_login_dialog
            val editText = dialogView.edittext_login_dialog_domain
            val button = dialogView.button_login_dialog_ok

            button.setOnClickListener {
                lifecycleScope.launch {
                    val domain = editText.text.toString()
                        .replace(Regex("https?://"), "")
                        .toLowerCase(Locale.ROOT)
                        .trim()

                    if (domain.isEmpty())
                        return@launch domainError(layout)

                    val (clientId, clientSecret, redirectUri) = MastodonRepository.createApp(domain, getString(R.string.app_name))
                        ?.let { res ->
                            res.body() ?: return@launch let {
                                if (res.code() == 404)
                                    domainError(layout)
                                else
                                    dialog.dismiss().also { error }
                            }
                        } ?: return@launch domainError(layout)

                    withContext(Dispatchers.IO) {
                        secureFile.sharedPreferences(SHARED_PREFS_NAME)
                            ?.edit()
                            ?.apply {
                                putString(MASTO_DOMAIN, domain)
                                putString(MASTO_CLIENT_ID, clientId)
                                putString(MASTO_CLIENT_SECRET, clientSecret)
                            }
                            ?.apply()
                    }

                    val params = mapOf(
                        "client_id" to clientId,
                        "redirect_uri" to redirectUri,
                        "response_type" to "code"
                    ).map { (k,v) ->
                        "$k=${URLEncoder.encode(v, "UTF-8")}"
                    }.joinToString("&")

                    val uri = "https://$domain/oauth/authorize?${params}".toUri()
                    dialog.dismiss()
                    launchOnBrowser(uri)
                }
            }
        }
    }

    private fun launchOnBrowser(uri: Uri) {
        val color = resources.getColor(R.color.red200, null)
        try {
            CustomTabsIntent.Builder()
                .setToolbarColor(color)
                .build()
                .launchUrl(fragActivity, uri)
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(fragActivity, R.string.login_error_browser, Toast.LENGTH_LONG).show()
        }
    }

    private val error: Unit
        get() {
            loading = false
            Toast.makeText(fragActivity, R.string.login_error_unexpected, Toast.LENGTH_LONG).show()
        }

    private val netError: Unit
        get() {
            loading = false
            Toast.makeText(fragActivity, R.string.login_error_network, Toast.LENGTH_LONG).show()
        }

    private fun domainError(layout: TextInputLayout) {
        layout.error = getString(R.string.login_dialog_domain_invalid)
    }

    private var loading = false
        set(value) {
            linearlayout_login_buttons.visibility = if (value) View.GONE else View.VISIBLE
            linearlayout_login_loading.visibility = if (value) View.VISIBLE else View.GONE
            field = value
        }
}
