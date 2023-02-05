package ru.yandex.market.clean.data.repository.cart

import com.annimon.stream.Exceptional
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.PublishSubject
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import org.mockito.kotlin.whenever
import ru.yandex.market.analytics.facades.health.EatsRetailHealthFacade
import ru.yandex.market.clean.data.fapi.contract.AddCartItemsContract
import ru.yandex.market.clean.data.fapi.dto.cart.frontApiAddCartItemsDtoTestInstance
import ru.yandex.market.clean.data.fapi.contract.addCartItemsContract_ItemTestInstance
import ru.yandex.market.clean.data.fapi.dto.cart.frontApiMergedCartDtoTestInstance
import ru.yandex.market.clean.data.fapi.source.cart.CartFapiClient
import ru.yandex.market.clean.data.mapper.CartCombineStrategyMapper
import ru.yandex.market.clean.data.mapper.CartMapper
import ru.yandex.market.clean.data.mapper.OfferAffectingInformationMapper
import ru.yandex.market.clean.data.mapper.ProductOfferMapper
import ru.yandex.market.clean.data.mapper.cart.AddCartItemsContractItemMapper
import ru.yandex.market.clean.data.mapper.cart.AddCartItemsResultMapper
import ru.yandex.market.clean.data.mapper.cart.CartBusinessGroupMapper
import ru.yandex.market.clean.data.mapper.cart.CartItemMapper
import ru.yandex.market.clean.data.mapper.cart.FrontApiCartItemDtoMapper
import ru.yandex.market.clean.data.mapper.cart.PersistentMergedCartItemDtoMapper
import ru.yandex.market.clean.data.mapper.sis.ShopInShopMetrikaParamsRequestDtoMapper
import ru.yandex.market.clean.data.mapper.sku.DetailedSkuMapper
import ru.yandex.market.clean.data.model.db.cartItemEntityTestInstance
import ru.yandex.market.clean.data.repository.AuthRepositoryImpl
import ru.yandex.market.clean.data.repository.DateInStockProductRepository
import ru.yandex.market.clean.data.repository.IsInPickupPromoCodeSegmentRepository
import ru.yandex.market.clean.data.repository.retail.RetailRepository
import ru.yandex.market.clean.data.repository.sku.DetailedSkuRepository
import ru.yandex.market.clean.data.store.cart.CartPersistentDataStore
import ru.yandex.market.clean.data.store.cartitem.CartBusinessGroupsDataStore
import ru.yandex.market.clean.data.store.cartitem.CartItemCacheDataStore
import ru.yandex.market.clean.data.store.cartitem.CartItemFapiDataStore
import ru.yandex.market.clean.domain.model.CartItem
import ru.yandex.market.clean.domain.model.OfferSpecificationInternal
import ru.yandex.market.clean.domain.model.cart.CartBusinessGroupType
import ru.yandex.market.clean.domain.model.cart.CartItemId
import ru.yandex.market.clean.domain.model.cart.cartCombineStrategyTestInstance
import ru.yandex.market.clean.domain.model.cart.cartItemIdTestInstance
import ru.yandex.market.clean.domain.model.cartAffectingDataTestInstance
import ru.yandex.market.clean.domain.model.cartIdentifierTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.giftOfferTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.clean.domain.usecase.AddItemsToCartMapByOfferAvailableUseCase
import ru.yandex.market.clean.domain.usecase.GetOfferConfigUseCase
import ru.yandex.market.clean.domain.usecase.cart.CartCombineStrategiesUseCase
import ru.yandex.market.clean.domain.usecase.cart.IsCartUpsellNeedForceWaitDigestUseCase
import ru.yandex.market.clean.domain.usecase.upsell.GetCartUpsellComplementaryFlagForCartItemsUseCase
import ru.yandex.market.common.featureconfigs.models.OfferMapperConfig
import ru.yandex.market.common.randomgenerator.RandomGenerator
import ru.yandex.market.common.schedulers.NetworkingScheduler
import ru.yandex.market.data.promo.mapper.OfferPromoTypeMapper
import ru.yandex.market.db.addCartItemsResultTestInstance
import ru.yandex.market.domain.auth.model.authTokenTestInstance
import ru.yandex.market.domain.auth.model.uuidTestInstance
import ru.yandex.market.domain.auth.usecase.GetUuidUseCase
import ru.yandex.market.domain.product.model.ProductId
import ru.yandex.market.optional.Optional
import ru.yandex.market.rub
import ru.yandex.market.test.extensions.arg
import ru.yandex.market.test.extensions.asSingle
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.utils.asOptional

