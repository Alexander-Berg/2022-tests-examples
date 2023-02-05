package ru.yandex.market.clean.presentation.formatter

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.yandex.market.clean.domain.model.OrderItem
import ru.yandex.market.clean.domain.model.orderItemTestInstance

@RunWith(Parameterized::class)
class BundleGrouperTest(
    private val input: List<OrderItem>,
    private val expectedResult: List<OrderItem>
) {
    private val grouper = BundleGrouper()

    @Test
    fun `Check actual result equal to expected`() {
        val actualResult = grouper.groupOrderItemsByBundle(input)
        assertThat(actualResult).isEqualTo(expectedResult)
    }

    companion object {

        @Parameterized.Parameters
        @JvmStatic
        fun data(): Iterable<Array<*>> {
            return listOf(

                // 0
                arrayOf(
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "", isPrimaryBundleItem = false)
                    ),
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "", isPrimaryBundleItem = false)
                    )
                ),

                // 1
                arrayOf(
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = true)
                    ),
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "2", bundleId = "1", isPrimaryBundleItem = false)
                    )
                ),

                // 2
                arrayOf(
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "1", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "2", bundleId = "", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = false)
                    ),
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "1", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "", isPrimaryBundleItem = false)
                    )
                ),

                // 3
                arrayOf(
                    listOf(),
                    listOf()
                ),

                // 4
                arrayOf(
                    listOf(orderItemTestInstance()),
                    listOf(orderItemTestInstance())
                ),

                // 5
                arrayOf(
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = true)
                    ),
                    listOf(
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "1", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "1", isPrimaryBundleItem = false)
                    )
                ),

                // 6
                arrayOf(
                    listOf(
                        orderItemTestInstance(skuId = "1", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "2", bundleId = "2", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "4", bundleId = "2", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "5", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "6", bundleId = "2", isPrimaryBundleItem = true)
                    ),
                    listOf(
                        orderItemTestInstance(skuId = "3", bundleId = "1", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "1", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "5", bundleId = "1", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "6", bundleId = "2", isPrimaryBundleItem = true),
                        orderItemTestInstance(skuId = "2", bundleId = "2", isPrimaryBundleItem = false),
                        orderItemTestInstance(skuId = "4", bundleId = "2", isPrimaryBundleItem = false)
                    )
                )
            )
        }
    }
}