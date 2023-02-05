package ru.yandex.market.clean.data.mapper

import android.os.Build
import com.annimon.stream.Exceptional
import dagger.MembersInjector
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.HamcrestCondition
import org.hamcrest.Matcher
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.instanceOf
import org.junit.Before
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import org.robolectric.ParameterizedRobolectricTestRunner
import org.robolectric.annotation.Config
import ru.yandex.market.TestApplication
import ru.yandex.market.TestComponent
import ru.yandex.market.clean.data.mapper.CartItemSnapshotMapperTest.CartItemSnapshotMapperParameterizedTest.Companion.DISCLAIMER
import ru.yandex.market.clean.domain.model.CartItem
import ru.yandex.market.clean.domain.model.CartItemSnapshot
import ru.yandex.market.clean.domain.model.Disclaimer
import ru.yandex.market.clean.domain.model.DisclaimerType
import ru.yandex.market.clean.domain.model.cartItemSnapshotTestInstance
import ru.yandex.market.clean.domain.model.cartItemTestInstance
import ru.yandex.market.clean.domain.model.offerPricesTestInstance
import ru.yandex.market.clean.domain.model.offerTestInstance
import ru.yandex.market.clean.domain.model.productInformationTestInstance
import ru.yandex.market.clean.domain.model.productOfferTestInstance
import ru.yandex.market.di.TestScope
import ru.yandex.market.domain.money.model.Money
import ru.yandex.market.test.matchers.ExceptionalMatchers.containsErrorThat
import ru.yandex.market.test.matchers.ExceptionalMatchers.hasValueThat
import ru.yandex.market.utils.UnixTime
import ru.yandex.market.utils.asExceptional
import ru.yandex.market.utils.millis
import javax.inject.Inject

@RunWith(Enclosed::class)
class CartItemSnapshotMapperTest {

    class AnotherTests {
        private val mapper = spy<CartItemSnapshotMapper>()

        @Test
        fun `mapList ignores exceptions`() {
            val item1 = cartItemTestInstance(skuId = "1")
            val item2 = cartItemTestInstance(skuId = "2")
            val snapshot1 = cartItemSnapshotTestInstance(
                stockKeepingUnitId = "1",
                selectedServiceId = SELECTED_SERVICE_ID
            )
            whenever(mapper.createSnapshot(item1)).thenReturn(snapshot1.asExceptional())
            whenever(mapper.createSnapshot(item2)).thenReturn(Exceptional.of(RuntimeException()))

            val result = mapper.mapList(
                listOf(item1, item2)
            )
            assertThat(result).contains(snapshot1)
        }

