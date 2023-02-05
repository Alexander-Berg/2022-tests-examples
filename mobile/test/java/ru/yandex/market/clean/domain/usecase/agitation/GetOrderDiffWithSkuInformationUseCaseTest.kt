package ru.yandex.market.clean.domain.usecase.agitation

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderDiffWithSkuInformationUseCaseTestEntity.DETAILED_SKU_MOCK
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderDiffWithSkuInformationUseCaseTestEntity.ORDER_DIFF_MOCK
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderDiffWithSkuInformationUseCaseTestEntity.ORDER_ID
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderDiffWithSkuInformationUseCaseTestEntity.ORDER_ITEM_DIFF_MOCK
import ru.yandex.market.clean.domain.usecase.agitation.GetOrderDiffWithSkuInformationUseCaseTestEntity.SUPPLIER_MOCK
import ru.yandex.market.clean.domain.usecase.order.GetSupplierUseCase
import ru.yandex.market.clean.domain.usecase.sku.GetSkusUseCase
import ru.yandex.market.mockResult
import ru.yandex.market.net.sku.SkuType

class GetOrderDiffWithSkuInformationUseCaseTest {

    private val getOrdersDiffUseCase = mock<GetOrdersDiffUseCase>()
    private val getSkusUseCase = mock<GetSkusUseCase>()
    private val getSupplierUseCase = mock<GetSupplierUseCase>()

    private val useCase = GetOrderDiffWithSkuInformationUseCase(

        getOrdersDiffUseCase = getOrdersDiffUseCase,
        getSkusUseCase = getSkusUseCase,
        getSupplierUseCase = getSupplierUseCase,
    )

    @Test
    fun `check on empty`() {

        getOrdersDiffUseCase.execute(ORDER_ID, false).mockResult(Single.just(listOf()))
        getSkusUseCase.execute(listOf()).mockResult(Single.just(DETAILED_SKU_MOCK))
        getSupplierUseCase.execute(listOf(), false).mockResult(Single.just(listOf(SUPPLIER_MOCK)))

        useCase.execute(ORDER_ID, false)
            .test()
            .assertNoErrors()
            .assertResult(Optional.empty())

        verify(getOrdersDiffUseCase).execute(ORDER_ID, false)
        verifyZeroInteractions(getSkusUseCase)
        verifyZeroInteractions(getSupplierUseCase)
    }

    @Test
    fun `check on empty with mismatch id`() {

        val anotherId = "another_id"

        getOrdersDiffUseCase.execute(anotherId, false).mockResult(Single.just(listOf(ORDER_DIFF_MOCK)))
        getSkusUseCase.execute(listOf()).mockResult(Single.just(DETAILED_SKU_MOCK))
        getSupplierUseCase.execute(listOf(), false).mockResult(Single.just(listOf(SUPPLIER_MOCK)))

        useCase.execute(anotherId, false)
            .test()
            .assertNoErrors()
            .assertResult(Optional.empty())

        verify(getOrdersDiffUseCase).execute(anotherId, false)
        verifyZeroInteractions(getSkusUseCase)
        verifyZeroInteractions(getSupplierUseCase)
    }

    @Test
    fun `check on full order result`() {

        val item = ORDER_ITEM_DIFF_MOCK.copy(skuId = DETAILED_SKU_MOCK.sku.id, supplierId = SUPPLIER_MOCK.id)
        val orderDiff = ORDER_DIFF_MOCK.copy(orderId = ORDER_ID, items = listOf(item))

        val resultItem = item.copy(

            offerId = DETAILED_SKU_MOCK.wareId,
            skuType = DETAILED_SKU_MOCK.sku.type,
            supplierName = SUPPLIER_MOCK.name
        )
        val result = orderDiff.copy(items = listOf(resultItem))

        val skuIds = listOf(item.skuId ?: "")
        val supplierIds = listOf(item.supplierId!!)

        getOrdersDiffUseCase.execute(ORDER_ID, false).mockResult(Single.just(listOf(orderDiff)))
        getSkusUseCase.execute(skuIds).mockResult(Single.just(listOf(DETAILED_SKU_MOCK)))
        getSupplierUseCase.execute(supplierIds, false).mockResult(Single.just(listOf(SUPPLIER_MOCK)))

        useCase.execute(ORDER_ID, false)
            .test()
            .assertNoErrors()
            .assertResult(Optional.of(result))

        verify(getOrdersDiffUseCase).execute(ORDER_ID, false)
        verify(getSkusUseCase).execute(skuIds)
        verify(getSupplierUseCase).execute(supplierIds, false)
    }

    @Test
    fun `check on full order result with mismatch skus and suppliers`() {

        val item = ORDER_ITEM_DIFF_MOCK.copy(skuId = "", supplierId = null)
        val orderDiff = ORDER_DIFF_MOCK.copy(orderId = ORDER_ID, items = listOf(item))

        val resultItem = item.copy(offerId = null, skuType = SkuType.UNKNOWN, supplierName = "")
        val result = orderDiff.copy(items = listOf(resultItem))

        getOrdersDiffUseCase.execute(ORDER_ID, false).mockResult(Single.just(listOf(orderDiff)))
        getSkusUseCase.execute(emptyList()).mockResult(Single.just(emptyList()))
        getSupplierUseCase.execute(emptyList(), false).mockResult(Single.just(emptyList()))

        useCase.execute(ORDER_ID, false)
            .test()
            .assertNoErrors()
            .assertResult(Optional.of(result))

        verify(getOrdersDiffUseCase).execute(ORDER_ID, false)
        verifyZeroInteractions(getSkusUseCase)
        verifyZeroInteractions(getSupplierUseCase)
    }
}