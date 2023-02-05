package ru.yandex.market.clean.presentation.feature.catalog

import io.reactivex.Observable
import io.reactivex.Single
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.analitycs.events.health.HealthEvent
import ru.yandex.market.analitycs.events.health.additionalData.CatalogShowErrorInfo
import ru.yandex.market.analytics.health.HealthLevel
import ru.yandex.market.analytics.health.HealthName
import ru.yandex.market.analytics.health.HealthPortion
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.feature.catalog.metricasender.CatalogMetricSender
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.featureconfigs.models.FmcgRedesignConfig
import ru.yandex.market.feature.manager.FmcgRedesignFeatureManager
import ru.yandex.market.fragment.search.SearchRequestTargetScreen
import ru.yandex.market.presentationSchedulersMock

class CatalogPresenterTest {

    private val nodeId = "testNodeId"
    private val view = mock<CatalogView>()
    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<CatalogUseCases> {
        on { getAuthStatusChanges() } doReturn Observable.empty()
        on { getRegionChanges() } doReturn Observable.empty()
    }
    private val errorHandler = mock<CommonErrorHandler>()
    private val router = mock<Router>()
    private val metricaSender = mock<CatalogMetricSender>()

    @Suppress("DEPRECATION")
    private val analyticsService = mock<ru.yandex.market.analitycs.AnalyticsService>()
    private val params = mock<CatalogParams> {
        on { nodeId } doReturn nodeId
        on { isFromCms } doReturn true
    }
    private val fmcgRedesignFeatureManager = mock<FmcgRedesignFeatureManager> {
        on { isEnabledFromCacheOrDefault() } doReturn false
        on { getConfigFromCacheOrDefault() } doReturn FmcgRedesignConfig(
            isEnabled = false,
            nid = "21345",
            businessId = "12345",
            FmcgRedesignConfig.OnboardingPageId(
                lavkaRetailVprok = "",
                retailVprok = "",
                lavkaVprok = "",
                vprok = ""
            )
        )
    }
    private val presenter = CatalogPresenter(
        schedulers,
        params,
        router,
        useCases,
        metricaSender,
        errorHandler,
        analyticsService,
        fmcgRedesignFeatureManager,
    )

    @Test
    fun `Health event send if catalog not loaded with runtime error`() {
        val error = NullPointerException()
        val expectedEvent = HealthEvent.builder()
            .name(HealthName.CATALOG_SHOW_ERROR)
            .portion(HealthPortion.CATALOG_SCREEN)
            .level(HealthLevel.ERROR)
            .info(
                CatalogShowErrorInfo(
                    exception = error,
                    nodeId = nodeId,
                    isFromCms = true
                )
            )
            .build()
        whenever(useCases.getNavigationNode(any(), any())) doReturn Single.error(error)

        presenter.attachView(view)
        verify(analyticsService).report(expectedEvent)
    }

    @Test
    fun `Health event send if catalog not loaded with not network cause communication error`() {
        val error = CommunicationException(Response.BAD_REQUEST)
        val expectedEvent = HealthEvent.builder()
            .name(HealthName.CATALOG_SHOW_ERROR)
            .portion(HealthPortion.CATALOG_SCREEN)
            .level(HealthLevel.ERROR)
            .info(
                CatalogShowErrorInfo(
                    exception = error,
                    nodeId = nodeId,
                    isFromCms = true
                )
            )
            .build()
        whenever(useCases.getNavigationNode(any(), any())) doReturn Single.error(error)

        presenter.attachView(view)
        verify(analyticsService).report(expectedEvent)
    }

    @Test
    fun `Health event not send if catalog not loaded with network error`() {
        val error = CommunicationException(Response.NETWORK_ERROR)
        val unexpectedEvent = HealthEvent.builder()
            .name(HealthName.CATALOG_SHOW_ERROR)
            .portion(HealthPortion.CATALOG_SCREEN)
            .level(HealthLevel.ERROR)
            .info(
                CatalogShowErrorInfo(
                    exception = error,
                    nodeId = nodeId,
                    isFromCms = true
                )
            )
            .build()
        whenever(useCases.getNavigationNode(any(), any())) doReturn Single.error(error)

        presenter.attachView(view)
        verify(analyticsService, never()).report(unexpectedEvent)
    }

    @Test
    fun `Health event has catalog portion if catalog is not root`() {
        val error = NullPointerException()
        val expectedEvent = HealthEvent.builder()
            .name(HealthName.CATALOG_SHOW_ERROR)
            .portion(HealthPortion.CATALOG_SCREEN)
            .level(HealthLevel.ERROR)
            .info(
                CatalogShowErrorInfo(
                    exception = error,
                    nodeId = nodeId,
                    isFromCms = false
                )
            )
            .build()
        whenever(useCases.getNavigationNode(any(), any())) doReturn Single.error(error)
        whenever(params.isFromCms) doReturn false

        presenter.attachView(view)
        verify(analyticsService).report(expectedEvent)
    }

    @Test
    fun `Use current screen on search click`() {
        val targetScreenCaptor = argumentCaptor<SearchRequestTargetScreen>()
        val currentScreen = mock<Screen>()

        whenever(router.currentScreen) doReturn currentScreen

        presenter.onSearchClicked()

        verify(router).navigateTo(targetScreenCaptor.capture())

        assertThat(targetScreenCaptor.firstValue.params.sourceScreen).isSameAs(currentScreen)
    }
}