class CartItemRepositoryTest {

    private val cartItemsStream: PublishSubject<Optional<List<CartItem>>> = PublishSubject.create()

    private val cartItemCacheDataStore = mock<CartItemCacheDataStore> {
        on { getCartItemsStream() } doReturn cartItemsStream
        on { getCartItemsCache(any()) } doReturn emptyList()
    }
    private val cartItemFapiDataStore = mock<CartItemFapiDataStore>()
    private val cartItemMapper = mock<CartItemMapper>()
    private val cartMapper = mock<CartMapper> {
        on { mapItems(any(), any(), any()) } doReturn listOf(cartItemTestInstance().asExceptional())
    }
    private val addCartItemsContractItemMapper = mock<AddCartItemsContractItemMapper>()
    private val addCartItemsResultMapper = mock<AddCartItemsResultMapper>()

    private val networkingScheduler = NetworkingScheduler(Schedulers.trampoline())
    private val cartItemScheduler = Schedulers.trampoline()

    private val authRepository = mock<AuthRepositoryImpl> {
        on { getAuthToken(any()) } doReturn authTokenTestInstance().asSingle()
        on { deprecatedAuthTokenStream } doReturn Observable.empty()
    }
    private val randomGenerator = mock<RandomGenerator>()
    private val frontApiCartItemDtoMapper = mock<FrontApiCartItemDtoMapper>()
    private val cartFapiClient = mock<CartFapiClient> {
        on { getCartStrategiesByBusinessGroups(any(), any(), any(), anyOrNull(), anyOrNull()) } doReturn Single.just(
            frontApiMergedCartDtoTestInstance()
        )
    }
    private val cartPersistentDataStore = mock<CartPersistentDataStore>()
    private val persistentCartItemDtoMapper = mock<PersistentMergedCartItemDtoMapper>()
    private val cartSynchronizationMonitor = mock<CartSynchronizationMonitor> {
        on { getCartSynchronizationEventsStream() } doReturn Observable.never()
    }
    private val getUuidUseCase = mock<GetUuidUseCase> {
        on { getUuid() } doReturn Single.just(uuidTestInstance())
    }
    private val cartCombineStrategyMapper = mock<CartCombineStrategyMapper>() {
        on { map(any(), any()) } doReturn cartCombineStrategyTestInstance().asExceptional()
    }
    private val dateInStockProductRepositoryMock = mock<DateInStockProductRepository>() {
        on { getProductsDateInStock(any<ProductId>()) } doReturn Single.just(Optional.empty())
        on { getProductsDateInStock(any<List<ProductId>>()) } doReturn Single.just(emptyMap())
    }

    private val productOfferMapper = mock<ProductOfferMapper>()
    private val shopInShopMetrikaParamsRequestDtoMapper = mock<ShopInShopMetrikaParamsRequestDtoMapper> {
        on { map(any()) } doReturn null
    }
    private val addItemsToCartMapByOfferAvailableUseCase = mock<AddItemsToCartMapByOfferAvailableUseCase> {
        on { execute() } doReturn Single.just(false)
    }
    private val getCartUpsellComplementaryFlagUseCase = mock<GetCartUpsellComplementaryFlagForCartItemsUseCase> {
        on { execute() } doReturn Single.just(false)
    }
    private val getOfferConfigUseCase = mock<GetOfferConfigUseCase> {
        on { execute() } doReturn Single.just(OfferMapperConfig(null, false))
    }

    private val isCartUpsellNeedForceWaitDigestUseCase = mock<IsCartUpsellNeedForceWaitDigestUseCase> {
        on { execute() } doReturn Single.just(false)
    }

