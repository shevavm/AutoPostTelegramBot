package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.tables.PostIdMessageId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.PluginName
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.PostsUsedTable
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.subscribe
import kotlinx.coroutines.experimental.channels.BroadcastChannel
import kotlinx.coroutines.experimental.channels.ReceiveChannel
import kotlinx.coroutines.experimental.launch
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

private const val subscriptionsCount = 256

class PostsLikesMessagesTable(
    private val postsLikesTable: PostsLikesTable
) : Table() {
    val ratingMessageRegisteredChannel = BroadcastChannel<PostIdMessageId>(subscriptionsCount)
    val ratingMessageUnregisteredChannel = BroadcastChannel<Int>(subscriptionsCount)

    private val postId = integer("postId").primaryKey()
    private val messageId = integer("messageId")

    private lateinit var lastEnableSubscription: ReceiveChannel<PostIdMessageId>
    private lateinit var lastDisableSubscription: ReceiveChannel<Int>

    internal var postsUsedTablePluginName: Pair<PostsUsedTable, PluginName>? = null
        set(value) {
            field ?.also {
                lastEnableSubscription.cancel()
                lastDisableSubscription.cancel()
            }
            field = value
            value ?.also {
                getEnabledPostsIdAndRatings().map {
                    it.first
                }.minus(
                    value.first.getPluginLinks(value.second)
                ).forEach {
                    value.first.registerLink(it, value.second)
                }
                lastEnableSubscription = ratingMessageRegisteredChannel.subscribe {
                    value.first.registerLink(it.first, value.second)
                }
                lastDisableSubscription = ratingMessageUnregisteredChannel.subscribe {
                    value.first.unregisterLink(it, value.second)
                }
            }
        }

    fun messageIdByPostId(postId: Int): Int? {
        return transaction {
            select {
                this@PostsLikesMessagesTable.postId.eq(postId)
            }.firstOrNull() ?.get(messageId)
        }
    }

    fun postIdByMessageId(messageId: Int): Int? {
        return transaction {
            select {
                this@PostsLikesMessagesTable.messageId.eq(messageId)
            }.firstOrNull() ?.let {
                it[postId]
            }
        }
    }

    fun enableLikes(postId: Int, messageId: Int): Boolean {
        return transaction {
            if (messageIdByPostId(postId) == null) {
                insert {
                    it[this@PostsLikesMessagesTable.postId] = postId
                    it[this@PostsLikesMessagesTable.messageId] = messageId
                }
                launch {
                    ratingMessageRegisteredChannel.send(postId to messageId)
                }
                true
            } else {
                false
            }
        }
    }

    fun disableLikes(postId: Int) {
        transaction {
            (deleteWhere {
                this@PostsLikesMessagesTable.postId.eq(postId)
            } > 0).also {
                if (it) {
                    launch {
                        ratingMessageUnregisteredChannel.send(postId)
                    }
                }
            }
        }
    }

    fun getEnabledPostsIdAndRatings(): List<PostIdRatingPair> {
        return transaction {
            selectAll().mapNotNull {
                it[postId]
            }.map {
                it to postsLikesTable.getPostRating(it)
            }
        }
    }
}