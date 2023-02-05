package com.yandex.mail.network

import com.yandex.mail.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Test

class NetworkModuleTest {
    @Test
    fun `should add path in host to resulting host`() {
        val host = NetworkModule.prepareHost("http://polonium.yandex-team.ru/configuration", BuildConfig.API_PATH);
        assertEquals("http://polonium.yandex-team.ru/configuration/api/mobile/v1/", host.toString())
    }

    @Test
    fun `should add path to host if no path in host`() {
        val host = NetworkModule.prepareHost("https://mail.yandex.ru", BuildConfig.API_PATH);
        assertEquals("https://mail.yandex.ru/api/mobile/v1/", host.toString())
    }

    @Test
    fun `should add path to mock web server localhost correctly`() {
        val host = NetworkModule.prepareHost("http://localhost:51128/", BuildConfig.API_PATH);
        assertEquals("http://localhost:51128/api/mobile/v1/", host.toString())
    }
}
