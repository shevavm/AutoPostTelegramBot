package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransaction
import com.github.insanusmokrassar.TelegramBotAPI.types.ChatIdentifier
import java.util.concurrent.ConcurrentHashMap

object CommonKnownPostsTransactions {
    private val usersTransactions = ConcurrentHashMap<ChatIdentifier, PostTransaction>()

    @Synchronized
    operator fun contains(chatIdentifier: ChatIdentifier): Boolean = usersTransactions[chatIdentifier] ?.let {
        !it.completed
    } ?: false

    @Synchronized
    fun startTransaction(chatIdentifier: ChatIdentifier): PostTransaction? = if (chatIdentifier in this) {
        null
    } else {
        PostTransaction().also {
            usersTransactions[chatIdentifier] = it
        }
    }

    @Synchronized
    operator fun get(chatIdentifier: ChatIdentifier): PostTransaction? = usersTransactions[chatIdentifier] ?.let {
        if (it.completed) {
            usersTransactions.remove(chatIdentifier)
            null
        } else {
            it
        }
    }

    @Synchronized
    fun getOrStart(chatIdentifier: ChatIdentifier): PostTransaction? = if (chatIdentifier in this) {
        usersTransactions[chatIdentifier]
    } else {
        startTransaction(chatIdentifier)
    }
}
