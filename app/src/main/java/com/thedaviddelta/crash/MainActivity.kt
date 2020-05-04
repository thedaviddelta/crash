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
import android.os.Handler
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.navigation.fragment.findNavController
import com.thedaviddelta.crash.util.SnackbarBuilder
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private val statusColor by lazy {
        window.statusBarColor
    }

    private var doubleBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        statusColor.let {
            Log.i("Main", "Initialized! $it")
        }
    }

    override fun onBackPressed() {
        when(nav_host_fragment.childFragmentManager.fragments.first()) {
            is MainFragment -> {
                if (doubleBack)
                    return finish()
                doubleBack = true

                SnackbarBuilder(nav_host_fragment.requireView())
                    .showing(R.string.main_quit_message)
                    .during(2000)
                    .tinted(R.color.red300)
                    .centered()
                    .buildAndShow()

                Handler().postDelayed({
                    doubleBack = false
                }, 2000)
            }
            is UserFragment -> {
                nav_host_fragment.findNavController().navigateUp()
                window.statusBarColor = statusColor
            }
        }
    }
}
