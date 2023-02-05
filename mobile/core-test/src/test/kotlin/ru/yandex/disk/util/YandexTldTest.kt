package ru.yandex.disk.util

import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.Test

class YandexTldTest {

    @Test
    fun `should parse tld from url`() {
        assertThat(YandexTld.parseTld("https://disk.yandex.ru/gift"), equalTo("ru"))
        assertThat(YandexTld.parseTld("https://disk.yandex.com/gift"), equalTo("com"))
        assertThat(YandexTld.parseTld("https://disk.yandex.ua/gift"), equalTo("ru"))
    }

    @Test
    fun `should parse com tr tld from url`() {
        assertThat(YandexTld.parseTld("https://disk.yandex.com.tr/gift"), equalTo("com.tr"))
    }

    @Test
    fun `should return null tld from bad url`() {
        assertThat(YandexTld.parseTld("disk.yandex.ru"), nullValue())
    }

    @Test
    fun `should return ru tld from unknown url`() {
        assertThat(YandexTld.parseTld("http://yadi.sk"), equalTo("ru"))
        assertThat(YandexTld.parseTld("http://localhost"), equalTo("ru"))
        assertThat(YandexTld.parseTld("http://127.0.0.1"), equalTo("ru"))
    }
}
