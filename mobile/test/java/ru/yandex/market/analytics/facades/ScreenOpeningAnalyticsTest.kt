package ru.yandex.market.analytics.facades

import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.appmetrica.AppMetricaAnalyticService
import ru.yandex.market.analytics.appmetrica.ScreenOpeningAnalytics
import ru.yandex.market.base.presentation.core.mvp.fragment.ScreenAnalyticsParams

class ScreenOpeningAnalyticsTest {
    private val appMetrica = mock<AppMetricaAnalyticService>()
    private val screenOpeningAnalytics = ScreenOpeningAnalytics(appMetrica)

    @Test
    fun `Test screen opening event`() {
        screenOpeningAnalytics.sendScreenOpeningEvent(
            ScreenAnalyticsParams(screenName = SCREEN_NAME, screenId = DUMMY_SCREEN_ID)
        )

        verify(appMetrica).report(eq(OPEN_PAGE_VISIBLE), any())
    }

    @Test
    fun `Test screen closed event`() {
        screenOpeningAnalytics.sendScreenClosingEvent(
            ScreenAnalyticsParams(screenName = SCREEN_NAME, screenId = DUMMY_SCREEN_ID)
        )
        verify(appMetrica).report(eq(CLOSE_PAGE), any())
    }

    @Test
    fun `Test screen opening event with url`() {
        screenOpeningAnalytics.sendScreenOpeningEvent(
            ScreenAnalyticsParams(screenName = SCREEN_NAME, screenId = DUMMY_SCREEN_ID, url = DUMMY_URL)
        )

        verify(appMetrica).report(eq(OPEN_PAGE_VISIBLE), any())
    }

    companion object {
        private const val SCREEN_NAME = "SCREEN_NAME"
        private const val OPEN_PAGE_VISIBLE = "OPEN-PAGE_VISIBLE"
        private const val CLOSE_PAGE = "CLOSE-PAGE"
        private const val DUMMY_URL = "DUMMY_URL"
        private const val DUMMY_SCREEN_ID = "1"
    }
}
