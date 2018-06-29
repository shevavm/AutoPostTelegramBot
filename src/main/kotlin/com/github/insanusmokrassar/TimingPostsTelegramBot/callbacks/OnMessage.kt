package com.github.insanusmokrassar.TimingPostsTelegramBot.callbacks

import com.github.insanusmokrassar.BotIncomeMessagesListener.UpdateCallback
import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.FinalConfig
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.FixPost
import com.github.insanusmokrassar.TimingPostsTelegramBot.commands.StartPost
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostTransactionTable
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message

private val commandRegex = Regex("^/[^\\s]*")

class OnMessage(
    private val config: FinalConfig,
    private val startPost: StartPost,
    private val fixPost: FixPost
) : UpdateCallback<Message> {

    private val commands = mapOf(
        "/startPost" to startPost,
        "/fixPost" to fixPost
    )

    override fun invoke(id: Int, update: IObject<Any>, message: Message) {
        if (message.chat().id().toString() == config.sourceChatId
            || message.chat().username() == config.sourceChatId) {
            message.text() ?. let {
                if (it.startsWith("/")) {
                    val command = commandRegex.find(it) ?. value
                    commands[command] ?. invoke(id, update, message)
                } else {
                    null
                }
            } ?:let {
                if (PostTransactionTable.inTransaction) {
                    PostTransactionTable.addMessageId(message.messageId())
                } else {
                    startPost(id, update, message)
                    PostTransactionTable.addMessageId(message.messageId())
                    fixPost(id, update, message)
                }
            }
        }
    }
}