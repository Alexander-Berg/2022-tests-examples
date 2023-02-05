package ru.yandex.yandexbus.inhouse.organization.card

import com.yandex.mapkit.GeoObject
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.mock
import org.mockito.Mockito.times
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.R
import ru.yandex.yandexbus.inhouse.SchedulerProvider
import ru.yandex.yandexbus.inhouse.any
import ru.yandex.yandexbus.inhouse.common.cards.CardState
import ru.yandex.yandexbus.inhouse.geometry.MapkitPoint
import ru.yandex.yandexbus.inhouse.geometry.toDataClass
import ru.yandex.yandexbus.inhouse.map.MapProxy
import ru.yandex.yandexbus.inhouse.model.GeoModel
import ru.yandex.yandexbus.inhouse.model.route.TaxiRouteModel
import ru.yandex.yandexbus.inhouse.mvp.createAttachStart
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationCardAnalyticsSender.CloseAction
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationCardContract.Action
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationCardContract.MapAndHeaderContentView
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationCardContract.OrganizationViewState
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationCardContract.RoutesViewState
import ru.yandex.yandexbus.inhouse.organization.card.OrganizationCardContract.View
import ru.yandex.yandexbus.inhouse.organization.card.model.OrganizationCardData
import ru.yandex.yandexbus.inhouse.organization.card.model.OrganizationCardInteractor
import ru.yandex.yandexbus.inhouse.organization.card.model.OrganizationInfo
import ru.yandex.yandexbus.inhouse.organization.card.view.OrganizationBadge
import ru.yandex.yandexbus.inhouse.route.routesetup.EtaBlock
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteVariants
import ru.yandex.yandexbus.inhouse.search.BusinessSummary
import ru.yandex.yandexbus.inhouse.search.MapkitInformationSource
import ru.yandex.yandexbus.inhouse.search.OrganizationLink
import ru.yandex.yandexbus.inhouse.search.OrganizationPhone
import ru.yandex.yandexbus.inhouse.search.OrganizationPictures
import ru.yandex.yandexbus.inhouse.search.SearchMetadata
import ru.yandex.yandexbus.inhouse.search.SharedSearchState
import ru.yandex.yandexbus.inhouse.service.BackNotificationService
import ru.yandex.yandexbus.inhouse.service.taxi.Cost
import ru.yandex.yandexbus.inhouse.service.taxi.Ride
import ru.yandex.yandexbus.inhouse.service.taxi.TaxiOperator
import ru.yandex.yandexbus.inhouse.ui.main.StatusBarController
import ru.yandex.yandexbus.inhouse.utils.ClipboardHelper
import ru.yandex.yandexbus.inhouse.utils.ResourceProvider
import ru.yandex.yandexbus.inhouse.view.RelativeRect
import ru.yandex.yandexbus.inhouse.whenever
import ru.yandex.yandexbus.inhouse.ymaps.FollowYMapsAnalytics
import rx.Emitter
import rx.Observable
import rx.Single
import rx.schedulers.TestScheduler
import rx.subjects.PublishSubject
import rx.subscriptions.Subscriptions
import java.net.URL
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

class OrganizationCardPresenterTest : BaseTest() {

    @Mock
    private lateinit var view: View
    @Mock
    private lateinit var mapAndHeaderContentView: MapAndHeaderContentView.Regular
    @Mock
    private lateinit var navigator: OrganizationCardNavigator
    @Mock
    private lateinit var interactor: OrganizationCardInteractor
    @Mock
    private lateinit var analyticsSender: OrganizationCardAnalyticsSender
    @Mock
    private lateinit var mapProxy: MapProxy
    @Mock
    private lateinit var clipboardHelper: ClipboardHelper
    @Mock
    private lateinit var statusBarController: StatusBarController
    @Mock
    private lateinit var resourceProvider: ResourceProvider
    @Mock
    private lateinit var geoModel: GeoModel
    @Mock
    private lateinit var backNotificationService: BackNotificationService
    @Mock
    private lateinit var sharedSearchState: SharedSearchState
    @Mock
    private lateinit var followYMapsAnalytics: FollowYMapsAnalytics

    private lateinit var geoObject: GeoObject
    private lateinit var presenter: OrganizationCardPresenter

    private lateinit var clicks: PublishSubject<Action>

    private lateinit var toolbarBackClicks: PublishSubject<Unit>

    private lateinit var cardStates: PublishSubject<Pair<CardState?, CardState>>

    private lateinit var clearSearchClicks: PublishSubject<Unit>

