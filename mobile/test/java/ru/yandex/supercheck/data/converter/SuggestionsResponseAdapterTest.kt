package ru.yandex.supercheck.data.converter

import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ru.yandex.supercheck.model.data.network.api.search.suggestions.SuggestionsResponse

class SuggestionsResponseAdapterTest {

    companion object {
        val targetJson = """
            [
  "мо",
  "молоко latter ультрапастеризованное питьевое безлактозное 1,5% , 1 л",
  [
    [
      "молоко",
      0.00180636
    ],
    [
      "молоко parmalat",
      0.000541908
    ],
    [
      "молоко агуша",
      0.000361272
    ],
    [
      "молоко алексеевское",
      0.000361272
    ]
  ],
  {
    "suggestions": [
      [
        "молоко latter ультрапастеризованное питьевое безлактозное 1,5% , 1 л",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко latter ультрапастеризованное питьевое безлактозное 1,5% , 1 л\", \"priceFrom\": 12308, \"tags\": [\"hid_982439\", \"name_milk_cheese_eggs\"], \"dbId\": \"11916\", \"image\": {\"image\": \"product_11916_070070016f456c3bda14c99904184365\", \"group\": \"1336043\"}, \"entity\": \"product\", \"prices\": {\"9\": 12308}, \"primaryUnits\": \"1 л\", \"images\": [{\"image\": \"product_11916_070070016f456c3bda14c99904184365\", \"group\": \"1336043\"}], \"id\": \"11916\"}"
        }
      ],
      [
        "молоко parmalat стерилизованное 3,5%, 200г",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко Parmalat стерилизованное 3,5%, 200г\", \"priceFrom\": 2770, \"tags\": [\"hid_982439\", \"name_milk_cheese_eggs\"], \"dbId\": \"45213\", \"image\": {\"image\": \"product_45213_297ee9df3c4d4c58976a5e8f40975b54\", \"group\": \"1349012\"}, \"entity\": \"product\", \"prices\": {\"9\": 2770}, \"primaryUnits\": \"200 мл\", \"images\": [{\"image\": \"product_45213_297ee9df3c4d4c58976a5e8f40975b54\", \"group\": \"1349012\"}], \"id\": \"45213\"}"
        }
      ],
      [
        "молоко parmalat ультрапастеризованное 0,5% 1л",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко Parmalat ультрапастеризованное 0,5% 1л\", \"priceFrom\": 8279, \"tags\": [\"hid_982439\", \"name_milk_cheese_eggs\"], \"dbId\": \"27583\", \"image\": {\"image\": \"product_27583_f97fa0f98fb368e31c5ec80dc55e91d7\", \"group\": \"1349012\"}, \"entity\": \"product\", \"prices\": {\"9\": 8279, \"12\": 8400}, \"primaryUnits\": \"1 л\", \"images\": [{\"image\": \"product_27583_f97fa0f98fb368e31c5ec80dc55e91d7\", \"group\": \"1349012\"}], \"id\": \"27583\"}"
        }
      ],
      [
        "молоко parmalat ультрапастеризованное 3,5%, 1л",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко parmalat ультрапастеризованное 3,5%, 1л\", \"priceFrom\": 8650, \"tags\": [\"hid_982439\", \"name_milk_cheese_eggs\"], \"dbId\": \"15867\", \"image\": {\"image\": \"product_15867_c7270b15c0771556bd4a023fd7622071\", \"group\": \"1393174\"}, \"entity\": \"product\", \"prices\": {\"12\": 8650}, \"primaryUnits\": \"1 л\", \"images\": [{\"image\": \"product_15867_c7270b15c0771556bd4a023fd7622071\", \"group\": \"1393174\"}], \"id\": \"15867\"}"
        }
      ],
      [
        "молоко агуша детское с витаминами с 8 месяцев 2,5%, 0,2л",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко агуша детское с витаминами с 8 месяцев 2,5%, 0,2л\", \"priceFrom\": 2900, \"tags\": [\"hid_15870267\", \"name_child_food\"], \"dbId\": \"11148\", \"image\": {\"image\": \"product_11148_db10c80ee5c82fab216581d8ffda6f63\", \"group\": \"1393174\"}, \"entity\": \"product\", \"prices\": {\"12\": 2900}, \"primaryUnits\": \"200 мл\", \"images\": [{\"image\": \"product_11148_db10c80ee5c82fab216581d8ffda6f63\", \"group\": \"1393174\"}], \"id\": \"11148\"}"
        }
      ],
      [
        "молоко агуша стерилизованное с витаминами а и с 3,2%, 0,5л",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко агуша стерилизованное с витаминами а и с 3,2%, 0,5л\", \"priceFrom\": 5050, \"tags\": [\"hid_15870267\", \"name_child_food\"], \"dbId\": \"11586\", \"image\": {\"image\": \"product_11586_facf2760980b0a4b3ca703ebd51c957f\", \"group\": \"1336043\"}, \"entity\": \"product\", \"prices\": {\"9\": 5646, \"12\": 5050}, \"primaryUnits\": \"0.5 л\", \"images\": [{\"image\": \"product_11586_facf2760980b0a4b3ca703ebd51c957f\", \"group\": \"1336043\"}], \"id\": \"11586\"}"
        }
      ],
      [
        "молоко алексеевское сгущенное цельное с сахаром 8,5%, 270г",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко алексеевское сгущенное цельное с сахаром 8,5%, 270г\", \"priceFrom\": 7550, \"tags\": [\"hid_15720395\", \"name_milk_cheese_eggs\"], \"dbId\": \"32320\", \"image\": {\"image\": \"product_32320_1cb5e31876840be938a58941491eebae\", \"group\": \"1393174\"}, \"entity\": \"product\", \"prices\": {\"12\": 7550}, \"primaryUnits\": \"270 г\", \"images\": [{\"image\": \"product_32320_1cb5e31876840be938a58941491eebae\", \"group\": \"1393174\"}], \"id\": \"32320\"}"
        }
      ],
      [
        "молоко алексеевское цельное сгущенное с сахаром 8,5%, 650г",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко Алексеевское цельное сгущенное с сахаром 8,5%, 650г\", \"priceFrom\": 16550, \"tags\": [\"hid_15720395\", \"name_milk_cheese_eggs\"], \"dbId\": \"13758\", \"image\": {\"image\": \"product_13758_82ef6419d8af4895d30002fad8fa93f9\", \"group\": \"1336043\"}, \"entity\": \"product\", \"prices\": {\"12\": 16550}, \"primaryUnits\": \"650 г\", \"images\": [{\"image\": \"product_13758_82ef6419d8af4895d30002fad8fa93f9\", \"group\": \"1336043\"}], \"id\": \"13758\"}"
        }
      ],
      [
        "молоко главпродукт сгущенное с сахаром 8,5% 380г",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко Главпродукт сгущенное с сахаром 8,5% 380г\", \"priceFrom\": 4990, \"tags\": [\"hid_15720395\", \"name_milk_cheese_eggs\"], \"dbId\": \"13438\", \"image\": {\"image\": \"product_13438_9ab69001d1fe7ae7bd3beb212cbd0c5d\", \"group\": \"1393174\"}, \"entity\": \"product\", \"prices\": {\"12\": 4990}, \"isPromo\": true, \"primaryUnits\": \"380 г\", \"images\": [{\"image\": \"product_13438_9ab69001d1fe7ae7bd3beb212cbd0c5d\", \"group\": \"1393174\"}], \"id\": \"13438\"}"
        }
      ],
      [
        "молоко городецкий цельное 3,4%-4%, 1л",
        0.0001806358341,
        {
          "src": "B",
          "group": "product",
          "url": "{\"name\": \"Молоко Городецкий цельное 3,4%-4%, 1л\", \"priceFrom\": 6290, \"tags\": [\"hid_982439\", \"name_milk_cheese_eggs\"], \"dbId\": \"14172\", \"image\": {\"image\": \"product_14172_0864522525cbab19fde611a4fb9d23d9\", \"group\": \"1327371\"}, \"entity\": \"product\", \"prices\": {\"12\": 6290}, \"primaryUnits\": \"1 л\", \"images\": [{\"image\": \"product_14172_0864522525cbab19fde611a4fb9d23d9\", \"group\": \"1327371\"}], \"id\": \"14172\"}"
        }
      ],
      [
        "молоко, сливки",
        0.0001806358341,
        {
          "src": "B",
          "group": "category",
          "url": "{\"defaultParent\": \"10888\", \"retailChains\": [\"9\", \"12\"], \"id\": \"10906\", \"name\": \"Молоко, сливки\", \"entity\": \"category\"}"
        }
      ],
      [
        "молоко, сыр, яйца",
        0.0001806358341,
        {
          "src": "B",
          "group": "category",
          "url": "{\"defaultParent\": \"root\", \"retailChains\": [\"9\", \"12\"], \"id\": \"10888\", \"name\": \"Молоко, сыр, яйца\", \"entity\": \"category\"}"
        }
      ],
      [
        "молочные смеси",
        0.0001806358341,
        {
          "src": "B",
          "group": "category",
          "url": "{\"defaultParent\": \"10898\", \"retailChains\": [\"9\", \"12\"], \"id\": \"11010\", \"name\": \"Молочные смеси\", \"entity\": \"category\"}"
        }
      ],
      [
        "детские молочные продукты",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"10898\", \"retailChains\": [\"9\", \"12\"], \"id\": \"11011\", \"name\": \"Детские молочные продукты\", \"entity\": \"category\"}"
        }
      ],
      [
        "замороженные морепродукты",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"10890\", \"retailChains\": [\"12\"], \"id\": \"10932\", \"name\": \"Замороженные морепродукты\", \"entity\": \"category\"}"
        }
      ],
      [
        "полуфабрикаты из рыбы и морепродуктов",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"10890\", \"retailChains\": [\"12\"], \"id\": \"10931\", \"name\": \"Полуфабрикаты из рыбы и морепродуктов\", \"entity\": \"category\"}"
        }
      ],
      [
        "пресервы из рыбы и морепродуктов",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"10890\", \"retailChains\": [\"9\", \"12\"], \"id\": \"10934\", \"name\": \"Пресервы из рыбы и морепродуктов\", \"entity\": \"category\"}"
        }
      ],
      [
        "рыба, икра, морепродукты",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"root\", \"retailChains\": [\"9\", \"12\"], \"id\": \"10890\", \"name\": \"Рыба, икра, морепродукты\", \"entity\": \"category\"}"
        }
      ],
      [
        "сгущенное молоко",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"10888\", \"retailChains\": [\"9\", \"12\"], \"id\": \"10914\", \"name\": \"Сгущенное молоко\", \"entity\": \"category\"}"
        }
      ],
      [
        "соки, нектары, морсы",
        1.806358341e-06,
        {
          "src": "W",
          "group": "category",
          "url": "{\"defaultParent\": \"10894\", \"retailChains\": [\"9\", \"12\"], \"id\": \"10978\", \"name\": \"Соки, нектары, морсы\", \"entity\": \"category\"}"
        }
      ]
    ]
  }
]
        """.trimIndent()
    }

    private lateinit var moshi: Moshi

    @Before
    fun initMoshi() {
        moshi = Moshi.Builder()
            .add(SuggestionsResponseAdapter())
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun fromJson() {
        val adapter = moshi.adapter(SuggestionsResponse::class.java)
        val response = adapter.fromJson(targetJson)
        assertNotNull(response)
        assertEquals(response?.searchQuery, "мо")
        assertEquals(
            response?.mostRelevantSuggestion,
            "молоко latter ультрапастеризованное питьевое безлактозное 1,5% , 1 л"
        )
    }
}