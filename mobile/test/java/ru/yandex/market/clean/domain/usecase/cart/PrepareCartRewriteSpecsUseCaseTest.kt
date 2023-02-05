package ru.yandex.market.clean.domain.usecase.cart

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.checkout.domain.model.ErrorsPack
import ru.yandex.market.checkout.domain.model.ItemErrorsPack
import ru.yandex.market.checkout.domain.model.ShopErrorsPack
import ru.yandex.market.checkout.domain.model.error.CheckoutError
import ru.yandex.market.checkout.domain.model.error.ErrorType
import ru.yandex.market.checkout.domain.model.error.bundleJoinErrorTestInstance
import ru.yandex.market.checkout.domain.model.error.bundleRemovedErrorTestInstance
import ru.yandex.market.checkout.domain.model.error.bundleSplitErrorTestInstance
import ru.yandex.market.clean.data.mapper.cart.RewriteCartSpecItemMapper
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.orderItemMappingTestInstance
import ru.yandex.market.clean.domain.model.orderItemTestInstance
import ru.yandex.market.clean.domain.model.rewriteCartSpec_ItemTestInstance
import ru.yandex.market.rub
import ru.yandex.market.test.extensions.assertValue
import ru.yandex.market.utils.enumSetOf

class PrepareCartRewriteSpecsUseCaseTest {

    private val configuration = PrepareCartRewriteSpecsUseCase.Configuration(
        structuralBundleModifications = enumSetOf(ErrorType.BUNDLE_SPLIT)
    )
    private val rewriteCartSpecItemMapper = mock<RewriteCartSpecItemMapper>()
    private val useCase = PrepareCartRewriteSpecsUseCase(configuration, rewriteCartSpecItemMapper)

    @Test
    fun `Use carter count for items without structural bundle modifications`() {
        val carterCount = 2
        val actualizedCount = 1
        val matchingKey = "key"
        whenever(rewriteCartSpecItemMapper.map(any(), any(), any(), any())) doReturn rewriteCartSpec_ItemTestInstance(
            count = carterCount
        )
        useCase.prepareRewriteSpecs(
            mappings = listOf(
                orderItemMappingTestInstance(
                    sourceCartItem = cartItemTestInstance(),
                    actualizedItem = orderItemTestInstance(count = actualizedCount),
                    correspondingCartItem = cartItemTestInstance(userBuyCount = carterCount)
                )
            ),
            missingCartItems = emptyList(),
            errors = createErrorsPackWithItemErrors(matchingKey, bundleJoinErrorTestInstance())
        )
            .test()
            .assertNoErrors()
            .assertValue("Количество первого айтема равно количеству из картера ($carterCount)") {
                items.first().count == carterCount
            }
    }

    @Test
    fun `Use actualized count for items with structural bundle modification`() {
        val carterCount = 2
        val actualizedCount = 1
        val matchingKey = "key"
        whenever(rewriteCartSpecItemMapper.map(any(), any(), any(), any())) doReturn rewriteCartSpec_ItemTestInstance(
            count = actualizedCount
        )
        useCase.prepareRewriteSpecs(
            mappings = listOf(
                orderItemMappingTestInstance(
                    sourceCartItem = cartItemTestInstance(matchingKey = matchingKey),
                    actualizedItem = orderItemTestInstance(matchingKey = matchingKey, count = actualizedCount),
                    correspondingCartItem = cartItemTestInstance(matchingKey = matchingKey, userBuyCount = carterCount)
                )
            ),
            missingCartItems = emptyList(),
            errors = createErrorsPackWithItemErrors(matchingKey, bundleSplitErrorTestInstance())
        )
            .test()
            .assertNoErrors()
            .assertValue { it.items.first().count == actualizedCount }
            .assertValue("Количество первого айтема равно актуализированному ($actualizedCount)") {
                items.first().count == actualizedCount
            }
    }

    @Test
    fun `Coalesce count to at least 1 even when items has structural bundle modification`() {
        val carterCount = 2
        val actualizedCount = 0
        val matchingKey = "key"
        whenever(rewriteCartSpecItemMapper.map(any(), any(), any(), any())) doReturn rewriteCartSpec_ItemTestInstance(
            count = 1
        )
        useCase.prepareRewriteSpecs(
            mappings = listOf(
                orderItemMappingTestInstance(
                    sourceCartItem = cartItemTestInstance(matchingKey = matchingKey),
                    actualizedItem = orderItemTestInstance(matchingKey = matchingKey, count = actualizedCount),
                    correspondingCartItem = cartItemTestInstance(matchingKey = matchingKey, userBuyCount = carterCount)
                )
            ),
            missingCartItems = emptyList(),
            errors = createErrorsPackWithItemErrors(matchingKey, bundleSplitErrorTestInstance())
        )
            .test()
            .assertNoErrors()
            .assertComplete()
            .assertValue("Количество первого айтема роавно 1") { items.first().count == 1 }
    }

    @Test
    fun `Use carter price when item don't have bundle modifications`() {
        val matchingKey = "key"
        val checkouterPrice = 200.rub
        val carterPrice = 100.rub
        whenever(rewriteCartSpecItemMapper.map(any(), any(), any(), any())) doReturn rewriteCartSpec_ItemTestInstance(
            price = carterPrice
        )
        useCase.prepareRewriteSpecs(
            mappings = listOf(
                orderItemMappingTestInstance(
                    sourceCartItem = cartItemTestInstance(matchingKey = matchingKey),
                    actualizedItem = orderItemTestInstance(matchingKey = matchingKey, price = checkouterPrice),
                    correspondingCartItem = cartItemTestInstance(matchingKey = matchingKey, carterPrice = carterPrice)
                )
            ),
            missingCartItems = emptyList(),
            errors = ErrorsPack.empty()
        )
            .test()
            .assertNoErrors()
            .assertValue("Цена первого айтема равна цене из картера ($carterPrice)") {
                items.first().price == carterPrice
            }
    }

    @Test
    fun `Use checkouter price when item have bundle modifications`() {
        val matchingKey = "key"
        val checkouterPrice = 200.rub
        val carterPrice = 100.rub
        whenever(rewriteCartSpecItemMapper.map(any(), any(), any(), any())) doReturn rewriteCartSpec_ItemTestInstance(
            price = checkouterPrice
        )
        useCase.prepareRewriteSpecs(
            mappings = listOf(
                orderItemMappingTestInstance(
                    sourceCartItem = cartItemTestInstance(matchingKey = matchingKey),
                    actualizedItem = orderItemTestInstance(matchingKey = matchingKey, price = checkouterPrice),
                    correspondingCartItem = cartItemTestInstance(matchingKey = matchingKey, carterPrice = carterPrice)
                )
            ),
            missingCartItems = emptyList(),
            errors = createErrorsPackWithItemErrors(matchingKey, bundleRemovedErrorTestInstance())
        )
            .test()
            .assertNoErrors()
            .assertValue { it.items.first().price == checkouterPrice }
            .assertValue("Цена первого айтема равна цене из чекаутера ($checkouterPrice)") {
                items.first().price == checkouterPrice
            }
    }

    private fun createErrorsPackWithItemErrors(
        matchingKey: String,
        vararg itemErrors: CheckoutError
    ): ErrorsPack {

        return ErrorsPack.testBuilder()
            .shopErrorsPacks(
                listOf(
                    ShopErrorsPack.testBuilder()
                        .itemErrorsPacks(
                            listOf(
                                ItemErrorsPack.testBuilder()
                                    .matchingKey(matchingKey)
                                    .errors(itemErrors.toList())
                                    .build()
                            )
                        )
                        .build()
                )
            )
            .build()
    }
}