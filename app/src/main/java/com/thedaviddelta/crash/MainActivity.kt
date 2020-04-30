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
import androidx.appcompat.app.AppCompatActivity
import com.thedaviddelta.crash.util.SnackbarFactory
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    private var doubleBack = false

    override fun onBackPressed() {
        when(nav_host_fragment.childFragmentManager.fragments.first()) {
            is MainFragment -> {
                if (doubleBack)
                    return finish()
                doubleBack = true

                SnackbarFactory(nav_host_fragment.requireView())
                    .showing(R.string.main_quit_message)
                    .during(2000)
                    .tinted(R.color.red300)
                    .centered()
                    .buildAndShow()

                Handler().postDelayed({
                    doubleBack = false
                }, 2000)
            }
        }
    }
}
