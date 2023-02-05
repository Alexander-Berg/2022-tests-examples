package ru.yandex.market.clean.domain.usecase.health.facades

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyZeroInteractions
import ru.yandex.market.analytics.health.HealthService
import ru.yandex.market.base.network.common.Response
import ru.yandex.market.base.network.common.exception.CommunicationException

class CmsHealthFacadeTest {

    private val healthService = mock<HealthService>()
    private val analyticsFacade = CmsHealthFacade(healthService, mock())

    @Test
    fun `send cms empty event`() {
        analyticsFacade.sendCmsShowEmptyHeath()

        verify(healthService).report(eq("PROMO_SHOW_ERROR"), any(), any(), any(), anyOrNull(), anyOrNull())
    }

    @Test
    fun `dont send cms error health when network error`() {
        val error = CommunicationException(Response.NETWORK_ERROR)
        val requestId = "requestId"

        analyticsFacade.sendCmsShowErrorHeath(error, requestId)

        verifyZeroInteractions(healthService)
    }

    @Test
    fun `send cms error health when network error`() {
        val error = CommunicationException(Response.BAD_REQUEST)
        val requestId = "requestId"

        analyticsFacade.sendCmsShowErrorHeath(error, requestId)

        verify(healthService).report(eq("PROMO_SHOW_ERROR"), any(), any(), any(), anyOrNull(), anyOrNull())
    }
}