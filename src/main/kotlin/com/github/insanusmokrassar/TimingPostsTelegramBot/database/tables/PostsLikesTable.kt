package com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables

import com.github.insanusmokrassar.TimingPostsTelegramBot.database.exceptions.NoRowFoundException
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.transactions.transaction

object PostsLikesTable : Table() {
    private val userId = long("userId").primaryKey()
    private val postId = integer("postId").references(PostsTable.id).primaryKey()
    private val like = bool("like").default(false)

    fun userLikePost(userId: Long, postId: Int) {
        userLike(userId, postId, true)
    }

    fun userDislikePost(userId: Long, postId: Int) {
        userLike(userId, postId, false)
    }

    fun postLikes(postId: Int): Int = postLikeCount(postId, true)

    fun postDislikes(postId: Int): Int = postLikeCount(postId, false)

    fun getPostRating(postId: Int): Int {
        return transaction {
            select {
                createChooser(postId, like = true)
            }.count() - select {
                createChooser(postId, like = false)
            }.count()
        }
    }

    private fun postLikeCount(postId: Int, like: Boolean): Int = transaction {
        select {
            PostsLikesTable.postId.eq(postId).and(PostsLikesTable.like.eq(like))
        }.count()
    }

    private fun createChooser(postId: Int, userId: Long? = null, like: Boolean? = null): Op<Boolean> {
        return PostsLikesTable.postId.eq(postId).let {
            userId ?.let {
                userId ->
                it.and(PostsLikesTable.userId.eq(userId))
            } ?: it
        }.let {
            like ?. let {
                like ->
                it.and(PostsLikesTable.like.eq(like))
            } ?: it
        }
    }

    private fun userLike(userId: Long, postId: Int, like: Boolean) {
        val chooser = createChooser(postId, userId)
        transaction {
            val record = select {
                chooser
            }.firstOrNull()
            record ?.let {
                if (it[PostsLikesTable.like] == like) {
                    deleteWhere { chooser }
                } else {
                    update(
                        {
                            chooser
                        }
                    ) {
                        it[PostsLikesTable.like] = like
                    }
                }
            } ?:let {
                addUser(userId, postId, like)
            }
        }
    }

    private fun addUser(userId: Long, postId: Int, like: Boolean) {
        transaction {
            insert {
                it[PostsLikesTable.postId] = postId
                it[PostsLikesTable.userId] = userId
                it[PostsLikesTable.like] = like
            }
        }
    }
}