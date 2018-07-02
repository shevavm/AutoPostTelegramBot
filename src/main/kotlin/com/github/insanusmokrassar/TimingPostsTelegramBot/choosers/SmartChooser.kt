package com.github.insanusmokrassar.TimingPostsTelegramBot.choosers

import com.github.insanusmokrassar.IObjectK.interfaces.IObject
import com.github.insanusmokrassar.IObjectKRealisations.toObject
import com.github.insanusmokrassar.TimingPostsTelegramBot.database.tables.PostsLikesTable
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.DateTimeFormat
import java.util.*

private val timeFormat = DateTimeFormat.forPattern("HH:mm")
private const val next24Hour = 24 * 60 * 60 * 1000L

private fun String.toTime(offset: String = "+00:00"): DateTime {
    return timeFormat.withZone(
        DateTimeZone.forID(offset)
    ).parseDateTime(
        this
    )
}

private fun Long.fromTime(offset: String = "+00:00"): String {
    return timeFormat.withZone(
        DateTimeZone.forID(offset)
    ).print(
        this
    )
}

private class SmartChooserConfigItem (
    val minRate: Int? = null,
    val maxRate: Int? = null,
    val timeOffset: String = "+00:00",
    val time: Array<String?> = arrayOf(
        "00:00",
        null
    )
) {
    private var realTimePairs: List<Pair<Long, Long>>? = null

    private val timePairs: List<Pair<Long, Long>>
        get() {
            return realTimePairs ?:let {
                val pairs = mutableListOf<Pair<Long, Long>>()
                var currentPair: Pair<Long?, Long?>? = null
                time.forEach {
                    s ->
                    currentPair ?.let {
                        currentPairNN ->
                        val first = currentPairNN.first ?: 0L
                        val second = s ?. toTime(timeOffset) ?. millis ?: next24Hour

                        if (first > second) {
                            pairs.add(first to next24Hour)
                            pairs.add(0L to second)
                        } else {
                            pairs.add(first to second)
                        }

                        currentPair = null
                    } ?:let {
                        currentPair = s ?. toTime(timeOffset) ?. millis to null
                    }
                }
                realTimePairs = pairs
                pairs
            }
        }

    val actual: Boolean
        get() {
            DateTime.now().let {
                timeFormat.print(it.millis)
            }.let {
                timeFormat.parseDateTime(it).millis
            }.let {
                now ->
                timePairs.forEach {
                    if (it.first <= now && now < it.second) {
                        return true
                    }
                }
            }
            return false
        }

    override fun toString(): String {
        val stringBuilder = StringBuilder()
        stringBuilder.append("Rating: ${minRate ?: "any low"} - ${maxRate ?: "any big"}\n")
        stringBuilder.append("Time:\n")
        timePairs.forEach {
            stringBuilder.append("  ${it.first.fromTime(timeOffset)} - ${it.second.fromTime(timeOffset)}\n")
        }
        return stringBuilder.toString()
    }
}

private class SmartChooserConfig(
    val times: List<SmartChooserConfigItem> = emptyList(),
    val countToChoose: Int = 1,
    val pickRandom: Boolean = true
)

class SmartChooser(
    config: IObject<Any>
) : Chooser {
    private val config = config.toObject(SmartChooserConfig::class.java)
    private val random: Random? = if (this.config.pickRandom) {
        Random()
    } else {
        null
    }

    init {
        println("Smart chooser inited: ${this.config.times.joinToString(separator = "\n") { it.toString() }}")
        println("Actual: ${this.config.times.firstOrNull { it.actual } ?.toString() ?: "Nothing"}")
    }

    override fun triggerChoose(): Collection<Int> {
        return config.times.firstOrNull { it.actual } ?.let {
            PostsLikesTable.getRateRange(
                it.minRate,
                it.maxRate
            )
        } ?.let {
            chosenList ->
            random ?.let {
                random ->
                val mutableChosen = chosenList.toMutableList()
                val result = mutableListOf<Int>()
                while (result.size < config.countToChoose && mutableChosen.size > 0) {
                    mutableChosen.removeAt(
                        random.nextInt(mutableChosen.size)
                    ).let {
                        result.add(it)
                    }
                }
                result
            } ?:let {
                if (config.countToChoose > chosenList.size) {
                    chosenList
                } else {
                    chosenList.subList(
                        0,
                        config.countToChoose
                    )
                }
            }
        } ?: emptyList()
    }
}