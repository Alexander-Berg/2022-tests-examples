package ru.yandex.market.analytics.facades

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import ru.yandex.market.analytics.appmetrica.AppMetricaAnalyticService

class PlusTrialAnalyticsTest {

    private val appMetrica = mock<AppMetricaAnalyticService>()
    private val analyticsFacade = PlusTrialAnalytics(appMetrica)

    @Test
    fun getPlusTrialDialogVisible() {
        analyticsFacade.getPlusTrialDialogVisible()
        verify(appMetrica).report("BRAND-DAY-FREE-PLUS-POPUP_VISIBLE")
    }

    @Test
    fun getPlusTrialClick() {
        analyticsFacade.getPlusTrialClick()
        verify(appMetrica).report("BRAND-DAY-FREE-PLUS-POPUP_NAVIGATE")
    }

    @Test
    fun plusTrialReceivedDialogVisible() {
        analyticsFacade.plusTrialReceivedDialogVisible()
        verify(appMetrica).report("BRAND-DAY-SUCCESS-PLUS-POPUP_VISIBLE")
    }

    @Test
    fun plusTrialReceivedButtonClick() {
        analyticsFacade.plusTrialReceivedButtonClick()
        verify(appMetrica).report("BRAND-DAY-SUCCESS-PLUS-POPUP_NAVIGATE")
    }
}