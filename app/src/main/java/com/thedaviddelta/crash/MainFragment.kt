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

import android.os.Bundle
import android.view.*
import android.widget.ImageView
import androidx.appcompat.widget.SearchView
import androidx.core.graphics.drawable.RoundedBitmapDrawableFactory
import androidx.core.graphics.scale
import androidx.core.os.bundleOf
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.DividerItemDecoration
import com.google.android.material.appbar.MaterialToolbar
import com.thedaviddelta.crash.adapter.UserAdapter
import com.thedaviddelta.crash.repository.ImageRepository
import com.thedaviddelta.crash.util.Accounts
import com.thedaviddelta.crash.util.SnackbarBuilder
import com.thedaviddelta.crash.viewmodel.MainViewModel
import kotlinx.android.synthetic.main.fragment_main.*
import kotlinx.coroutines.launch


class MainFragment : Fragment() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = UserAdapter {
            findNavController().navigate(R.id.action_main_to_user, bundleOf("user" to it))
        }

        recyclerview_main.apply {
            this.adapter = adapter
            addItemDecoration(DividerItemDecoration(requireActivity(), DividerItemDecoration.VERTICAL))
        }
        viewModel.list.observe(viewLifecycleOwner, Observer(adapter::setItems))

        swiperefreshlayout_main.apply {
            setOnRefreshListener(::loadUsers)
            setColorSchemeColors(
                resources.getColor(R.color.red700, null)
            )
            viewModel.list.value ?: setRefreshing(true).also { loadUsers() }
        }

        val searchView = toolbar_main.menu.findItem(R.id.search_menu_main_action).actionView as SearchView
        searchView.apply {
            setIconifiedByDefault(false)
            queryHint = "${getString(R.string.menu_main_search)}..."
            setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextChange(newText: String?): Boolean {
                    adapter.filter(newText)
                    return true
                }
                override fun onQueryTextSubmit(query: String?): Boolean {
                    adapter.filter(query)
                    return true
                }
            })
        }

        toolbar_main.menu.findItem(R.id.account_menu_main_action).apply {
            setOnMenuItemClickListener {
                findNavController().navigate(R.id.action_main_to_accounts)
                true
            }
            Accounts.current?.let { current ->
                ImageRepository.loadImage(current.avatarUrl) {
                    val size = resources.getDimension(R.dimen.size_main_toolbar_avatar).toInt()
                    val scaled = it.scale(size, size, filter = true)
                    icon = RoundedBitmapDrawableFactory.create(resources, scaled).apply { isCircular = true }
                }
            }
        }

        toolbar_main.logoView?.setOnClickListener {
            recyclerview_main.scrollToPosition(0)
        }
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val success = viewModel.load()
            if (!success)
                SnackbarBuilder(requireView())
                    .error(R.string.main_error_mutuals)
                    .buildAndShow()
            swiperefreshlayout_main.isRefreshing = false
        }
    }

    private val MaterialToolbar.logoView: ImageView?
        get() {
            return children.firstOrNull {
                it is ImageView && it.drawable == logo
            } as? ImageView
        }
}
