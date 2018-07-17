package com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.commands

import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.exceptions.NoRowFoundException
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.database.tables.PostsTable
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.models.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginManager
import com.github.insanusmokrassar.TimingPostsTelegramBot.base.plugins.PluginVersion
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.choosers.Chooser
import com.github.insanusmokrassar.TimingPostsTelegramBot.plugins.publishers.Publisher
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class PublishPost : CommandPlugin() {
    override val version: PluginVersion = 0L
    override val commandRegex: Regex = Regex("^/publishPost( \\d+)?$")

    private var publisherWR = WeakReference<Publisher>(null)
    private var chooserWR = WeakReference<Chooser>(null)
    private var logsChatId: Long = 0

    override fun onInit(
        bot: TelegramBot,
        baseConfig: FinalConfig,
        pluginManager: PluginManager
    ) {
        super.onInit(bot, baseConfig, pluginManager)
        (pluginManager.plugins.firstOrNull { it is Publisher } as? Publisher) ?.also {
            publisherWR = WeakReference(it)
        }
        (pluginManager.plugins.firstOrNull { it is Chooser } as? Chooser) ?.also {
            chooserWR = WeakReference(it)
        }
        logsChatId = baseConfig.logsChatId
    }

    override fun onCommand(updateId: Int, message: Message) {
        val publisher = publisherWR.get() ?: return

        val choosen = mutableListOf<Int>()

        message.replyToMessage() ?.also {
            try {
                choosen.add(
                    PostsTable.findPost(it.messageId())
                )
            } catch (e: NoRowFoundException) {
                botWR ?.get() ?.executeAsync(
                    SendMessage(
                        message.chat().id(),
                        "Message is not related to any post"
                    ).replyToMessageId(
                        it.messageId()
                    )
                )
            }
        } ?:also {
            try {
                val chooser = chooserWR.get() ?: return
                val splitted = message.text().split(" ")
                val count = if (splitted.size > 1) {
                    splitted[1].toInt()
                } else {
                    1
                }

                while (choosen.size < count) {
                    val toAdd = chooser.triggerChoose().filter {
                        !choosen.contains(it)
                    }.let {
                        val futureSize = choosen.size + it.size
                        val toAdd = if (futureSize > count) {
                            futureSize - count
                        } else {
                            it.size
                        }
                        it.toList().subList(0, toAdd)
                    }
                    if (toAdd.isEmpty()) {
                        break
                    }
                    choosen.addAll(
                        toAdd
                    )
                }
            } catch (e: NumberFormatException) {
                println("Can't extract number of posts")
                return
            }
        }

        botWR ?.get() ?.let {
            bot ->

            bot.executeAsync(
                SendMessage(
                    logsChatId,
                    "Was chosen to publish: ${choosen.size}. (Repeats of choosing was excluded)"
                ).parseMode(
                    ParseMode.Markdown
                ),
                onResponse = {
                    _,  _ ->
                    choosen.forEach {
                        publisher.publishPost(
                            it
                        )
                    }
                }
            )
        }
    }
}