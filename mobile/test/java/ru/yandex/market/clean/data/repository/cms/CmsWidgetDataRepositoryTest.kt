package ru.yandex.market.clean.data.repository.cms

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.mapper.cms.CmsAnnounceMapper
import ru.yandex.market.clean.data.repository.AnnouncesRepository
import ru.yandex.market.clean.data.repository.AuthRepositoryImpl
import ru.yandex.market.clean.data.repository.NavigationNodeWidgetDataRepository
import ru.yandex.market.clean.data.repository.div.DivDataRepository
import ru.yandex.market.clean.data.repository.lavka.LavkaUpsaleRepository
import ru.yandex.market.clean.data.repository.shop.ShopInfoRepository
import ru.yandex.market.clean.data.repository.sis.ShopInShopRepository
import ru.yandex.market.clean.data.repository.vendors.DjUniversalVendorsRepository
import ru.yandex.market.clean.data.source.FeedlistDataSource
import ru.yandex.market.clean.data.source.cms.CmsActualOrdersDataSource
import ru.yandex.market.clean.data.source.cms.CmsArticlesRepository
import ru.yandex.market.clean.data.source.cms.CmsCategoriesDataSource
import ru.yandex.market.clean.data.source.cms.CmsCoinsLoginBannerDataSource
import ru.yandex.market.clean.data.source.cms.CmsDJCategoriesLinkRepository
import ru.yandex.market.clean.data.source.cms.CmsDJUniversalProductsRepository
import ru.yandex.market.clean.data.source.cms.CmsDealsDataSource
import ru.yandex.market.clean.data.source.cms.CmsExpressNavigationTreeSource
import ru.yandex.market.clean.data.source.cms.CmsHtmlTextDataSource
import ru.yandex.market.clean.data.source.cms.CmsLavkaActualOrdersDataSource
import ru.yandex.market.clean.data.source.cms.CmsLiveStreamRepository
import ru.yandex.market.clean.data.source.cms.CmsMailSubscriptionRepository
import ru.yandex.market.clean.data.source.cms.CmsPictureLinkDataSource
import ru.yandex.market.clean.data.source.cms.CmsPopularProductsDataSource
import ru.yandex.market.clean.data.source.cms.CmsPriceDropComplementaryRepository
import ru.yandex.market.clean.data.source.cms.CmsPriceDropRepository
import ru.yandex.market.clean.data.source.cms.CmsProductsByHistoryBlueRepository
import ru.yandex.market.clean.data.source.cms.CmsPromoLandingEntryPointRepository
import ru.yandex.market.clean.data.source.cms.CmsSearchDataSource
import ru.yandex.market.clean.data.source.cms.CmsSkusDataSource
import ru.yandex.market.clean.data.source.cms.CmsSoftUpdateRepository
import ru.yandex.market.clean.data.source.cms.CmsSponsoredOffersDataSource
import ru.yandex.market.clean.data.source.cms.CmsStaticDataSource
import ru.yandex.market.clean.data.source.cms.CmsStoriesRepository
import ru.yandex.market.clean.data.source.cms.CmsTextWithIconDataSource
import ru.yandex.market.clean.data.source.cms.CmsUserCoinsRepository
import ru.yandex.market.clean.data.source.cms.CmsVendorsDataRepository
import ru.yandex.market.clean.data.source.cms.CmsWishListRepository
import ru.yandex.market.clean.data.source.cms.SkuComplementaryProductGroupsRepository
import ru.yandex.market.clean.domain.mapper.ProductGroupingMapper
import ru.yandex.market.clean.domain.model.cms.CmsItem
import ru.yandex.market.clean.domain.model.cms.CmsProduct
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.domain.model.cms.WidgetData
import ru.yandex.market.clean.domain.model.cms.garson.GroupSkuByIdCmsWidgetGarson
import ru.yandex.market.clean.domain.model.cms.garson.SkuAnalogsCmsWidgetGarson
import ru.yandex.market.clean.domain.model.offerAffectingInformationTestInstance
import ru.yandex.market.data.regions.SelectedRegionRepository
import ru.yandex.market.feature.manager.EatsRetailFeatureManager
import ru.yandex.market.feature.manager.LavkaInMarketFeatureManager
import ru.yandex.market.feature.manager.PromoAnnouncementFeatureManager
import ru.yandex.market.test.extensions.asObservable
import ru.yandex.market.test.extensions.asSingle

