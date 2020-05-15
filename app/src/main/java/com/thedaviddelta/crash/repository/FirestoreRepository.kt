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

package com.thedaviddelta.crash.repository

import android.util.Log
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.thedaviddelta.crash.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

object FirestoreRepository {
    private val twitter = Firebase.firestore.collection("twitter")
    private val mastodon = Firebase.firestore.collection("mastodon")

    private const val DIFF_TIME = 1 * 7 * 24 * 60 * 60 * 1000

    suspend fun addCrush(account: Account, crush: User): Boolean {
        if (account is TwitterAccount && crush is TwitterUser)
            return addTwitterCrush(account.id, crush.id)
        if (account is MastodonAccount && crush is MastodonUser)
            return addMastodonCrush(account.id, account.domain, crush.id, crush.domain)
        return false
    }

    suspend fun addTwitterCrush(userId: Long, crushId: Long): Boolean {
        return try {
            twitter.document().set(
                mapOf(
                    "id" to userId,
                    "crushId" to crushId,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "add-twitter-crush ${e.localizedMessage}")
            false
        }
    }

    suspend fun addMastodonCrush(userId: Long, userDomain: String, crushId: Long, crushDomain: String): Boolean {
        return try {
            mastodon.document().set(
                mapOf(
                    "id" to userId,
                    "domain" to userDomain,
                    "crushId" to crushId,
                    "crushDomain" to crushDomain,
                    "timestamp" to FieldValue.serverTimestamp()
                )
            ).await()
            true
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "add-mastodon-crush ${e.localizedMessage}")
            false
        }
    }

    suspend fun deleteCrush(account: Account, crush: User): Long {
        if (account is TwitterAccount && crush is TwitterUser)
            return deleteTwitterCrush(account.id, crush.id)
        if (account is MastodonAccount && crush is MastodonUser)
            return deleteMastodonCrush(account.id, account.domain, crush.id, crush.domain)
        return -1L
    }

    suspend fun deleteTwitterCrush(userId: Long, crushId: Long): Long {
        return try {
            val doc = twitter.whereEqualTo("id", userId).whereEqualTo("crushId", crushId)
                .get().await().firstOrNull() ?: return -1L

            doc.getDate("timestamp")?.let {
                val left = DIFF_TIME - (Date().time - it.time)
                if (left > 0) return left
            } ?: return -1L

            twitter.document(doc.id).delete().await()
            0L
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "delete-twitter-crush ${e.localizedMessage}")
            -1L
        }
    }

    suspend fun deleteMastodonCrush(userId: Long, userDomain: String, crushId: Long, crushDomain: String): Long {
        return try {
            val doc = mastodon.whereEqualTo("id", userId).whereEqualTo("domain", userDomain)
                .whereEqualTo("crushId", crushId).whereEqualTo("crushDomain", crushDomain)
                .get().await().firstOrNull() ?: return -1L

            doc.getDate("timestamp")?.let {
                val left = DIFF_TIME - (Date().time - it.time)
                if (left > 0) return left
            } ?: return -1L

            mastodon.document(doc.id).delete().await()
            0L
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "delete-mastodon-crush ${e.localizedMessage}")
            -1L
        }
    }

    suspend fun getTwitterCrushes(userId: Long): List<Long>? {
        return try {
            twitter.whereEqualTo("id", userId).get().await().documents.map {
                it.getLong("crushId") ?: return null
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "get-twitter-crushes ${e.localizedMessage}")
            null
        }
    }

    suspend fun getMastodonCrushes(userId: Long, userDomain: String): List<Pair<Long, String>>? {
        return try {
            mastodon.whereEqualTo("id", userId).whereEqualTo("domain", userDomain)
                .get().await().documents.map {
                    val id = it.getLong("crushId") ?: return null
                    val domain = it.getString("crushDomain") ?: return null
                    id to domain
                }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "get-mastodon-crushes ${e.localizedMessage}")
            null
        }
    }

    suspend fun getTwitterCrushedBy(userId: Long): List<Long>? {
        return try {
            twitter.whereEqualTo("crushId", userId).get().await().documents.map {
                it.getLong("id") ?: return null
            }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "get-twitter-crushedby ${e.localizedMessage}")
            null
        }
    }

    suspend fun getMastodonCrushedBy(userId: Long, userDomain: String): List<Pair<Long, String>>? {
        return try {
            mastodon.whereEqualTo("crushId", userId).whereEqualTo("crushDomain", userDomain)
                .get().await().documents.map {
                    val id = it.getLong("id") ?: return null
                    val domain = it.getString("domain") ?: return null
                    id to domain
                }
        } catch (e: Exception) {
            Log.e("FirestoreRepository", "get-mastodon-crushedby ${e.localizedMessage}")
            null
        }
    }

    suspend fun checkIfCrushIsMutual(account: Account, crush: User): Boolean {
        if (account is TwitterAccount && crush is TwitterUser) {
            val crushedBy = getTwitterCrushedBy(account.id)
                ?: return false
            return crush.id in crushedBy
        }
        if (account is MastodonAccount && crush is MastodonUser) {
            val crushedBy = getMastodonCrushedBy(account.id, account.domain)
                ?: return false
            return crush.id to crush.domain in crushedBy
        }
        return false
    }

    private suspend fun <T> Task<T>.await(): T = withContext(Dispatchers.IO) {
        Tasks.await(this@await)
    }
}