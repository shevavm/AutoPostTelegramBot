package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.forwarders

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostMessage
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.model.request.ParseMode
import com.pengrad.telegrambot.request.SendMessage
import java.io.IOException

class TextForwarder : Forwarder {

    override val importance: Int = MIDDLE_PRIORITY

    override fun canForward(message: PostMessage): Boolean {
        return message.message ?. text() != null
    }

    override fun forward(bot: TelegramBot, targetChatId: Long, vararg messages: PostMessage): Map<PostMessage, Message> {
        return messages.mapNotNull {
            postMessage ->
            val message = postMessage.message ?: return@mapNotNull null
            postMessage to SendMessage(
                targetChatId,
                message.text()
            ).parseMode(
                ParseMode.Markdown
            )
        }.map {
            pair ->
            bot.execute(pair.second).let {
                response ->
                response.message() ?.let {
                    pair.first to it
                } ?:let {
                    throw IOException("${response.errorCode()}: ${response.description()}")
                }
            }
        }.toMap()
    }
}