    private lateinit var startSearchClicks: PublishSubject<Unit>

    private lateinit var visibleMapWindows: PublishSubject<RelativeRect>

    private lateinit var dataSubject: PublishSubject<OrganizationCardData>

    private lateinit var testScheduler: TestScheduler

    override fun setUp() {
        super.setUp()

        testScheduler = TestScheduler()

        geoObject = OrganizationGeoObjectTestFactory.mockGeoObject(
            ORGANIZATION_NAME, ORGANIZATION_LOCATION, ORGANIZATION_URI
        )

        whenever(geoModel.geoObject).thenReturn(geoObject)

        dataSubject = PublishSubject.create()
        whenever(interactor.data(any())).thenReturn(dataSubject)
        whenever(interactor.routes(any())).thenReturn(Observable.never<RouteVariants>().toSingle())

        clicks = PublishSubject.create()
        whenever(view.actions).thenReturn(clicks)

        cardStates = PublishSubject.create()
        whenever(view.cardStates).thenReturn(cardStates)

        visibleMapWindows = PublishSubject.create()
        whenever(view.visibleMapWindow).thenReturn(visibleMapWindows)

        whenever(resourceProvider.getString(Mockito.eq(R.string.verified_owner_details_link)))
            .thenReturn(VERIFIED_OWNER_INFO_URI)

        whenever(resourceProvider.getString(Mockito.eq(R.string.geo_product_priority_details_link)))
            .thenReturn(GEO_PRODUCT_INFO_URI)

        toolbarBackClicks = PublishSubject.create()
        whenever(mapAndHeaderContentView.backClicks).thenReturn(toolbarBackClicks)
        whenever(mapAndHeaderContentView.toolbarTransparency).thenReturn(Observable.never())
        whenever(view.mapAndHeaderView).thenReturn(mapAndHeaderContentView)

        whenever(backNotificationService.subscribe(any())).thenReturn(Subscriptions.empty())

        clearSearchClicks = PublishSubject.create()
        startSearchClicks = PublishSubject.create()

        whenever(sharedSearchState.clearSearchClicks).thenReturn(clearSearchClicks)
        whenever(sharedSearchState.startSearchClicks).thenReturn(startSearchClicks)

        presenter = OrganizationCardPresenter(
            geoModel, OrganizationCardMode.REGULAR, navigator, interactor, analyticsSender,
            mapProxy, clipboardHelper, statusBarController, resourceProvider,
            SchedulerProvider(testScheduler, testScheduler, testScheduler),
            backNotificationService,
            sharedSearchState,
            "",
            followYMapsAnalytics
        )
    }

