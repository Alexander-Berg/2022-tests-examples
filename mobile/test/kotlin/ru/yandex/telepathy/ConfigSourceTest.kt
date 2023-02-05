package ru.yandex.telepathy

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.net.URL

class ConfigSourceTest {

    val applicationName = "yandex-launcher"

    val configName = "nuclearCodes.json"

    val customSource = "http://myownapi.ru:12345/myownconfig?platform=android"

    @Test
    fun externalProdUrlTest() {
        val url = ConfigSource.externalProd(applicationName, configName).url
        assertThat(url.host).isEqualTo("mobile-configs.s3.yandex.net")
        assertThat(url.path).isEqualTo("/$applicationName/$configName")
        checkCommonUrlParts(url)
    }

    @Test
    fun internalProdUrlTest() {
        val url = ConfigSource.internalProd(applicationName, configName).url
        assertThat(url.host).isEqualTo("mobile-configs.s3.mds.yandex.net")
        checkCommonUrlParts(url)
    }

    @Test
    fun testingUrlTest() {
        val url = ConfigSource.testing(applicationName, configName).url
        assertThat(url.host).isEqualTo("mobile-configs.s3.mdst.yandex.net")
        checkCommonUrlParts(url)
    }

    @Test
    fun customUrlTest() {
        val url = ConfigSource.custom(customSource).url
        assertThat(url.host).isEqualTo("myownapi.ru")
        assertThat(url.path).isEqualTo("/myownconfig")
        assertThat(url.protocol).isEqualTo("http")
        assertThat(url.port).isEqualTo(12345)
        assertThat(url.query).isEqualTo("platform=android")
    }

    @Test
    fun customUrl_shouldReturnTheSameAsCustomString() {
        assertThat(ConfigSource.custom(URL(customSource)))
            .isEqualToComparingFieldByField(ConfigSource.custom(customSource))
    }

    private fun checkCommonUrlParts(url: URL) {
        assertThat(url.protocol).isEqualTo("https")
        assertThat(url.port).isEqualTo(-1)
        assertThat(url.query).isBlank()
        assertThat(url.path).isEqualTo("/$applicationName/$configName")
    }
}