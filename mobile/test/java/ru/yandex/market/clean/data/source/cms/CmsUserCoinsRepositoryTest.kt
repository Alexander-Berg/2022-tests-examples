package ru.yandex.market.clean.data.source.cms

import io.reactivex.Observable
import org.junit.Test
import org.junit.experimental.runners.Enclosed
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.beru.android.R
import ru.yandex.market.clean.data.repository.smartshopping.SmartShoppingRepository
import ru.yandex.market.clean.domain.model.CmsSmartCoinBanner
import ru.yandex.market.clean.domain.model.CmsSmartCoinBannerTarget
import ru.yandex.market.clean.domain.model.SmartCoin
import ru.yandex.market.clean.domain.model.SmartCoinsCollection
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.internal.sync.Synchronized
import ru.yandex.market.internal.sync.Synchronizing
import java.util.Date

@RunWith(Enclosed::class)
class CmsUserCoinsRepositoryTest {

    class SimpleTests {
        private val smartShoppingRepository = mock<SmartShoppingRepository>()
        private val resourcesDataStore = mock<ResourcesManager> {
            on { getString(R.string.smart_coin_want_bonus) } doAnswer {
                R.string.smart_coin_want_bonus.toString()
            }
            on { getString(R.string.smart_coin_order_to_get) } doAnswer {
                R.string.smart_coin_order_to_get.toString()
            }
            on { getString(R.string.close_sign_up_onboarding) } doAnswer {
                R.string.close_sign_up_onboarding.toString()
            }
        }

        private val repository = CmsUserCoinsRepository(
            smartShoppingRepository,
            resourcesDataStore,
        )

        @Test
        fun `take only synchronized values`() {
            whenever(smartShoppingRepository.getCoinsCollectionStream()).thenReturn(Observable.just(Synchronizing()))

            repository.getUserCoinsStream()
                .test()
                .assertComplete()
                .assertNoValues()
                .assertNoErrors()
        }

        @Test
        fun `coins list`() {
            val smartCoin = SmartCoin.testInstance()
            val coins: SmartCoinsCollection = SmartCoinsCollection.testBuilder()
                .userCoins(listOf(smartCoin))
                .build()
            whenever(smartShoppingRepository.getCoinsCollectionStream()).thenReturn(Observable.just(Synchronized(coins)))

            repository.getUserCoinsStream()
                .test()
                .assertValue(coins.userCoins())
                .assertNoErrors()
        }

        @Test
        fun `when no user coins result return common banner`() {
            val coins: SmartCoinsCollection = SmartCoinsCollection.testBuilder()
                .userCoins(emptyList())
                .build()
            whenever(smartShoppingRepository.getCoinsCollectionStream()).thenReturn(Observable.just(Synchronized(coins)))

            repository.getUserCoinsStream()
                .test()
                .assertValue(
                    listOf(
                        CmsSmartCoinBanner(
                            id = "BUY-MORE",
                            title = R.string.smart_coin_want_bonus.toString(),
                            subtitle = R.string.smart_coin_order_to_get.toString(),
                            buttonText = R.string.close_sign_up_onboarding.toString(),
                            bannerTarget = CmsSmartCoinBannerTarget.CatalogScreen,
                        )
                    )
                )
                .assertNoErrors()
        }
    }

    @RunWith(Parameterized::class)
    class SortTest(
        private val input: List<DateAndState>,
        private val expectedResult: List<DateAndState>
    ) {

        private val smartShoppingRepository = mock<SmartShoppingRepository>()

        private val resourcesDataStore = mock<ResourcesManager> {
            on { getString(any()) } doAnswer { "some String" }
        }
        private val repository = CmsUserCoinsRepository(
            smartShoppingRepository,
            resourcesDataStore,
        )

        @Test
        fun `coins sort by creationDate and state`() {
            val coins = input.map { SmartCoin.testBuilder().creationDate(it.date).state(it.state).build() }
            whenever(smartShoppingRepository.getCoinsCollectionStream()).thenReturn(
                Observable.just(
                    Synchronized(
                        SmartCoinsCollection.testBuilder()
                            .userCoins(coins)
                            .build()
                    )
                )
            )

            val resultCoins = expectedResult.map {
                SmartCoin.testBuilder().creationDate(it.date).state(it.state).build()
            }
            repository.getUserCoinsStream()
                .test()
                .assertValue(resultCoins)
        }

        companion object {

            @Parameterized.Parameters(name = "{index}: {0} == {1}")
            @JvmStatic
            fun parameters(): Iterable<Array<*>> {
                val date1 = Date(1000)
                val date2 = Date(2000)
                val date3 = Date(3000)
                val date4 = Date(4000)
                return listOf(
                    arrayOf(
                        listOf(
                            DateAndState(date1, SmartCoin.State.ACTIVE),
                            DateAndState(date2, SmartCoin.State.ACTIVE)
                        ),
                        listOf(
                            DateAndState(date2, SmartCoin.State.ACTIVE),
                            DateAndState(date1, SmartCoin.State.ACTIVE)
                        )
                    ),
                    arrayOf(
                        listOf(
                            DateAndState(date1, SmartCoin.State.INACTIVE),
                            DateAndState(date2, SmartCoin.State.NONE)
                        ),
                        listOf(
                            DateAndState(date2, SmartCoin.State.NONE),
                            DateAndState(date1, SmartCoin.State.INACTIVE)
                        )
                    ),
                    arrayOf(
                        listOf(
                            DateAndState(date1, SmartCoin.State.ACTIVE),
                            DateAndState(date2, SmartCoin.State.INACTIVE)
                        ),
                        listOf(
                            DateAndState(date1, SmartCoin.State.ACTIVE),
                            DateAndState(date2, SmartCoin.State.INACTIVE)
                        )
                    ),
                    arrayOf(
                        listOf(
                            DateAndState(date4, SmartCoin.State.INACTIVE),
                            DateAndState(date1, SmartCoin.State.NONE),
                            DateAndState(date3, SmartCoin.State.UNKNOWN),
                            DateAndState(date2, SmartCoin.State.ACTIVE)
                        ),
                        listOf(
                            DateAndState(date2, SmartCoin.State.ACTIVE),
                            DateAndState(date4, SmartCoin.State.INACTIVE),
                            DateAndState(date3, SmartCoin.State.UNKNOWN),
                            DateAndState(date1, SmartCoin.State.NONE)
                        )
                    )
                )
            }
        }

        class DateAndState(
            val date: Date,
            val state: SmartCoin.State
        )
    }

}
