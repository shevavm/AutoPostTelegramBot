package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.utils.initObject

val choosers = mapOf(
    "mostRated" to MostRatedChooser::class.java.canonicalName,
    "mostRatedRandom" to MostRatedRandomChooser::class.java.canonicalName
)

fun initChooser(chooserName: String, paramsSection: IObject<Any>? = null): Chooser {
    return choosers[chooserName] ?.let {
        initObject<Chooser>(it, paramsSection)
    } ?: throw IllegalArgumentException("Wrong name of chooser. Known choosers:\n${choosers.keys.joinToString()}")
}
