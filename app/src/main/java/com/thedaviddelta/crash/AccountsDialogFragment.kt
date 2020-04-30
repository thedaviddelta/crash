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
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.thedaviddelta.crash.adapter.AccountAdapter
import com.thedaviddelta.crash.model.MastodonAccount
import com.thedaviddelta.crash.model.TwitterAccount
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.Accounts
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
        val rest = Accounts.readOnlyList.filter { it != current }

        val adapter = AccountAdapter(rest) {
            Accounts.current = it
            findNavController().navigate(R.id.action_accounts_to_main)
        }.onRemove { account, v ->
            PopupMenu(requireActivity(), v).apply {
                inflate(R.menu.menu_context_accounts)
                setOnMenuItemClickListener { item ->
                    when(item.itemId) {
                        R.id.yes_menu_accounts_action -> {
                            lifecycleScope.launch {
                                Accounts.remove(account).let {
                                    if (it)
                                        this@AccountsDialogFragment.dismiss()
                                    else
                                        Toast.makeText(requireActivity(), R.string.accounts_error_remove, Toast.LENGTH_LONG).show()
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
            addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
        }

        textview_accounts_fullname.text = current.fullName
        textview_accounts_username.text = "@${current.username}"
        textview_accounts_domain.text = when(current) {
            is TwitterAccount -> "@twitter.com"
            is MastodonAccount -> "@${current.domain}"
            else -> ""
        }

        ImageRepository.apply {
            loadImage(current.avatarUrl, imageview_accounts_avatar::setImageBitmap)
            current.bannerUrl?.let { loadImage(it, imageview_accounts_banner::setImageBitmap) }
        }

        button_accounts_add.setOnClickListener {
            findNavController().navigate(R.id.action_accounts_to_login)
        }

        (dialog as BottomSheetDialog).behavior.peekHeight = Resources.getSystem().displayMetrics.heightPixels
    }
}
