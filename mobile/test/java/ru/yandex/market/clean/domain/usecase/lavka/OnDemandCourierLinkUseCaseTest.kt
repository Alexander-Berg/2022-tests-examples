package ru.yandex.market.clean.domain.usecase.lavka

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.clean.data.repository.OnDemandCourierLinkRepository
import ru.yandex.market.clean.domain.model.lavka.OnDemandCourierLink
import ru.yandex.market.domain.auth.model.credentialsTestInstance
import ru.yandex.market.domain.auth.usecase.CredentialsUseCase
import ru.yandex.market.mockResult
import ru.yandex.market.optional.Optional

class OnDemandCourierLinkUseCaseTest {

    private val onDemandCourierLinkRepository = mock<OnDemandCourierLinkRepository>()
    private val credentialsUseCase = mock<CredentialsUseCase>()

    private val getLavkaCheckoutAndTrackingPathUseCase = mock<GetLavkaCheckoutAndTrackingPathUseCase> {
        on { execute(any(), any(), anyOrNull(), anyOrNull(), anyOrNull()) } doReturn Single.just(Optional.empty())
    }

    private val useCase = OnDemandCourierLinkUseCase(
        onDemandCourierLinkRepository = onDemandCourierLinkRepository,
        credentialsUseCase = credentialsUseCase,
        getLavkaCheckoutAndTrackingPathUseCase = getLavkaCheckoutAndTrackingPathUseCase,
    )

    @Test
    fun `check on correct getting courier links`() {

        val courierLink = OnDemandCourierLink("url", "appLink", true, null, false)

        onDemandCourierLinkRepository.getOnDemandCourierLink(any(), any(), any(), any())
            .mockResult(Single.just(courierLink))
        credentialsUseCase.getCredentials().mockResult(Single.just(credentialsTestInstance()))

        useCase.execute(true, "661", "", null, null, null)
            .test()
            .assertNoErrors()
            .assertResult(courierLink)

        verify(onDemandCourierLinkRepository).getOnDemandCourierLink(any(), any(), any(), any())
        verify(credentialsUseCase).getCredentials()
    }
}