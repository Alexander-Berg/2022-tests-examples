package ru.yandex.market.clean.presentation.feature.sis.mainpage

import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import io.reactivex.Observable
import io.reactivex.Single
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.clean.domain.model.ShopInShopPageId
import ru.yandex.market.clean.domain.model.cms.CmsWidget
import ru.yandex.market.clean.domain.model.navigationNodeTestInstance
import ru.yandex.market.clean.domain.model.plushome.homeCmsPlusHomeEntryPointTestInstance
import ru.yandex.market.clean.domain.model.shop.ShopType
import ru.yandex.market.clean.domain.model.shop.shopInfoTestInstance
import ru.yandex.market.clean.domain.model.sis.NearestShop
import ru.yandex.market.clean.presentation.error.CommonErrorHandler
import ru.yandex.market.clean.presentation.feature.checkout.confirm.changeaddress.AddressToUserAddressMapper
import ru.yandex.market.clean.presentation.feature.cms.formatter.HomeCmsHyperlocalFormatter
import ru.yandex.market.clean.presentation.feature.express.vo.ExpressAddressVo
import ru.yandex.market.clean.presentation.feature.sis.mainpage.formatter.ShopInShopFormatter
import ru.yandex.market.clean.presentation.feature.sis.mainpage.vo.ShopInShopInfoVo
import ru.yandex.market.clean.presentation.formatter.ShopInShopBottomBarVoFormatter
import ru.yandex.market.clean.presentation.navigation.Router
import ru.yandex.market.clean.presentation.view.ShopInShopBottomBar
import ru.yandex.market.common.android.ResourcesManagerImpl
import ru.yandex.market.common.errors.common.commonErrorVoTestInstance
import ru.yandex.market.common.featureconfigs.provider.FeatureConfigsProvider
import ru.yandex.market.data.passport.Address
import ru.yandex.market.domain.hyperlocal.model.HyperlocalAddress
import ru.yandex.market.domain.models.region.geoCoordinatesTestInstance
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.presentationSchedulersMock

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.P])
class ShopInShopCmsPresenterTest {

    private val nearestShop = NearestShop(
        id = "id",
        businessId = ID,
        shopId = ID,
        warehouseId = ID
    )

    private val expressShopType = ShopType.EXPRESS_SHOP
    private val shopDeliveryType = ShopDeliveryType.EXPRESS
    private val retailShopType = ShopType.RETAIL_SHOP
    private val eatsRetailShopType = ShopType.EATS_RETAIL
    private val anotherShopDeliveryType = ShopDeliveryType.MAIN
    private val eatsRetailShopDeliveryType = ShopDeliveryType.EATS_RETAIL

    private val homeCmsPlusHomeEntryPoint = homeCmsPlusHomeEntryPointTestInstance()

    private val geoCoordinates = geoCoordinatesTestInstance()

    private val userAddress = userAddressTestInstance()

    private val hyperlocalAddress = HyperlocalAddress.Exists.Expired(
        coordinates = geoCoordinates,
        userAddress = userAddress
    )

    private val shopInfo = shopInfoTestInstance()
    private val shopInShopInfoVo = ShopInShopInfoVo(
        title = shopInfo.name,
        imageReference = shopInfo.logo,
        rating = shopInfo.rating,
        operationalRatingVo = null,
        workScheduleVo = null,
        backgroundColor = null,
        deliveryTime = null,
        deliveryTimeMinutes = null
    )

    private val listShopInfo = listOf(
        shopInfo
    )

    private val cmsWidget = CmsWidget.testBuilder().build()

    private val cmsWidgets = listOf(
        cmsWidget
    )

    private val anotherCmsWidgets = listOf(
        cmsWidget.copy(id = "another id")
    )

    private val navigationNode = navigationNodeTestInstance()

