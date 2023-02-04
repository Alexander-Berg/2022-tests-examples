package ru.auto.feature.loans.domain

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.data.model.data.offer.SellerType
import ru.auto.data.network.scala.request.credits.CreditTrafficStream

@RunWith(AllureRunner::class) class SelectTrafficStreamTest {

    @Test
    fun `given price less than million when private seller should return USED_CHEAP_PRIVATE`() {
        assertThat(
            LoanInteractor.chooseTrafficStream(
                rurPrice = 900_000,
                sellerType = SellerType.PRIVATE,
                isOfficial = false
            )
        ).isEqualTo(CreditTrafficStream.USED_CHEAP_PRIVATE)
    }

    @Test
    fun `given price million or more when private seller should return USED_EXPENSIVE_PRIVATE`() {
        assertThat(
            LoanInteractor.chooseTrafficStream(
                rurPrice = 1_000_000,
                sellerType = SellerType.PRIVATE,
                isOfficial = false
            )
        ).isEqualTo(CreditTrafficStream.USED_EXPENSIVE_PRIVATE)
    }

    @Test
    fun `given price less than million when salon should return USED_CHEAP_SALON`() {
        assertThat(
            LoanInteractor.chooseTrafficStream(
                rurPrice = 900_000,
                sellerType = SellerType.COMMERCIAL,
                isOfficial = false
            )
        ).isEqualTo(CreditTrafficStream.USED_CHEAP_SALON)
    }

    @Test
    fun `given price million or more when salon should return USED_EXPENSIVE_SALON`() {
        assertThat(
            LoanInteractor.chooseTrafficStream(
                rurPrice = 1_000_000,
                sellerType = SellerType.COMMERCIAL,
                isOfficial = false
            )
        ).isEqualTo(CreditTrafficStream.USED_EXPENSIVE_SALON)
    }

    @Test
    fun `given price less than million when official dealer should return USED_CHEAP_DEALER`() {
        assertThat(
            LoanInteractor.chooseTrafficStream(
                rurPrice = 900_000,
                sellerType = SellerType.COMMERCIAL,
                isOfficial = true
            )
        ).isEqualTo(CreditTrafficStream.USED_CHEAP_DEALER)
    }

    @Test
    fun `given price million or more when official dealer should return USED_EXPENSIVE_DEALER`() {
        assertThat(
            LoanInteractor.chooseTrafficStream(
                rurPrice = 1_000_000,
                sellerType = SellerType.COMMERCIAL,
                isOfficial = true
            )
        ).isEqualTo(CreditTrafficStream.USED_EXPENSIVE_DEALER)
    }


}
