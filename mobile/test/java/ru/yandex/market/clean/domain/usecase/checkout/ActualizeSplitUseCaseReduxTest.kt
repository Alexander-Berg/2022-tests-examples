package ru.yandex.market.clean.domain.usecase.checkout

import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.domain.usecase.CreateSummaryOptionsUseCaseRedux
import ru.yandex.market.clean.domain.model.checkout.CheckoutSplit
import ru.yandex.market.clean.domain.model.checkout.checkoutCommonUserInputTestInstance
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.model.order.orderOptionsActualizationModelTestInstance
import ru.yandex.market.clean.domain.model.order.shopOrderOptionsModelTestInstance
import ru.yandex.market.clean.domain.usecase.checkout.checkout2.GetOrderOptionsUseCase
import ru.yandex.market.data.order.error.ShopError
import ru.yandex.market.data.order.options.OrderOptions

class ActualizeSplitUseCaseReduxTest {

    private val shopErrors = listOf(ShopError())
    private val shopOrderOptionsModelWithErrors = shopOrderOptionsModelTestInstance(errors = shopErrors)
    private val orderOptionsActualizationModel = orderOptionsActualizationModelTestInstance(
        shopOrderOptionsList = listOf(shopOrderOptionsModelWithErrors)
    )
    private val split = checkoutSplitTestInstance(id = DUMMY_SPLIT_ID)
    private val splits = listOf(split)
    private val buckets = splits.flatMap(CheckoutSplit::buckets)
    private val checkoutCommonUserInput = checkoutCommonUserInputTestInstance()
    private val skipSendDeliveryOptionId = false
    private val coinIds: Map<String, Set<String>> = emptyMap()
    private val orderOptions = mock<OrderOptions>() //cant generate test instance cause of userPresets: List<Any>
    private val orderOptionsList = listOf(orderOptions)
    private val selectedAddresses = splits.mapNotNull(CheckoutSplit::selectedUserAddress)

    private val getOrderOptionsUseCase = mock<GetOrderOptionsUseCase> {
        on {
            execute(
                orderOptions = orderOptionsList,
                selectedAddresses = selectedAddresses,
                isOutletNeed = true
            )
        } doReturn Single.just(orderOptionsActualizationModel)
    }
    private val createSummaryOptionsUseCase = mock<CreateSummaryOptionsUseCaseRedux>()

    private val useCase = ActualizeSplitUseCaseRedux(
        getOrderOptionsUseCase,
        createSummaryOptionsUseCase,
    )

    @Test
    fun `Should return error if there's shops error and shops errors supported`() {
        mockCreateOrderOptions()

        useCase.execute(
            splits = splits,
            buckets = buckets,
            checkoutCommonUserInput = checkoutCommonUserInput,
            skipSendDeliveryOptionId = skipSendDeliveryOptionId,
            supportShopsError = true,
            coinIds = coinIds,
        )
            .test()
            .assertError(NoActualDeliveryOptionException::class.java)
    }

    @Test
    fun `Should ignore shop error if there's shops error and shops errors are not supported`() {
        mockCreateOrderOptions()

        useCase.execute(
            splits = splits,
            buckets = buckets,
            checkoutCommonUserInput = checkoutCommonUserInput,
            skipSendDeliveryOptionId = skipSendDeliveryOptionId,
            supportShopsError = false,
            coinIds = coinIds,
        )
            .test()
            .assertComplete()
    }

    @Test
    fun `Should return OrderOptionsActualizationModel`() {
        mockCreateOrderOptions()

        useCase.execute(
            splits = splits,
            buckets = buckets,
            checkoutCommonUserInput = checkoutCommonUserInput,
            skipSendDeliveryOptionId = skipSendDeliveryOptionId,
            supportShopsError = false,
            coinIds = coinIds,
        )
            .test()
            .assertComplete()
            .assertValue(orderOptionsActualizationModel)
    }

    private fun mockCreateOrderOptions() {
        whenever(
            createSummaryOptionsUseCase.createOrderOptions(
                splits = splits,
                buckets = buckets,
                checkoutCommonUserInput = checkoutCommonUserInput,
                coinIds = coinIds,
                skipSendDeliveryOptionId = skipSendDeliveryOptionId
            )
        ).thenReturn(Single.just(orderOptionsList))
    }

    private companion object {
        const val DUMMY_SPLIT_ID = "DUMMY_SPLIT_ID"
    }


}