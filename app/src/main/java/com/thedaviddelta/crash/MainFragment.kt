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
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.DividerItemDecoration
import com.thedaviddelta.crash.adapter.UserAdapter
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
            Toast.makeText(requireActivity(), it.id.toString(), Toast.LENGTH_SHORT).show()
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
            setRefreshing(true).also { loadUsers() }
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
    }

    private fun loadUsers() {
        lifecycleScope.launch {
            val success = viewModel.load()
            if (!success)
                Toast.makeText(requireActivity(), R.string.main_error_mutuals, Toast.LENGTH_LONG).show()
            swiperefreshlayout_main.isRefreshing = false
        }
    }
}
