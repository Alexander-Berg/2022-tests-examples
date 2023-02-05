package ru.yandex.market.clean.domain.usecase.onboarding

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.common.experiments.experiment.onboarding.WelcomeOnboardingConfig
import ru.yandex.market.data.regions.SelectedRegionRepository
import ru.yandex.market.domain.onboarding.model.OnboardingState
import ru.yandex.market.domain.onboarding.usecase.GetOnboardingConfigUseCase
import ru.yandex.market.domain.onboarding.usecase.GetOnboardingWasShownUseCase
import java.lang.IllegalStateException

class GetOnboardingStateUseCaseTest {

    private val selectedRegionRepository = mock<SelectedRegionRepository>()
    private val getOnboardingConfigUseCase = mock<GetOnboardingConfigUseCase>()
    private val getOnboardingWasShownUseCase = mock<GetOnboardingWasShownUseCase>()

    private val useCase = GetOnboardingStateUseCase(
        selectedRegionRepository = selectedRegionRepository,
        getOnboardingConfigUseCase = getOnboardingConfigUseCase,
        getOnboardingWasShownUseCase = getOnboardingWasShownUseCase
    )

    @Test
    fun `return error`() {
        val error = IllegalStateException()
        whenever(selectedRegionRepository.isRegionSelectedSingle()) doReturn Single.error(error)
        whenever(getOnboardingWasShownUseCase.getOnboardingWasShown()) doReturn Single.error(error)
        whenever(getOnboardingConfigUseCase.execute()) doReturn Single.error(error)
        useCase.execute().test().assertError(error)
    }

    @Test
    fun `return ShowWelcomeOnboarding state`() {
        val expected = OnboardingState.ShowWelcomeOnboarding(
            isFirstLaunch = true,
        )
        whenever(selectedRegionRepository.isRegionSelectedSingle()) doReturn Single.just(false)
        whenever(getOnboardingWasShownUseCase.getOnboardingWasShown()) doReturn Single.just(false)
        whenever(getOnboardingConfigUseCase.execute()) doReturn Single.just(
            WelcomeOnboardingConfig(
                withOnboarding = true,
            )
        )
        useCase.execute().test().assertValue(expected)
    }

    @Test
    fun `return ShowOtherOnboarding state`() {
        val expected = OnboardingState.ShowOtherOnboarding(
            isFirstLaunch = false,
        )
        whenever(selectedRegionRepository.isRegionSelectedSingle()) doReturn Single.just(true)
        whenever(getOnboardingWasShownUseCase.getOnboardingWasShown()) doReturn Single.just(true)
        whenever(getOnboardingConfigUseCase.execute()) doReturn Single.just(
            WelcomeOnboardingConfig(
                withOnboarding = true,
            )
        )
        useCase.execute().test().assertValue(expected)
    }

    @Test
    fun `return OnboardingWasShownOrSkipped state and onboarding was shown`() {
        val expected = OnboardingState.OnboardingWasShownOrSkipped(
            isFirstLaunch = true,
            wasShown = false
        )
        whenever(selectedRegionRepository.isRegionSelectedSingle()) doReturn Single.just(false)
        whenever(getOnboardingWasShownUseCase.getOnboardingWasShown()) doReturn Single.just(true)
        whenever(getOnboardingConfigUseCase.execute()) doReturn Single.just(
            WelcomeOnboardingConfig(
                withOnboarding = true,
            )
        )
        useCase.execute().test().assertValue(expected)
    }

    @Test
    fun `return OnboardingWasShownOrSkipped state and onboarding was skipped`() {
        val expected = OnboardingState.OnboardingWasShownOrSkipped(
            isFirstLaunch = true,
            wasShown = true
        )
        whenever(selectedRegionRepository.isRegionSelectedSingle()) doReturn Single.just(false)
        whenever(getOnboardingWasShownUseCase.getOnboardingWasShown()) doReturn Single.just(false)
        whenever(getOnboardingConfigUseCase.execute()) doReturn Single.just(
            WelcomeOnboardingConfig(
                withOnboarding = false,
            )
        )
        useCase.execute().test().assertValue(expected)
    }

    @Test
    fun `return OnboardingWasShownOrSkipped state and onboarding will not be shown`() {
        val expected = OnboardingState.OnboardingWasShownOrSkipped(
            isFirstLaunch = true,
            wasShown = true
        )
        whenever(selectedRegionRepository.isRegionSelectedSingle()) doReturn Single.just(false)
        whenever(getOnboardingWasShownUseCase.getOnboardingWasShown()) doReturn Single.just(true)
        whenever(getOnboardingConfigUseCase.execute()) doReturn Single.just(
            WelcomeOnboardingConfig(
                withOnboarding = false,
            )
        )
        useCase.execute().test().assertValue(expected)
    }
}
