package ru.yandex.market.clean.domain.usecase

import io.reactivex.schedulers.TestScheduler
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import ru.yandex.market.common.experiments.service.ExperimentConfigService
import ru.yandex.market.common.experiments.service.ExperimentConfigServiceHolder
import ru.yandex.market.presentationSchedulersMock

class WaitExperimentsUseCaseTest {

    private val timerScheduler = TestScheduler()
    private val workerScheduler = TestScheduler()

    private val experimentConfigServiceFactory = mock<ExperimentConfigServiceHolder>() {
        on { experimentConfigServiceInstance } doReturn experimentConfigService
    }

    private val experimentConfigService = mock<ExperimentConfigService>() {
        on { waitForActualizedConfigs() } doThrow RuntimeException()
    }

    private val presentationSchedulers = presentationSchedulersMock {
        on { timer } doReturn timerScheduler
        on { worker } doReturn workerScheduler
    }

    private val useCase = WaitExperimentsUseCase(
        experimentConfigServiceFactory,
        presentationSchedulers,
    )

    @Test
    fun `Cms load if experiments loading was failed`() {
        val testObserver = useCase.execute().test()

        workerScheduler.triggerActions()

        testObserver
            .assertComplete()
            .assertNoErrors()
    }
}