class CmsWidgetDataRepositoryTest {
    private val categoriesDataSource = mock<CmsCategoriesDataSource>()
    private val dealsDataSource = mock<CmsDealsDataSource>()
    private val skusDataSource = mock<CmsSkusDataSource>()
    private val bannerDataSource = mock<CmsBannerRepository>()
    private val cmsPopularProductsDataSource = mock<CmsPopularProductsDataSource>()
    private val cmsSearchDataSource = mock<CmsSearchDataSource>()
    private val cmsPictureLinkDataSource = mock<CmsPictureLinkDataSource>()
    private val cmsHtmlTextDataSource = mock<CmsHtmlTextDataSource>()
    private val cmsVendorsDataRepository = mock<CmsVendorsDataRepository>()
    private val cmsActualOrdersDataSource = mock<CmsActualOrdersDataSource>()
    private val cmsLavkaActualOrdersDataSource = mock<CmsLavkaActualOrdersDataSource>()
    private val authenticationRepository = mock<AuthRepositoryImpl>()
    private val cmsStaticDataSource = mock<CmsStaticDataSource>()
    private val cmsUserCoinsRepository = mock<CmsUserCoinsRepository>()
    private val cmsWishListRepository = mock<CmsWishListRepository>()
    private val cmsMailSubscriptionRepository = mock<CmsMailSubscriptionRepository>()
    private val cmsSoftUpdateRepository = mock<CmsSoftUpdateRepository>()
    private val cmsTextWithIconDataSource = mock<CmsTextWithIconDataSource>()
    private val cmsPriceDropRepository = mock<CmsPriceDropRepository>()
    private val cmsPromoLandingEntryPointRepository = mock<CmsPromoLandingEntryPointRepository>()
    private val skuWidgetDataRepository = mock<SkuWidgetDataRepository>()
    private val modelWidgetDataRepository = mock<ModelWidgetDataRepository>()
    private val offerWidgetDataRepository = mock<OfferWidgetDataRepository>()
    private val cmsDJUniversalProductsRepository = mock<CmsDJUniversalProductsRepository>()
    private val cmsProductsByHistoryBlueRepository = mock<CmsProductsByHistoryBlueRepository>()
    private val cmsStoriesRepository = mock<CmsStoriesRepository>()
    private val cmsArticlesRepository = mock<CmsArticlesRepository>()
    private val cmsCoinsLoginBannerDataSource = mock<CmsCoinsLoginBannerDataSource>()
    private val cmsDJCategoriesLinkRepository = mock<CmsDJCategoriesLinkRepository>()
    private val cmsLiveStreamRepository = mock<CmsLiveStreamRepository>()
    private val cmsComplementaryProductGroupsRepository = mock<SkuComplementaryProductGroupsRepository>()
    private val feedlistDataSource = mock<FeedlistDataSource>()
    private val cmsSponsoredOfferDataSource = mock<CmsSponsoredOffersDataSource>()
    private val navigationNodeWidgetDataRepository = mock<NavigationNodeWidgetDataRepository>()
    private val cmsExpressNavTreeSource = mock<CmsExpressNavigationTreeSource>()
    private val lavkaUpsaleRepository = mock<LavkaUpsaleRepository>()
    private val selectedRegionRepository = mock<SelectedRegionRepository>()
    private val divDataRepository = mock<DivDataRepository>()
    private val shopInfoRepository = mock<ShopInfoRepository>()
    private val eatsRetailFeatureManager = mock<EatsRetailFeatureManager>()
    private val cmsPriceDropComplementaryRepository = mock<CmsPriceDropComplementaryRepository>()
    private val shopInShopRepository = mock<ShopInShopRepository>()
    private val lavkaInMarketFeatureManager = mock<LavkaInMarketFeatureManager>()
    private val productGroupingMapper = mock<ProductGroupingMapper>()
    private val announcesRepository = mock<AnnouncesRepository>()
    private val announceMapper = mock<CmsAnnounceMapper>()
    private val promoAnnounceFeatureManager = mock<PromoAnnouncementFeatureManager>()
    private val djUniversalVendorsRepository = mock<DjUniversalVendorsRepository>()

