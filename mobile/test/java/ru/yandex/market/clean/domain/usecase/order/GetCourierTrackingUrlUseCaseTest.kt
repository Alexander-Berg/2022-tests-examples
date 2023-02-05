package ru.yandex.market.clean.domain.usecase.order

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import io.reactivex.Single
import org.junit.Test
import ru.yandex.market.domain.auth.model.AuthToken
import ru.yandex.market.domain.auth.model.TokenType
import ru.yandex.market.clean.data.repository.OrderOptionsAvailabilitiesRepository
import ru.yandex.market.domain.auth.model.Credentials
import ru.yandex.market.domain.auth.model.MarketUid
import ru.yandex.market.domain.auth.usecase.CredentialsUseCase

class GetCourierTrackingUrlUseCaseTest {

    private val testAuthToken = AuthToken(TokenType.OAUTH, TEST_AUTH_TOKEN)
    private val testMarketUid = MarketUid(TEST_MARKET_UID)

    private val testCredentials = Credentials(
        authToken = testAuthToken,
        muid = testMarketUid
    )

    private val credentialsUseCase = mock<CredentialsUseCase>()
    private val orderOptionsAvailabilitiesRepository = mock<OrderOptionsAvailabilitiesRepository>()

    private val getCourierTrackingUrlUseCase = GetCourierTrackingUrlUseCase(
        credentialsUseCase = credentialsUseCase,
        orderOptionsAvailabilitiesRepository = orderOptionsAvailabilitiesRepository
    )

    @Test
    fun `Get url successfully`() {
        setUpCredentials(testCredentials)
        setUpCourietUrl(TEST_COURIER_MAP_URL)

        val observer = getCourierTrackingUrlUseCase.getTrackingUrl(TEST_ORDER_ID).test()

        observer
            .assertNoErrors()
            .assertValue(TEST_COURIER_MAP_URL)
    }

    private fun setUpCourietUrl(courierMapUrl: String) {
        whenever(
            orderOptionsAvailabilitiesRepository.getCourierTrackingUrl(
                orderId = TEST_ORDER_ID.toString(),
                credentials = testCredentials
            )
        ).thenReturn(Single.just(courierMapUrl))
    }

    private fun setUpCredentials(credentials: Credentials) {
        whenever(credentialsUseCase.getCredentials()).thenReturn(Single.just(credentials))
    }

    companion object {
        private const val TEST_AUTH_TOKEN = "123abc123"
        private const val TEST_MARKET_UID = "456rty456"
        private const val TEST_ORDER_ID = 1234L
        private const val TEST_COURIER_MAP_URL = "https://some/url"
    }
}