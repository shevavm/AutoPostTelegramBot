package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.receivers

import com.github.insanusmokrassar.IObjectK.exceptions.ReadException
import com.github.insanusmokrassar.IObjectKRealisations.toIObject
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.CallbackQueryReceiver
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.disableLikesForPost
import com.github.insanusmokrassar.AutoPostTelegramBot.plugins.rating.database.PostsLikesMessagesTable
import com.github.insanusmokrassar.AutoPostTelegramBot.realMessagesListener
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.*
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.CallbackQuery
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

const val disable = "❌"

internal fun makeDisableText() = disable

private const val disableIdentifier = "disableRatings"

fun makeDisableInline(postId: Int): String = "$disableIdentifier: $postId"
fun extractDisableInline(from: String): Int? = try {
    from.toIObject().get<String>(disableIdentifier).toInt()
} catch (e: ReadException) {
    null
}

private fun makeTextToApproveRemove(postId: Int) =
    "Please, write to me `${makeDisableInline(postId)}` if you want to disable ratings for this post"

private typealias UserIdPostId = Pair<Long, Int>

class DisableReceiver(
    bot: TelegramBot,
    sourceChatId: Long,
    postsLikesMessagesTable: PostsLikesMessagesTable
) : CallbackQueryReceiver(bot) {
    private val awaitApprove = HashSet<UserIdPostId>()

    init {
        val botWR = WeakReference(bot)
        realMessagesListener.broadcastChannel.subscribeChecking {
            message ->
            val userId = message.second.chat().id()

            val bot = botWR.get() ?: return@subscribeChecking false
            awaitApprove.firstOrNull { it.first == userId } ?.let {
                val (userId, postId) = it
                if (extractDisableInline(message.second.text()) == postId) {
                    awaitApprove.remove(it)
                    disableLikesForPost(
                        postId,
                        bot,
                        sourceChatId,
                        postsLikesMessagesTable
                    )

                    bot.executeAsync(
                        SendMessage(
                            userId,
                            "Rating was disabled"
                        ).parseMode(
                            ParseMode.Markdown
                        )
                    )
                }
            } ?:let {
                val forwardFrom = message.second.forwardFromChat()
                if (forwardFrom != null && forwardFrom.id() == sourceChatId) {
                    val postId = postsLikesMessagesTable.postIdByMessageId(
                        message.second.forwardFromMessageId()
                    ) ?: return@let
                    bot.executeAsync(
                        SendMessage(
                            userId,
                            makeTextToApproveRemove(
                                postId
                            )
                        ).parseMode(
                            ParseMode.Markdown
                        ),
                        onResponse = {
                            _, _ ->
                            awaitApprove.add(userId to postId)
                        }
                    )
                }
            }
            true
        }
    }

    override fun invoke(
        query: CallbackQuery,
        bot: TelegramBot?
    ) {
        bot ?: return
        extractDisableInline(query.data())?.let {
            val userId = query.from().id().toLong()
            awaitApprove.add(userId to it)
            bot.queryAnswer(
                query.id(),
                makeTextToApproveRemove(it),
                true
            )
        }
    }
}