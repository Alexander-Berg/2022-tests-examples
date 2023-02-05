package ru.yandex.market.clean.domain.usecase.gdpr

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.clean.data.repository.PreferencesRepository
import ru.yandex.market.clean.data.repository.SystemInformationRepository
import ru.yandex.market.domain.models.region.CountryCode
import ru.yandex.market.clean.domain.model.LanguageCode

class ShouldShowGdprNotificationUseCaseTest {

    private val countryCodeUseCase = mock<GetCurrentCountryCodeUseCase>()

    private val systemInformationRepository = mock<SystemInformationRepository>()

    private val preferencesRepository = mock<PreferencesRepository>()

    private val useCase = ShouldShowGdprNotificationUseCase(
        countryCodeUseCase,
        systemInformationRepository, preferencesRepository
    )

    @Test
    fun `Should not show GDPR notification if both locale and location is Russia`() {
        whenever(preferencesRepository.wasGdprNotificationShown())
            .thenReturn(Single.just(false))
        whenever(systemInformationRepository.systemLanguage).thenReturn(Single.just(LanguageCode.RU))
        whenever(countryCodeUseCase.getCurrentCountryCode()).thenReturn(Single.just(CountryCode.RU))

        useCase.shouldShowNotification()
            .test()
            .assertResult(false)
    }

    @Test
    fun `Should not show GDPR notification was already shown`() {
        whenever(preferencesRepository.wasGdprNotificationShown()).thenReturn(Single.just(true))

        useCase.shouldShowNotification()
            .test()
            .assertResult(false)
    }

    @Test
    fun `Should show GDPR notification if locale is not Russian`() {
        whenever(preferencesRepository.wasGdprNotificationShown()).thenReturn(Single.just(false))
        whenever(systemInformationRepository.systemLanguage).thenReturn(Single.just(LanguageCode.UNKNOWN))
        whenever(countryCodeUseCase.getCurrentCountryCode()).thenReturn(Single.just(CountryCode.RU))

        useCase.shouldShowNotification()
            .test()
            .assertResult(true)
    }

    @Test
    fun `Should show GDPR notification if location is not Russia`() {
        whenever(preferencesRepository.wasGdprNotificationShown()).thenReturn(Single.just(false))
        whenever(systemInformationRepository.systemLanguage).thenReturn(Single.just(LanguageCode.RU))
        whenever(countryCodeUseCase.getCurrentCountryCode()).thenReturn(Single.just(CountryCode.UNKNOWN))

        useCase.shouldShowNotification()
            .test()
            .assertResult(true)
    }

    @Test
    fun `Should show GDPR notification if failed to get locale`() {
        whenever(preferencesRepository.wasGdprNotificationShown()).thenReturn(Single.just(false))
        whenever(systemInformationRepository.systemLanguage).thenReturn(
            Single.error(
                RuntimeException()
            )
        )

        useCase.shouldShowNotification()
            .test()
            .assertResult(true)
    }

    @Test
    fun `Should show GDPR notification if failed to get location`() {
        whenever(preferencesRepository.wasGdprNotificationShown()).thenReturn(Single.just(false))
        whenever(systemInformationRepository.systemLanguage).thenReturn(Single.just(LanguageCode.UNKNOWN))
        whenever(countryCodeUseCase.getCurrentCountryCode()).thenReturn(Single.error(RuntimeException()))

        useCase.shouldShowNotification()
            .test()
            .assertResult(true)
    }
}