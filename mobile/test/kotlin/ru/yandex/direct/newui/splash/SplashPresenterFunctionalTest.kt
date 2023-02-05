// Copyright (c) 2018 Yandex LLC. All rights reserved.
// Author: Ivan Poroshin poroshin-ivan@yandex-team.ru

package ru.yandex.direct.newui.splash

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.doThrow
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
import com.yandex.passport.api.PassportUid
import io.reactivex.Completable
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import ru.yandex.direct.Configuration
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.DirectAppPinCode
import ru.yandex.direct.domain.RegionInfo
import ru.yandex.direct.domain.enums.Currency
import ru.yandex.direct.domain.enums.EventType
import ru.yandex.direct.domain.enums.PushPollingInterval
import ru.yandex.direct.eventbus.RxBus
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.interactor.dict.InterestsInteractor
import ru.yandex.direct.interactor.dict.LoadCurrencyInteractor
import ru.yandex.direct.interactor.dict.RegionsInteractor
import ru.yandex.direct.interactor.pincode.PinCodeInteractor
import ru.yandex.direct.interactor.push.PushInteractor
import ru.yandex.direct.remoteconfig.RemoteConfigInteractor
import ru.yandex.direct.repository.changes.ChangesLocalRepository
import ru.yandex.direct.repository.changes.ChangesRemoteRepository
import ru.yandex.direct.repository.changes.ChangesRepository
import ru.yandex.direct.repository.clients.ClientsRemoteQuery
import ru.yandex.direct.repository.clients.ClientsRemoteRepository
import ru.yandex.direct.repository.dicts.CurrencyQuery
import ru.yandex.direct.repository.dicts.CurrencyRemoteRepository
import ru.yandex.direct.repository.dicts.RegionsLocalQuery
import ru.yandex.direct.repository.dicts.RegionsRemoteQuery
import ru.yandex.direct.repository.dicts.RegionsRemoteRepository
import ru.yandex.direct.repository.push.PushSubscriptionRepository
import ru.yandex.direct.util.Optional
import ru.yandex.direct.utils.FunctionalTestEnvironment
import ru.yandex.direct.utils.SimpleLocalRepository
import ru.yandex.direct.web.api4.push.Subscription
import ru.yandex.direct.web.api5.IDirectApi5
import ru.yandex.direct.web.exception.ApiException
import java.io.IOException
import java.util.concurrent.TimeUnit

class SplashPresenterFunctionalTest {
    @Test
    fun presenter_shouldNavigateToAm_ifHasNoAuthData() {
        TestEnvironment().apply {
            stubAuthData(null, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view).navigateToAccountManager(any())
        }
    }

