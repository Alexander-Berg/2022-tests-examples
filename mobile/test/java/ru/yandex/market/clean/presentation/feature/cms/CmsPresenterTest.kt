package ru.yandex.market.clean.presentation.feature.cms

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.common.WidgetMetricaSender
import ru.yandex.market.analytics.speed.SpeedService
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException
import ru.yandex.market.clean.data.model.dto.cms.CmsPageId
import ru.yandex.market.clean.domain.model.offerAffectingInformationTestInstance
import ru.yandex.market.clean.domain.usecase.health.facades.CmsHealthFacade
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.presentationSchedulersMock

class CmsPresenterTest {

    private val pageId = CmsPageId.create("test")
    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<CmsUseCases>() {
        on { checkRegionForDelivery() } doReturn Observable.empty()
        on { saveAgitationWidgetsIds(any()) } doReturn Completable.complete()
        on { checkOfferAffectingInfo() } doReturn Observable.just(offerAffectingInformationTestInstance())
        on { getRefreshCompletionEventsStream() } doReturn Observable.just(Unit)
    }
    private val errorHandler = mock<CommonErrorHandler>()
    private val router = mock<Router>()
    private val metricaSender = mock<WidgetMetricaSender>()

    private val cmsHealth = mock<CmsHealthFacade>()
    private val resourcesDataStore = mock<ResourcesManager>()
    private val speedService = mock<SpeedService>()

    private val presenter = CmsPresenter(
        useCases,
        pageId,
        router,
        schedulers,
        cmsHealth,
        metricaSender,
        errorHandler,
        resourcesDataStore,
        speedService,
    )

    @Test
    fun `Health event send if page not loaded with runtime error`() {
        val error = NullPointerException()

        whenever(useCases.getWidgets(pageId)) doReturn Single.error(error)

        presenter.onFirstViewAttach()
        verify(cmsHealth).sendCmsShowErrorHeath(error, null)
    }

    @Test
    fun `Health event send if page not loaded with not network cause communication error`() {
        val marketReqId = "marketReqId"
        val error = CommunicationException(Response.BAD_REQUEST, marketReqId)

        whenever(useCases.getWidgets(pageId)) doReturn Single.error(error)

        presenter.onFirstViewAttach()
        verify(cmsHealth).sendCmsShowErrorHeath(error, marketReqId)
    }

    @Test
    fun `Health event send if page loaded but widgets list is empty`() {

        whenever(useCases.getWidgets(pageId)) doReturn Single.just(emptyList())

        presenter.onFirstViewAttach()
        verify(cmsHealth).sendCmsShowEmptyHeath()
    }
}
