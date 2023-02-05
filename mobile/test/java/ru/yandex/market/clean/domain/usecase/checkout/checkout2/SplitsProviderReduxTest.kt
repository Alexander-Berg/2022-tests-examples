package ru.yandex.market.clean.domain.usecase.checkout.checkout2

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.data.PaymentMethodPriorityProvider
import ru.yandex.market.checkout.data.mapper.BucketInfo2Mapper
import ru.yandex.market.checkout.data.mapper.DeliveryOptionActualizer
import ru.yandex.market.checkout.domain.model.ErrorsPack
import ru.yandex.market.checkout.domain.model.packPositionTestInstance
import ru.yandex.market.checkout.domain.usecase.payment.GetOrderItemsCartCachedDataUseCase
import ru.yandex.market.checkout.domain.usecase.payment.googlepay.GooglePayAvailabilityUseCase
import ru.yandex.market.clean.domain.model.checkout.BucketFieldModification
import ru.yandex.market.clean.domain.model.checkout.BucketInfo2
import ru.yandex.market.clean.domain.model.checkout.CheckoutSplit
import ru.yandex.market.clean.domain.model.checkout.FieldModification
import ru.yandex.market.clean.domain.model.checkout.SplitFieldModification
import ru.yandex.market.clean.domain.model.checkout.bucketInfo2TestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.checkout.deliveryOptionModelTestInstance
import ru.yandex.market.clean.domain.model.checkout.orderItemCartCachedDataTestInstance
import ru.yandex.market.clean.domain.model.order.shopOrderOptionsModelTestInstance
import ru.yandex.market.data.order.options.deliveryOptionTestInstance
import ru.yandex.market.data.payment.network.dto.PaymentMethod
import ru.yandex.market.data.payment.network.dto.PaymentMethod.CASH_ON_DELIVERY
import ru.yandex.market.data.payment.network.dto.PaymentMethod.GOOGLE_PAY
import ru.yandex.market.data.payment.network.dto.PaymentMethod.YANDEX
import ru.yandex.market.domain.delivery.model.DeliveryType
import ru.yandex.market.safe.Safe

class SplitsProviderReduxTest {

    private val bucketInfo2Mapper = mock<BucketInfo2Mapper>()
    private val deliveryOptionActualizer = mock<DeliveryOptionActualizer>()
    private val paymentMethodPriorityProvider = mock<PaymentMethodPriorityProvider>()
    private val orderItemsCartCachedData = orderItemCartCachedDataTestInstance()
    private val getOrderItemsCartCachedDataUseCase = mock<GetOrderItemsCartCachedDataUseCase>() {
        on { execute() } doReturn Single.just(listOf(orderItemsCartCachedData))
    }

    private val googlePayAvailabilityUseCase = mock<GooglePayAvailabilityUseCase> {
        on { isAvailable() } doReturn Single.just(true)
    }

    private val splitsProvider = SplitsProviderRedux(
        bucketInfo2Mapper,
        deliveryOptionActualizer,
        paymentMethodPriorityProvider,
        googlePayAvailabilityUseCase,
        getOrderItemsCartCachedDataUseCase,
    )

    private val errorsPack = mock<ErrorsPack>()

    //buckets
    private val serverBucket = bucketInfo2TestInstance(packPositionTestInstance(id = DUMMY_PACK_ID_0))
    private val localBucket = bucketInfo2TestInstance(packPositionTestInstance(id = DUMMY_PACK_ID_0))

    //splits
    private val split = checkoutSplitTestInstance(id = DUMMY_SPLIT_ID_0, buckets = listOf(localBucket))
    private val splitsList = listOf(split)

    //shops
    private val shop = shopOrderOptionsModelTestInstance(label = DUMMY_SHOP_LABEL_0)
    private val shopsList = listOf(shop)

    //delivery options mocks
    private val deliveryOptionsZero = deliveryOptionTestInstance(packId = DUMMY_PACK_ID_0)
    private val deliveryOptionsOne = deliveryOptionTestInstance(packId = DUMMY_PACK_ID_0)
    private val deliveryOptionsTwo = deliveryOptionTestInstance(packId = DUMMY_PACK_ID_1)
    private val deliveryOptionsList = listOf(deliveryOptionsZero, deliveryOptionsOne, deliveryOptionsTwo)

    //delivery option
    private val deliveryOptionModel = deliveryOptionModelTestInstance()


