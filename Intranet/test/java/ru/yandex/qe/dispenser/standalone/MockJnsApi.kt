package ru.yandex.qe.dispenser.standalone

import mu.KotlinLogging
import ru.yandex.qe.dispenser.domain.jns.JnsApi
import ru.yandex.qe.dispenser.domain.jns.JnsRequest
import java.util.concurrent.atomic.AtomicReference
import java.util.function.Consumer

private val logger = KotlinLogging.logger {}

class MockJnsApi: JnsApi {
    var sendHandler: Consumer<JnsRequest>? = null
    private val lastRequest: AtomicReference<JnsRequest> = AtomicReference()

    override fun send(request: JnsRequest) {
        lastRequest.set(request)
        logger.info { "Got notification: $request" }
        sendHandler?.accept(request)
    }

    fun lastRequest(): JnsRequest? = lastRequest.get()

    fun reset() {
        lastRequest.set(null)
    }
}
