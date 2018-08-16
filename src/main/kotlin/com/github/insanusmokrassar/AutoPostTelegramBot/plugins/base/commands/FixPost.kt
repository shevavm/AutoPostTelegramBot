package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.base.commands

import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.PostTransactionTable
import com.github.insanusmokrassar.AutoPostTelegramBot.base.database.exceptions.NothingToSaveException
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.commands.Command
import com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions.executeAsync
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Message
import com.pengrad.telegrambot.request.SendMessage
import java.lang.ref.WeakReference

class FixPost(
    private val botWR: WeakReference<TelegramBot>
) : Command() {
    override val commandRegex: Regex = Regex("^/fixPost$")

    override fun onCommand(updateId: Int, message: Message) {
        try {
            PostTransactionTable.saveNewPost()
        } catch (e: NothingToSaveException) {
            botWR.get() ?.executeAsync(
                SendMessage(
                    message.chat().id(),
                    "Nothing to save, transaction is empty"
                )
            )
        }
    }
}