package ru.yandex.direct.newui.events

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doReturn
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.stub
import com.nhaarman.mockito_kotlin.verify
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import ru.yandex.direct.data.ApiSampleData
import ru.yandex.direct.domain.ShortCampaignInfo
import ru.yandex.direct.domain.enums.EventStatus
import ru.yandex.direct.domain.enums.EventType
import ru.yandex.direct.domain.enums.PushPollingInterval
import ru.yandex.direct.domain.events.LightWeightEvent
import ru.yandex.direct.eventbus.RxBus
import ru.yandex.direct.eventbus.event.PushNotificationEvent
import ru.yandex.direct.interactor.account.SharedAccountInteractor
import ru.yandex.direct.interactor.banners.BannersInteractor
import ru.yandex.direct.interactor.campaigns.CampaignsInteractor
import ru.yandex.direct.interactor.clients.CurrentClientInteractor
import ru.yandex.direct.interactor.events.EventsInteractor
import ru.yandex.direct.interactor.phrases.PhrasesInteractor
import ru.yandex.direct.newui.events.navigation.NavigationScenarioImpl
import ru.yandex.direct.repository.events.EventsLocalRepository
import ru.yandex.direct.repository.events.EventsRemoteRepository
import ru.yandex.direct.repository.events.LocalEventsQuery
import ru.yandex.direct.util.Optional
import ru.yandex.direct.utils.FunctionalTestEnvironment

class EventsListPresenterFunctionalTest {

    companion object {
        private val firstCampaign = ShortCampaignInfo().apply {
            campaignId = 1L
        }

        private val secondCampaign = ShortCampaignInfo().apply {
            campaignId = 2L
        }
    }

    private val events = EventType.values()
        .filter { it != EventType.UNKNOWN }
        .map {
            LightWeightEvent().apply {
                eventType = it
                timestamp = "1970-01-01T00:00:00Z"
                campaignID = 0
            }
        }

    private val freshEvent = LightWeightEvent().apply {
        eventType = EventType.MONEY_IN
        timestamp = "1970-01-01T00:00:00Z"
        eventStatus = EventStatus.FRESH
        campaignID = firstCampaign.id
    }

    private val staleEvent = LightWeightEvent().apply {
        eventType = EventType.MONEY_OUT
        timestamp = "1970-01-01T00:00:00Z"
        eventStatus = EventStatus.STALE
        campaignID = firstCampaign.id
    }

    private val firstCampaignEvent = LightWeightEvent().apply {
        eventType = EventType.MONEY_IN
        timestamp = "1970-01-01T00:00:00Z"
        campaignID = firstCampaign.id
    }

    private val secondCampaignEvent = LightWeightEvent().apply {
        eventType = EventType.MONEY_OUT
        timestamp = "1970-01-01T00:00:00Z"
        campaignID = secondCampaign.id
    }

    private lateinit var environment: Environment

    @Before
    fun runBeforeAnyTest() {
        environment = Environment()
        environment.localRepository.stub {
            on { select(any()) } doReturn events
        }
    }

    @Test
    fun basicTest() {
        with(environment) {
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showContent(events)
        }
    }

    @Test
    fun eventsFilter_shouldWork_withEachSingleEventTypeExceptUnknown() {
        with(environment) {
            presenter.attachView(view, null)
            scheduler.triggerActions()

            for (eventType in EventType.values().filter { it != EventType.UNKNOWN }) {
                configuration.stub { on { eventTypesToShow } doReturn listOf(eventType) }
                presenter.beginLoadEvents()
                scheduler.triggerActions()
                verify(view).showContent(events.filter { it.eventType == eventType })
            }
        }
    }

    @Test
    fun eventsFilter_shouldWork_forCampaign() {
        with(environment) {
            localRepository.stub {
                on { select(LocalEventsQuery.forCampaignEvents(firstCampaign.id)) } doReturn
                        listOf(firstCampaignEvent)
                on { select(LocalEventsQuery.forAllEvents()) } doReturn
                        listOf(firstCampaignEvent, secondCampaignEvent)
            }
            presenter.setCampaignId(firstCampaign.id)
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showContent(listOf(firstCampaignEvent))
        }
    }

    @Test
    fun eventsFilter_shouldWork_withAllEventsTypesWithoutSingleOne() {
        with(environment) {
            presenter.attachView(view, null)
            scheduler.triggerActions()

            for (eventType in EventType.values().filter { it != EventType.UNKNOWN }) {
                configuration.stub { on { eventTypesToShow } doReturn allEventTypesExcept(eventType) }
                presenter.beginLoadEvents()
                scheduler.triggerActions()
                verify(view).showContent(events.filter { it.eventType != eventType })
            }
        }
    }

    @Test
    fun eventsFilter_shouldWork_forFreshEvents() {
        with(environment) {
            presenter.attachView(view, null)
            scheduler.triggerActions()

            configuration.stub { on { areOnlyFreshEventsShown() } doReturn true }
            localRepository.stub { on { select(any()) } doReturn listOf(freshEvent, staleEvent) }
            presenter.beginLoadEvents()
            scheduler.triggerActions()
            verify(view).showContent(listOf(freshEvent))
        }
    }

