package ru.yandex.market.domain.onboarding.usecase

import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.subjects.SingleSubject
import org.junit.Test
import org.mockito.ArgumentMatchers.anyList
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.domain.onboarding.model.OnboardingInfo
import ru.yandex.market.domain.onboarding.model.onboardingInfoTestInstance
import ru.yandex.market.domain.onboarding.model.onboardingSourceScreenTestInstance
import ru.yandex.market.domain.onboarding.repository.OnboardingRepository
import ru.yandex.market.safe.Safe

class GetOnboardingUseCaseTest {
    private val onboardingInfo: SingleSubject<List<Safe<OnboardingInfo>>> = SingleSubject.create()

    private val onboardingRepository = mock<OnboardingRepository> {
        on { getLastShownOnboardingDate() } doReturn Single.just(10L)
        on { getShownOnboardings() } doReturn Single.just(emptyList())
        on { getOnboardingToShow(anyLong(), anyList(), any()) } doReturn onboardingInfo
        on { setPages(any(), any()) } doReturn Completable.complete()
        on { isLaunchAfterWelcomeOnboarding() } doReturn Single.just(false)

    }

    private val processOnboardingsUseCase = mock<ProcessOnboardingsUseCase>()

    private val useCase = GetOnboardingUseCase(
        onboardingRepository = onboardingRepository,
        processOnboardingsUseCase = processOnboardingsUseCase
    )

    @Test
    fun `return info from repository`() {
        val expected = onboardingInfoTestInstance()
        whenever(processOnboardingsUseCase.execute(any())) doReturn Maybe.just(expected)
        onboardingInfo.onSuccess(listOf(Safe.value(expected)))
        useCase.getOnboardingToShow(onboardingSourceScreenTestInstance())
            .test()
            .assertValue(expected)
    }

    @Test
    fun `return empty from repository`() {
        whenever(processOnboardingsUseCase.execute(any())) doReturn Maybe.empty()
        onboardingInfo.onSuccess(emptyList())
        useCase.getOnboardingToShow(onboardingSourceScreenTestInstance())
            .test()
            .assertComplete()
    }

    @Test
    fun `do not handle error from repository`() {
        val expected = IllegalArgumentException()
        onboardingInfo.onError(expected)
        useCase.getOnboardingToShow(onboardingSourceScreenTestInstance())
            .test()
            .assertError(expected)
    }
}
