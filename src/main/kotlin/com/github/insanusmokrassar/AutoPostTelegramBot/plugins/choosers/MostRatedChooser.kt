package com.github.insanusmokrassar.AutoPostTelegramBot.plugins.choosers

import com.github.insanusmokrassar.AutoPostTelegramBot.base.models.PostId
import com.github.insanusmokrassar.AutoPostTelegramBot.base.plugins.abstractions.RatingPair
import kotlinx.serialization.Serializable
import org.joda.time.DateTime

@Serializable
class MostRatedChooser : RateChooser() {
    override suspend fun triggerChoose(time: DateTime, exceptions: List<PostId>): Collection<Int> {
        val mostRated = mutableListOf<RatingPair>()
        ratingPlugin.getRegisteredPosts().flatMap {
            ratingPlugin.getPostRatings(it)
        }.forEach {
            val postId = ratingPlugin.resolvePostId(it.first) ?: return@forEach
            if (postId !in exceptions) {
                mostRated.firstOrNull() ?.also { (_, rating) ->
                    when {
                        it.second > rating -> {
                            mostRated.clear()
                            mostRated.add(it)
                        }
                        it.second == rating -> {
                            mostRated.add(it)
                        }
                    }
                } ?: mostRated.add(it)
            }
        }
        return mostRated.minBy { (ratingId, _) -> ratingId } ?.let { (ratingId, _) ->
            ratingPlugin.resolvePostId(ratingId) ?.let {
                listOf(it)
            }
        } ?: emptyList()
    }
}