    @Test
    fun presenter_shouldBeginFadeInAnimation() {
        TestEnvironment().apply {
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view).beginFadeInAnimation()
        }
    }

    @Test
    fun presenter_shouldSaveAuthData_afterLogin() {
        TestEnvironment().apply {
            stubAuthData(null, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onLogIn(passportUid)
            scheduler.triggerActions()
            verify(configuration).passportUid = passportUid
        }
    }

    @Test
    fun presenter_shouldCheckAgency_afterLogin() {
        TestEnvironment().apply {
            stubAuthData(null, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onLogIn(passportUid)
            stubAuthData(passportUid, configuration)
            scheduler.triggerActions()
            verify(clientsRemoteRepository).fetch(ClientsRemoteQuery.ofAllAgencyClients())
        }
    }

    @Test
    fun presenter_shouldFinishActivity_ifLoginUnsuccessful() {
        TestEnvironment().apply {
            stubAuthData(null, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onLogIn(null)
            scheduler.triggerActions()
            verify(clientsRemoteRepository, never()).fetch(ClientsRemoteQuery.ofAllAgencyClients())
            verify(view).finish()
        }
    }

    @Test
    fun presenter_shouldCheckAgencyOnStartup_ifHasAuthData() {
        TestEnvironment().apply {
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view, never()).navigateToAccountManager(any())
            verify(clientsRemoteRepository).fetch(ClientsRemoteQuery.ofAllAgencyClients())
        }
    }

    @Test
    fun presenter_shouldLoadCurrentClient_ifNotIsAgency() {
        TestEnvironment().apply {
            stubAuthData(passportUid, configuration)
            whenever(clientsRemoteRepository.fetch(ClientsRemoteQuery.ofAllAgencyClients()))
                    .doThrow(ApiException(IDirectApi5.ErrorCode.INSUFFICIENT_PRIVILEGES, ""))
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view, never()).navigateToAgencyClientsList()
            verify(clientsRemoteRepository).fetch(ClientsRemoteQuery.ofIndependentClient())
        }
    }

    @Test
    fun presenter_shouldShowAgencyClientsList_ifAgency() {
        TestEnvironment().apply {
            stubAuthData(passportUid, configuration)
            whenever(clientsRemoteRepository.fetch(ClientsRemoteQuery.ofAllAgencyClients()))
                    .doReturn(testingClients)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view).navigateToAgencyClientsList()
            verify(clientsRemoteRepository, never()).fetch(ClientsRemoteQuery.ofIndependentClient())
        }
    }

    @Test
    fun presenter_shouldSaveAgencyClient_whenClientIsSelected() {
        TestEnvironment().apply {
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(testingClients[0])
            scheduler.triggerActions()
            verify(configuration).currentClient = testingClients[0]
        }
    }

    @Test
    fun presenter_shouldNavigateToAm_whenClientIsNull() {
        TestEnvironment().apply {
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(null)
            scheduler.triggerActions()
            verify(view).navigateToAccountManager(any())
        }
    }

    @Test
    fun presenter_shouldLoadCurrencyAndRegions_whenClientIsSelected() {
        TestEnvironment().apply {
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(testingClients[0])
            scheduler.triggerActions()
            verify(regionsRemoteRepository).fetch(RegionsRemoteQuery.ofAllRegions())
            verify(currencyRemoteRepository).fetch(CurrencyQuery.ofAllCurrencies())
        }
    }

    @Test
    fun presenter_shouldLoadCurrencyAndRegions_ifClientExistsInConfiguration() {
        TestEnvironment().apply {
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            whenever(configuration.currentClient).doReturn(testingClients[0])
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(regionsRemoteRepository).fetch(RegionsRemoteQuery.ofAllRegions())
            verify(currencyRemoteRepository).fetch(CurrencyQuery.ofAllCurrencies())
        }
    }

    @Test
    fun presenter_shouldSaveCurrencyAndRegions() {
        TestEnvironment().apply {
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            whenever(configuration.currentClient).doReturn(testingClients[0])
            whenever(regionsRemoteRepository.fetch(any())).doReturn(ApiSampleData.regions)
            whenever(currencyRemoteRepository.fetch(any())).doReturn(ApiSampleData.currency)
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            assertThat(currencyLocalRepository.select(CurrencyQuery.ofAllCurrencies()))
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(ApiSampleData.currency)
            assertThat(regionsLocalRepository.select(RegionsLocalQuery.byId(ApiSampleData.regions.map { r -> r.id })))
                    .usingRecursiveFieldByFieldElementComparator()
                    .isEqualTo(ApiSampleData.regions)
        }
    }

    @Test
    fun presenter_shouldNavigateToMainActivity_whenAgencyClientIsSelected() {
        TestEnvironment().apply {
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(testingClients[0])
            scheduler.triggerActions()
            verify(view).navigateToMainActivity()
        }
    }

    @Test
    fun presenter_shouldNavigateToMainActivity_whenLoginSuccessAndNotIsAgency() {
        TestEnvironment().apply {
            stubAuthData(null, configuration)
            whenever(clientsRemoteRepository.fetch(ClientsRemoteQuery.ofIndependentClient())).doReturn(testingClients)
            whenever(clientsRemoteRepository.fetch(ClientsRemoteQuery.ofAllAgencyClients()))
                    .doThrow(ApiException(IDirectApi5.ErrorCode.INSUFFICIENT_PRIVILEGES, ""))
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            whenever(regionsRemoteRepository.fetch(any())).doReturn(ApiSampleData.regions)
            whenever(currencyRemoteRepository.fetch(any())).doReturn(ApiSampleData.currency)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onLogIn(passportUid)
            whenever(configuration.passportUid) doReturn passportUid
            scheduler.triggerActions()
            verify(view).navigateToMainActivity()
        }
    }

    @Test
    fun presenter_shouldNavigateToMainActivity_ifClientIsAlreadyLoggedIn() {
        TestEnvironment().apply {
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllNo)
            whenever(configuration.currentClient).doReturn(testingClients[0])
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            verify(view).navigateToMainActivity()
        }
    }

    @Test
    fun presenter_shouldLoadCurrencyBeforeClientInfo() {
        TestEnvironment().apply {
            whenever(currencyRemoteRepository.fetch(CurrencyQuery.ofAllCurrencies()))
                    .doReturn(ApiSampleData.currency)
            whenever(clientsRemoteRepository.fetch(ClientsRemoteQuery.ofIndependentClient()))
                    .doReturn(testingClients)
            whenever(clientsRemoteRepository.fetch(ClientsRemoteQuery.ofAllAgencyClients()))
                    .doThrow(ApiException(IDirectApi5.ErrorCode.INSUFFICIENT_PRIVILEGES, ""))
            whenever(regionsRemoteRepository.fetch(RegionsRemoteQuery.ofAllRegions()))
                    .doReturn(ApiSampleData.regions)

            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onLogIn(passportUid)
            whenever(configuration.passportUid) doReturn passportUid
            scheduler.triggerActions()

            val inOrder = inOrder(currencyRemoteRepository, clientsRemoteRepository)
            inOrder.verify(currencyRemoteRepository).fetch(CurrencyQuery.ofAllCurrencies())
            inOrder.verify(clientsRemoteRepository).fetch(ClientsRemoteQuery.ofAllAgencyClients())
            inOrder.verify(clientsRemoteRepository).fetch(ClientsRemoteQuery.ofIndependentClient())
            inOrder.verifyNoMoreInteractions()
        }
    }

    @Test
    fun presenter_shouldSubscribeToPushNotifications_ifLoginSuccessful() {
        TestEnvironment().apply {
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            whenever(pushSettingsRepository.subscription).doThrow(
                ApiException(IDirectApi5.ErrorCode.SUBSCRIPTION_NOT_FOUND, null)
            )
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(testingClients[0])
            scheduler.triggerActions()
            scheduler.advanceTimeBy(1, TimeUnit.MINUTES)     // Wait delayed subscribe()
            scheduler.triggerActions()
            verify(pushSettingsRepository).subscription
            verify(pushSettingsRepository).saveSubscription(any(), any())
        }
    }

    @Test
    fun presenter_shouldNotSubscribeToPushNotifications_ifSubscriptionAlreadyExist() {
        TestEnvironment().apply {
            whenever(pushSettingsRepository.subscription).doReturn(Subscription())
            whenever(changesRemoteRepository.fetch(any())).doReturn(ApiSampleData.dictionaryResultAllYes)
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(testingClients[0])
            scheduler.triggerActions()
            verify(pushSettingsRepository).subscription
            verify(pushSettingsRepository, never()).saveSubscription(any(), any())
        }
    }

    @Test
    fun presenter_shouldNotSubscribeToPushNotifications_ifGotApiError() {
        TestEnvironment().apply {
            whenever(currencyRemoteRepository.fetch(any())).doThrow(IOException())
            stubAuthData(passportUid, configuration)
            presenter.attachView(view, null)
            presenter.beginLoginSequence()
            scheduler.triggerActions()
            presenter.onAgencyClientSelected(testingClients[0])
            scheduler.triggerActions()
            verify(pushSettingsRepository, never()).subscription
            verify(pushSettingsRepository, never()).saveSubscription(any(), any())
        }
    }

    private fun stubAuthData(passportUid: PassportUid?, configuration: Configuration) {
        configuration.stub {
            on { getPassportUid() } doReturn passportUid
        }
    }

    private class TestEnvironment : FunctionalTestEnvironment() {
        val testingClients = listOf(ApiSampleData.clientInfo)

        val passportUid = mock<PassportUid>()

        val clientsRemoteRepository = mock<ClientsRemoteRepository> {
            on { fetch(any()) } doReturn testingClients
        }

        val currencyLocalRepository = SimpleLocalRepository<CurrencyQuery, List<Currency>> { _, currency ->
            currency ?: emptyList()
        }

        val currencyRemoteRepository = mock<CurrencyRemoteRepository> {
            on { fetch(any()) } doReturn ApiSampleData.currency
        }

        val regionsLocalRepository = SimpleLocalRepository<RegionsLocalQuery, List<RegionInfo>> { query, regions ->
            (regions?.apply { query.ids?.map { this.first { region -> region.id == it } } }) ?: emptyList()
        }

        val regionsRemoteRepository = mock<RegionsRemoteRepository> {
            on { fetch(any()) } doReturn ApiSampleData.regions
        }

        val changesLocalRepository = mock<ChangesLocalRepository> {
            on { select(any()) } doReturn Optional.nothing()
        }

        val changesRemoteRepository = mock<ChangesRemoteRepository>()
        val changesRepository = ChangesRepository(changesLocalRepository,
                changesRemoteRepository, scheduler, scheduler)

        val currentClientInteractor = CurrentClientInteractor(clientsRemoteRepository,
                configuration, scheduler, scheduler)

        val currencyInteractor = LoadCurrencyInteractor(currencyLocalRepository,
                currencyRemoteRepository, scheduler, scheduler)

        val regionsInteractor = RegionsInteractor(regionsLocalRepository,
                regionsRemoteRepository, changesRepository, scheduler, scheduler)

        val pinCodeInteractor = mock<PinCodeInteractor> {
            on { pinCode } doReturn Single.just(DirectAppPinCode.fromString(null))
            on { syncStashedPinCode() } doReturn Completable.complete()
        }

        val pushSettingsRepository = mock<PushSubscriptionRepository>()

        val pushSettingsInteractor = PushInteractor(
            configuration,
            pushSettingsRepository, scheduler, scheduler,
            mock()
        )

        val interestsInteractor = InterestsInteractor(mock(), mock(), scheduler, scheduler)

        val remoteConfigInteractor = mock<RemoteConfigInteractor> {
            on { tryLoadRemoteConfig() } doReturn Completable.complete()
        }

        val rxBus = RxBus()

        val presenter = SplashPresenter(mock(), defaultErrorResolution, currentClientInteractor, currencyInteractor,
                regionsInteractor, passportInteractor, scheduler, mock(), mock(), pinCodeInteractor,
                pushSettingsInteractor, mock(), interestsInteractor, remoteConfigInteractor, rxBus)

        val view = mock<SplashView>().stubViewMethods()

        init {
            configuration.stub {
                on { pushPollingInterval } doReturn PushPollingInterval.ONE_HOUR
                on { eventTypesForPush } doReturn EventType.getEventTypesForPush().toSet()
            }
        }
    }
}
