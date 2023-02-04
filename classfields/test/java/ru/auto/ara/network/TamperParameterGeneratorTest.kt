package ru.auto.ara.network

import io.qameta.allure.kotlin.junit4.AllureRunner
 import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
 import ru.auto.ara.network.interceptor.TamperGenerator

@RunWith(AllureRunner::class) class TamperParameterGeneratorTest {

    @Test
    fun `should generate correct tamper parameter for request with no body`() {
        val uid = "g60e6edea5msenf07n2bl1epd134qpq2.80229ce079c87ce44b54972701945878"
        val localSalt = "testSalt"
        val query = listOf(
            "price_change_from" to "2021-07-08T16:16:12.674+03:00",
            "with_data" to "true",
            "with_recommended" to "true"
        )
        val correctHash = "4579d2f00a359f856ccadbf7abe34b40"
        val hash = TamperGenerator.generateTamper(
            bodySize = 0,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate correct tamper parameter for different order of query`() {
        val query = listOf(
            "with_data" to "true",
            "price_change_from" to "2021-07-08T16:16:12.674+03:00",
            "with_recommended" to "true"
        )
        val uid = "g60e6edea5msenf07n2bl1epd134qpq2.80229ce079c87ce44b54972701945878"
        val localSalt = "testSalt"
        val correctHash = "4579d2f00a359f856ccadbf7abe34b40"
        val hash = TamperGenerator.generateTamper(
            bodySize = 0,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate correct tamper parameter for request with body`() {
        val query = listOf("context" to "LISTING")
        val uid = "g60e6edea5msenf07n2bl1epd134qpq2.80229ce079c87ce44b54972701945878"
        val localSalt = "testSalt"
        val correctHash = "a0da59653265fad570efc26221a036e9"
        val hash = TamperGenerator.generateTamper(
            bodySize = 304,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate correct tamper parameter for request with no query`() {
        val uid = "g60e6edea5msenf07n2bl1epd134qpq2.80229ce079c87ce44b54972701945878"
        val localSalt = "testSalt"
        val correctHash = "177ec1e2a7af711747dbf808c8af69e9"
        val hash = TamperGenerator.generateTamper(
            bodySize = 304,
            query = emptyList(),
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate correct tamper parameter for query with same key`() {
        val query = listOf(
            "arePicturesRequired" to "false",
            "rgid" to "213",
            "geoRadius" to "200",
            "categoryId" to "760",
            "brandId" to "300",
            "brandId" to "2562",
            "brandId" to "2716",
            "shipping" to "ALL",
            "isNew" to "all"
        )
        val uid = "g60e6edea5msenf07n2bl1epd134qpq2.80229ce079c87ce44b54972701945878"
        val localSalt = "testSalt"
        val correctHash = "691ea24930669110cbf7b39e5f994f77"
        val hash = TamperGenerator.generateTamper(
            bodySize = 304,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate correct tamper for lowercase query parameters`() {
        val query = listOf("bc_lookup" to "mercedes#cla_klasse", "bc_lookup" to "mercedes#c_klasse")
        val uid = "g6131d7295ibfmkto5oqt7pknabb20lb.bff42ffa980cf42c314ed1ac6826751b"
        val localSalt = "testSalt"
        val correctHash = "c4a76a77bc1c41238013ba336498d5bc"
        val hash = TamperGenerator.generateTamper(
            bodySize = 0,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate correct tamper for uppercase query parameters`() {
        val query = listOf("bc_lookup" to "MERCEDES#CLA_KLASSE", "bc_lookup" to "MERCEDES#C_KLASSE")
        val uid = "g6131d7295ibfmkto5oqt7pknabb20lb.bff42ffa980cf42c314ed1ac6826751b"
        val localSalt = "testSalt"
        val correctHash = "0f448d33176c0c6760f0e79e0bbaae87"
        val hash = TamperGenerator.generateTamper(
            bodySize = 0,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }

    @Test
    fun `should generate same tamper for query parameters in different order`() {
        val queryA = listOf("bc_lookup" to "MERCEDES#CLA_KLASSE", "bc_lookup" to "MERCEDES#C_KLASSE")
        val queryB = listOf("bc_lookup" to "MERCEDES#C_KLASSE", "bc_lookup" to "MERCEDES#CLA_KLASSE")
        val uid = "g6131d7295ibfmkto5oqt7pknabb20lb.bff42ffa980cf42c314ed1ac6826751b"
        val localSalt = "testSalt"
        val hashA = TamperGenerator.generateTamper(
            bodySize = 0,
            query = queryA,
            uuid = uid,
            localSalt = localSalt,
        )
        val hashB = TamperGenerator.generateTamper(
            bodySize = 0,
            query = queryB,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hashA).isEqualTo(hashB)
    }

    @Test
    fun `should generate correct tamper for query when it value contains query`() {
        val query = listOf(
            "link" to "https://auto.ru/cars/kia/carnival/3483732/used/?pinned_offer_id=autoru-1097048030" +
                    "&utm_source=google_adwords&utm_medium=cpc" +
                    "&utm_campaign=kms-gdn_allrus_dinamicheskiy-remarketing-app-android" +
                    "&utm_content=app-remarketing_allrus_android&gclid=Cj0KCQjwjo2JBhCRARIsAFG667VajiWcKBGTen3IiygoBdJ-b" +
                    "GVd4OGYd03_Qqu2Wsg1begn6oTfBv0aAkumEALw_wcB"
        )
        val uid = "g6131d7295ibfmkto5oqt7pknabb20lb.bff42ffa980cf42c314ed1ac6826751b"
        val localSalt = "testSalt"
        val correctHash = "c6604eb4469610c9a7c1eb0c159476d5"
        val hash = TamperGenerator.generateTamper(
            bodySize = 0,
            query = query,
            uuid = uid,
            localSalt = localSalt,
        )
        assertThat(hash).isEqualTo(correctHash)
    }


}
