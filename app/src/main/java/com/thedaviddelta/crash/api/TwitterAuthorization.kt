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

package com.thedaviddelta.crash.api

import android.net.Uri
import android.util.Base64
import okhttp3.FormBody
import okhttp3.Request
import java.net.URLEncoder
import java.util.*
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import kotlin.math.absoluteValue
import kotlin.random.Random

private object OAUTH {
    const val CALLBACK = "oauth_callback"
    const val CONSUMER_KEY = "oauth_consumer_key"
    const val NONCE = "oauth_nonce"
    const val TIMESTAMP = "oauth_timestamp"
    const val SIGNATURE_METHOD = "oauth_signature_method"
    const val SIGNATURE = "oauth_signature"
    const val TOKEN = "oauth_token"
    const val VERSION = "oauth_version"
}
private const val SIGNATURE_METHOD = "HMAC-SHA1"

fun twitterAuthorization(
    request: Request,
    token: String? = null,
    tokenSecret: String? = null
): String {
    val url = Uri.parse(request.url().toString())
    val method = request.method()

    val consumerKey = Api.Twitter.CONSUMER_KEY
    val consumerSecret = Api.Twitter.CONSUMER_SECRET

    val nonce = getNonce()
    val timestamp = getTimestamp()

    val postParams = getPostParams(request)
    val callback = postParams?.get(OAUTH.CALLBACK)

    val signatureBase = signatureBase(url, method, postParams, consumerKey, nonce, timestamp, token)
    val signature = signature(consumerSecret, tokenSecret, signatureBase)
    return buildHeader(callback, consumerKey, nonce, signature, timestamp, token)
}

private fun buildHeader(
    callback: String?,
    consumerKey: String,
    nonce: String,
    signature: String,
    timestamp: String,
    token: String?
): String {
    val params = mutableMapOf<String, String>()

    callback?.let { params[OAUTH.CALLBACK] = it }
    params[OAUTH.CONSUMER_KEY] = consumerKey
    params[OAUTH.NONCE] = nonce
    params[OAUTH.SIGNATURE] = signature
    params[OAUTH.SIGNATURE_METHOD] = SIGNATURE_METHOD
    params[OAUTH.TIMESTAMP] = timestamp
    token?.let { params[OAUTH.TOKEN] = it }
    params[OAUTH.VERSION] = "1.0"

    return "OAuth " + params
        .map { (k,v) -> "$k=\"$v\"" }
        .reduce { acc, s -> "$acc, $s" }
}

private fun signatureBase(
    url: Uri,
    method: String,
    postParams: Map<String, String>?,
    consumerKey: String,
    nonce: String,
    timestamp: String,
    token: String?
): String {
    val params = mutableMapOf<String, String>()

    url.queryParameterNames.forEach { paramName ->
        url.getQueryParameter(paramName)?.let { params[paramName] = it }
    }
    params[OAUTH.CONSUMER_KEY] = consumerKey
    params[OAUTH.NONCE] = nonce
    params[OAUTH.TIMESTAMP] = timestamp
    params[OAUTH.SIGNATURE_METHOD] = SIGNATURE_METHOD
    params[OAUTH.VERSION] = "1.0"
    token?.let { params[OAUTH.TOKEN] = it }
    postParams?.let { params.putAll(it) }

    val baseUrl = "${url.scheme}://${url.host}${url.path}"
    val encodedParams = params.toSortedMap()
        .map { (k,v) -> "${k.encode}%3D${v.encode}" }
        .reduce { acc, s -> "$acc%26$s" }

    return "${method.toUpperCase(Locale.ROOT)}&${baseUrl.encode}&$encodedParams"
}

private fun signature(
    consumerSecret: String,
    tokenSecret: String?,
    base: String
): String {
    val key = "${consumerSecret.encode}&${tokenSecret?.encode ?: ""}"
    val mac = Mac.getInstance(SIGNATURE_METHOD)
    mac.init(SecretKeySpec(key.toByteArray(), SIGNATURE_METHOD))
    return Base64.encodeToString(mac.doFinal(base.toByteArray()), Base64.NO_WRAP).encode
}

private fun getNonce(): String {
    return "${System.nanoTime()}${Random.nextLong().absoluteValue}"
}

private fun getTimestamp(): String {
    return (System.currentTimeMillis() / 1000).toString()
}

private fun getPostParams(request: Request): Map<String, String>? {
    val body = request.body()
    if (request.method() != "POST" || body !is FormBody || body.size() == 0)
        return null

    val params = mutableMapOf<String, String>()
    for (i in 0 until body.size())
        params[body.encodedName(i)] = body.encodedValue(i)
    return params
}

private val String.encode get() = URLEncoder.encode(this, "UTF-8")