        @Test
        fun `mapList sorts list by skuId`() {
            val result = mapper.mapList(
                listOf(
                    createCartItemWithDisclaimer(skuId = "3", disclaimerText = DISCLAIMER),
                    createCartItemWithDisclaimer(skuId = "4", disclaimerText = DISCLAIMER),
                    createCartItemWithDisclaimer(skuId = "1", disclaimerText = "")
                )
            )
            assertThat(result).contains(
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "1",
                    disclaimerText = "",
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "1"
                ),
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "3",
                    disclaimerText = DISCLAIMER,
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "3",
                ),
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "4",
                    disclaimerText = DISCLAIMER,
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "4",
                )
            )
        }

        @Test
        fun `mapList distinct values by skuId`() {
            val result = mapper.mapList(
                listOf(
                    createCartItemWithDisclaimer(skuId = "1", disclaimerText = ""),
                    createCartItemWithDisclaimer(skuId = "4", disclaimerText = DISCLAIMER),
                    createCartItemWithDisclaimer(skuId = "1", disclaimerText = "")
                )
            )
            assertThat(result).contains(
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "1",
                    disclaimerText = "",
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "1",
                ),
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "4",
                    disclaimerText = DISCLAIMER,
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "4",
                )
            )
        }

        @Test
        fun `mapList filters gift offers`() {
            val result = mapper.mapList(
                listOf(
                    createCartItemWithDisclaimer(
                        skuId = "1",
                        bundleId = "1",
                        isPrimaryBundleItem = false,
                        disclaimerText = ""
                    ),
                    createCartItemWithDisclaimer(
                        skuId = "2",
                        bundleId = "2",
                        isPrimaryBundleItem = true,
                        disclaimerText = DISCLAIMER
                    ),
                    createCartItemWithDisclaimer(
                        skuId = "3",
                        bundleId = "",
                        isPrimaryBundleItem = false,
                        disclaimerText = ""
                    )
                )
            )
            assertThat(result).contains(
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "2",
                    disclaimerText = DISCLAIMER,
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "2",
                ),
                cartItemSnapshotTestInstance(
                    stockKeepingUnitId = "3",
                    disclaimerText = "",
                    selectedServiceId = SELECTED_SERVICE_ID,
                    skuId = "3",
                )
            )
        }

        private fun createCartItemWithDisclaimer(
            skuId: String,
            disclaimerText: String,
            bundleId: String = "",
            isPrimaryBundleItem: Boolean = false
        ): CartItem {
            return cartItemTestInstance(
                skuId = skuId,
                bundleId = bundleId,
                isPrimaryBundleItem = isPrimaryBundleItem,
                offer = productOfferTestInstance(
                    information = productInformationTestInstance(
                        disclaimers = listOf(
                            Disclaimer(
                                DisclaimerType.ALCO,
                                "alco",
                                disclaimerText
                            )
                        )
                    )
                )
            )
        }

        companion object {
            const val SELECTED_SERVICE_ID = "serviceId"
        }
    }

    @RunWith(ParameterizedRobolectricTestRunner::class)
    @Config(sdk = [Build.VERSION_CODES.P])
    class CartItemSnapshotMapperParameterizedTest(
        private val cartItem: CartItem,
        private val resultMatcher: Matcher<Exceptional<CartItemSnapshot>>
    ) {

        @Inject
        lateinit var mapper: CartItemSnapshotMapper

        @Before
        fun setUp() {
            DaggerCartItemSnapshotMapperTest_CartItemSnapshotMapperParameterizedTest_Component.builder()
                .testComponent(TestApplication.instance.component)
                .build()
                .injectMembers(this)
        }

        @Test
        fun `Check input mapped to expected result`() {
            val itemSnapshot = mapper.createSnapshot(cartItem)
            assertThat(itemSnapshot).`is`(HamcrestCondition(resultMatcher))
        }

        @TestScope
        @dagger.Component(dependencies = [TestComponent::class])
        interface Component : MembersInjector<CartItemSnapshotMapperParameterizedTest>

        companion object {
            private const val BASE_PRICE_RUB = 100
            private const val PURCHASE_PRICE_RUB = 90
            private const val DROP_PRICE_RUB = 10
            private const val IS_PD_APPLIED = false
            private const val IS_PREORDER = false
            const val DISCLAIMER = "Some random text"
            const val SELECTED_SERVICE_ID = "serviceId"

            private val ITEM_SNAPSHOT = CartItemSnapshot(
                persistentOfferId = "123",
                stockKeepingUnitId = "abc",
                count = 1,
                categoryId = 42L,
                modelId = 42L,
                isPriceDropPromoEnabled = false,
                creationTime = UnixTime(10.millis),
                name = "name",
                purchasePrice = Money.createRub(PURCHASE_PRICE_RUB),
                basePrice = Money.createRub(BASE_PRICE_RUB),
                dropPrice = Money.createRub(DROP_PRICE_RUB),
                isPriceDropPromoApplied = IS_PD_APPLIED,
                disclaimerText = DISCLAIMER,
                isPreorder = IS_PREORDER,
                selectedServiceId = SELECTED_SERVICE_ID,
                skuId = "abc",
            )

            @ParameterizedRobolectricTestRunner.Parameters
            @JvmStatic
            fun parameters(): Iterable<Array<*>> = listOf(

                // 0
                arrayOf(
                    cartItemTestInstance(
                        userBuyCount = 1,
                        skuId = "abc",
                        offer = null,
                        persistentOfferId = "123",
                        categoryId = 42L,
                        isPriceDropPromoEnabled = IS_PD_APPLIED,
                        creationTime = UnixTime(10.millis)
                    ),
                    hasValueThat(
                        equalTo(
                            CartItemSnapshot(
                                persistentOfferId = "123",
                                stockKeepingUnitId = "abc",
                                count = 1,
                                categoryId = 42L,
                                modelId = 42L,
                                isPriceDropPromoEnabled = false,
                                creationTime = UnixTime(10.millis),
                                name = "name",
                                purchasePrice = null,
                                basePrice = null,
                                dropPrice = null,
                                isPriceDropPromoApplied = IS_PD_APPLIED,
                                disclaimerText = "",
                                isPreorder = IS_PREORDER,
                                selectedServiceId = SELECTED_SERVICE_ID,
                                skuId = "abc",
                            )
                        )
                    )
                ),

                // 1
                arrayOf(
                    cartItemTestInstance(userBuyCount = 0),
                    expectError()
                ),

                // 2
                arrayOf(
                    cartItemTestInstance(
                        userBuyCount = 1,
                        skuId = "abc",
                        offer = productOfferTestInstance(
                            modelId = 42L,
                            offer = offerTestInstance(
                                prices = offerPricesTestInstance(
                                    purchasePrice = Money.createRub(PURCHASE_PRICE_RUB),
                                    basePrice = Money.createRub(BASE_PRICE_RUB),
                                    dropPrice = Money.createRub(DROP_PRICE_RUB)
                                ),
                                isPreorder = IS_PREORDER
                            ),
                            information = productInformationTestInstance(
                                disclaimers = listOf(Disclaimer(DisclaimerType.ALCO, "alco", DISCLAIMER))
                            )
                        ),
                        persistentOfferId = "123",
                        categoryId = 42L,
                        isPriceDropPromoEnabled = IS_PD_APPLIED,
                        creationTime = UnixTime(10.millis)
                    ),
                    hasValueThat(equalTo(ITEM_SNAPSHOT))
                )
            )

            private fun expectError(): Matcher<Exceptional<CartItemSnapshot>> {
                return containsErrorThat(instanceOf(IllegalArgumentException::class.java))
            }
        }
    }
}