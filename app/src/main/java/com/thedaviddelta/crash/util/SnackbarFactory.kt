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

package com.thedaviddelta.crash.util

import android.view.View
import android.widget.TextView
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import com.google.android.material.snackbar.Snackbar
import com.thedaviddelta.crash.R

class SnackbarFactory(private val view: View) {
    private val snackbar: Snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)

    fun showing(msg: String): SnackbarFactory {
        snackbar.setText(msg)
        return this
    }

    fun showing(@StringRes resId: Int): SnackbarFactory {
        snackbar.setText(resId)
        return this
    }

    fun during(duration: Int): SnackbarFactory {
        snackbar.duration = duration
        return this
    }

    fun centered(): SnackbarFactory {
        val text = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return this
    }

    fun tinted(@ColorRes resId: Int): SnackbarFactory {
        snackbar.setBackgroundTint(view.resources.getColor(resId, null))
        return this
    }

    fun doing(msg: String, listener: (View) -> Unit): SnackbarFactory {
        snackbar.setAction(msg, listener)
        return this
    }

    fun doing(@StringRes resId: Int, listener: (View) -> Unit): SnackbarFactory {
        snackbar.setAction(resId, listener)
        return this
    }

    fun build(): Snackbar {
        return snackbar
    }

    fun buildAndShow() {
        snackbar.show()
    }

    fun error(@StringRes resId: Int): SnackbarFactory {
        return this.showing(resId)
            .tinted(R.color.red900)
            .centered()
    }
}
