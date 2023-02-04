package ru.yandex.partner.core.bs.enrich

import org.assertj.core.api.Assertions
import org.junit.jupiter.api.Test

internal class BkDataServiceTest {

    @Test
    fun basicSpaceBetweenTest() {
        val parsed = BkDataService.parsePageId("{\"PageID\": \"153645\"}")
        Assertions.assertThat(parsed).isEqualTo(153645L)
    }

    @Test
    fun basicNoSpaceBetweenTest() {
        val parsed = BkDataService.parsePageId("{ \"PageID\":\"123\" }")
        Assertions.assertThat(parsed).isEqualTo(123L)
    }

    @Test
    fun pageIdNullTest() {
        val parsed = BkDataService.parsePageId("{ \"PageID\": null }")
        Assertions.assertThat(parsed).isEqualTo(null)
    }

    @Test
    fun emptyStringTest() {
        val parsed = BkDataService.parsePageId("")
        Assertions.assertThat(parsed).isEqualTo(null)
    }

}
