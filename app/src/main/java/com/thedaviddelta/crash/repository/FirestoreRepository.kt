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
import com.thedaviddelta.crash.util.Accounts
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.*

/**
 * Repository for Firestore service, following [Jetpack App Architecture](https://developer.android.com/jetpack/docs/guide)
 */
object FirestoreRepository {
    /** Firestore `twitter` collection reference */
    private val twitter = Firebase.firestore.collection("twitter")
    /** Firestore `mastodon` collection reference */
    private val mastodon = Firebase.firestore.collection("mastodon")

    /** Min. time before deleting a crush (1 week) */
    private const val DIFF_TIME = 1 * 7 * 24 * 60 * 60 * 1000

    /**
     * Checks [account]'s & [crush]'s platforms and adds relationship document to pertinent collection
     *
     * @param account [current Account][Accounts.current]
     * @param crush mutual to be crushed
     * @return `true` if added correctly, or `false` if an error occurred
     */
    suspend fun addCrush(account: Account, crush: User): Boolean {
        if (account is TwitterAccount && crush is TwitterUser)
            return addTwitterCrush(account.id, crush.id)
        if (account is MastodonAccount && crush is MastodonUser)
            return addMastodonCrush(account.id, account.domain, crush.id, crush.domain)
        return false
    }

    /**
     * Adds relationship document to `twitter` collection
     *
     * @param userId ID from [current Account][Accounts.current]
     * @param crushId ID from mutual to be crushed
     * @return `true` if added correctly, or `false` if an error occurred
     */
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

    /**
     * Adds relationship document to `mastodon` collection
     *
     * @param userId ID from [current Account][Accounts.current]
     * @param userDomain instance from [current Account][Accounts.current]
     * @param crushId ID from mutual to be crushed
     * @param crushDomain instance from mutual to be crushed
     * @return `true` if added correctly, or `false` if an error occurred
     */
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

    /**
     * Checks [account]'s & [crush]'s platforms and deletes relationship document from pertinent collection
     *
     * @param account [current Account][Accounts.current]
     * @param crush mutual to be un-crushed
     * @return `0` if deleted correctly, `-1` if an error occurred, or the remaining time before being able to delete
     */
    suspend fun deleteCrush(account: Account, crush: User): Long {
        if (account is TwitterAccount && crush is TwitterUser)
            return deleteTwitterCrush(account.id, crush.id)
        if (account is MastodonAccount && crush is MastodonUser)
            return deleteMastodonCrush(account.id, account.domain, crush.id, crush.domain)
        return -1L
    }

    /**
     * Deletes relationship document from `twitter` collection
     *
     * @param userId ID from [current Account][Accounts.current]
     * @param crushId ID from mutual to be un-crushed
     * @return `0` if deleted correctly, `-1` if an error occurred, or the remaining time before being able to delete
     */
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

    /**
     * Deletes relationship document from `mastodon` collection
     *
     * @param userId ID from [current Account][Accounts.current]
     * @param userDomain instance from [current Account][Accounts.current]
     * @param crushId ID from mutual to be un-crushed
     * @param crushDomain instance from mutual to be un-crushed
     * @return `0` if deleted correctly, `-1` if an error occurred, or the remaining time before being able to delete
     */
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

    /**
     * Retrieves IDs from users crushed by [userId]
     *
     * @param userId ID from [current Account][Accounts.current]
     * @return list of crushes IDs, or `null` if an error occurred
     */
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

    /**
     * Retrieves IDs from users crushed by [userId]@[userDomain]
     *
     * @param userId ID from [current Account][Accounts.current]
     * @param userDomain instance from [current Account][Accounts.current]
     * @return list of crushes ID & instance pairs, or `null` if an error occurred
     */
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

    /**
     * Retrieves IDs from users which [userId] is crushed by
     *
     * @param userId ID from [current Account][Accounts.current]
     * @return list of crushedBy IDs, or `null` if an error occurred
     */
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

    /**
     * Retrieves IDs from users which [userId]@[userDomain] is crushed by
     *
     * @param userId ID from [current Account][Accounts.current]
     * @param userDomain instance from [current Account][Accounts.current]
     * @return list of crushedBy ID & instance pairs, or `null` if an error occurred
     */
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

    /**
     * Checks if given [crush] have also crushed [account]
     *
     * @param account [current Account][Accounts.current]
     * @param crush mutual that have been crushed
     * @return `true` if it's mutual, or `false` if it isn't
     */
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

    /** Awaits for a task completion */
    private suspend inline fun <T> Task<T>.await(): T = withContext(Dispatchers.IO) {
        Tasks.await(this@await)
    }
}
