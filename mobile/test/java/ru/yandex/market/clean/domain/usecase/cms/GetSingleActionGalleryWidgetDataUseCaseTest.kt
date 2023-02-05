package ru.yandex.market.clean.domain.usecase.cms

import io.reactivex.processors.BehaviorProcessor
import io.reactivex.subjects.BehaviorSubject
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.atMost
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.cms.SingleActionGalleryWidgetStatusRepository
import ru.yandex.market.clean.data.source.cms.CmsActualOrdersDataSource
import ru.yandex.market.clean.domain.model.cms.CmsFont
import ru.yandex.market.clean.domain.model.cms.CmsItem
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.domain.model.cms.CmsWidgetSubtitle
import ru.yandex.market.clean.domain.model.cms.CmsWidgetTitle
import ru.yandex.market.clean.domain.model.cms.GarsonType
import ru.yandex.market.clean.domain.model.cms.garson.MergedWidgetParams
import ru.yandex.market.clean.domain.model.cms.garson.MergedWidgetParamsGarson
import ru.yandex.market.clean.domain.model.cms.garson.actualLavkaOrdersGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.actualOrdersGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.growingCashbackGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.plusBenefitsGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.plusHomeGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.referralProgramGarsonTestInstance
import ru.yandex.market.clean.domain.model.cms.garson.softUpdateGarsonTestInstance
import ru.yandex.market.clean.domain.usecase.lavka.GetCmsLavkaOrdersUseCase
import ru.yandex.market.clean.domain.usecase.softupdate.GetSoftUpdateCmsItemUseCase
import ru.yandex.market.domain.system.model.BatteryStatus
import ru.yandex.market.domain.system.usecase.GetBatteryStatusStreamUseCase
import ru.yandex.market.test.extensions.asObservable

class GetSingleActionGalleryWidgetDataUseCaseTest {

    private val benefitsSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val actualOrderSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val lavkaOrderSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val softUpdateSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val advertisingCampaignSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val referralProgramSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val plusHomeSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val growingCashbackSubject: BehaviorSubject<List<CmsItem>> = BehaviorSubject.create()
    private val widgetStatusSubject: BehaviorSubject<Unit> = BehaviorSubject.create()
    private val attachedViewProcessor = BehaviorProcessor.createDefault(true).toSerialized()

    private val benefitsItem = mock<CmsItem>()
    private val actualOrderItem = mock<CmsItem>()
    private val lavkaOrderItem = mock<CmsItem>()
    private val softUpdateItem = mock<CmsItem>()
    private val referralProgramItem = mock<CmsItem>()
    private val plusHomeItem = mock<CmsItem>()
    private val growingCashbackItem = mock<CmsItem>()

    private val batteryStatusOk = BatteryStatus.OK
    private val batteryStatusLow = BatteryStatus.LOW

    private val cmsPlusBenefitsUseCase = mock<CmsPlusBenefitsUseCase> {
        on { execute() } doReturn benefitsSubject
    }
    private val cmsActualOrdersDataSource = mock<CmsActualOrdersDataSource> {
        on { getFapiActualOrders(any()) } doReturn actualOrderSubject
    }
    private val getCmsLavkaOrdersUseCase = mock<GetCmsLavkaOrdersUseCase> {
        on { execute() } doReturn lavkaOrderSubject
    }
    private val getSoftUpdateCmsItemUseCase = mock<GetSoftUpdateCmsItemUseCase> {
        on { execute(any(), any()) } doReturn softUpdateSubject
    }
    private val singleActionGalleryWidgetStatusRepository = mock<SingleActionGalleryWidgetStatusRepository> {
        on { getDataReloadRequestStream(WIDGET_ID) } doReturn widgetStatusSubject
    }

    private val cmsAdvertisingCampaignUseCase = mock<CmsAdvertisingCampaignUseCase> {
        on { execute(any()) } doReturn advertisingCampaignSubject
    }

    private val cmsReferralProgramUseCase = mock<CmsReferralProgramUseCase> {
        on { execute(any()) } doReturn referralProgramSubject
    }

    private val cmsPlusHomeUseCase = mock<CmsPlusHomeUseCase> {
        on { execute(any()) } doReturn plusHomeSubject
    }

    private val growingCashbackUseCase = mock<CmsGrowingCashbackUseCase> {
        on { execute(any()) } doReturn growingCashbackSubject
    }

    private val getBatteryStatusStreamUseCase = mock<GetBatteryStatusStreamUseCase> {
        on { execute() } doReturn batteryStatusOk.asObservable()
    }

    private val widget = CmsWidget.testBuilder()
        .id(WIDGET_ID)
        .garsons(
            listOf(
                plusBenefitsGarsonTestInstance(),
                ACTUAL_ORDERS_GARSON,
                actualLavkaOrdersGarsonTestInstance(),
                SOFT_UPDATE_GARSON,
                referralProgramGarsonTestInstance(),
                PLUS_HOME_GARSON,
                growingCashbackGarsonTestInstance(),
                MergedWidgetParamsGarson(
                    mapOf(
                        GarsonType.SOFT_UPDATE to SOFT_UPDATE_TITLE_PARAMS,
                        GarsonType.ACTUAL_ORDERS to ACTUAL_ORDERS_PARAMS
                    )
                )
            )
        )
        .build()

