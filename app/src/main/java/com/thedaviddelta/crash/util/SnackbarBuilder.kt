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

class SnackbarBuilder(private val view: View) {
    private val snackbar: Snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)

    fun showing(msg: String): SnackbarBuilder {
        snackbar.setText(msg)
        return this
    }

    fun showing(@StringRes resId: Int): SnackbarBuilder {
        snackbar.setText(resId)
        return this
    }

    fun during(duration: Int): SnackbarBuilder {
        snackbar.duration = duration
        return this
    }

    fun centered(): SnackbarBuilder {
        val text = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return this
    }

    fun inMultipleLines(): SnackbarBuilder {
        val text = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.maxLines = Int.MAX_VALUE
        return this
    }

    fun tinted(@ColorRes resId: Int): SnackbarBuilder {
        snackbar.setBackgroundTint(view.resources.getColor(resId, null))
        return this
    }

    fun doing(msg: String, listener: (View) -> Unit): SnackbarBuilder {
        snackbar.setAction(msg, listener)
        return this
    }

    fun doing(@StringRes resId: Int, listener: (View) -> Unit): SnackbarBuilder {
        snackbar.setAction(resId, listener)
        return this
    }

    fun untilClose(msg: String): SnackbarBuilder {
        return this.during(Snackbar.LENGTH_INDEFINITE)
            .doing(msg) { snackbar.dismiss() }
    }

    fun untilClose(@StringRes resId: Int = R.string.confirmation_ok): SnackbarBuilder {
        return this.during(Snackbar.LENGTH_INDEFINITE)
            .doing(resId) { snackbar.dismiss() }
    }

    fun build(): Snackbar {
        return snackbar
    }

    fun buildAndShow() {
        snackbar.show()
    }

    fun error(@StringRes resId: Int): SnackbarBuilder {
        return this.showing(resId)
            .tinted(R.color.red900)
            .centered()
    }
}
