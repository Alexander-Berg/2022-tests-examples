package ru.yandex.market.clean.data

import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.hamcrest.Matchers
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import ru.yandex.market.common.android.ResourcesManager
import ru.yandex.market.web.MarketHost
import ru.yandex.market.web.MarketHostProvider

class MarketTouchWebLinksFactoryTest {

    private val resourcesDataStore = mock<ResourcesManager>()
    private lateinit var marketTouchWebLinksFactory: MarketTouchWebLinksFactory

    @Before
    fun setUp() {
        val marketHostProvider = mock<MarketHostProvider>()
        whenever(marketHostProvider.whiteMarketTouch()).thenReturn(marketHost)
        marketTouchWebLinksFactory = MarketTouchWebLinksFactory(
            marketHostProvider = marketHostProvider,
            resourcesManager = resourcesDataStore
        )
    }

    @Test
    fun `Empty string convert to empty string`() {
        Assert.assertThat(
            marketTouchWebLinksFactory.createUrlFromRelativePathIfNeeded(""),
            Matchers.equalTo("")
        )
    }

    @Test
    fun `Check correct path`() {
        val paths = listOf(
            "/",
            "/123",
            "/hello/world",
            "/hello/world?param=123"
        )
        paths.forEach {
            Assert.assertThat(
                marketTouchWebLinksFactory.createUrlFromRelativePathIfNeeded(it),
                Matchers.equalTo(MAIN_URL + it)
            )
        }
    }

    @Test
    fun `Check trusted urls`() {
        val paths = listOf(
            "https://yandex.ru",
            "http://yandex.ru",
            "https://beru.ru",
            "http://beru.ru",
            "https://beru.ru/hello",
            "https://beru.ru/hello/world",
            "https://beru.ru/hello?param=value"
        )
        paths.forEach {
            Assert.assertThat(
                marketTouchWebLinksFactory.createUrlFromRelativePathIfNeeded(it),
                Matchers.equalTo(it)
            )
        }
    }

    companion object {
        private const val HOST = "m.beru.ru"
        private const val MAIN_URL = "https://" + HOST
        private val marketHost: MarketHost = object : MarketHost {
            override fun getHost(): String {
                return HOST
            }

            override fun getMainUrl(): String {
                return MAIN_URL
            }
        }
    }

}