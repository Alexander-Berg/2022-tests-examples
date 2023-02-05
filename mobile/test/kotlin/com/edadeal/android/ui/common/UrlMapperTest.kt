package com.edadeal.android.ui.common

import android.net.Uri
import com.edadeal.android.dto.AppBar
import com.edadeal.android.dto.CalibratorResponse
import com.edadeal.android.model.NavigationConfig
import com.edadeal.android.model.TabBarConfig
import com.edadeal.android.model.calibrator.CalibratorConfigs
import com.edadeal.android.ui.common.navigation.intents.UrlMapper
import com.nhaarman.mockito_kotlin.whenever
import io.reactivex.subjects.PublishSubject
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(MockitoJUnitRunner::class)
class UrlMapperTest {
    private val webViewUri = Uri.parse("edadeal://webView")
    private val itemListUri = Uri.parse("edadeal://itemList")
    private val mainScreenUri = Uri.parse("edadeal://mainScreen")
    private val navigationConfigSubject = PublishSubject.create<NavigationConfig>()

    @Mock
    private lateinit var configs: CalibratorConfigs
    private lateinit var urlMapper: UrlMapper

    @BeforeTest
    fun prepare() {
        whenever(configs.navigationConfigFetches).thenReturn(navigationConfigSubject)
        urlMapper = UrlMapper(configs)
    }

    @Test
    fun `getDeepLinkForUrl should return deeplink only if url matches regexp`() {
        updateUrlMappings(
            "^edadeal://webApp" to webViewUri.toString(),
            "^edadeal://retailerOffers" to itemListUri.toString(),
            "^https://edadeal.ru" to mainScreenUri.toString()
        )

        assertEquals(webViewUri, getDeepLinkForUrl("edadeal://webApp?json=%7B%7D"))
        assertEquals(mainScreenUri, getDeepLinkForUrl("https://edadeal.ru/moscow"))
        assertEquals(itemListUri, getDeepLinkForUrl("edadeal://retailerOffers?json=%7B%7D"))
        assertNull(getDeepLinkForUrl("https://feedback.edadeal.ru"))
        assertNull(getDeepLinkForUrl("edadeal://itemList?json=%7B%7D"))
    }

    @Test
    fun `getDeepLinkForUrl should return null if regexp is incorrect`() {
        updateUrlMappings(
            "" to "",
            "^edadeal://web[App" to webViewUri.toString(),
            "^https://edadeal.ru$" to mainScreenUri.toString()
        )

        assertNull(getDeepLinkForUrl("https://edadeal.ru/moscow"))
        assertNull(getDeepLinkForUrl("edadeal://webApp?json=%7B%7D"))
    }

    @Test
    fun `getDeepLinkForUrl should return new deeplink when navigationConfig is updated`() {
        val url = "edadeal://webApp?json=%7B%7D"
        assertNull(getDeepLinkForUrl(url))

        updateUrlMappings(
            "^edadeal://webApp" to webViewUri.toString(),
        )
        assertEquals(webViewUri, getDeepLinkForUrl(url))

        updateUrlMappings(
            "^edadeal://webApp" to mainScreenUri.toString(),
        )
        assertEquals(mainScreenUri, getDeepLinkForUrl(url))
    }

    private fun updateUrlMappings(vararg unilinks: Pair<String, String>) {
        val navigationConfig = NavigationConfig(
            tabBar = TabBarConfig(emptyList()), searchBar = CalibratorResponse.SearchBar(),
            appBar = AppBar(),
            unilinks = unilinks.map { (regexp, deeplink) -> CalibratorResponse.UrlMapping(regexp, deeplink) }
        )
        navigationConfigSubject.onNext(navigationConfig)
    }

    private fun getDeepLinkForUrl(url: String): Uri? {
        return urlMapper.getDeepLinkForUrl(Uri.parse(url))
    }
}
