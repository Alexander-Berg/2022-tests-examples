package ru.yandex.market.checkout.pickup.single

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.isA
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.TestingFeaturesAnalyticsFacade
import ru.yandex.market.checkout.pickup.multiple.PickupPointVO
import ru.yandex.market.checkout.pickup.multiple.PickupPointViewObjectMapper
import ru.yandex.market.clean.domain.model.fashion.FashionFbsConfig
import ru.yandex.market.clean.presentation.formatter.StorageLimitDateAndRenewalFormatter
import ru.yandex.market.clean.presentation.navigation.DialerTargetScreen
import ru.yandex.market.clean.presentation.navigation.MapRouteTargetScreen
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.vo.BoostOutletsVo
import ru.yandex.market.common.errors.ErrorVoFormatter
import ru.yandex.market.data.order.OutletInfo
import ru.yandex.market.data.order.options.point.OutletPoint
import ru.yandex.market.data.searchitem.offer.Coordinates
import ru.yandex.market.feature.manager.CheckoutDynamicDeliveryPriceFeatureManager
import ru.yandex.market.images.ImageUrlFormatter
import ru.yandex.market.presentationSchedulersMock
import ru.yandex.market.utils.asOptional

class PickupPointPresenterTest {

    private val mapper = mock<PickupPointViewObjectMapper>()
    private val spyOutletInfo = spy(OutletInfo.testInstance())
    private val spyOutletPoint = spy(OutletPoint(spyOutletInfo, 42L))
    private val outletPoint = OutletPoint(OutletInfo.testInstance(), 42L)
    private val router = mock<Router>()
    private val view = mock<PickupPointView>()
    private val mapTargetScreenProvider = mock<PickupMapScreenProvider>()
    private val errorVoFormatter = mock<ErrorVoFormatter>()
    private val boostOutlet = BoostOutletsVo.empty

    @Suppress("DEPRECATION")
    private val analyticsService = mock<TestingFeaturesAnalyticsFacade>()
    private val schedulers = presentationSchedulersMock()

    private val storageLimitDateAndRenewalFormatter = mock<StorageLimitDateAndRenewalFormatter>()
    private val imageUrlFormatter = mock<ImageUrlFormatter>()

    private val checkoutDynamicDeliveryPriceFeatureManager = mock<CheckoutDynamicDeliveryPriceFeatureManager> {
        on { isEnabledSingle() } doReturn Single.just(false)
    }

    private val args = spy(
        PickupPointArguments.Builder()
            .outlet(outletPoint)
            .isClickAndCollect(false)
            .build()
    )

    private val presenter = PickupPointPresenter(
        schedulers,
        args,
        mapper,
        router,
        mapTargetScreenProvider,
        analyticsService,
        errorVoFormatter,
        storageLimitDateAndRenewalFormatter,
        imageUrlFormatter,
        checkoutDynamicDeliveryPriceFeatureManager,
    )

    @Test
    fun `Map outlet info and pass view object to view`() {
        val viewObject = PickupPointVO.testBuilder().build()
        whenever(
            mapper.map(
                eq(outletPoint),
                eq(null),
                eq(emptyList()),
                eq(false),
                eq(false),
                eq(true),
                eq(false),
                eq(true),
                eq(true),
                any(),
                anyOrNull(),
                any(),
                eq(boostOutlet),
                eq(false),
                eq(null),
                eq(FashionFbsConfig.NO_FBS),
                eq(false),
                eq(false),
            )
        ).thenReturn(viewObject)

        presenter.attachView(view)

        verify(view).showPickupPointInformation(viewObject)
    }

    @Test
    fun `Map outlet info and show map point when coordinates present`() {
        val latitude = 10.0
        val longitude = 20.0
        doReturn(Coordinates(latitude, longitude).asOptional())
            .whenever(spyOutletInfo).coordinates

        doReturn(spyOutletPoint).whenever(args).outlet
        whenever(
            mapper.map(
                any(),
                anyOrNull(),
                eq(emptyList()),
                eq(false),
                eq(false),
                eq(true),
                eq(false),
                eq(true),
                eq(true),
                any(),
                anyOrNull(),
                any(),
                eq(boostOutlet),
                eq(false),
                eq(null),
                eq(FashionFbsConfig.NO_FBS),
                eq(false),
                eq(false),
            )
        ).thenReturn(PickupPointVO.testBuilder().build())

        presenter.attachView(view)

        verify(view).showMapPin(latitude, longitude)
    }

    @Test
    fun `Map outlet info and hide map when coordinates are not present`() {
        doReturn(Optional.empty<Coordinates>()).whenever(spyOutletInfo).coordinates
        doReturn(spyOutletPoint).whenever(args).outlet

        whenever(
            mapper.map(
                eq(outletPoint),
                eq(null),
                eq(emptyList()),
                eq(false),
                eq(false),
                eq(true),
                eq(false),
                eq(true),
                eq(true),
                any(),
                anyOrNull(),
                any(),
                eq(boostOutlet),
                eq(false),
                eq(null),
                eq(FashionFbsConfig.NO_FBS),
                eq(false),
                eq(false),
            )
        ).thenReturn(PickupPointVO.testBuilder().build())

        presenter.attachView(view)

        verify(view).hideMap()
    }

    @Test
    fun `Closes screen sending result when pickup point selected`() {
        presenter.selectPickupPoint()

        verify(router).backWithResult(outletPoint)
    }

    @Test
    fun `Asks router to open dialer when view wants to call phone`() {
        val phone = "+7 999 999-99-99"

        presenter.callPhone(phone)

        verify(router).navigateTo(isA<DialerTargetScreen>())
    }

    @Test
    fun `Do nothing when view wants to call empty phone`() {
        presenter.callPhone("")

        verify(router, never()).navigateTo(any())
    }

    @Test
    fun `Asks router to show route on map when view asks so`() {
        doReturn(Coordinates(10.0, 20.0).asOptional())
            .whenever(spyOutletInfo).coordinates

        presenter.showRoute()

        verify(router).navigateTo(isA<MapRouteTargetScreen>())
    }

    @Test
    fun `Hide route button when pickup point coordinates are not present`() {
        doReturn(Optional.empty<Coordinates>()).whenever(spyOutletInfo).coordinates
        doReturn(spyOutletPoint).whenever(args).outlet

        whenever(
            mapper.map(
                any(),
                anyOrNull(),
                eq(emptyList()),
                eq(false),
                eq(false),
                eq(true),
                eq(false),
                eq(true),
                eq(true),
                any(),
                anyOrNull(),
                any(),
                eq(boostOutlet),
                eq(false),
                eq(null),
                eq(FashionFbsConfig.NO_FBS),
                eq(false),
                eq(false),
            )
        ).thenReturn(PickupPointVO.testBuilder().build())

        presenter.attachView(view)

        verify(view).hideRouteButton()
    }

}
