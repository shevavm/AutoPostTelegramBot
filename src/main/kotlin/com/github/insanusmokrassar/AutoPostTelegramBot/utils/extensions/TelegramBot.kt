package com.github.insanusmokrassar.AutoPostTelegramBot.utils.extensions

import com.pengrad.telegrambot.Callback
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.request.AnswerCallbackQuery
import com.pengrad.telegrambot.request.BaseRequest
import com.pengrad.telegrambot.response.BaseResponse
import kotlinx.coroutines.experimental.*
import org.slf4j.LoggerFactory
import java.io.IOException
import java.lang.ref.WeakReference
import kotlin.coroutines.experimental.suspendCoroutine

private val logger = LoggerFactory.getLogger("TelegramAsyncExecutions")

private class DefaultCallback<T: BaseRequest<T, R>, R: BaseResponse>(
        private val onFailureCallback: ((T, IOException?) -> Unit)?,
        private val onResponseCallback: ((T, R) -> Unit)?,
        bot: TelegramBot,
        private var retries: Int = 0,
        private val retriesDelay: Long = 1000L
) : Callback<T, R> {
    private val bot = WeakReference(bot)
    override fun onFailure(request: T, e: IOException?) {
        logger.warn("Request failure: {}; Error: {}", request, e)
        onFailureCallback ?. invoke(request, e)
        if (retries > 0) {
            async {
                delay(retriesDelay)
                bot.get() ?. executeAsync(
                    request,
                    onFailureCallback,
                    onResponseCallback,
                    retries - 1,
                    retriesDelay
                )
            }
        }
    }

    override fun onResponse(request: T, response: R) {
        logger.info("Request success: {}\nResponse: {}", request, response)
        if (response.isOk) {
            onResponseCallback ?. invoke(request, response)
        } else {
            onFailure(request, IOException(response.description()))
        }
    }
}

fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeAsync(
        request: T,
        onFailure: ((T, IOException?) -> Unit)? = null,
        onResponse: ((T, R) -> Unit)? = null,
        retries: Int = 0,
        retriesDelay: Long = 1000L
) {
    logger.info("Try to put request for executing: {}", request)
    execute(
            request,
            DefaultCallback(
                    onFailure,
                    onResponse,
                    this,
                    retries,
                    retriesDelay
            )
    )
}

@Throws(IOException::class, IllegalStateException::class)
fun <T: BaseRequest<T, R>, R: BaseResponse> TelegramBot.executeSync(
    request: T,
    retries: Int = 0,
    retriesDelay: Long = 1000L
): R {
    return runBlocking {
        suspendCoroutine<R> {
            continuation ->
            executeAsync(
                request,
                {
                    _, ioException ->
                    continuation.resumeWithException(
                        ioException ?: IllegalStateException("Something went wrong")
                    )
                },
                {
                    _, r ->
                    continuation.resume(r)
                },
                retries,
                retriesDelay
            )
        }
    }
}



fun TelegramBot.queryAnswer(
        id: String,
        answerText: String,
        asAlert: Boolean = false
) {
    executeAsync(
            AnswerCallbackQuery(
                    id
            )
                    .text(answerText)
                    .showAlert(asAlert)
    )
}


