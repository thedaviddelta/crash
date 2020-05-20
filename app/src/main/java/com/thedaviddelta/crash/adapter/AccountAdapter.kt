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

package com.thedaviddelta.crash.adapter

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.thedaviddelta.crash.R
import com.thedaviddelta.crash.model.Account
import com.thedaviddelta.crash.model.MastodonAccount
import com.thedaviddelta.crash.model.TwitterAccount
import com.thedaviddelta.crash.repository.ImageRepository
import kotlinx.android.synthetic.main.listitem_accounts.view.*

/**
 * [RecyclerView Adapter][RecyclerView.Adapter] for instances of [Account] model class
 *
 * @constructor Instances an adapter with a [list of accounts][list] and an [on item click listener][listener]
 * @param list (optional) list of accounts to show
 * @param listener lambda that will be executed on item click
 */
class AccountAdapter(
    private val list: List<Account> = listOf(),
    private val listener: (Account) -> Unit
) : RecyclerView.Adapter<AccountAdapter.ViewHolder>() {

    private lateinit var removeListener: (Account, View) -> Unit

    /**
     * Adds an [item remove listener][listener] in a *fluent* way
     *
     * @param listener lambda that will be executed on item remove button click
     * @return the [adapter][AccountAdapter] instance
     */
    fun onRemove(listener: (Account, View) -> Unit): AccountAdapter {
        if (!this::removeListener.isInitialized)
            removeListener = listener
        return this
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.listitem_accounts, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount(): Int = list.size

    /**
     * [RecyclerView ViewHolder][RecyclerView.ViewHolder] for [AccountAdapter]
     *
     * @param view the view whose data is held by the current [ViewHolder]
     */
    inner class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        /**
         * Binds the [given Account][item] to the current [ViewHolder]
         *
         * @param item the [Account] to be bound
         */
        @SuppressLint("SetTextI18n")
        fun bind(item: Account) = with(view) {
            textview_listitem_accounts_fullname.text = item.fullName
            textview_listitem_accounts_username.text = "@${item.username}"

            ImageRepository.loadImage(item.avatarUrl, imageview_listitem_accounts_avatar::setImageBitmap)

            if (item is MastodonAccount && item.fullName.isBlank())
                textview_listitem_accounts_fullname.text = item.username

            imageview_listitem_accounts_platform.setImageResource(
                when (item) {
                    is TwitterAccount -> R.drawable.ic_twitter_logo
                    is MastodonAccount -> R.drawable.ic_mastodon_logo
                    else -> R.drawable.ic_logo
                }
            )

            setOnClickListener { listener(item) }
            button_listitem_accounts_remove.setOnClickListener { removeListener(item, it) }
        }
    }
}
