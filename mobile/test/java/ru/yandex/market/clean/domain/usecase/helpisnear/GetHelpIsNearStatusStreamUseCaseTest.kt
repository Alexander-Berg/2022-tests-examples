package ru.yandex.market.clean.domain.usecase.helpisnear

import com.annimon.stream.Optional
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.yandex.market.clean.data.repository.helpisnear.HelpIsNearRepository
import ru.yandex.market.clean.domain.model.HelpIsNearSubscriptionStatus
import ru.yandex.market.domain.auth.model.authTokenTestInstance
import ru.yandex.market.domain.auth.usecase.GetAuthTokenStreamUseCase
import ru.yandex.market.domain.money.model.moneyTestInstance
import ru.yandex.market.mockResult

class GetHelpIsNearStatusStreamUseCaseTest {

    private val getAuthTokenStreamUseCase = mock<GetAuthTokenStreamUseCase>()
    private val helpIsNearRepository = mock<HelpIsNearRepository>()

    private val useCase = GetHelpIsNearStatusStreamUseCase(

        getAuthTokenStreamUseCase = getAuthTokenStreamUseCase,
        helpIsNearRepository = helpIsNearRepository
    )

    @Test
    fun `check getting with auth token`() {

        getAuthTokenStreamUseCase.getAuthTokenStream().mockResult(Observable.just(Optional.of(authTokenTestInstance())))
        helpIsNearRepository.getHelpIsNearSubscriptionStatus().mockResult(Single.just(STATUS))

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(STATUS)

        verify(getAuthTokenStreamUseCase).getAuthTokenStream()
        verify(helpIsNearRepository).getHelpIsNearSubscriptionStatus()
    }

    @Test
    fun `check getting without auth token`() {

        getAuthTokenStreamUseCase.getAuthTokenStream().mockResult(Observable.just(Optional.empty()))
        helpIsNearRepository.getHelpIsNearSubscriptionStatus().mockResult(Single.just(STATUS))

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertResult(HelpIsNearSubscriptionStatus.NOT_SUBSCRIBED)

        verify(getAuthTokenStreamUseCase).getAuthTokenStream()
        verifyZeroInteractions(helpIsNearRepository)
    }

    private companion object {

        val STATUS = HelpIsNearSubscriptionStatus(true, moneyTestInstance())
    }
}
