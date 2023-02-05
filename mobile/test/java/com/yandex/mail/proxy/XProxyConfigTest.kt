package com.yandex.mail.proxy

import com.yandex.mail.runners.UnitTestRunner
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(UnitTestRunner::class)
class XProxyConfigTest {
    private val configContent = """{"yandexHost": "http://xp.yandex-team.ru/cfg"}"""

    @Test
    fun `should deserialize correctly`() {
        val config = XProxyConfig.parse(JSONObject(configContent))
        assertEquals("http://xp.yandex-team.ru/cfg", config.yandexHost)
    }

    @Test
    fun `should deserialize if no yandex host`() {
        val config = XProxyConfig.parse(JSONObject("""{}"""))
        assertNull(config.yandexHost)
    }
}
