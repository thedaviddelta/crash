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

/**
 * Class for easily creating [Snackbars][Snackbar]
 *
 * @param view the view to find a parent from
 */
class SnackbarBuilder(private val view: View) {
    private val snackbar: Snackbar = Snackbar.make(view, "", Snackbar.LENGTH_LONG)

    /**
     * Sets [Snackbar] message (empty by default)
     *
     * @param msg message to be shown
     * @return the [builder][SnackbarBuilder] instance
     */
    fun showing(msg: String): SnackbarBuilder {
        snackbar.setText(msg)
        return this
    }

    /**
     * Sets [Snackbar] message (empty by default)
     *
     * @param resId identifier of the message to be shown
     * @return the [builder][SnackbarBuilder] instance
     */
    fun showing(@StringRes resId: Int): SnackbarBuilder {
        snackbar.setText(resId)
        return this
    }

    /**
     * Sets [Snackbar] duration ([Snackbar.LENGTH_LONG] by default)
     *
     * @param duration [Snackbar.LENGTH_SHORT], [Snackbar.LENGTH_LONG], [Snackbar.LENGTH_INDEFINITE] or custom time in milliseconds
     * @return the [builder][SnackbarBuilder] instance
     */
    fun during(duration: Int): SnackbarBuilder {
        snackbar.duration = duration
        return this
    }

    /**
     * Centers [Snackbar] text
     *
     * Don't use with [doing]
     *
     * @return the [builder][SnackbarBuilder] instance
     */
    fun centered(): SnackbarBuilder {
        val text = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.textAlignment = View.TEXT_ALIGNMENT_CENTER
        return this
    }

    /**
     * Makes [Snackbar] text take up multiple lines
     *
     * @return the [builder][SnackbarBuilder] instance
     */
    fun inMultipleLines(): SnackbarBuilder {
        val text = snackbar.view.findViewById<TextView>(com.google.android.material.R.id.snackbar_text)
        text.maxLines = Int.MAX_VALUE
        return this
    }

    /**
     * Sets [Snackbar] background color
     *
     * @param resId identifier of the color
     * @return the [builder][SnackbarBuilder] instance
     */
    fun tinted(@ColorRes resId: Int): SnackbarBuilder {
        snackbar.setBackgroundTint(view.resources.getColor(resId, null))
        return this
    }

    /**
     * Sets [Snackbar] action
     *
     * @param msg the action button message
     * @param listener lambda that will be executed on click
     * @return the [builder][SnackbarBuilder] instance
     */
    fun doing(msg: String, listener: (View) -> Unit): SnackbarBuilder {
        snackbar.setAction(msg, listener)
        return this
    }

    /**
     * Sets [Snackbar] action
     *
     * @param resId identifier of the action button message
     * @param listener lambda that will be executed on click
     * @return the [builder][SnackbarBuilder] instance
     */
    fun doing(@StringRes resId: Int, listener: (View) -> Unit): SnackbarBuilder {
        snackbar.setAction(resId, listener)
        return this
    }

    /**
     * Keeps [Snackbar] open until action click
     *
     * @param msg the action button message
     * @return the [builder][SnackbarBuilder] instance
     */
    fun untilClose(msg: String): SnackbarBuilder {
        return this.during(Snackbar.LENGTH_INDEFINITE)
            .doing(msg) { snackbar.dismiss() }
    }

    /**
     * Keeps [Snackbar] opened until action click
     *
     * @param resId (optional) identifier of the action button message
     * @return the [builder][SnackbarBuilder] instance
     */
    fun untilClose(@StringRes resId: Int = R.string.confirmation_ok): SnackbarBuilder {
        return this.during(Snackbar.LENGTH_INDEFINITE)
            .doing(resId) { snackbar.dismiss() }
    }

    /**
     * Returns the current [Snackbar] instance
     *
     * @return the [Snackbar] object
     */
    fun build(): Snackbar {
        return snackbar
    }

    /**
     * Shows the current [Snackbar]
     */
    fun buildAndShow() {
        snackbar.show()
    }

    /**
     * Sets error color, text to center and custom message
     *
     * @param resId identifier of the custom error message
     * @return the [builder][SnackbarBuilder] instance
     */
    fun error(@StringRes resId: Int): SnackbarBuilder {
        return this.showing(resId)
            .tinted(R.color.red900)
            .centered()
    }
}
