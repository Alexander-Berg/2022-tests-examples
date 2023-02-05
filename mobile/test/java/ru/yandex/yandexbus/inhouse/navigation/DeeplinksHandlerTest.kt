package ru.yandex.yandexbus.inhouse.navigation

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.yandex.yandexbus.inhouse.BaseTest
import ru.yandex.yandexbus.inhouse.navigation.NavigationRequest.DeeplinkNavigationRequest
import ru.yandex.yandexbus.inhouse.navigation.NavigationRequest.OpenMenuNavigationRequest
import ru.yandex.yandexbus.inhouse.navigation.RootNavigator.ScreenRecord
import ru.yandex.yandexbus.inhouse.route.routesetup.RouteSetupArgs
import ru.yandex.yandexbus.inhouse.stop.StopModel
import ru.yandex.yandexbus.inhouse.stop.card.StopCardArgs
import ru.yandex.yandexbus.inhouse.utils.Screen
import ru.yandex.yandexbus.inhouse.utils.analytics.GenaAppAnalytics.MapShowStopCardSource
import ru.yandex.yandexbus.inhouse.utils.analytics.GenaAppAnalytics.RouteStartRoutingSource

class DeeplinksHandlerTest : BaseTest() {

    @Test
    fun `proper answer for main screen from deeplink`() {
        assertResult("yandextransport://main", DeeplinkNavigationRequest())
        assertResult("yandextransport://show_main", DeeplinkNavigationRequest())
        assertResult("yandextransport://show_main?tab=map", DeeplinkNavigationRequest())
    }

    @Test
    fun `proper answer for routes screen from deeplink`() {
        assertResult("yandextransport://routes", DeeplinkNavigationRequest(ROUTE_SCREEN_RECORD))
        assertResult("yandextransport://show_main?tab=route", DeeplinkNavigationRequest(ROUTE_SCREEN_RECORD))
    }

    @Test
    fun `proper answer for settings screens from deeplink`() {
        assertResult("yandextransport://settings", DeeplinkNavigationRequest(SETTINGS_SCREEN_RECORD))
        assertResult("yandextransport://show_settings", DeeplinkNavigationRequest(SETTINGS_SCREEN_RECORD))
    }

    @Test
    fun `proper answer for promocodes from deeplink`() {
        assertResult("yandextransport://promocodes", DeeplinkNavigationRequest(PROMOCODES_SCREEN_RECORD))
    }

    @Test
    fun `proper answer for stop card from deeplink`() {
        assertResult("yandextransport://stop?stopId=$STOP_ID", DeeplinkNavigationRequest(STOP_CARD_RECORD))
        assertResult("yandextransport://show_hotspot_minicard?hotspotId=$STOP_ID", DeeplinkNavigationRequest(STOP_CARD_RECORD))
    }

    @Test
    fun `proper answer for open menu deeplink`() {
        assertOpenMenuResult("yandextransport://menu", OpenMenuNavigationRequest)
    }

    @Test
    fun `null for unsupported scheme or link`() {
        assertResult("yatransport://stop?stopId=$STOP_ID", expected = null)
        assertResult("yandextransport://org_card", expected = null)
    }

    private fun assertResult(uri: String, expected: DeeplinkNavigationRequest?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        val navRequest = DeeplinksHandler().processIntent(intent) as DeeplinkNavigationRequest?
        assertEquals(expected, navRequest)
    }

    private fun assertOpenMenuResult(uri: String, expected: OpenMenuNavigationRequest?) {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uri))
        val navRequest = DeeplinksHandler().processIntent(intent) as OpenMenuNavigationRequest?
        assertEquals(expected, navRequest)
    }

    companion object {
        private val ROUTE_SCREEN_RECORD = ScreenRecord(Screen.ROUTE_SETUP, RouteSetupArgs(null, null, RouteStartRoutingSource.ROUTING, true))
        private val SETTINGS_SCREEN_RECORD = ScreenRecord(Screen.SETTINGS, args = null)
        private val PROMOCODES_SCREEN_RECORD = ScreenRecord(Screen.PROMOCODES, args = null)
        private const val STOP_ID = "some_stop_id"
        private val STOP_CARD_RECORD = ScreenRecord(Screen.CARD_STOP, StopCardArgs(StopModel(STOP_ID, "", null), MapShowStopCardSource.URL))
    }
}
