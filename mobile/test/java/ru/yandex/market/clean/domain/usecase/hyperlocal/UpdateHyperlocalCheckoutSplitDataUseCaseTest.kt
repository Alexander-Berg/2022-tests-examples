package ru.yandex.market.clean.domain.usecase.hyperlocal

import com.annimon.stream.Optional
import io.reactivex.Completable
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import ru.yandex.market.clean.domain.model.checkout.checkoutSplitTestInstance
import ru.yandex.market.clean.domain.usecase.checkout.checkout2.CheckoutDeliveryOptionUseCase
import ru.yandex.market.clean.domain.usecase.checkout.checkout2.CheckoutSplitsUseCase
import ru.yandex.market.domain.useraddress.model.userAddressTestInstance
import ru.yandex.market.mockResult

class UpdateHyperlocalCheckoutSplitDataUseCaseTest {

    private val checkoutSplitUseCase = mock<CheckoutSplitsUseCase>()
    private val findHyperlocalUserAddressUseCase = mock<FindHyperlocalUserAddressUseCase>()
    private val deliveryOptionsUseCase = mock<CheckoutDeliveryOptionUseCase>()

    private val useCase = UpdateHyperlocalCheckoutSplitDataUseCase(

        checkoutSplitUseCase = checkoutSplitUseCase,
        findHyperlocalUserAddressUseCase = findHyperlocalUserAddressUseCase,
        deliveryOptionsUseCase = deliveryOptionsUseCase
    )

    @Test
    fun `check on userlocal address null`() {

        checkoutSplitUseCase.getCheckoutSplits().mockResult(Single.just(listOf(checkoutSplitTestInstance())))
        checkoutSplitUseCase.modifyCheckoutSplits(any()).mockResult(Completable.complete())
        findHyperlocalUserAddressUseCase.execute().mockResult(Single.just(Optional.empty()))
        deliveryOptionsUseCase.getPacksDeliveryOptions().mockResult(Single.just(mapOf()))

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(checkoutSplitUseCase).getCheckoutSplits()
        verify(checkoutSplitUseCase, never()).modifyCheckoutSplits(any())
    }

    @Test
    fun `check on correct update`() {

        checkoutSplitUseCase.getCheckoutSplits().mockResult(Single.just(listOf(checkoutSplitTestInstance())))
        checkoutSplitUseCase.modifyCheckoutSplits(any()).mockResult(Completable.complete())
        findHyperlocalUserAddressUseCase.execute().mockResult(Single.just(Optional.of(userAddressTestInstance())))
        deliveryOptionsUseCase.getPacksDeliveryOptions().mockResult(Single.just(mapOf()))

        useCase.execute()
            .test()
            .assertNoErrors()
            .assertComplete()

        verify(checkoutSplitUseCase).getCheckoutSplits()
        verify(checkoutSplitUseCase).modifyCheckoutSplits(any())
        verify(findHyperlocalUserAddressUseCase).execute()
        verify(deliveryOptionsUseCase).getPacksDeliveryOptions()
    }
}