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
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintSet
import androidx.core.os.bundleOf
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.palette.graphics.Palette
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.InterstitialAd
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.snackbar.Snackbar
import com.thedaviddelta.crash.model.*
import com.thedaviddelta.crash.repository.FirestoreRepository
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.util.SnackbarBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_user.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import kotlin.properties.Delegates

class UserFragment : Fragment() {

    companion object {
        private const val MAX_CRUSHES = 3
    }

    private var numCrushes by Delegates.notNull<Int>()

    private val isFull: Boolean
        get() = numCrushes >= MAX_CRUSHES

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_user, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val fragActivity = requireActivity()
        val actWindow = fragActivity.window
        val statusColor = actWindow.statusBarColor

        val adRequest = AdRequest.Builder()
            .addNetworkExtrasBundle(
                AdMobAdapter::class.java,
                bundleOf("npa" to "1")
            ).build()

        val interstitialAd = InterstitialAd(fragActivity).apply {
            adUnitId = resources.getString(R.string.admob_adunit_id_user_to_main_interstitial)
            loadAd(adRequest)
        }

        adview_user_banner.loadAd(adRequest)

        toolbar_user.setNavigationOnClickListener {
            SnackbarBuilder(requireView())
                .tinted(android.R.color.transparent)
                .during(1)
                .buildAndShow()

            if (interstitialAd.isLoaded)
                interstitialAd.show()

            findNavController().navigateUp()
            actWindow.statusBarColor = statusColor
        }

        val user = arguments?.run {
            numCrushes = getInt("numCrushes").takeUnless {
                it == -1
            } ?: return@run null

            getSerializable("user")?.let {
                if (it is User) it else null
            }
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

        imageview_user_avatar.setOnClickListener {
            openProfile(user)
        }

        toolbar_user.menu.findItem(R.id.share_menu_user_action).setOnMenuItemClickListener {
            share()
            true
        }

        button_user_heart.apply {
            setImageResource(user.crush.drawable)
            if (isFull && user.crush == CrushType.NONE)
                alpha = 0.7F

            setOnClickListener {
                val current = Accounts.current!!
                when(user.crush) {
                    CrushType.NONE -> {
                        if (isFull) {
                            SnackbarBuilder(requireView())
                                .showing(resources.getString(R.string.user_max, MAX_CRUSHES))
                                .tinted(R.color.red300)
                                .centered()
                                .buildAndShow()
                            return@setOnClickListener
                        }
                        MaterialAlertDialogBuilder(fragActivity)
                            .setTitle(R.string.confirmation_sure)
                            .setMessage(resources.getString(R.string.user_add_msg, MAX_CRUSHES - numCrushes))
                            .setPositiveButton(R.string.confirmation_yes) { dialog, _ ->
                                lifecycleScope.launch {
                                    FirestoreRepository.addCrush(current, user).takeIf { !it }?.let {
                                        return@launch SnackbarBuilder(requireView())
                                            .error(R.string.user_add_error)
                                            .buildAndShow()
                                    }
                                    FirestoreRepository.checkIfCrushIsMutual(current, user).takeIf { it }
                                        ?.run {
                                            user.crush = CrushType.MUTUAL
                                            SnackbarBuilder(requireView())
                                                .showing(R.string.user_add_mutual)
                                                .inMultipleLines()
                                                .tinted(R.color.forestGreen)
                                                .during(Snackbar.LENGTH_INDEFINITE)
                                                .doing(R.string.user_add_dm) {
                                                    sendDm(user)
                                                }.buildAndShow()
                                        } ?: run {
                                            user.crush = CrushType.CRUSH
                                            SnackbarBuilder(requireView())
                                                .showing(R.string.user_add_successful)
                                                .inMultipleLines()
                                                .tinted(R.color.red300)
                                                .during(Snackbar.LENGTH_INDEFINITE)
                                                .doing(R.string.menu_user_share) {
                                                    share()
                                                }.buildAndShow()
                                        }
                                    setImageResource(user.crush.drawable)
                                    numCrushes++
                                }
                                dialog.dismiss()
                            }.setNegativeButton(R.string.confirmation_cancel) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    }
                    CrushType.CRUSH,
                    CrushType.MUTUAL -> {
                        MaterialAlertDialogBuilder(fragActivity)
                            .setTitle(R.string.confirmation_sure)
                            .setMessage(R.string.user_delete_msg)
                            .setPositiveButton(R.string.confirmation_yes) { dialog, _ ->
                                lifecycleScope.launch {
                                    FirestoreRepository.deleteCrush(current, user).takeIf { it != 0L }?.let {
                                        return@launch if (it == -1L) {
                                            SnackbarBuilder(requireView())
                                                .error(R.string.user_delete_error)
                                                .buildAndShow()
                                        } else {
                                            val days = TimeUnit.MILLISECONDS.toDays(it)
                                            val hours = TimeUnit.MILLISECONDS.toHours(
                                                it - TimeUnit.DAYS.toMillis(days)
                                            )
                                            val minutes = TimeUnit.MILLISECONDS.toMinutes(
                                                it - TimeUnit.DAYS.toMillis(days) - TimeUnit.HOURS.toMillis(hours)
                                            )
                                            SnackbarBuilder(requireView())
                                                .showing(resources.getString(R.string.user_time, days, hours, minutes))
                                                .inMultipleLines()
                                                .tinted(R.color.red300)
                                                .untilClose()
                                                .buildAndShow()
                                        }
                                    }
                                    user.crush = CrushType.NONE
                                    SnackbarBuilder(requireView())
                                        .showing(R.string.user_delete_successful)
                                        .tinted(R.color.red300)
                                        .centered()
                                        .buildAndShow()
                                    setImageResource(user.crush.drawable)
                                    numCrushes--
                                }
                                dialog.dismiss()
                            }.setNegativeButton(R.string.confirmation_cancel) { dialog, _ ->
                                dialog.dismiss()
                            }.show()
                    }
                }
            }
        }

        resources.configuration.orientation.let {
            val isLandscape = it == Configuration.ORIENTATION_LANDSCAPE
            ConstraintSet().apply {
                clone(constraintlayout_user_appbar)
                setDimensionRatio(
                    imageview_user_banner.id,
                    if (isLandscape) "0" else "3"
                )
                applyTo(constraintlayout_user_appbar)
            }
        }
    }