    @Test
    fun `Should ignore bad mappings to bucketInfo`() {
        val incorrectShop = shop.copy(label = DUMMY_ID)
        val shopsList = listOf(shop, incorrectShop)
        whenever(
            bucketInfo2Mapper.map(
                shop = shop,
                globalErrorsPack = errorsPack,
                index = 0,
                countAllBuckets = 2,
                isGooglePayAvailable = true,
                deliveryOptions = listOf(deliveryOptionsZero, deliveryOptionsOne),
                orderItemsCartCachedData = listOf(orderItemsCartCachedData),
            )
        ).thenReturn(Safe.value(serverBucket))

        whenever(
            bucketInfo2Mapper.map(
                shop = incorrectShop,
                globalErrorsPack = errorsPack,
                index = 1,
                countAllBuckets = 2,
                isGooglePayAvailable = true,
                deliveryOptions = emptyList(),
                orderItemsCartCachedData = listOf(orderItemsCartCachedData),
            )
        ).thenReturn(Safe.error(RuntimeException()))

        whenever(deliveryOptionActualizer.mapDeliveryOptionModel(any(), any(), any(), any(), any())).thenReturn(
            DeliveryType.DELIVERY to null
        )
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
    }

    @Test
    fun `Should set last selected payment method if it was selected and available`() {
        val bucket = localBucket.copy(selectedPaymentMethod = CASH_ON_DELIVERY)
        val splits = split.copy(buckets = bucket.asList()).asList()
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splits, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                assertPaymentModificationCorrect(
                    splits = splits,
                    modifications = modifications,
                    selectedPaymentMethod = CASH_ON_DELIVERY
                )
            }
    }

    @Test
    fun `Should set payment method by priority if previous payment method was not selected`() {
        val localBucket = localBucket.copy(selectedPaymentMethod = null, isOnDemandDeliverySelected = false)
        val splits = split.copy(buckets = localBucket.asList()).asList()
        val serverBucket = serverBucket.copy(availablePaymentMethods = listOf(YANDEX, CASH_ON_DELIVERY))
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splits, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                assertPaymentModificationCorrect(
                    splits = splits,
                    modifications = modifications,
                    selectedPaymentMethod = YANDEX
                )
            }
    }

    @Test
    fun `Should set payment method by priority if previous payment method was selected but not available`() {
        val localBucket = localBucket.copy(selectedPaymentMethod = YANDEX, isOnDemandDeliverySelected = false)
        val splits = split.copy(buckets = localBucket.asList()).asList()
        val serverBucket = serverBucket.copy(availablePaymentMethods = listOf(GOOGLE_PAY, CASH_ON_DELIVERY))
        prepareMocks(serverBucket)

        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splits, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modificatios) ->
                assertPaymentModificationCorrect(
                    splits = splits,
                    modifications = modificatios,
                    selectedPaymentMethod = GOOGLE_PAY
                )
            }
    }

    @Test
    fun `Should create modification for selected delivery option`() {
        prepareMocks(serverBucket)
        whenever(deliveryOptionActualizer.actualize(any(), any(), any())).thenReturn(
            mapOf(DeliveryType.DELIVERY to deliveryOptionModel)
        )
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultBucket = resultSplit.buckets.first()
                val selectedDeliveryOption = mapOf(DeliveryType.DELIVERY to deliveryOptionModel)
                val resultModification = BucketFieldModification.BucketSelectedDeliveryOption(
                    splitId = resultSplit.id,
                    packId = resultBucket.packId,
                    selectedDeliveryOption = selectedDeliveryOption
                )
                val deliveryOptionsChanged =
                    resultBucket.selectedDeliveryOption[DeliveryType.DELIVERY] == deliveryOptionModel
                val hasDeliveryOptionModification = modifications.contains(resultModification)
                deliveryOptionsChanged && hasDeliveryOptionModification
            }
    }

    @Test
    fun `Should change onDemandDeliveryOption when lavka is enabled`() {
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultBucket = resultSplit.buckets.first()
                val resultModification = BucketFieldModification.BucketOnDemandDeliveryOption(
                    splitId = resultSplit.id,
                    packId = resultBucket.packId,
                    onDemandDeliveryOption = deliveryOptionModel
                )
                val deliveryOptionsChanged = resultBucket.onDemandDeliveryOption == deliveryOptionModel
                val hasOnDemandDeliveryModification = modifications.contains(resultModification)
                deliveryOptionsChanged && hasOnDemandDeliveryModification
            }
    }

    @Test
    fun `Should set on isOnDemandSelected if lavka is enabled`() {
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultBucket = resultSplit.buckets.first()
                val resultModification = BucketFieldModification.BucketIsOnDemandDeliverySelected(
                    splitId = resultSplit.id,
                    packId = resultBucket.packId,
                    isOnDemandDeliverySelected = true
                )
                val hasOnDemandDeliverySelectedModification = modifications.contains(resultModification)
                resultBucket.isOnDemandDeliverySelected && hasOnDemandDeliverySelectedModification
            }
    }

    @Test
    fun `Should set selectedAddress if there's no notDeliveryOptions in split`() {
        whenever(deliveryOptionActualizer.actualize(any(), any(), any())).thenReturn(
            mapOf(
                DeliveryType.DELIVERY to deliveryOptionModelTestInstance(),
                DeliveryType.PICKUP to deliveryOptionModelTestInstance()
            )
        )
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultModification = SplitFieldModification.SplitSelectedUserAddress(
                    splitId = resultSplit.id,
                    selectedUserAddress = split.selectedUserAddress
                )
                val hasSelectedAddressModification = modifications.contains(resultModification)
                split.selectedUserAddress == resultSplit.selectedUserAddress && hasSelectedAddressModification
            }
    }

    @Test
    fun `Should set selectedOutlet if there's no notDeliveryOptions in split`() {
        whenever(deliveryOptionActualizer.actualize(any(), any(), any())).thenReturn(
            mapOf(
                DeliveryType.DELIVERY to deliveryOptionModelTestInstance(),
                DeliveryType.PICKUP to deliveryOptionModelTestInstance()
            )
        )
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultModification = SplitFieldModification.SplitSelectedPostOutletPoint(
                    splitId = resultSplit.id,
                    selectedPostOutletPoint = split.selectedPostOutletPoint
                )
                val hasSelectedAddressModification = modifications.contains(resultModification)
                split.selectedPostOutletPoint == resultSplit.selectedPostOutletPoint && hasSelectedAddressModification
            }
    }

    @Test
    fun `Should reset selectedAddress if for notDeliveryOptions in split`() {
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultModification = SplitFieldModification.SplitSelectedUserAddress(
                    splitId = resultSplit.id,
                    selectedUserAddress = null
                )
                val hasSelectedAddressModification = modifications.contains(resultModification)
                resultSplit.selectedUserAddress == null && hasSelectedAddressModification
            }
    }


    @Test
    fun `Should reset selectedOutlet if for notDeliveryOptions in split`() {
        prepareMocks(serverBucket)
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNoErrors()
            .assertValue { (splits, modifications) ->
                val resultSplit = splits.first()
                val resultModification = SplitFieldModification.SplitSelectedPostOutletPoint(
                    splitId = resultSplit.id,
                    selectedPostOutletPoint = null
                )
                val hasSelectedAddressModification = modifications.contains(resultModification)
                resultSplit.selectedPostOutletPoint == null && hasSelectedAddressModification
            }
    }

    @Test
    fun `Should return error if server buckets differs from local buckets`() {
        prepareMocks(serverBucket.copy(packPositionTestInstance(id = DUMMY_ID)))
        splitsProvider.getSplitsWithModifications(shopsList, errorsPack, splitsList, deliveryOptionsList)
            .test()
            .assertNotComplete()
            .assertError(IllegalArgumentException::class.java)
    }

    private fun assertPaymentModificationCorrect(
        splits: List<CheckoutSplit>,
        modifications: List<FieldModification<*>>,
        selectedPaymentMethod: PaymentMethod
    ): Boolean {
        val resultSplit = splits.first()
        val resultBucket = resultSplit.buckets.first()
        val paymentModification = BucketFieldModification.BucketSelectedPaymentMethod(
            splitId = resultSplit.id,
            packId = resultBucket.packId,
            selectedPaymentMethod = selectedPaymentMethod
        )
        val paymentMethodChanged = resultBucket.selectedPaymentMethod == selectedPaymentMethod
        val hasPaymentMethodModification = modifications.contains(paymentModification)
        return paymentMethodChanged && hasPaymentMethodModification
    }

    private fun prepareMocks(serverBucket: BucketInfo2) {
        whenever(
            bucketInfo2Mapper.map(
                shop = shop,
                globalErrorsPack = errorsPack,
                index = 0,
                countAllBuckets = 1,
                isGooglePayAvailable = true,
                deliveryOptions = listOf(deliveryOptionsZero, deliveryOptionsOne),
                orderItemsCartCachedData = listOf(orderItemsCartCachedData),
            )
        ).thenReturn(Safe.value(serverBucket))

        whenever(deliveryOptionActualizer.mapDeliveryOptionModel(any(), any(), any(), any(), any())).thenReturn(
            DeliveryType.DELIVERY to deliveryOptionModel
        )
    }

    private fun <T> T.asList() = listOf(this)

    private companion object {
        const val DUMMY_PACK_ID_0 = "DUMMY_PACK_ID_0"
        const val DUMMY_PACK_ID_1 = "DUMMY_PACK_ID_1"
        const val DUMMY_SHOP_LABEL_0 = DUMMY_PACK_ID_0
        const val DUMMY_ID = "DUMMY_ID"
        const val DUMMY_SPLIT_ID_0 = "DUMMY_SPLIT_ID_0"
    }
}