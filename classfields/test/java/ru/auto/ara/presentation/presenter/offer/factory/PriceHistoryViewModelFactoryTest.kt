package ru.auto.ara.presentation.presenter.offer.factory

import io.qameta.allure.kotlin.junit4.AllureParametrizedRunner
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import ru.auto.ara.viewmodel.offer.PriceChangeViewModel
import ru.auto.data.model.VehicleCategory
import ru.auto.data.model.data.offer.Offer
import ru.auto.data.model.data.offer.PriceInfo
import ru.auto.data.model.data.offer.SellerType
import java.util.*
import kotlin.test.assertEquals

@RunWith(AllureParametrizedRunner::class)
class PriceHistoryViewModelFactoryTest(
    private val offer: Offer,
    private val isUserOffer: Boolean,
    private val desc: Boolean,
    private val expectedViewModels: List<PriceChangeViewModel>
) {

    @Test
    fun `price history view models should be correct`() {
        assertEquals(
            expected = expectedViewModels,
            actual = PriceHistoryViewModelFactory.createViewModel(offer, isUserOffer, desc)
        )
    }

    companion object {
        @JvmStatic
        @Parameterized.Parameters(name = "index = {index}")
        fun data(): Collection<Array<out Any>> {

            return listOf(
                // empty price history
                arrayOf(
                    buildOffer(emptyList()),
                    false,
                    false,
                    emptyList<PriceChangeViewModel>()
                ),
                // asc prices and asc view models
                arrayOf(
                    buildOffer(
                        listOf(
                            PriceInfo(100_000, priceRUR = 100_000f, creationDate = Date(1)),
                            PriceInfo(300_000, priceRUR = 300_000f, creationDate = Date(2)),
                            PriceInfo(200_000, priceRUR = 200_000f, creationDate = Date(3))
                        )
                    ),
                    true,
                    false,
                    listOf(
                        PriceChangeViewModel(Date(1), 100_000L, null),
                        PriceChangeViewModel(Date(2), 300_000L, 200_000L),
                        PriceChangeViewModel(Date(3), 200_000L, -100_000L)
                    )
                ),
                // desc prices and asc view models
                arrayOf(
                    buildOffer(
                        listOf(
                            PriceInfo(200_000, priceRUR = 200_000f, creationDate = Date(3)),
                            PriceInfo(300_000, priceRUR = 300_000f, creationDate = Date(2)),
                            PriceInfo(100_000, priceRUR = 100_000f, creationDate = Date(1))
                        )
                    ),
                    false,
                    false,
                    listOf(
                        PriceChangeViewModel(Date(1), 100_000L, null),
                        PriceChangeViewModel(Date(2), 300_000L, 200_000L),
                        PriceChangeViewModel(Date(3), 200_000L, -100_000L)
                    )
                ),
                // asc prices and desc view models
                arrayOf(
                    buildOffer(
                        listOf(
                            PriceInfo(100_000, priceRUR = 100_000f, creationDate = Date(1)),
                            PriceInfo(300_000, priceRUR = 300_000f, creationDate = Date(2)),
                            PriceInfo(200_000, priceRUR = 200_000f, creationDate = Date(3))
                        )
                    ),
                    false,
                    true,
                    listOf(
                        PriceChangeViewModel(Date(3), 200_000L, -100_000L),
                        PriceChangeViewModel(Date(2), 300_000L, 200_000L),
                        PriceChangeViewModel(Date(1), 100_000L, null)
                    )
                ),
                // desc prices and desc view models
                arrayOf(
                    buildOffer(
                        listOf(
                            PriceInfo(200_000, priceRUR = 200_000f, creationDate = Date(3)),
                            PriceInfo(300_000, priceRUR = 300_000f, creationDate = Date(2)),
                            PriceInfo(100_000, priceRUR = 100_000f, creationDate = Date(1))
                        )
                    ),
                    false,
                    true,
                    listOf(
                        PriceChangeViewModel(Date(3), 200_000L, -100_000L),
                        PriceChangeViewModel(Date(2), 300_000L, 200_000L),
                        PriceChangeViewModel(Date(1), 100_000L, null)
                    )
                ),
            )
        }

        private fun buildOffer(priceHistory: List<PriceInfo>): Offer = Offer(
            category = VehicleCategory.CARS,
            id = "",
            sellerType = SellerType.PRIVATE,
            priceHistory = priceHistory
        )
    }
}