    private val useCases = mock<ShopInShopCmsUseCases> {

        on { getNearestShopWithType(ID, ShopInShopPageId.MAIN) } doReturn Single.just(nearestShop to expressShopType)

        on { getPlusHomeEntryPoint() } doReturn Observable.just(homeCmsPlusHomeEntryPoint)

        on { observeHyperlocalAddress() } doReturn Observable.just(hyperlocalAddress)

        on { getShopInfo(ID) } doReturn Single.just(listShopInfo)

        on {
            getCmsPage(
                shopType = expressShopType,
                businessId = ID,
                expressWarehouseId = ID,
                isExpress = true,
                isRetail = false,
                shopId = ID,
                sisIcon = shopInShopInfoVo.imageReference,
                sisName = shopInShopInfoVo.title,
            )
        } doReturn Single.just(cmsWidgets)

        on {
            getCmsPage(
                shopType = retailShopType,
                businessId = ID,
                expressWarehouseId = ID,
                isExpress = false,
                isRetail = false,
                shopId = ID,
                sisIcon = shopInShopInfoVo.imageReference,
                sisName = shopInShopInfoVo.title,
            )
        } doReturn Single.just(anotherCmsWidgets)

        on {
            getCmsPage(
                shopType = eatsRetailShopType,
                businessId = ID,
                expressWarehouseId = ID,
                isExpress = false,
                isRetail = false,
                shopId = ID,
                sisIcon = shopInShopInfoVo.imageReference,
                sisName = shopInShopInfoVo.title,
            )
        } doReturn Single.just(anotherCmsWidgets)

        on { getRootNavNode() } doReturn Single.just(navigationNode)

        on { getCartItemsCountByBusinessIdStream(any()) } doReturn Observable.just(0)

        on { hasHyperlocalAddress() } doReturn Single.just(true)
    }

    private val router = mock<Router>()

    private val args = ShopInShopCmsFragment.Arguments(
        businessId = ID,
        isExpress = false,
    )

    private val shopInShopFormatter = mock<ShopInShopFormatter>() {
        on { format(listShopInfo.first(), shopDeliveryType) } doReturn shopInShopInfoVo
    }

    private val address = Address.builder()
        .regionId(userAddress.regionId)
        .country(userAddress.country)
        .city(userAddress.city)
        .build()

    private val enabledExpressAddressVo = ExpressAddressVo.Enabled(
        optionalText = address.city,
        requiredText = address.city,
        isAddressSelected = true
    )

    private val homeCmsHyperlocalFormatter = mock<HomeCmsHyperlocalFormatter>() {
        on { format(address) } doReturn enabledExpressAddressVo
    }

    private val userAddressMapper = mock<AddressToUserAddressMapper>() {
        on { map(hyperlocalAddress.userAddress) } doReturn address
    }

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val resourcesManager = ResourcesManagerImpl(context.resources)

    val featureConfigsProvider = mock<FeatureConfigsProvider>()

    private val loadError = Error()
    private val loadErrorVo = commonErrorVoTestInstance(title = "load error")

    private val commonErrorHandler = mock<CommonErrorHandler>() {
        on {
            format(
                eq(loadError),
                eq(router),
                anyOrNull(),
                anyOrNull(),
                any()
            )
        } doReturn loadErrorVo
    }

    private val shopInShopBottomBarVoFormatter = mock<ShopInShopBottomBarVoFormatter> {
        on { format(CART_COUNT) } doReturn ShopInShopBottomBar.Vo("1 товар в корзине")
    }

    private val presenter = ShopInShopCmsPresenter(
        schedulers = presentationSchedulersMock(),
        useCases = useCases,
        router = router,
        args = args,
        shopInShopFormatter = shopInShopFormatter,
        homeCmsHyperlocalFormatter = homeCmsHyperlocalFormatter,
        userAddressMapper = userAddressMapper,
        resourcesManager = resourcesManager,
        commonErrorHandler = commonErrorHandler,
        shopInShopBottomBarVoFormatter = shopInShopBottomBarVoFormatter,
        shopActualizedDeliveryFormatter = mock(),
        eatsRetailAnalytics = mock(),
        eatsRetailHealthFacade = mock { on { get() } doReturn mock() },
        shopInShopAnalytics = mock(),
        sisCartNavigationDelegate = mock(),
    )

    private val view = mock<ShopInShopCmsView>()

    @Test
    fun `Address and plus data was observed`() {
        presenter.attachView(view)

        verify(useCases, times(1)).getPlusHomeEntryPoint()
        verify(useCases, times(1)).observeHyperlocalAddress()
        verify(view, times(1)).setupCashback(any())
        verify(view, times(1)).setupHyperlocal(any())
    }

    @Test
    fun `Shop delivery type was selected`() {
        presenter.attachView(view)

        verify(useCases, times(1)).getNearestShopWithType(ID, ShopInShopPageId.MAIN)
        verify(view, times(1)).setShopTypeSelectorVisibility(false)
        verify(view, times(1)).setShopDeliverySelected(shopDeliveryType)
    }