    private val useCase = GetSingleActionGalleryWidgetDataUseCase(
        { cmsPlusBenefitsUseCase },
        { cmsActualOrdersDataSource },
        { getCmsLavkaOrdersUseCase },
        { getSoftUpdateCmsItemUseCase },
        { cmsAdvertisingCampaignUseCase },
        { cmsReferralProgramUseCase },
        { cmsPlusHomeUseCase },
        { growingCashbackUseCase },
        { getBatteryStatusStreamUseCase },
        singleActionGalleryWidgetStatusRepository
    )

    @Before
    fun setup() {
        benefitsSubject.onNext(listOf(benefitsItem))
        actualOrderSubject.onNext(listOf(actualOrderItem))
        lavkaOrderSubject.onNext(listOf(lavkaOrderItem))
        softUpdateSubject.onNext(listOf(softUpdateItem))
        referralProgramSubject.onNext(listOf(referralProgramItem))
        plusHomeSubject.onNext(listOf(plusHomeItem))
        growingCashbackSubject.onNext(listOf(growingCashbackItem))
    }

    @Test
    fun `Combine values from all data sources`() {
        useCase.execute(widget, attachedViewProcessor).test()
            .assertNotComplete()
            .assertValues(
                listOf(
                    benefitsItem,
                    actualOrderItem,
                    lavkaOrderItem,
                    softUpdateItem,
                    referralProgramItem,
                    plusHomeItem,
                    growingCashbackItem
                )
            )
    }

    @Test
    fun `Reload data on widget status updates`() {
        val testSubscription = useCase.execute(widget, attachedViewProcessor).test()
        widgetStatusSubject.onNext(Unit)

        testSubscription
            .assertNotComplete()
            .assertValueCount(2)
    }

    @Test
    fun `Data source error not cause error on use case result`() {
        //пропускаем значение без ошибок, оно проверяются в другом тесте
        val testSubscription = useCase.execute(widget, attachedViewProcessor).skip(1).test()

        benefitsSubject.onError(Error())
        actualOrderSubject.onError(Error())
        lavkaOrderSubject.onError(Error())
        softUpdateSubject.onError(Error())
        referralProgramSubject.onError(Error())
        plusHomeSubject.onError(Error())
        growingCashbackSubject.onError(Error())

        testSubscription
            .assertNotComplete()
            .assertNoErrors()
            .assertValues(
                listOf(
                    actualOrderItem,
                    lavkaOrderItem,
                    softUpdateItem,
                    referralProgramItem,
                    plusHomeItem,
                    growingCashbackItem
                ),
                listOf(
                    lavkaOrderItem,
                    softUpdateItem,
                    referralProgramItem,
                    plusHomeItem,
                    growingCashbackItem
                ),
                listOf(
                    softUpdateItem,
                    referralProgramItem,
                    plusHomeItem,
                    growingCashbackItem
                ),
                listOf(
                    referralProgramItem,
                    plusHomeItem,
                    growingCashbackItem
                ),
                listOf(
                    plusHomeItem,
                    growingCashbackItem
                ),
                listOf(
                    growingCashbackItem
                ),
                emptyList()
            )
    }

    @Test
    fun `Wrong widget garsons return empty items as result`() {
        useCase.execute(CmsWidget.testBuilder().id(WIDGET_ID).build(), attachedViewProcessor)
            .test()
            .assertValue(emptyList())
    }

    @Test
    fun `Check used garsons for soft update data source`() {
        useCase.execute(widget, attachedViewProcessor).test()

        verify(getSoftUpdateCmsItemUseCase).execute(SOFT_UPDATE_GARSON, SOFT_UPDATE_TITLE_PARAMS)
    }

    @Test
    fun `Check reload reloadable garson on view attach`() {
        val testSubscription = useCase.execute(widget, attachedViewProcessor).test()

        attachedViewProcessor.onNext(true)

        verify(cmsActualOrdersDataSource, atMost(2)).getFapiActualOrders(ORDERS_GARSON)

        testSubscription
            .assertNotComplete()
            .assertValueCount(2)

    }

    @Test
    fun `Filter out soft update if battery low`() {
        whenever(getBatteryStatusStreamUseCase.execute()) doReturn batteryStatusLow.asObservable()

        useCase.execute(widget, attachedViewProcessor)
            .test()
            .assertValue { items -> items.none { it == softUpdateItem } }
    }

    companion object {
        private const val WIDGET_ID = "test widget id"
        private val ACTUAL_ORDERS_GARSON = actualOrdersGarsonTestInstance()
        private val SOFT_UPDATE_GARSON = softUpdateGarsonTestInstance()
        private val PLUS_HOME_GARSON = plusHomeGarsonTestInstance()
        private val ORDERS_GARSON = actualOrdersGarsonTestInstance()
        private val SOFT_UPDATE_TITLE_PARAMS = MergedWidgetParams(
            CmsWidgetTitle.testBuilder().name("favorite categories").build(),
            CmsWidgetSubtitle("favorite categories sub title", CmsFont.normalCmsFont(), null),
            false
        )

        private val ACTUAL_ORDERS_PARAMS = MergedWidgetParams(
            null,
            null,
            true
        )
    }
}