    private val detailedSkuRepository = mock<DetailedSkuRepository>()
    private val detailedSkuMapper = mock<DetailedSkuMapper>()
    private val configuration = OfferPromoTypeMapper.Configuration(
        minimumBundleSize = 2,
        maxBlueSetAdditionalItemsCount = 2,
        promoTypeGifts = "bg",
        promoTypeGiftAdditionalItem = "gbg",
        promoTypeCheapestAsGift = "cg",
        promoTypeFlashSales = "fs",
        promoTypeBlueSet = "bs",
        promoTypeBlueSetAdditionalItem = "bsai",
        promoTypePriceDrop = "pd",
        promoTypeDirectDiscount = "dd",
        promoTypeSecretSale = "ss",
        cashback = "cb",
        promoTypePromoCode = "pc",
        promoTypeSpreadDiscountCount = "ptsdc",
        promoTypeSpreadDiscountReceipt = "ptsdr",
        promoCashbackCollection = "cbc",
        promoPaymentSystemCashbackCollection = "promoPaymentSystemCashbackCollection",
        promoCashbackYaCardCollection = "promoCashbackYaCardCollection",
        promoTypeParentPromo = "promoTypeParentPromo",
    )
    private val cartCombineStrategiesUseCase = mock<CartCombineStrategiesUseCase>() {
        on { getSelectedStrategy() } doReturn cartCombineStrategyTestInstance().asOptional().asSingle()
        on { setStrategies(any()) } doReturn Completable.complete()
    }

    private val cartBusinessGroupsDataStore = mock<CartBusinessGroupsDataStore>()
    private val cartBusinessGroupMapper = mock<CartBusinessGroupMapper>()

    private val isInPickupPromoCodeSegmentRepository = mock<IsInPickupPromoCodeSegmentRepository>()

    private val retailRepository = mock<RetailRepository> {
        on { saveCartItemsAsRetailCarts(any()) } doReturn Completable.complete()
    }

    private val eatsRetailHealthFacade = mock<EatsRetailHealthFacade>()

    private val cartItemRepositoryLocalCart = CartItemRepositoryLocalCart(
        cartItemCacheDataStore,
        cartItemFapiDataStore,
        cartMapper,
        cartItemMapper,
        addCartItemsResultMapper,
        networkingScheduler,
        cartItemScheduler,
        frontApiCartItemDtoMapper,
        cartFapiClient,
        authRepository,
        randomGenerator,
        cartSynchronizationMonitor,
        getUuidUseCase,
        cartCombineStrategyMapper,
        addCartItemsContractItemMapper,
        configuration,
        cartCombineStrategiesUseCase,
        detailedSkuRepository,
        persistentCartItemDtoMapper,
        shopInShopMetrikaParamsRequestDtoMapper,
        cartPersistentDataStore,
        detailedSkuMapper,
        productOfferMapper,
        cartBusinessGroupsDataStore,
        cartBusinessGroupMapper,
        dateInStockProductRepositoryMock,
        getCartUpsellComplementaryFlagUseCase,
        isCartUpsellNeedForceWaitDigestUseCase,
        getOfferConfigUseCase,
    )

    private val offerAffectingInformationMapper = mock<OfferAffectingInformationMapper>()

    private val cartItemRepository = CartItemRepository(
        cartItemCacheDataStore,
        cartItemFapiDataStore,
        cartMapper,
        addCartItemsResultMapper,
        networkingScheduler,
        cartItemScheduler,
        authRepository,
        randomGenerator,
        cartSynchronizationMonitor,
        addCartItemsContractItemMapper,
        getUuidUseCase,
        cartCombineStrategyMapper,
        configuration,
        cartCombineStrategiesUseCase,
        detailedSkuRepository,
        detailedSkuMapper,
        offerAffectingInformationMapper,
        shopInShopMetrikaParamsRequestDtoMapper,
        cartBusinessGroupsDataStore,
        cartFapiClient,
        cartBusinessGroupMapper,
        dateInStockProductRepositoryMock,
        retailRepository,
        addItemsToCartMapByOfferAvailableUseCase,
        eatsRetailHealthFacade = { eatsRetailHealthFacade },
        isInPickupPromoCodeSegmentRepository,
        getCartUpsellComplementaryFlagUseCase,
        isCartUpsellNeedForceWaitDigestUseCase,
        getOfferConfigUseCase,
    )

