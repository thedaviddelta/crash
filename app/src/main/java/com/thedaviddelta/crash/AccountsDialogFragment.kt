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
import android.content.res.Resources
import android.graphics.Canvas
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.children
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.mikepenz.aboutlibraries.LibsBuilder
import com.thedaviddelta.crash.adapter.AccountAdapter
import com.thedaviddelta.crash.model.MastodonAccount
import com.thedaviddelta.crash.model.TwitterAccount
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.util.SnackbarBuilder
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.fragment_dialog_accounts.*
import kotlinx.coroutines.launch

class AccountsDialogFragment : BottomSheetDialogFragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_dialog_accounts, container, false)
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val current = Accounts.current!!
        val rest = Accounts.readOnlyList!!.filter { it != current }

        val adapter = AccountAdapter(rest) {
            Accounts.current = it
            findNavController().navigate(R.id.action_accounts_to_main)
        }.onRemove { account, v ->
            PopupMenu(requireActivity(), v).apply {
                inflate(R.menu.menu_accounts_remove)
                setOnMenuItemClickListener { item ->
                    when(item.itemId) {
                        R.id.yes_menu_accounts_action -> {
                            lifecycleScope.launch {
                                Accounts.remove(account).also {
                                    this@AccountsDialogFragment.dismiss()
                                }.takeIf { !it }?.let {
                                    SnackbarBuilder(requireActivity().nav_host_fragment.requireView())
                                        .error(R.string.accounts_error_remove)
                                        .buildAndShow()
                                }
                            }
                            true
                        }
                        R.id.cancel_menu_accounts_action -> true
                        else -> super.onOptionsItemSelected(item)
                    }
                }
                show()
                menu.performIdentifierAction(R.id.title_menu_accounts_action, 0)
            }
        }

        recyclerview_accounts.apply {
            this.adapter = adapter
            if (rest.isEmpty())
                visibility = View.GONE
            addItemDecoration(object : DividerItemDecoration(requireActivity(), VERTICAL) {
                override fun onDraw(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
                    val left = parent.paddingLeft
                    val right = parent.width - parent.paddingRight
                    children.drop(1).forEach {
                        val params = it.layoutParams as RecyclerView.LayoutParams
                        val top = it.top + params.topMargin
                        val bottom = top + drawable!!.intrinsicHeight
                        drawable!!.setBounds(left, top, right, bottom)
                        drawable!!.draw(c)
                    }
                }
            })
        }

        textview_accounts_fullname.text = current.fullName
        textview_accounts_username.text = "@${current.username}"
        textview_accounts_domain.text = when(current) {
            is TwitterAccount -> "@twitter.com"
            is MastodonAccount -> "@${current.domain}"
            else -> ""
        }

        if (current is MastodonAccount && current.fullName.isBlank())
            textview_accounts_fullname.text = current.username

        ImageRepository.apply {
            loadImage(current.avatarUrl, imageview_accounts_avatar::setImageBitmap)
            current.bannerUrl?.let { url ->
                loadImage(url) { imageview_accounts_banner?.setImageBitmap(it) }
            }
        }

        button_accounts_add.setOnClickListener {
            findNavController().navigate(R.id.action_accounts_to_login)
        }

        button_accounts_info.setOnClickListener {
            LibsBuilder()
                .withAboutAppName(resources.getString(R.string.app_name))
                .withActivityTitle(resources.getString(R.string.accounts_info))
                .withAboutDescription(resources.getString(R.string.about_description))
                .withAboutSpecial1(resources.getString(R.string.about_license_button))
                .withAboutSpecial1Description(resources.getString(R.string.about_license))
                .withAboutSpecial2(resources.getString(R.string.about_privacy_policy_button))
                .withAboutSpecial2Description(resources.getString(R.string.about_privacy_policy))
                .withAboutVersionShown(false)
                .withAboutVersionShownName(true)
                .withLicenseShown(true)
                .withLicenseDialog(true)
                .withExcludedLibraries(*resources.getString(R.string.about_excluded_libraries).split(",").toTypedArray())
                .start(requireActivity())
            findNavController().navigateUp()
        }

        DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL).drawable.let {
            linearlayout_accounts.dividerDrawable = it
            linearlayout_accounts_options.dividerDrawable = it
        }

        (dialog as BottomSheetDialog).behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
    }
}
