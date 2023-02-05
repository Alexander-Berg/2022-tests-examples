package ru.yandex.market.clean.domain.usecase.order

import com.annimon.stream.Optional
import io.reactivex.Single
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.repository.OrderOptionsAvailabilitiesRepository
import ru.yandex.market.clean.domain.model.order.AvailableOption
import ru.yandex.market.domain.auth.model.Credentials
import ru.yandex.market.clean.domain.model.order.OrderOptionsAvailabilities
import ru.yandex.market.clean.domain.model.orderfeedback.OrderGrade
import ru.yandex.market.domain.auth.model.AuthToken
import ru.yandex.market.domain.auth.model.MarketUid
import ru.yandex.market.domain.auth.model.TokenType
import ru.yandex.market.domain.auth.usecase.CredentialsUseCase

class GetOrderOptionsAvailabilitiesWithGradesUseCaseTest {

    private val testAuthToken = AuthToken(TokenType.OAUTH, TEST_AUTH_TOKEN)
    private val testMuid = MarketUid(TEST_MARKET_UID)

    private val testCredentials = Credentials(
        authToken = testAuthToken,
        muid = testMuid
    )

    private val expectedOptionsAvailabilities = createTestOrderOptionsAvailabilities()

    private val orderOptionsAvailabilitiesRepository = mock<OrderOptionsAvailabilitiesRepository>()
    private val credentialsUseCase = mock<CredentialsUseCase>()

    private val getOrderOptionsAvailabilitiesUseCase = GetOrderOptionsAvailabilitiesWithGradesUseCase(
        orderOptionsAvailabilitiesRepository = orderOptionsAvailabilitiesRepository,
        credentialsUseCase = credentialsUseCase
    )

    @Test
    fun `Get order options availabilities`() {
        setUpCredentials(testCredentials)
        setUpOrderOptionsAvailabilities(expectedOptionsAvailabilities.first)

        val observer = getOrderOptionsAvailabilitiesUseCase
            .getOrderOptionsAvailabilities(TEST_ORDER_ID.toString()).test()

        observer
            .assertNoErrors()
            .assertValue(expectedOptionsAvailabilities)

        verifyOptionsAvailabilitiesLoadedFromRepository()
    }

    private fun verifyOptionsAvailabilitiesLoadedFromRepository() {
        verify(orderOptionsAvailabilitiesRepository).getOrderOptionsAvailabilitiesWithGrades(
            orderId = TEST_ORDER_ID.toString(),
            credentials = testCredentials
        )
    }

    private fun setUpCredentials(credentials: Credentials) {
        whenever(credentialsUseCase.getCredentials()).thenReturn(Single.just(credentials))
    }

    private fun setUpOrderOptionsAvailabilities(
        orderOptionsAvailabilities: OrderOptionsAvailabilities
    ) {
        whenever(
            orderOptionsAvailabilitiesRepository.getOrderOptionsAvailabilitiesWithGrades(
                orderId = TEST_ORDER_ID.toString(),
                credentials = testCredentials
            )
        ).thenReturn(Single.just(Pair(orderOptionsAvailabilities, Optional.empty<OrderGrade>())))
    }

    private fun createTestOrderOptionsAvailabilities(): Pair<OrderOptionsAvailabilities, Optional<OrderGrade>> {
        return Pair(
            OrderOptionsAvailabilities(
                orderId = TEST_ORDER_ID,
                availabilities = listOf(AvailableOption.COURIER_ON_MAP)
            ),
            Optional.empty<OrderGrade>()
        )
    }

    companion object {
        private const val TEST_ORDER_ID = 1234L
        private const val TEST_AUTH_TOKEN = "123abc123"
        private const val TEST_MARKET_UID = "456rty456"
    }
}