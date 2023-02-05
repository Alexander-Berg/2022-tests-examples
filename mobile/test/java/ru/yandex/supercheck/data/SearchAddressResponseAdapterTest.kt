package ru.yandex.supercheck.data

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ru.yandex.supercheck.data.converter.SearchAddressResponseAdapter
import ru.yandex.supercheck.model.data.network.api.search.address.AddressSearchResponse

class SearchAddressResponseAdapterTest {

    companion object {
        val targetJson = """
            {
  "part": "больш",
  "results": [
    {
      "name": "улица Большая Дмитровка",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.76235962,
      "lon": 37.61359787
    },
    {
      "name": "Большая Никитская улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.75753403,
      "lon": 37.59842682
    },
    {
      "name": "Большая Академическая улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.83596039,
      "lon": 37.53783417
    },
    {
      "name": "Большая Очаковская улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.6831398,
      "lon": 37.45697784
    },
    {
      "name": "Большая Грузинская улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.76950455,
      "lon": 37.58136368
    },
    {
      "name": "Большая Черёмушкинская улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.68362808,
      "lon": 37.59088898
    },
    {
      "name": "Большая Никитская улица, 13",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.75638962,
      "lon": 37.60494995
    },
    {
      "name": "улица Большая Ордынка",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.73784256,
      "lon": 37.62439728
    },
    {
      "name": "Большая Черкизовская улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.79819107,
      "lon": 37.72838211
    },
    {
      "name": "Большая Пироговская улица",
      "local": true,
      "type": "geo",
      "desc": "Москва, Россия",
      "lat": 55.73205948,
      "lon": 37.57156754
    }
  ]
}
        """.trimIndent()
    }

    private lateinit var moshi: Moshi

    @Before
    fun initMoshi() {
        moshi = Moshi.Builder()
            .add(SearchAddressResponseAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun fromJson() {
        val adapter = moshi.adapter(AddressSearchResponse::class.java)
        val response = adapter.fromJson(targetJson)
        assertNotNull(response)
        assertEquals(response?.searchQuery, "больш")
        assertEquals(
            response?.results?.get(2)?.name,
            "Большая Академическая улица"
        )
    }

}