package ru.yandex.market.clean.data.mapper.antirobot

import org.assertj.core.api.Assertions
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import ru.yandex.market.clean.data.store.antirobot.JwsTokenEntity
import ru.yandex.market.clean.domain.model.antirobot.JwsToken
import ru.yandex.market.datetime.DateTimeProvider
import ru.yandex.market.utils.minutes

class JwsTokenMapperTest {

    private val dateTimeProvider = mock<DateTimeProvider>()

    private val expirationTimeExtractor = mock<ExpirationTimeExtractor>()

    private val jwsTokenMapper = JwsTokenMapper(dateTimeProvider, expirationTimeExtractor)

    @Test
    fun `Return Valid token when expiration time is not reached`() {
        whenever(dateTimeProvider.currentUnixTimeInMillis).thenReturn(1.minutes.longMillis)
        whenever(expirationTimeExtractor.extractExpirationTime(STUB_TOKEN_ENTITY))
            .thenReturn(5.minutes.longMillis)

        val actualToken = jwsTokenMapper.map(STUB_TOKEN_ENTITY)

        Assertions.assertThat(actualToken).matches { it is JwsToken.Valid }
    }

    @Test
    fun `Return AlmostExpired token when expiration time is almost reached`() {
        whenever(dateTimeProvider.currentUnixTimeInMillis).thenReturn(4.5.minutes.longMillis)
        whenever(expirationTimeExtractor.extractExpirationTime(STUB_TOKEN_ENTITY))
            .thenReturn(5.minutes.longMillis)

        val actualToken = jwsTokenMapper.map(STUB_TOKEN_ENTITY)

        Assertions.assertThat(actualToken).matches { it is JwsToken.AlmostExpired }
    }

    @Test
    fun `Return Expired token when expiration time is reached`() {
        whenever(dateTimeProvider.currentUnixTimeInMillis).thenReturn(6.minutes.longMillis)
        whenever(expirationTimeExtractor.extractExpirationTime(STUB_TOKEN_ENTITY))
            .thenReturn(5.minutes.longMillis)

        val actualToken = jwsTokenMapper.map(STUB_TOKEN_ENTITY)

        Assertions.assertThat(actualToken).matches { it is JwsToken.Expired }
    }

    @Test
    fun `Return time until expiration minus one minute`() {
        whenever(dateTimeProvider.currentUnixTimeInMillis).thenReturn(2.minutes.longMillis)
        whenever(expirationTimeExtractor.extractExpirationTime(STUB_TOKEN_ENTITY))
            .thenReturn(5.minutes.longMillis)

        val actualToken = jwsTokenMapper.map(STUB_TOKEN_ENTITY)

        Assertions.assertThat(actualToken.timeUtilExpiredInMillis).isEqualTo(2.minutes.longMillis)
    }

    @Test
    fun `Return zero time until expiration when token expired`() {
        whenever(dateTimeProvider.currentUnixTimeInMillis).thenReturn(4.5.minutes.longMillis)
        whenever(expirationTimeExtractor.extractExpirationTime(STUB_TOKEN_ENTITY))
            .thenReturn(5.minutes.longMillis)

        val actualToken = jwsTokenMapper.map(STUB_TOKEN_ENTITY)

        Assertions.assertThat(actualToken.timeUtilExpiredInMillis).isEqualTo(0)
    }

    companion object {
        private val STUB_TOKEN_ENTITY = JwsTokenEntity(
            token = "some-token",
            lastUpdateTime = 1234
        )
    }
}