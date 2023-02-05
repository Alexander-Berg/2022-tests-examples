package ru.yandex.market.clean.presentation.feature.product.cms

import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analitycs.health.MetricaSender
import ru.yandex.market.analytics.facades.TongueAnalyticsFacade
import ru.yandex.market.analytics.facades.health.PurchaseByListHealthFacade
import ru.yandex.market.analytics.facades.pharma.PharmaAnalogsAnalytics
import ru.yandex.market.clean.domain.model.product.ProductCmsData
import ru.yandex.market.clean.domain.model.product.SkuProductData
import ru.yandex.market.clean.domain.model.sku.skuTestInstance
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.feature.product.ProductFragment
import ru.yandex.market.clean.presentation.formatter.TongueConfigFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.navigation.Screen
import ru.yandex.market.common.experiments.experiment.linecarousel.LineCarouselExperiment
import ru.yandex.market.common.featureconfigs.managers.SkuSpeedUpToggleManager
import ru.yandex.market.common.featureconfigs.models.FeatureToggle
import ru.yandex.market.domain.product.model.skuIdTestInstance
import ru.yandex.market.feature.manager.PromoTongueFeatureManager
import ru.yandex.market.presentationSchedulersMock

class ProductCmsPresenterTest {

    private val productId = skuIdTestInstance()
    private val arguments =
        ProductFragment.Arguments(
            productId = productId,
            offerCpc = "offerCpc",
            redirectText = "redirectText",
            showUid = "showUid",
            forcedDefaultOfferId = null,
        )
    private val productCmsView = mock<ProductCmsView>()
    private val productData = mock<SkuProductData> {
        on { sku } doReturn skuTestInstance()
    }
    private val schedulers = presentationSchedulersMock()
    private val useCases = mock<ProductCmsUseCases> {
        on {
            getProductCmsData(
                any(), any(), any(), anyOrNull()
            )
        } doReturn Observable.just(MOCK_CMS_DATA)
        on {
            getLineExperimentPosition()
        } doReturn Single.just(LineCarouselExperiment.Position.DEFAULT)
        on {
            saveUpsellWidgets(any(), any())
        } doReturn Completable.complete()
    }

    private val errorHandler = mock<CommonErrorHandler>()
    private val router = mock<Router>() {
        on { currentScreen } doReturn Screen.SKU
    }
    private val metricaSender = mock<MetricaSender>()
    private val promoTongueFeatureManager = mock<PromoTongueFeatureManager> {
        on { isEnabled() } doReturn false
    }
    private val skuSpeedUpToggleManager = mock<SkuSpeedUpToggleManager> {
        on { get() } doReturn FeatureToggle(isEnabled = false)
    }
    private val tongueAnalyticsFacade = mock<TongueAnalyticsFacade>()
    private val tongueConfigFormatter = mock<TongueConfigFormatter>()
    private val purchaseByListHealthFacade = mock<PurchaseByListHealthFacade>()
    private val pharmaAnalogsAnalytics = mock<PharmaAnalogsAnalytics>()

    private val productCmsPresenter = ProductCmsPresenter(
        schedulers,
        skuSpeedUpToggleManager,
        metricaSender,
        tongueAnalyticsFacade,
        tongueConfigFormatter,
        promoTongueFeatureManager,
        router,
        useCases,
        arguments,
        pharmaAnalogsAnalytics,
        purchaseByListHealthFacade,
        errorHandler,
    )

    @Before
    fun setup() {
        productCmsPresenter.attachView(productCmsView)
    }

    @Test
    fun `Show CMS data on event`() {
        val scrollToStart = false
        productCmsPresenter.loadProductWidgets(productData, scrollToStart)
        verify(productCmsView).showWidgets(
            MOCK_CMS_DATA.widgets,
            arguments.isSisVersion,
            arguments.isAdsVersion,
            productData.sku.productOffer == null
        )
    }

    companion object {

        private val MOCK_CMS_DATA = ProductCmsData(emptyList(), null)
    }

}
