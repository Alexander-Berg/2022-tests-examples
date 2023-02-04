package ru.yandex.qe.dispenser.standalone

import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component
import ru.yandex.qe.dispenser.ws.dispatchers.CustomExecutor

/**
 * Observability executor blocking stub.
 *
 * @author Dmitriy Timashov <dm-tim@yandex-team.ru>
 */
@Component("observabilityExecutor")
@Primary
class ObservabilityExecutorBlockingStub: CustomExecutor {

    override fun launch(block: () -> Unit) {
        block()
    }

}