    @Test
    fun `Another shop delivery type was clicked`() {
        presenter.attachView(view)
        presenter.onShopTypeClick(anotherShopDeliveryType)

        verify(view, times(1)).setShopDeliverySelected(anotherShopDeliveryType)
        verify(view, times(1)).updateHeaderChips(any(), eq(anotherShopDeliveryType))
        verify(useCases, times(1))
            .getCmsPage(
                shopType = retailShopType,
                businessId = ID,
                expressWarehouseId = ID,
                isExpress = false,
                isRetail = false,
                shopId = ID,
                sisIcon = shopInShopInfoVo.imageReference,
                sisName = shopInShopInfoVo.title,
            )
        verify(view, times(1)).showWidgets(
            widgets = anotherCmsWidgets,
            navNode = navigationNode,
            fesh = ID,
            expressWarehouseId = ID,
            isExpress = false
        )
    }

    @Test
    fun `Eats retail shop delivery type was clicked`() {
        presenter.attachView(view)
        presenter.onShopTypeClick(eatsRetailShopDeliveryType)

        verify(view, times(1)).setShopDeliverySelected(eatsRetailShopDeliveryType)
        verify(view, times(1)).updateHeaderChips(any(), eq(eatsRetailShopDeliveryType))
        verify(useCases, times(1))
            .getCmsPage(
                shopType = eatsRetailShopType,
                businessId = ID,
                expressWarehouseId = ID,
                isExpress = false,
                isRetail = false,
                shopId = ID,
                sisIcon = shopInShopInfoVo.imageReference,
                sisName = shopInShopInfoVo.title,
            )
        verify(view, times(1)).showWidgets(
            widgets = anotherCmsWidgets,
            navNode = navigationNode,
            fesh = ID,
            expressWarehouseId = ID,
            isExpress = false
        )
    }

    @Test
    fun `Error was shown instead of delivery type`() {
        whenever(useCases.getNearestShopWithType(any(), any())) doReturn Single.error(loadError)

        presenter.attachView(view)

        verify(view, times(1)).showError(any())
    }

    @Test
    fun `Header info was shown`() {
        presenter.attachView(view)

        verify(useCases, times(1)).getShopInfo(ID)
        verify(view, times(1)).showHeaderInfo(shopInShopInfoVo, shopDeliveryType)
    }

    @Test
    fun `Error was shown instead of header info`() {
        whenever(useCases.getShopInfo(any())) doReturn Single.error(loadError)

        presenter.attachView(view)

        verify(view, times(1)).showError(any())
    }

    @Test
    fun `Widgets was shown`() {
        presenter.attachView(view)

        verify(useCases, times(1))
            .getCmsPage(
                shopType = expressShopType,
                businessId = ID,
                expressWarehouseId = ID,
                isExpress = true,
                isRetail = false,
                shopId = ID,
                sisIcon = shopInShopInfoVo.imageReference,
                sisName = shopInShopInfoVo.title,
            )
        verify(view, times(1)).showWidgets(
            widgets = cmsWidgets,
            navNode = navigationNode,
            fesh = ID,
            expressWarehouseId = ID,
            isExpress = true
        )
    }

    @Test
    fun `Error was shown instead of widgets`() {
        whenever(useCases.getCmsPage(any(), any(), any(), any(), any(), any(), any(), any())) doReturn Single.error(loadError)

        presenter.attachView(view)

        verify(view, times(1)).showError(any())
    }

    @Test
    fun `Hyper local address is absent`() {
        whenever(useCases.observeHyperlocalAddress()) doReturn Observable.just(HyperlocalAddress.Absent)

        presenter.attachView(view)

        verify(useCases, times(1)).getDeliveryLocality()
    }

    @Test
    fun `Sis goods in cart`() {
        whenever(useCases.getCartItemsCountByBusinessIdStream(ID)) doReturn Observable.just(CART_COUNT)

        presenter.attachView(view)

        verify(view, times(1))
            .setShopInShopBottomBarAvailable(true, ShopInShopBottomBar.Vo("1 товар в корзине"))
    }

    companion object {
        private const val ID = 1L
        private const val CART_COUNT = 1
    }
}