    private val repository = CmsWidgetDataRepository(
        { categoriesDataSource },
        { dealsDataSource },
        { skusDataSource },
        { bannerDataSource },
        { cmsPopularProductsDataSource },
        { cmsSearchDataSource },
        { cmsPictureLinkDataSource },
        { cmsHtmlTextDataSource },
        { cmsVendorsDataRepository },
        { cmsActualOrdersDataSource },
        { cmsLavkaActualOrdersDataSource },
        { authenticationRepository },
        { cmsStaticDataSource },
        { cmsUserCoinsRepository },
        { cmsWishListRepository },
        { cmsMailSubscriptionRepository },
        { cmsSoftUpdateRepository },
        { cmsTextWithIconDataSource },
        { cmsPriceDropRepository },
        { cmsPromoLandingEntryPointRepository },
        { skuWidgetDataRepository },
        { modelWidgetDataRepository },
        { offerWidgetDataRepository },
        { cmsDJUniversalProductsRepository },
        { cmsProductsByHistoryBlueRepository },
        { cmsStoriesRepository },
        { cmsArticlesRepository },
        { cmsCoinsLoginBannerDataSource },
        { cmsDJCategoriesLinkRepository },
        { cmsLiveStreamRepository },
        { cmsComplementaryProductGroupsRepository },
        { feedlistDataSource },
        { cmsSponsoredOfferDataSource },
        { navigationNodeWidgetDataRepository },
        { cmsExpressNavTreeSource },
        { lavkaUpsaleRepository },
        { selectedRegionRepository },
        { divDataRepository },
        { shopInfoRepository },
        { eatsRetailFeatureManager },
        { lavkaInMarketFeatureManager },
        { cmsPriceDropComplementaryRepository },
        { shopInShopRepository },
        { productGroupingMapper },
        { announcesRepository },
        { promoAnnounceFeatureManager },
        { djUniversalVendorsRepository },
    )

