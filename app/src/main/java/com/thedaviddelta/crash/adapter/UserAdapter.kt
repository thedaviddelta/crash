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
import com.thedaviddelta.crash.model.MastodonUser
import com.thedaviddelta.crash.model.User
import com.thedaviddelta.crash.repository.ImageRepository
import kotlinx.android.synthetic.main.listitem_main.view.*

/**
 * [RecyclerView Adapter][RecyclerView.Adapter] for instances of [User] model class
 *
 * @constructor Instances an adapter with a [list of users][list] and an [on item click listener][listener]
 * @param list (optional) list of users to show
 * @param listener lambda that will be executed on item click
 */
class UserAdapter(
    private var list: List<User> = listOf(),
    private val listener: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    /** Copy of non-filtered list */
    private var backup: List<User> = list

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder = ViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.listitem_main, parent, false)
    )

    override fun onBindViewHolder(holder: ViewHolder, position: Int) = holder.bind(list[position])

    override fun getItemCount(): Int = list.size

    /**
     * Updates the current [User] list shown on the Recycler
     *
     * @param items new list of users to show
     */
    fun setItems(items: List<User>) {
        list = items.sortedByDescending {
            it.crush
        }
        backup = list
        notifyDataSetChanged()
    }

    /**
     * Filters the current [User] list by the [given query][text]
     *
     * @param text the query to filter through
     */
    fun filter(text: CharSequence?) {
        list = if (text.isNullOrBlank())
            backup
        else
            backup.filter {
                it.fullName.contains(text, true)
                || it.username.contains(text, true)
            }
        notifyDataSetChanged()
    }

    /**
     * [RecyclerView ViewHolder][RecyclerView.ViewHolder] for [UserAdapter]
     *
     * @param view the view whose data is held by the current [ViewHolder]
     */
    inner class ViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        /**
         * Binds the [given User][item] to the current [ViewHolder]
         *
         * @param item the [User] to be bound
         */
        @SuppressLint("SetTextI18n")
        fun bind(item: User) = with(view) {
            textview_listitem_main_fullname.text = item.fullName
            textview_listitem_main_username.text = "@${item.username}"

            ImageRepository.loadImage(item.avatarUrl) {
                imageview_listitem_main_avatar?.setImageBitmap(it)
                constraintlayout_listitem_main_avatar?.visibility = View.VISIBLE
                progressbar_listitem_main_avatar?.visibility = View.GONE
            }

            imageview_listitem_main_crush.setImageResource(item.crush.drawable)

            if (item is MastodonUser) {
                textview_listitem_main_domain.text = "@${item.domain}"
                textview_listitem_main_domain.visibility = View.VISIBLE
                if (item.fullName.isBlank())
                    textview_listitem_main_fullname.text = item.username
            }

            setOnClickListener { listener(item) }
        }
    }
}
