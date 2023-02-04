package ru.yandex.intranet.d

import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.apache.logging.log4j.core.LogEvent
import org.springframework.context.annotation.Profile
import org.springframework.core.Ordered
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import ru.yandex.intranet.d.kotlin.mono

@Component
@Profile("local", "dev", "integration-tests")
class LogCollectingFilter : WebFilter, Ordered {

    companion object {
        private val events = mutableListOf<LogEvent>()

        @JvmStatic
        fun events() = events as List<LogEvent>

        @JvmStatic
        fun clear() {
            events.clear()
            LogCollector.clear()
        }
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> = mono {
        LogCollector.collectLogs {
            chain.filter(exchange).awaitSingleOrNull()
        }
        events += LogCollector.takeLogEvents(from = events.size)
    }.then()

    override fun getOrder() = Ordered.LOWEST_PRECEDENCE
}