    @Test
    fun `Get group sku by ids using single request`() {
        whenever(
            skusDataSource.getSkus(
                any<List<String>>(),
                any<List<String>>(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ) doReturn
            (1..4).map { CmsProduct.testBuilder().skuId(it.toString()).build() }.asSingle()

        repository.getWidgetData(
            CmsWidget.testBuilder()
                .garsons(
                    listOf(
                        GroupSkuByIdCmsWidgetGarson.builder()
                            .groups(
                                listOf(listOf("1", "2"), listOf("3", "4")).map {
                                    GroupSkuByIdCmsWidgetGarson.Group.builder()
                                        .skuIds(it)
                                        .build()
                                }
                            )
                            .build()
                    )
                )
                .build(),
            offerAffectingInformationTestInstance(),
            "UNKNOWN",
            emptyList(),
        )
            .test()
            .assertNoErrors()
            .assertResult(
                WidgetData.onlyItems(
                    listOf(
                        CmsProduct.testBuilder()
                            .skuId("1")
                            .build(),
                        CmsProduct.testBuilder()
                            .skuId("3")
                            .build()
                    )
                )
            )

        verify(skusDataSource).getSkus(any<List<String>>(), any<List<String>>(), any(), any(), any(), any(), any())
    }

    @Test
    fun `Use fallback garson to get data for empty groups`() {

        whenever(
            skusDataSource.getSkus(
                any<List<String>>(),
                any<List<String>>(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ) doReturn listOf(
            CmsProduct.testBuilder()
                .skuId("1")
                .build(),
            CmsProduct.testBuilder()
                .skuId("2")
                .build()
        ).asSingle()

        whenever(skusDataSource.getModelAnalogs(any(), any(), any(), any())) doReturn listOf<CmsItem>(
            CmsProduct.testBuilder()
                .skuId("3")
                .build(),
            CmsProduct.testBuilder()
                .skuId("4")
                .build()
        ).asObservable()

        repository.getWidgetData(
            CmsWidget.testBuilder()
                .garsons(
                    listOf(
                        GroupSkuByIdCmsWidgetGarson.builder()
                            .groups(
                                listOf(
                                    GroupSkuByIdCmsWidgetGarson.Group.builder()
                                        .skuIds(listOf("1", "2"))
                                        .build(),
                                    GroupSkuByIdCmsWidgetGarson.Group.builder()
                                        .skuIds(listOf("3", "4"))
                                        .garson(SkuAnalogsCmsWidgetGarson.testInstance())
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build(),
            offerAffectingInformationTestInstance(),
            "UNKNOWN",
            emptyList(),
        )
            .test()
            .assertNoErrors()
            .assertResult(
                WidgetData.onlyItems(
                    listOf(
                        CmsProduct.testBuilder()
                            .skuId("1")
                            .build(),
                        CmsProduct.testBuilder()
                            .skuId("3")
                            .build()
                    )
                )
            )

        verify(skusDataSource).getModelAnalogs(any(), any(), any(), any())
    }

    @Test
    fun `Switch to multiple requests when single request failed`() {
        whenever(
            skusDataSource.getSkus(
                any<List<String>>(),
                any<List<String>>(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        ).doReturn(
            Single.error(RuntimeException()),

            listOf(
                CmsProduct.testBuilder()
                    .skuId("1")
                    .build(),
                CmsProduct.testBuilder()
                    .skuId("2")
                    .build()
            ).asSingle(),

            listOf(
                CmsProduct.testBuilder()
                    .skuId("3")
                    .build(),
                CmsProduct.testBuilder()
                    .skuId("4")
                    .build()
            ).asSingle()
        )

        repository.getWidgetData(
            CmsWidget.testBuilder()
                .garsons(
                    listOf(
                        GroupSkuByIdCmsWidgetGarson.builder()
                            .groups(
                                (1..4).chunked(2)
                                    .map { ids ->
                                        GroupSkuByIdCmsWidgetGarson.Group.builder()
                                            .skuIds(ids.map { it.toString() })
                                            .build()
                                    }
                            )
                            .build()
                    )
                )
                .build(),
            offerAffectingInformationTestInstance(),
            "UNKNOWN",
            emptyList(),
        )
            .test()
            .assertNoErrors()
            .assertResult(
                WidgetData.onlyItems(
                    listOf(
                        CmsProduct.testBuilder()
                            .skuId("1")
                            .build(),
                        CmsProduct.testBuilder()
                            .skuId("3")
                            .build()
                    )
                )
            )

        verify(skusDataSource, times(3)).getSkus(
            any<List<String>>(),
            any<List<String>>(),
            any(),
            any(),
            any(),
            any(),
            any()
        )
    }

    @Test
    fun `Use fallback garsons when switching to multiple requests`() {
        whenever(
            skusDataSource.getSkus(
                any<List<String>>(),
                any<List<String>>(),
                any(),
                any(),
                any(),
                any(),
                any(),
            )
        ) doReturn Single.error(
            RuntimeException()
        )

        whenever(skusDataSource.getModelAnalogs(any(), any(), any(), any())) doReturn listOf<CmsItem>(
            CmsProduct.testBuilder()
                .skuId("1")
                .build(),
            CmsProduct.testBuilder()
                .skuId("2")
                .build()
        ).asObservable()

        repository.getWidgetData(
            CmsWidget.testBuilder()
                .garsons(
                    listOf(
                        GroupSkuByIdCmsWidgetGarson.builder()
                            .groups(
                                listOf(
                                    GroupSkuByIdCmsWidgetGarson.Group.builder()
                                        .skuIds(listOf("1", "2"))
                                        .garson(SkuAnalogsCmsWidgetGarson.testInstance())
                                        .build()
                                )
                            )
                            .build()
                    )
                )
                .build(),
            offerAffectingInformationTestInstance(),
            "UNKNOWN",
            emptyList(),
        )
            .test()
            .assertNoErrors()
            .assertResult(
                WidgetData.onlyItems(
                    listOf(
                        CmsProduct.testBuilder()
                            .skuId("1")
                            .build()
                    )
                )
            )

        verify(skusDataSource).getModelAnalogs(any(), any(), any(), any())
    }
}