    @Test
    fun `Delete secondary bundle items first`() {

        val deleteCartItemCalls = mutableListOf<Long>()
        whenever(cartItemFapiDataStore.deleteItems(any(), anyOrNull(), anyOrNull(), anyOrNull())) doAnswer {
            (it.arg() as List<CartItemId>).forEach { id ->
                deleteCartItemCalls.add(id.cartItemId)
            }
            Completable.complete()
        }

        cartItemRepositoryLocalCart.deleteCartItems(
            cartItemIds = listOf(
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = true, cartItemId = 0),
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = false, cartItemId = 1),
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = false, cartItemId = 2),
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = true, cartItemId = 3),
                cartItemIdTestInstance(bundleId = "", isPrimaryBundleItem = false, cartItemId = 4)
            ),
            cartAffectingData = cartAffectingDataTestInstance(),
            shopInShopMetrikaParams = null,
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        assertThat(deleteCartItemCalls).containsOnly(4L, 1, 0, 2, 3)
    }

    @Test
    fun `Update primary bundle items count first when using fapi`() {
        val updateCartItemCalls = mutableListOf<Long>()
        whenever(cartItemFapiDataStore.updateCartItemsCount(any(), anyOrNull(), any())) doAnswer {
            (it.arg() as Map<CartItemId, Int>).forEach { (id, _) ->
                updateCartItemCalls.add(id.cartItemId)
            }
            Completable.complete()
        }

        cartItemRepositoryLocalCart.updateCartItemsCount(
            itemIdsToCount = mapOf(
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = true, cartItemId = 0) to 2,
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = false, cartItemId = 1) to 2,
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = false, cartItemId = 2) to 2,
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = true, cartItemId = 3) to 2,
                cartItemIdTestInstance(bundleId = "", isPrimaryBundleItem = false, cartItemId = 4) to 2
            ),
            cartAffectingData = cartAffectingDataTestInstance()
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        assertThat(updateCartItemCalls).containsOnly(4L, 0, 1, 3, 2)
    }

    @Test
    fun `Use passed price when adding bundle to cart`() {
        val primaryPrice = 8_000.rub
        val randomString = "not empty string"
        val offer = productOfferTestInstance()
        whenever(
            cartItemFapiDataStore.addOffer(any(), anyOrNull(), any(), anyOrNull())
        ) doReturn frontApiAddCartItemsDtoTestInstance().asSingle()
        whenever(
            addCartItemsResultMapper.map(
                result = any(),
                productOffersByLabel = any(),
                entities = any(),
                dateInStockMap = any()
            )
        ) doReturn addCartItemsResultTestInstance()
        whenever(randomGenerator.getRandomUuid()).thenReturn(randomString)
        whenever(
            addCartItemsContractItemMapper.map(
                offer = offer,
                count = 1,
                isPrimaryBundleItem = true,
                bundleId = "bundleId",
                price = primaryPrice,
                label = randomString,
                alternativeOfferReason = null,
                configuration = configuration,
                selectedServiceId = null,
                isDirectShopInShop = false,
            )
        ) doReturn addCartItemsContract_ItemTestInstance(
            price = AddCartItemsContract.Price(
                value = primaryPrice.amount.value,
                currency = primaryPrice.currency.name
            )
        )
        whenever(
            cartItemMapper.map(
                item = cartItemEntityTestInstance(),
                dateInStock = null,
                offer = null,
                packInfo = null,
                cartItemCount = null,
                unavailableReason = null,
                offerFeaturesFallback = emptyList(),
                internalOfferProperties = OfferSpecificationInternal(),
            )
        ) doReturn Exceptional.of { cartItemTestInstance(carterPrice = primaryPrice) }
        whenever(cartItemFapiDataStore.addOffer(any(), anyOrNull(), any(), anyOrNull())) doReturn
            frontApiAddCartItemsDtoTestInstance().asSingle()
        cartItemRepositoryLocalCart.addOfferToCart(
            offer = offer,
            price = primaryPrice,
            count = 1,
            giftOffers = listOf(giftOfferTestInstance(primaryOfferPrice = primaryPrice)),
            bundleId = "bundleId",
            cartAffectingData = cartAffectingDataTestInstance(),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopMetrikaParams = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemFapiDataStore).addOffer(
            argThat { first().price.value.compareTo(primaryPrice.amount.value) == 0 },
            anyOrNull(),
            any(),
            anyOrNull()
        )
    }

    @Test
    fun `Delete secondary bundle items first non local`() {
        whenever(isInPickupPromoCodeSegmentRepository.setIsInPickupPromoCodeSegment(any()))
            .thenReturn(Completable.complete())
        val deleteCartItemCalls = mutableListOf<Long>()
        whenever(cartItemFapiDataStore.deleteItems(any(), anyOrNull(), anyOrNull(), anyOrNull())) doAnswer {
            (it.arg() as List<CartItemId>).forEach { id ->
                deleteCartItemCalls.add(id.cartItemId)
            }
            Completable.complete()
        }

        cartItemRepository.deleteCartItems(
            cartItemIds = listOf(
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = true, cartItemId = 0),
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = false, cartItemId = 1),
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = false, cartItemId = 2),
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = true, cartItemId = 3),
                cartItemIdTestInstance(bundleId = "", isPrimaryBundleItem = false, cartItemId = 4)
            ),
            cartAffectingData = cartAffectingDataTestInstance(),
            shopInShopMetrikaParams = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        assertThat(deleteCartItemCalls).containsOnly(4L, 1, 0, 2, 3)
    }

    @Test
    fun `Update primary bundle items count first when using fapi non local`() {
        val updateCartItemCalls = mutableListOf<Long>()
        whenever(cartItemFapiDataStore.updateCartItemsCount(any(), anyOrNull(), any())) doAnswer {
            (it.arg() as Map<CartItemId, Int>).forEach { (id, _) ->
                updateCartItemCalls.add(id.cartItemId)
            }
            Completable.complete()
        }

        whenever(isInPickupPromoCodeSegmentRepository.setIsInPickupPromoCodeSegment(any()))
            .thenReturn(Completable.complete())

        cartItemRepository.updateCartItemsCount(
            itemIdsToCount = mapOf(
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = true, cartItemId = 0) to 2,
                cartItemIdTestInstance(bundleId = "1", isPrimaryBundleItem = false, cartItemId = 1) to 2,
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = false, cartItemId = 2) to 2,
                cartItemIdTestInstance(bundleId = "2", isPrimaryBundleItem = true, cartItemId = 3) to 2,
                cartItemIdTestInstance(bundleId = "", isPrimaryBundleItem = false, cartItemId = 4) to 2
            ),
            cartAffectingData = cartAffectingDataTestInstance()
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        assertThat(updateCartItemCalls).containsOnly(4L, 0, 1, 3, 2)
    }

    @Test
    fun `Use passed price when adding bundle to cart non local`() {
        val primaryPrice = 8_000.rub
        val randomString = "not empty string"
        val offer = productOfferTestInstance()
        whenever(cartItemFapiDataStore.addOffer(any(), anyOrNull(), any(), anyOrNull())) doReturn
            frontApiAddCartItemsDtoTestInstance().asSingle()
        whenever(
            addCartItemsResultMapper.map(
                result = any(),
                productOffersByLabel = any(),
                dateInStockMap = any(),
                isFallbackMapEnabled = anyBoolean(),
            )
        ) doReturn addCartItemsResultTestInstance()
        whenever(randomGenerator.getRandomUuid()).thenReturn("not empty string")
        whenever(cartItemFapiDataStore.addOffer(any(), anyOrNull(), any(), anyOrNull())) doReturn
            frontApiAddCartItemsDtoTestInstance().asSingle()
        whenever(randomGenerator.getRandomUuid()).thenReturn(randomString)
        whenever(
            addCartItemsContractItemMapper.map(
                offer = offer,
                count = 1,
                isPrimaryBundleItem = true,
                bundleId = "bundleId",
                price = primaryPrice,
                label = randomString,
                alternativeOfferReason = null,
                configuration = configuration,
                selectedServiceId = null,
                isDirectShopInShop = false,
            )
        ) doReturn addCartItemsContract_ItemTestInstance(
            price = AddCartItemsContract.Price(
                value = primaryPrice.amount.value,
                currency = primaryPrice.currency.name
            )
        )
        whenever(isInPickupPromoCodeSegmentRepository.setIsInPickupPromoCodeSegment(any()))
            .thenReturn(Completable.complete())
        cartItemRepository.addOfferToCart(
            offer = productOfferTestInstance(),
            price = primaryPrice,
            count = 1,
            giftOffers = listOf(giftOfferTestInstance(primaryOfferPrice = primaryPrice)),
            bundleId = "bundleId",
            cartAffectingData = cartAffectingDataTestInstance(),
            alternativeOfferReason = null,
            selectedServiceId = null,
            isDirectShopInShop = false,
            shopInShopMetrikaParams = null
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemFapiDataStore).addOffer(
            argThat { first().price.value.compareTo(primaryPrice.amount.value) == 0 },
            anyOrNull(),
            any(),
            anyOrNull()
        )
    }

    @Test
    fun `Sync CartItems with Retail items (non local)`() {
        val expectedItems = listOf(
            cartItemTestInstance(
                cartIdentifier = cartIdentifierTestInstance(type = CartBusinessGroupType.DEFAULT),
            ),
            cartItemTestInstance(
                cartIdentifier = cartIdentifierTestInstance(type = CartBusinessGroupType.FOODTECH),
            ),
        )

        val expectedRetailItems = expectedItems
            .filter { it.cartIdentifier?.type == CartBusinessGroupType.FOODTECH }

        whenever(
            cartMapper.mapItems(
                mergedDto = any(),
                selectedStrategy = any(),
                dateInStockMap = any(),
                cartBusinessGroups = any(),
                offerMapperConfig = any(),
            )
        ).thenReturn(expectedItems)

        whenever(isInPickupPromoCodeSegmentRepository.setIsInPickupPromoCodeSegment(any()))
            .thenReturn(Completable.complete())

        cartItemRepository.syncCartItems(
            cartAffectingData = cartAffectingDataTestInstance(),
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemCacheDataStore, times(2)).setCartItems(any(), eq(expectedItems))
        verify(retailRepository).saveCartItemsAsRetailCarts(eq(expectedRetailItems))

        verifyZeroInteractions(
            cartItemCacheDataStore,
            retailRepository,
            eatsRetailHealthFacade,
        )
    }

    @Test
    fun `Sync CartItems with Retail items with shop is null (non local)`() {
        val expectedItems = listOf(
            cartItemTestInstance(
                cartId = 30,
                shopId = 31,
                cartIdentifier = cartIdentifierTestInstance(type = CartBusinessGroupType.FOODTECH),
                shop = null,
            ),
        )

        whenever(
            cartMapper.mapItems(
                mergedDto = any(),
                selectedStrategy = any(),
                dateInStockMap = any(),
                cartBusinessGroups = any(),
                offerMapperConfig = any(),
            )
        ).thenReturn(expectedItems)

        whenever(isInPickupPromoCodeSegmentRepository.setIsInPickupPromoCodeSegment(any()))
            .thenReturn(Completable.complete())

        cartItemRepository.syncCartItems(
            cartAffectingData = cartAffectingDataTestInstance(),
        )
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(cartItemCacheDataStore, times(2)).setCartItems(any(), eq(expectedItems))
        verify(retailRepository).saveCartItemsAsRetailCarts(eq(expectedItems))

        verify(eatsRetailHealthFacade, times(1)).cartItemWithoutShop(
            cartId = 30L,
            shopId = 31L,
        )

        verifyZeroInteractions(
            cartItemCacheDataStore,
            retailRepository,
            eatsRetailHealthFacade,
        )
    }
}