    @Test
    fun interactor_shouldFilterEvent_ifEventTypeUnknownOrNull() {
        with(environment) {
            val unknownEvent = LightWeightEvent().apply { eventType = EventType.UNKNOWN }
            val nullEvent = LightWeightEvent().apply { eventType = null }
            localRepository.stub {
                on { select(LocalEventsQuery.forAllEvents()) } doReturn listOf(unknownEvent, nullEvent)
            }
            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showContent(emptyList())
        }
    }

    @Test
    fun interactor_shouldShowEvent_ifCampaignIsMissingOnServer() {
        with(environment) {
            localRepository.stub {
                on { select(LocalEventsQuery.forAllEvents()) } doReturn listOf(firstCampaignEvent, secondCampaignEvent)
            }
            campaignsInteractor.stub {
                on { getCachedCampaignsByIds(any()) } doReturn Single.just(listOf(firstCampaign))
                on { fetchCampaignsByIdForced(any()) } doReturn Single.just(listOf(firstCampaign))
            }

            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showContent(listOf(firstCampaignEvent, secondCampaignEvent))
        }
    }

    @Test
    fun interactor_shouldShowEvent_ifCampaignIsMissingOnCacheButPresentOnServer() {
        with(environment) {
            localRepository.stub {
                on { select(LocalEventsQuery.forAllEvents()) } doReturn listOf(firstCampaignEvent)
            }
            campaignsInteractor.stub {
                on { getCachedCampaignsByIds(any()) } doReturn Single.just(emptyList())
                on { fetchCampaignsByIdForced(any()) } doReturn Single.just(listOf(firstCampaign))
            }

            presenter.attachView(view, null)
            scheduler.triggerActions()
            verify(view).showContent(listOf(firstCampaignEvent))
        }
    }

    private fun allEventTypesExcept(eventType: EventType): List<EventType> {
        return EventType.values().filter { it != eventType }
    }

    class Environment : FunctionalTestEnvironment() {

        val localRepository = mock<EventsLocalRepository>().stub {
            on { containsActualData(any()) } doReturn true
            on { mostRecentEvent } doReturn Optional.nothing<LightWeightEvent>()
        }

        val remoteRepository = mock<EventsRemoteRepository>()

        val campaignsInteractor = mock<CampaignsInteractor>().stub {
            on {
                getCachedCampaignsByIds(emptyList())
            } doReturn Single.just(emptyList())

            on {
                getCachedCampaignsByIds(listOf(firstCampaign.id))
            } doReturn Single.just(listOf(firstCampaign))

            on {
                getCachedCampaignsByIds(listOf(secondCampaign.id))
            } doReturn Single.just(listOf(secondCampaign))

            on {
                getCachedCampaignsByIds(listOf(firstCampaign.id, secondCampaign.id))
            } doReturn Single.just(listOf(firstCampaign, secondCampaign))

            on {
                fetchCampaignsByIdForced(emptyList())
            } doReturn Single.just(emptyList())

            on {
                fetchCampaignsByIdForced(listOf(firstCampaign.id))
            } doReturn Single.just(listOf(firstCampaign))

            on {
                fetchCampaignsByIdForced(listOf(secondCampaign.id))
            } doReturn Single.just(listOf(secondCampaign))

            on {
                fetchCampaignsByIdForced(listOf(firstCampaign.id, secondCampaign.id))
            } doReturn Single.just(listOf(firstCampaign, secondCampaign))
        }

        val bannersInteractor = mock<BannersInteractor>()

        val phrasesInteractor = mock<PhrasesInteractor>()

        val sharedAccountInteractor = mock<SharedAccountInteractor>()

        val currentClientInteractor = mock<CurrentClientInteractor>()

        val navigationScenario = NavigationScenarioImpl(configuration, mock())

        val adapter = mock<EventsListAdapter>().stub {
            on { clicks } doReturn Observable.empty()
        }

        val view = mock<EventsListView>().stubAdapterViewMethods(adapter).stub {
            on { refreshSwipes } doReturn Observable.empty()
        }

        val eventsInteractor = EventsInteractor(
            localRepository,
            remoteRepository,
            scheduler,
            scheduler,
            scheduler,
            configuration,
            campaignsInteractor
        )

        val eventBus = mock<RxBus>().stub {
            on { listen(PushNotificationEvent::class.java) } doReturn Observable.empty()
        }

        val presenter = EventsListPresenter(
            defaultErrorResolution,
            scheduler,
            eventsInteractor,
            campaignsInteractor,
            bannersInteractor,
            phrasesInteractor,
            sharedAccountInteractor,
            currentClientInteractor,
            passportInteractor,
            navigationScenario,
            eventBus
        )

        init {
            configuration.stub {
                on { eventTypesToShow } doReturn EventType.values().toList()
                on { areOnlyFreshEventsShown() } doReturn false
                on { currentClient } doReturn ApiSampleData.clientInfo
            }
        }
    }
}