    private fun openProfile(user: User) {
        when(user) {
            is TwitterUser -> {
                val url = "https://twitter.com/${user.username}"
                    .let { Uri.parse(it) }
                openInSocialNet(url)
            }
            is MastodonUser -> {
                val url = "https://${user.domain}/@${user.username}"
                    .let { Uri.parse(it) }
                openInSocialNet(url)
            }
        }
    }

    private fun share() {
        val current = Accounts.current!!
        val msg = resources.getString(R.string.user_share_message)
            .let { Uri.encode(it) }
        val link = "https://play.google.com/store/apps/details?id=${BuildConfig.APPLICATION_ID}"
            .let { Uri.encode(it) }

        when(current) {
            is TwitterAccount -> {
                val url = "https://twitter.com/intent/tweet?text=$msg&url=$link"
                    .let { Uri.parse(it) }
                openInSocialNet(url)
            }
            is MastodonAccount -> {
                val url = "https://${current.domain}/share?text=$msg&url=$link"
                    .let { Uri.parse(it) }
                openInSocialNet(url)
            }
        }
    }

    private fun sendDm(user: User) {
        when(user) {
            is TwitterUser -> {
                val url = "https://twitter.com/messages/compose?recipient_id=${user.id}"
                    .let { Uri.parse(it) }
                openInSocialNet(url)
            }
            is MastodonUser -> {
                val current = Accounts.current
                if (current !is MastodonAccount)
                    return
                val domain = if (user.domain != current.domain) "@${user.domain}" else ""

                val url = "https://${current.domain}/share?text=@${user.username}$domain"
                    .let { Uri.parse(it) }
                openInSocialNet(url)
            }
        }
    }

    private fun openInSocialNet(url: Uri) {
        Intent().apply {
            action = Intent.ACTION_VIEW
            data = url
            startActivity(this)
        }
    }
}