    @Test
    fun `provides loading state when no data yet available`() {
        presenter.createAttachStart(view)
        verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))

        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `provides card data`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Loading))

        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `provides error state when data request failed`() {
        presenter.createAttachStart(view)
        dataSubject.onError(Exception())

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Error(geoObject.name.orEmpty()))

        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `reloads card data on RefreshCard action`() {
        val isCalled = AtomicBoolean()
        val data = Observable.create<OrganizationCardData>({
            if (!isCalled.getAndSet(true)) {
                it.onError(Exception())
            } else {
                it.onNext(DATA)
            }
        }, Emitter.BackpressureMode.LATEST)

        whenever(interactor.data(any())).thenReturn(data)

        presenter.createAttachStart(view)

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Error(geoObject.name.orEmpty()))

        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)

        clicks.onNext(Action.RefreshCard)

        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Loading))

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `provides routes`() {
        val routes = PublishSubject.create<RouteVariants>()
        whenever(interactor.routes(any())).thenReturn(routes.first().toSingle())

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)
        routes.onNext(ROUTE_VARIANTS)

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Loading))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Data(ROUTE_VARIANTS, null)))

        view.verifyOtherStuffMethodsCalled()

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `provides routes error state when routes request failed`() {
        whenever(interactor.routes(any())).thenReturn(Single.error(Exception()))

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Error))

        view.verifyOtherStuffMethodsCalled()

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `reloads routes on RefreshRoutes action`() {
        val isCalled = AtomicBoolean()
        val routes = Single.create<RouteVariants> {
            if (!isCalled.getAndSet(true)) {
                it.onError(Exception())
            } else {
                it.onSuccess(ROUTE_VARIANTS)
            }
        }

        whenever(interactor.routes(any())).thenReturn(routes)

        presenter.createAttachStart(view)

        dataSubject.onNext(DATA)


        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Error))

        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)

        clicks.onNext(Action.RefreshRoutes)

        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Loading))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Data(ROUTE_VARIANTS, null)))

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `reloads routes on every view start`() {
        val routeVariants1 = ROUTE_VARIANTS

        val taxiRoute = mock(TaxiRouteModel::class.java)
        val routeVariants2 = ROUTE_VARIANTS.copy(taxi = EtaBlock.Taxi(listOf(taxiRoute)))

        val isCalled = AtomicBoolean()
        val routesResponse = Single.create<RouteVariants> {
            if (!isCalled.getAndSet(true)) {
                it.onSuccess(routeVariants1)
            } else {
                it.onSuccess(routeVariants2)
            }
        }

        whenever(interactor.routes(any())).thenReturn(routesResponse)

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Data(routeVariants1, null)))

        view.verifyOtherStuffMethodsCalled()

        verifyNoMoreInteractions(view)

        presenter.onViewStop()
        presenter.onViewStart()

        inOrder.verify(view).show(OrganizationViewState.Data(DATA, RoutesViewState.Data(routeVariants2, taxiRoute)))

        verify(view, times(2)).visibleMapWindow
        verify(view, times(2)).cardStates
        verify(view, times(2)).actions
        verify(view, times(3)).mapAndHeaderView

        verifyNoMoreInteractions(view)
    }

    @Test
    fun `goes back on card hide`() {
        presenter.createAttachStart(view)
        cardStates.onNext(null to CardState.HIDDEN)
        verify(navigator).goBack()
    }

    @Test
    fun `opens promo screen on promo click`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)
        clicks.onNext(Action.OpenPromo(DATA.organizationInfo))
        verify(navigator).toPromo(DATA.organizationInfo)
        verify(analyticsSender).onOpenPromo(DATA.organizationInfo)
    }

    @Test
    fun `opens routes screen on all routes click`() {
        whenever(view.currentCardState).thenReturn(CardState.SUMMARY)

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        clicks.onNext(Action.OpenAllRoutes(ROUTE_VARIANTS))
        verify(analyticsSender).onOpenAllRoutes(ROUTE_VARIANTS, INFO, false)
        verify(navigator).toRouteVariantsView(ROUTE_VARIANTS)
    }

    @Test
    fun `requests taxi on taxi click`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        clicks.onNext(Action.RequestTaxi(TAXI_RIDE))
        verify(navigator).requestTaxi(TAXI_RIDE)
        verify(analyticsSender).onRequestTaxi(DATA.organizationInfo)
    }

    @Test
    fun `dials number on phone click`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        val phone = DATA.organizationInfo.phones.first()
        clicks.onNext(Action.Call(phone, Action.Source.INFO_BLOCK))
        verify(navigator).call(phone.formattedNumber)
        verify(analyticsSender).onCall(phone.formattedNumber, DATA.organizationInfo, Action.Source.INFO_BLOCK)
    }

    @Test
    fun `opens url on link click`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        val link = DATA.organizationInfo.links.first()
        clicks.onNext(Action.OpenWeb(link, Action.Source.INFO_BLOCK))
        verify(navigator).openUrl(link.url)
        verify(analyticsSender).onOpenWeb(link.url, DATA.organizationInfo, Action.Source.INFO_BLOCK)
    }

    @Test
    fun `copies address to clipboard and shows message on copy address click`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        val address = requireNotNull(DATA.organizationInfo.address)
        clicks.onNext(Action.CopyAddress(address))

        verify(clipboardHelper).copyToClipboard(address)
        verify(view).showAddressCopiedMessage()
        verify(analyticsSender).onCopyAddress(DATA.organizationInfo)
    }

    @Test
    fun `opens provider uri on provider click`() {
        val providerUri = requireNotNull(DATA.organizationInfo.dataProviders.first().uri)
        presenter.createAttachStart(view)
        clicks.onNext(Action.OpenDataProviderUri(providerUri))
        verify(navigator).openUri(providerUri)
    }

    @Test
    fun `opens badge details on badge click`() {
        presenter.createAttachStart(view)

        clicks.onNext(Action.OpenBadgeDetails(OrganizationBadge.VERIFIED_OWNER))
        verify(navigator).openUri(VERIFIED_OWNER_INFO_URI)

        clicks.onNext(Action.OpenBadgeDetails(OrganizationBadge.GEO_PRODUCT))
        verify(navigator).openUri(GEO_PRODUCT_INFO_URI)
    }

    @Test
    fun `show slow connection message if data has not arrived within 5 seconds`() {
        presenter.createAttachStart(view)
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).showSlowConnectionMessage()
        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `does not show slow connection message if error has happened within 5 seconds`() {
        presenter.createAttachStart(view)
        dataSubject.onError(Exception())
        testScheduler.advanceTimeBy(5, TimeUnit.SECONDS)

        val inOrder = inOrder(view)
        inOrder.verify(view).show(OrganizationViewState.Loading(geoObject.name.orEmpty()))
        inOrder.verify(view).show(OrganizationViewState.Error(geoObject.name.orEmpty()))
        view.verifyOtherStuffMethodsCalled()
        verifyNoMoreInteractions(view)
    }

    @Test
    fun `goes back on back click`() {
        whenever(view.currentCardState).thenReturn(CardState.SUMMARY)

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        toolbarBackClicks.onNext(Unit)
        verify(navigator).goBack()
        verify(analyticsSender).onCardClosed(DATA.organizationInfo, CloseAction.CLOSE, false)
    }

    @Test
    fun `sends open event to analytics`() {
        presenter.onCreate()
        dataSubject.onNext(DATA)
        verify(analyticsSender).onCardOpened(DATA.organizationInfo)
    }

    @Test
    fun `sends onExpanded event when card expands`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        cardStates.onNext(CardState.SUMMARY to CardState.EXPANDED)
        verify(analyticsSender).onCardExpanded(DATA.organizationInfo, null)
    }

    @Test
    fun `sends onCollapsed event when card collapses`() {
        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        cardStates.onNext(CardState.EXPANDED to CardState.SUMMARY)
        verify(analyticsSender).onCardCollapsed(DATA.organizationInfo)
    }

    @Test
    fun `sends onClosed event on edit search query`() {
        whenever(view.currentCardState).thenReturn(CardState.SUMMARY)

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        startSearchClicks.onNext(Unit)
        verify(analyticsSender).onCardClosed(DATA.organizationInfo, CloseAction.EDIT_SEARCH, false)
    }

    @Test
    fun `sends onClosed event on clear search bar`() {
        whenever(view.currentCardState).thenReturn(CardState.SUMMARY)

        presenter.createAttachStart(view)
        dataSubject.onNext(DATA)

        clearSearchClicks.onNext(Unit)
        verify(analyticsSender).onCardClosed(DATA.organizationInfo, CloseAction.CLOSE, false)
    }

    private fun View.verifyOtherStuffMethodsCalled() {
        verify(this).init(any())
        verify(this, times(2)).mapAndHeaderView
        verify(this).actions
        verify(this).cardStates
        verify(this).visibleMapWindow
    }

    private companion object {

        const val ORGANIZATION_NAME = "Yandex"
        const val ORGANIZATION_URI = "ymapsbm1://org/test"
        val ORGANIZATION_LOCATION = MapkitPoint(0.0, 0.0)

        val USER_LOCATION = MapkitPoint(1.0, 1.0)

        const val DISTANCE_TO_ORGANIZATION = 5.0

        val TAXI_RIDE = Ride(
            USER_LOCATION,
            ORGANIZATION_LOCATION,
            3,
            Cost(5.0, "BYN", "5 BYN"),
            TaxiOperator.YA_TAXI
        )

        val SUMMARY = BusinessSummary(
            uri = ORGANIZATION_URI,
            title = "Yandex",
            location = ORGANIZATION_LOCATION.toDataClass(),
            categories = emptyList(),
            verifiedOwner = true,
            organizationRating = null,
            businessId = null
        )

        val INFO = OrganizationInfo(
            shortAddress = "Niamiha 12",
            address = "Belarus, Minsk, Niamiha 12",
            businessSummary = SUMMARY,
            geoProduct = null,
            workingStatus = null,
            operatingStatus = null,
            organizationPictures = OrganizationPictures(emptyList(), null),
            nearbyUndergroundStops = emptyList(),
            phones = listOf(OrganizationPhone(OrganizationPhone.PhoneType.PHONE, "+375 17 328‑19-61")),
            links = listOf(OrganizationLink.Self(URL("https://yandex.by"))),
            dataProviders = listOf(MapkitInformationSource("Yandex", "https://yandex.by")),
            searchMetadata = SearchMetadata("logId", "reqId")
        )

        val DATA = OrganizationCardData(INFO, DISTANCE_TO_ORGANIZATION, TAXI_RIDE)

        val ROUTE_VARIANTS = RouteVariants.EMPTY

        const val VERIFIED_OWNER_INFO_URI = "https://yandex.by/verified_owner"
        const val GEO_PRODUCT_INFO_URI = "https://yandex.by/geo_product"
    }
}
