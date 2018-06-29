package com.github.insanusmokrassar.TimingPostsTelegramBot.utils

fun makeLinkToMessage(
    username: String,
    messageId: Int
): String = "https://telegram.me/$username/$messageId"

fun makePhotoLink(
    botToken: String,
    filePath: String
) = "https://api.telegram.org/file/bot$botToken/$filePath"
