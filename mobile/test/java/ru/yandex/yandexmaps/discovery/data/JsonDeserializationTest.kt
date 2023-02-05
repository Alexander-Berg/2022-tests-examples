package ru.yandex.yandexmaps.discovery.data

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import ru.yandex.yandexmaps.multiplatform.core.geometry.Point

class JsonDeserializationTest {

    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = DataExtractorImpl.getMoshi(null)
    }

    @Test
    fun test() {
        val adapter = moshi.adapter(DiscoveryPage::class.java)
        val fromJson = adapter.fromJson(testData)

        assertNotNull(fromJson)
    }

    @Test
    fun organizationBlockDeserialization() {
        assertDeserializationSuccess(OrganizationBlock::class.java, organizationBlockExpected, organizationBlockJson)
    }

    @Test
    fun shareBlockDeserialization() {
        assertDeserializationSuccess(ShareBlock::class.java, shareBlockExpected, shareBlockjson)
    }

    private fun <T> assertDeserializationSuccess(clazz: Class<T>, expected: T, actual: String) {
        val adapter = moshi.adapter(clazz)
        val fromJson = adapter.fromJson(actual)

        assertEquals(expected, fromJson)
    }
}

private const val organizationBlockJson = """
     {
        "oid": "37209529469",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Артурс Village Spa Отель»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b866d69b3f0c5fabfa2cce185b0/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b866a0a83023a5ee982036a7378/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/236825/2a0000015b866b356b72bf34bba5b1a678e1/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Хвойная ул., с26, д. Ларёво, дачный кооператив Космос",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "6100–22400 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 150-41-21"
          }
        ],
        "sentence": "И поработать, и отдохнуть — количество парных соревнуется с количеством конференц-залов.",
        "coordinate": {
          "lon": 37.540009,
          "lat": 56.093744
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.5,
          "ratings": 433,
          "reviews": 21
        }
    }
    """

private val organizationBlockExpected = OrganizationBlock(
    style = OrganizationBlock.Style.POOR,
    oid = "37209529469",
    address = "Хвойная ул., с26, д. Ларёво, дачный кооператив Космос",
    coordinate = Point(56.093744, 37.540009),
    businessRating = OrganizationBlock.Rating(score = 8.5f, ratings = 433, reviews = 21),
    rubric = "Гостиница",
    title = "«Артурс Village Spa Отель»",
    sentence = "И поработать, и отдохнуть — количество парных соревнуется с количеством конференц-залов.",
    description = "",
    images = listOf(
        Image(urlTemplate = "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b866d69b3f0c5fabfa2cce185b0/%s"),
        Image(urlTemplate = "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b866a0a83023a5ee982036a7378/%s"),
        Image(urlTemplate = "https://avatars.mds.yandex.net/get-altay/236825/2a0000015b866b356b72bf34bba5b1a678e1/%s")
    ),
    paragraphIcon = Icon(tag = "sanatorium"),
    placemarkIcon = Icon(tag = "sanatorium"),
    features = listOf(
        OrganizationBlock.Feature(
            name = "цена номера",
            key = "price_room",
            value = "6100–22400 руб/ночь"
        ),
        OrganizationBlock.Feature(
            name = "Телефон",
            key = "phone",
            value = "+7 (495) 150-41-21"
        )
    ),
)

private const val shareBlockjson = """
    {
        "type": "Share",
        "style": "large"
    }
    """

private val shareBlockExpected = ShareBlock(ShareBlock.Style.LARGE)

val testData = """
   {
  "data": {
    "icon": {
      "tag": "sanatorium"
    },
    "type": "OrganizationList",
    "alias": "gde-otdyhat-v-podmoskove",
    "image": {
      "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1327602/2a0000016364d814dde51ac1150cf2a4e1f9/%s"
    },
    "title": "Где отдыхать в Подмосковье?",
    "rubric": "Гостиница",
    "properties": {
      "meta": {
        "url": "gde-otdyhat-v-podmoskove",
        "image": {
          "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001638cd451592a58ccae4a5c0cda35/%s"
        },
        "title": "Где отдыхать в Подмосковье?",
        "keywords": "дом отдыха, подмосковье, санаторий, пансионат, куда поехать за город, отдых в подмосковье ",
        "description": "Рассказываем о местах, куда можно выбраться из города на корпоратив, где можно провести семейные выходные или выспаться под шум леса."
      }
    },
    "boundingBox": {
      "northEast": {
        "lat": 57.53961699999199,
        "lon": 41.51577999999998
      },
      "southWest": {
        "lat": 54.6938389999952,
        "lon": 35.600168999999994
      }
    },
    "description": "Рассказываем о местах, куда можно выбраться из города на корпоратив, где можно провести семейные выходные или выспаться под шум леса.",
    "geoRegionId": 213,
    "placeNumber": 34,
    "schemaVersion": 0,
    "blocks": [
      {
        "oid": "1671464151",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Болотов.Дача»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369fe18a8c6719b8462d15ea289/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1352335/2a00000162d4a1e59a1860e1bef0d2ef6701/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/912415/2a0000016369fe87b3fce360a47a8909d518/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Тульская область, Заокский район, деревня Дворяниново",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "2000–7500 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (903) 777-57-34"
          }
        ],
        "sentence": "Семейная дача с дружелюбной атмосферой и уютными номерами от создателей антикафе «Циферблат». Если захотите поселиться поблизости, обратите внимание — рядом строят дома на продажу.",
        "coordinate": {
          "lon": 37.558194,
          "lat": 54.693839
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.8,
          "ratings": 60,
          "reviews": 17
        }
      },
      {
        "oid": "146346836888",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Никола-Ленивец",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/235931/2a0000015ce927916e16dfa5804e354146e4/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/216588/2a0000015b21763e46ab4ebd99aa5870229b/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/248099/2a0000015ce9279e2885fd5b31faa7590684/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Калужская область, Дзержинский район, деревня Никола-Ленивец",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "2000–6200 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 150-54-75"
          }
        ],
        "sentence": "Огромная территория с лесом, рекой и диковинными арт-объектами Николая Полисского. Можно жить в палатке, хостеле, домике или коттедже с удобствами.",
        "coordinate": {
          "lon": 35.600169,
          "lat": 54.749627
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.3,
          "ratings": 208,
          "reviews": 16
        }
      },
      {
        "oid": "99014519451",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Арт-усадьба «Веретьево»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/215317/2a00000160d5ac46b8e62aeee8c06cbf7367/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1032555/2a00000160d5acb51cbe6bfc609c5044af58/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/902116/2a00000160d5a7db26926783066aa9a1aa93/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Московская область, Талдомский район, сельское поселение Темповое, деревня Веретьево",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "1800–9000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (916) 956-28-82"
          }
        ],
        "sentence": "Пансионат с колоритными инсталляциями — пионерлагерь для взрослых. Расположен на месте советского лагеря для трудных подростков. Поблизости ферма с домашним зверьём и оленятами.",
        "coordinate": {
          "lon": 37.274354,
          "lat": 56.696569
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.4,
          "ratings": 135,
          "reviews": 3
        }
      },
      {
        "oid": "1691639931",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Radisson Завидово»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/406255/2a00000162d3c3eabc0eaffa87772b0adea6/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363376/2a00000162d3c3c72635ad75c450e61e5b9e/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Тверской бул., 4, д. Вараксино",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "от 6300 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (48242) 7-80-78"
          }
        ],
        "sentence": "Многоэтажный люксовый отель с видом на воду и мультиспортивный комплекс «Акватория лета», где практикуют кайтсёрфинг, виндсёрфинг, вейксёрфинг и прочие водные забавы.",
        "coordinate": {
          "lon": 36.521379,
          "lat": 56.59461
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.4,
          "ratings": 34,
          "reviews": 4
        }
      },
      {
        "oid": "161015844940",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "Пансионат «Якорь»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1352335/2a0000016307d6b31765504fb476e55c1b9f/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1335362/2a0000016307d6bac33235ef97148f1e0587/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1372264/2a0000016307d6425077308bfe1efbe06177/%s"
          }
        ],
        "rubric": "Дом отдыха",
        "address": "ул. Декабристов, 8, Таруса",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "1200–1700 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (48435) 2-50-13"
          }
        ],
        "sentence": "Твин Пикс по-тарусски — бюджетный отель в деревенском стиле на берегу Оки. Внутри, однако, есть современные баня и спортзал. Достопримечательности Тарусы под боком.",
        "coordinate": {
          "lon": 37.181998,
          "lat": 54.727242
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 7.8,
          "ratings": 121,
          "reviews": 5
        }
      },
      {
        "oid": "1103827888",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "«Снегири»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001636a030641c330afb636cc0c3715/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636a03d974d67cbef9103c1e5fde/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/216588/2a0000015b218a73942e050c0ace1a85bb8f/%s"
          }
        ],
        "rubric": "Санаторий",
        "address": "Южная ул., 20, село Рождествено",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "1500–12000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 994-79-57"
          }
        ],
        "sentence": "Обновлённый сталинский санаторий: классические усадьбы, современный главный корпус, и деревянные коттеджи. Дети с бабушками приезжают на всё лето.",
        "coordinate": {
          "lon": 37.042754,
          "lat": 55.853349
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.2,
          "ratings": 96,
          "reviews": 8
        }
      },
      {
        "oid": "1103235335",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "Спа-отель «Бекасово»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/790902/2a0000016167e60b299e546323d5a489b1c1/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636510bfe2dc61b4c6b479a6cfff/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/953593/2a0000016167e5e1fd1ff0edcb38b5510001/%s"
          }
        ],
        "rubric": "Дом отдыха",
        "address": "Россия, Московская область, Наро-Фоминский городской округ, деревня Бекасово",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3500–20000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 992-67-67"
          }
        ],
        "sentence": "Загородный тимбилдинг под ключ: разные виды размещения, конференц-залы, бассейн, каток и собственная культурная программа.",
        "coordinate": {
          "lon": 36.797259,
          "lat": 55.425396
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.2,
          "ratings": 195,
          "reviews": 18
        }
      },
      {
        "oid": "1324352296",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Парк-отель & SPA «Солнечный»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363018/2a00000163423ea6385c1d355993ff81b2c8/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636512e2383b61858303ea1b41b0/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a0000016365137d2622c4441f53b7a9ad89/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Московская область, Солнечногорский район, сельское поселение Смирновское, д. Дулепово",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3600–5300 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (499) 755-88-88"
          }
        ],
        "sentence": "Отель в стиле европейского шале с системой «всё включено». Есть подогреваемый бассейн под открытым небом, особенно живописный зимой.",
        "coordinate": {
          "lon": 36.888466,
          "lat": 56.254697
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.5,
          "ratings": 422,
          "reviews": 112
        }
      },
      {
        "oid": "228997357044",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Конаково Ривер Клаб»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001636523ff27f8245c2b15193f505a/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636524976d403f18502cc60ecd95/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001636524cdce8422f2f483a65db398/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Пригородная ул., 70, Конаково",
        "features": [
          {
            "name": "цена номера",
            "key": "custom",
            "value": "4750–11400 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 626-23-33"
          }
        ],
        "sentence": "Сюда едут кататься. Летом — на яхтах и различных водных приспособлениях, зимой — на лыжах и сноуборде. Выдающаяся инфраструктура для активного отдыха. Катание на лошадях. Для уставших — баня, спа, мастер-классы по керамике.",
        "coordinate": {
          "lon": 36.742461,
          "lat": 56.760467
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 9.7,
          "ratings": 94,
          "reviews": 26
        }
      },
      {
        "oid": "1290676145",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Экоотель «Романов лес»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/911432/2a0000016109e8d656d41fc44c7e4b6a2827/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/903559/2a0000016109e9055d97635be2f47903a8b5/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1031166/2a00000161574038d4ec7a2bfc7ca6d73341/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Костромская область, Костромской район, поселок Лунево, 50",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "4300–8200 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (4942) 49-47-77"
          }
        ],
        "sentence": "Рай для уставшего тела и утомлённой души: множество спа-программ, русская баня, сауна, хаммам и зоопарк в окружении леса.",
        "coordinate": {
          "lon": 41.0872,
          "lat": 57.539617
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 9.1,
          "ratings": 258,
          "reviews": 19
        }
      },
      {
        "oid": "209854965128",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Клуб-отель «ВеЛес»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/467304/2a0000015ed52b2404f48bce0752544f9f02/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/240733/2a0000015ed52b53a34f746c7485a761c03c/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/248099/2a0000015ed52b5cb65efd6ed3d587cbe093/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Владимирская область, Камешковский район, деревня Дворики, 16",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3600–10200 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "8 (800) 775-33-83"
          }
        ],
        "sentence": "Деревянные коттеджи в сосновому лесу. Номера с пуховыми перинами, как у бабушки. Из городских развлечений — боулинг и бильярд.",
        "coordinate": {
          "lon": 40.836961,
          "lat": 56.175861
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 7.8,
          "ratings": 73,
          "reviews": 8
        }
      },
      {
        "oid": "1032757146",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "СПА отель «Свежий ветер»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b19561df7224aaba41ca6cc7344/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b19560b80b423ce8b6d88012e38/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636532fe3865fb6a194d19aa625d/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "74, д. Курово",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3900–19000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 777-26-65"
          }
        ],
        "sentence": "Стильный ландшафтный отель с номерами и домиками разного размера. Здесь можно покататься на лошади и даже на вертолете.",
        "coordinate": {
          "lon": 37.55419,
          "lat": 56.271404
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 9.1,
          "ratings": 624,
          "reviews": 30
        }
      },
      {
        "oid": "149320597246",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "«Бор»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/224414/2a0000015c20ec989d89b6152a13f79b9ca2/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/750770/2a0000016245a8f0a003318999cbcc40df80/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/367512/2a0000015c20ec9957f02d8f21be851358d4/%s"
          }
        ],
        "rubric": "Дом отдыха",
        "address": "Россия, Московская область, городской округ Домодедово, деревня Одинцово",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "4300–12900 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 616-08-05"
          }
        ],
        "sentence": "Немного старомодный пансионат с собственной конюшней и теннисными кортами в сосновом бору площадью больше 100 га.",
        "coordinate": {
          "lon": 37.709927,
          "lat": 55.359905
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.3,
          "ratings": 127,
          "reviews": 2
        }
      },
      {
        "oid": "68336777806",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "Cronwell Park «Яхонты Таруса»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a00000163696f211f76e6a9898f0fa65c4c/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/406255/2a00000162a9955c051d1693005be30a2c19/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1018126/2a00000161486e1ddc6f1cc71c069a2550e9/%s"
          }
        ],
        "rubric": "Дом отдыха",
        "address": "Россия, Калужская область, Жуковский район, деревня Грибовка",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "5250–17600 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "8 (800) 500-47-09"
          }
        ],
        "sentence": "Загородный отель на территории природного заказника Таруса с недорогими, но эффектными номерами и стандартным набором услуг.",
        "coordinate": {
          "lon": 36.83478,
          "lat": 54.934671
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.6,
          "ratings": 325,
          "reviews": 51
        }
      },
      {
        "oid": "1065088986",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Истра Holiday»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/223006/2a0000015b16a157e39732edf7ff443dcde3/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a000001636975c986856293dd4e562570f3/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636a10d9c217ba4de8fe5453fb58/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Московская область, Солнечногорский район, деревня Трусово",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "от 4500 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 775-55-50"
          }
        ],
        "sentence": "Деревянные терема среди ёлок и берез на берегу Истры. Тихо, красиво и комфортно. Летом можно плавать на лодке, зимой ездить на снегоходе или кататься коньках.",
        "coordinate": {
          "lon": 36.865375,
          "lat": 56.040083
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.7,
          "ratings": 190,
          "reviews": 11
        }
      },
      {
        "oid": "128498019628",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "«Йога Дача»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636978dff97f562b1cfc823d4362/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1327602/2a000001636a12087c2937ebd37893b0f636/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a000001636a125afe786edb3fb62736865c/%s"
          }
        ],
        "rubric": "Дом отдыха",
        "address": "Россия, Ярославская область, Переславский район, деревня Городище",
        "features": [
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (916) 408-40-01"
          }
        ],
        "sentence": "Дачный отдых в спартанских условиях для любителей йоги и просветления. Две йога-практики в день и полноценное веганское питание включены в цену проживания.",
        "coordinate": {
          "lon": 38.843128,
          "lat": 56.782552
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "ratings": 0,
          "reviews": 0
        }
      },
      {
        "oid": "214904717121",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "«Лисья нора»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363018/2a00000162d46f939f5d1c427a86f05f51a7/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a00000163698812ec5ee8de4659636a5375/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369885dcf53801fbdd2a6dbd2f3/%s"
          }
        ],
        "rubric": "Дом отдыха",
        "address": "Россия, Московская область, Дмитровский район, городское поселение Дмитров, село Игнатово, вл. 404",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3500–12000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 995-13-84"
          }
        ],
        "sentence": "Отель-туннель с дизайнерским интерьером и новой мебелью. В ресторане — блюда из дичи и большой выбор вин, из развлечений — тир, рыбалка и спа-программы.",
        "coordinate": {
          "lon": 37.543778,
          "lat": 56.167312
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "ratings": 0,
          "reviews": 0
        }
      },
      {
        "oid": "140271757935",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Шале-отель «Таежные дачи»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b17a9c476801216a79a482c08cb/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a00000163698e4f9b9b56f7e4c0d87de4de/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/912415/2a00000163698db86a14307d451fa1b2372f/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Московская область, Звенигород, поселок Санаторий Министерства Обороны, поселок дома отдыха Связист",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "1800–9300 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 411-99-72"
          }
        ],
        "sentence": "Приемлемый сервис в интерьерах избушки на курьих ножках с альпийскими мотивами. Кроме стандартных бань, предлагают «таёжное спа» — с использование лопухов, ивовых прутьев и пивных обливаний.",
        "coordinate": {
          "lon": 36.827998,
          "lat": 55.733858
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 6.9,
          "ratings": 17,
          "reviews": 2
        }
      },
      {
        "oid": "37209529469",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Артурс Village Spa Отель»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b866d69b3f0c5fabfa2cce185b0/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b866a0a83023a5ee982036a7378/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/236825/2a0000015b866b356b72bf34bba5b1a678e1/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Хвойная ул., с26, д. Ларёво, дачный кооператив Космос",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "6100–22400 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 150-41-21"
          }
        ],
        "sentence": "И поработать, и отдохнуть — количество парных соревнуется с количеством конференц-залов.",
        "coordinate": {
          "lon": 37.540009,
          "lat": 56.093744
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.5,
          "ratings": 433,
          "reviews": 21
        }
      },
      {
        "oid": "28557284021",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Велегож Парк»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1357607/2a0000016369ad8ba3721d8084ac25f494a4/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369ae6dcf93cdd7acddd50be1cc/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369aed34c2fb21a85c30da8654a/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Тульская область, Заокский район, деревня Скрипово, коттеджный поселок Велегож-Парк Смарт",
        "features": [
          {
            "name": "Аренда виллы",
            "key": "custom",
            "value": "10000–25000 руб/день"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 664-83-51"
          }
        ],
        "sentence": "Чистенькие домики около Оки. К каждому коттеджу прилагается сауна, мангал и гитара. Поблизости — музей-заповедник В. Д. Поленова и модный ресторан «Марк и Лев».",
        "coordinate": {
          "lon": 37.358699,
          "lat": 54.75171
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 7.4,
          "ratings": 11,
          "reviews": 2
        }
      },
      {
        "oid": "107450640921",
        "tag": "sanatorium",
        "type": "Organization",
        "style": "poor",
        "title": "Оздоровительный комплекс «Десна»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369b1225fb639aa3c8568581475/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369b175e6101b99829ff9e41105/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369b1b92b3d65c2c6c99dab797e/%s"
          }
        ],
        "rubric": "Санаторий",
        "address": "вл1, оздоровительный комплекс Десна",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3540–9740 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 540-43-58"
          }
        ],
        "sentence": "Государственный оздоровительный комплекс с простенькими номерами и территорией в 120 га. Физиотерапия, лечебная физкультура, пруд, катание на лодках и трассы для терренкура.",
        "coordinate": {
          "lon": 37.406669,
          "lat": 55.52144
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.1,
          "ratings": 90,
          "reviews": 5
        }
      },
      {
        "oid": "1367584561",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«ВКС-Кантри»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/200322/2a0000015b2179f6a179e9fd050069993252/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/200322/2a0000015b217b83526e983ebba98dda2dbf/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/216588/2a0000015b217b65d5801d2891e3c9decb53/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Владимирская область, Петушинский район, посёлок Сосновый Бор",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "1800–3840 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 135-00-95"
          }
        ],
        "sentence": "Симпатичные деревянные коттеджи посреди соснового леса, вдали от цивилизации. Для активного отдыха — верёвочный парк, прокат лыж и верховые лошади.",
        "coordinate": {
          "lon": 39.068931,
          "lat": 55.939594
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.3,
          "ratings": 129,
          "reviews": 17
        }
      },
      {
        "oid": "1063164722",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Парк-отель «Олимпиец»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369c206581da89a6f67608a2815/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a0000016369c2937899a12cc7d6ca17a5fc/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/374295/2a0000015b2172afa7c835f538e9025b53e4/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "2, микрорайон Клязьма-Старбеево, квартал Ивакино, Химки",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "от 3900 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 276-13-43"
          }
        ],
        "sentence": "Назад в будущее: олимпийский отель с освежёнными интерьерами восьмидесятых, специализирующийся на деловых мероприятиях.",
        "coordinate": {
          "lon": 37.468972,
          "lat": 55.952063
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.3,
          "ratings": 156,
          "reviews": 22
        }
      },
      {
        "oid": "240477298959",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Welna Eco SPA resort",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/790902/2a0000016287996feccb53e90cf60f9c1307/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1007647/2a0000016287996693fc2488a37d226bd8e8/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/934739/2a0000015ed849106f276f2d8c635dd7b302/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Серпуховское ш., 69, Таруса",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "4500–22500 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 988-09-96"
          }
        ],
        "sentence": "Моднейший подмосковный курорт с разными вариантами размещения, двумя бассейнами и медитативными мастер-классами по керамике и дереву.",
        "coordinate": {
          "lon": 37.165731,
          "lat": 54.747595
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.8,
          "ratings": 321,
          "reviews": 18
        }
      },
      {
        "oid": "1242864809",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "LES Art Resort",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369ca557a86d8236189d8ff7a5a/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369cb4ca300031f28ae07ddbf9d/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369cbb37f2478d0a536047a57de/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Московская область, Рузский городской округ, коттеджный посёлок Руза Резорт",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "7200–9000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 201-05-04"
          }
        ],
        "sentence": "Прелестный отель среди ёлок, где можно ни в чем себе не отказывать: еда, алкоголь, спа и развлечения в неограниченных объемах по системе «всё включено».",
        "coordinate": {
          "lon": 36.356198,
          "lat": 55.520965
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.6,
          "ratings": 528,
          "reviews": 105
        }
      },
      {
        "oid": "238688490401",
        "tag": "restaurants",
        "type": "Organization",
        "style": "poor",
        "title": "Artiland",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363018/2a00000162d475376474d96460573b16b107/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369d1ec459293815200ee1e2462/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1327602/2a0000016369d2c565194a2628b7e6766a81/%s"
          }
        ],
        "rubric": "Ресторан",
        "address": "Новское ш., 10, Балашиха",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "от 6800 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 276-21-21"
          }
        ],
        "sentence": "Загородный клуб, идеальное место для торжеств и конференций. Для семейного отдыха тоже подойдет — на ферме живут кролики, козочки и телята, есть спа, бассейн и тренажерный зал.",
        "coordinate": {
          "lon": 38.050704,
          "lat": 55.794568
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 7.6,
          "ratings": 13,
          "reviews": 16
        }
      },
      {
        "oid": "132739101696",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "«Лес и Море»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1352335/2a00000162d4828e40e83cba35ce42c73ab9/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363376/2a00000162d46cbd325faf8e64631dd5e669/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363250/2a00000162d4962a1c40f50a3fc516e19e7a/%s"
          }
        ],
        "rubric": "Кемпинг",
        "address": "Россия, Тверская область, Калязинский район, остров Спировский",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "5750–17250 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (917) 586-94-63"
          }
        ],
        "sentence": "Палаточный мини-отель на острове — лучший вариант для свадьбы в стиле бохо. Кто хочет быть ближе к природе, но в поход с рюкзаком пока не готов, хорошо проведет здесь выходные.",
        "coordinate": {
          "lon": 37.775545,
          "lat": 57.271897
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 5.7,
          "ratings": 3,
          "reviews": 1
        }
      },
      {
        "oid": "205691505574",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Бутик-отель «Родники»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b16a465f8f42b115d31b30860ec/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/200322/2a0000015b16a45e8746ca99650fbc7f1c48/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/374295/2a0000015b16a4175883b976b38e45062676/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Владимирская область, Александровский район",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "10990–25490 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 212-12-32"
          }
        ],
        "sentence": "Элитный отдых в лесу: в каждом номере камин и терраса с прекрасным видом. За отдельную плату — спа, полеты на вертолёте и экскурсии по Золотому кольцу.",
        "coordinate": {
          "lon": 38.50825,
          "lat": 56.322317
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 9.1,
          "ratings": 38,
          "reviews": 4
        }
      },
      {
        "oid": "1191694528",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Pine River",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/200322/2a0000015b16d030fe611e8daf1fe8e9743a/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369d76c7706aaf88572a78fb651/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/247136/2a0000015b21761ff021f0c09b4ace2f7b83/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Калужская область, Жуковский район, село Восход",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "4000–13300 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (915) 895-81-81"
          }
        ],
        "sentence": "Европейский уголок в русской тишине: уютные стильные номера, в меню омуль, муксун, жаркое из косули и суп из одуванчиков. Летом стартует детский лагерь по мотивам игры «Цивилизация».",
        "coordinate": {
          "lon": 36.77134,
          "lat": 54.955491
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.4,
          "ratings": 110,
          "reviews": 5
        }
      },
      {
        "oid": "1710014400",
        "tag": "restaurants",
        "type": "Organization",
        "style": "poor",
        "title": "«Гуляй город»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369d91f922a934c311d33865cdc/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369d97416bdccabce9e8df690e4/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/216588/2a0000015b167c813cc785287d3adb071c00/%s"
          }
        ],
        "rubric": "Ресторан",
        "address": "Россия, Тульская область, Заокский район, поселок Ланьшинский, Береговая улица",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "1700–12000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (903) 138-14-96"
          }
        ],
        "sentence": "Здесь предлагают глэмпинг — палаточный отдых повышенной комфортности. Также можно арендовать плот и плавать по Оке, как Том Сойер по Миссисипи.",
        "coordinate": {
          "lon": 37.266457,
          "lat": 54.836094
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 7.8,
          "ratings": 19,
          "reviews": 9
        }
      },
      {
        "oid": "1454414463",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Berta Spa Village",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/248099/2a0000016052b50ad8c4cf3341c59c4b7a42/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/402558/2a0000016052b51bf6e0c5591b69a8fba457/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369dcf72ecf26f1597223769d86/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Московская область, городской округ Истра, деревня Ламишино",
        "features": [
          {
            "name": "Аренда коттеджа",
            "key": "custom",
            "value": "14000–28000 руб/день"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 638-51-17"
          }
        ],
        "sentence": "База отдыха премиум-класса повышенной комфортности, заточенная под выездные свадьбы.",
        "coordinate": {
          "lon": 36.821135,
          "lat": 56.009677
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 7.6,
          "ratings": 14,
          "reviews": 1
        }
      },
      {
        "oid": "245644003749",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Отель-ресторан «Частный визит»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363018/2a00000163082e4ebe292a04726b83c4ca3f/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1363376/2a00000163082e50b386611c8032063a76ab/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/481843/2a0000016167b4c176fbcfbea1e35cea5ce2/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "ул. Горная Слобода, 7, Плёс",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "от 6445 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (499) 500-38-08"
          }
        ],
        "sentence": "Мини-отель с чеховским духом, перестроенный из старинного дома на обрыве над Волгой. Хозяйка угощает пирожками собственной выпечки.",
        "coordinate": {
          "lon": 41.51578,
          "lat": 57.456
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.8,
          "ratings": 121,
          "reviews": 13
        }
      },
      {
        "oid": "208885713214",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Art Village Club",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/753950/2a000001612235fbc3badaccb9785ce99b81/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/753950/2a000001612236ebdf2cc6fc3797048c51be/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-altay/1007082/2a00000161223702e0eb1647b9010e952f79/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Рябиновая ул., 1А, д. Голиково",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3490–10490 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (495) 787-24-14"
          }
        ],
        "sentence": "Дизайнерский апарт-отель для загородных свадеб и романтических выходных в любое время года.",
        "coordinate": {
          "lon": 37.31057,
          "lat": 55.923557
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.2,
          "ratings": 33,
          "reviews": 6
        }
      },
      {
        "oid": "142610206596",
        "tag": "hotels",
        "type": "Organization",
        "style": "poor",
        "title": "Эко-отель «Веточка»",
        "images": [
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369e27ba0594591dcf003e83211/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/218162/2a0000016369e2c63b8978e740d040a9b723/%s"
          },
          {
            "urlTemplate": "https://avatars.mds.yandex.net/get-discovery-int/1339925/2a0000016369e38beb970a5be9d310480a7d/%s"
          }
        ],
        "rubric": "Гостиница",
        "address": "Россия, Тверская область, Конаковский район, деревня Устье",
        "features": [
          {
            "name": "цена номера",
            "key": "price_room",
            "value": "3700–10000 руб/ночь"
          },
          {
            "name": "Телефон",
            "key": "phone",
            "value": "+7 (499) 403-39-44"
          }
        ],
        "sentence": "Минималистичные домики-апартаменты на русских просторах. Летом можно летать на гидросамолете над Волгой, зимой кататься на коньках по ней же.",
        "coordinate": {
          "lon": 36.747642,
          "lat": 56.79608
        },
        "description": "",
        "paragraphIcon": {
          "tag": "sanatorium"
        },
        "placemarkIcon": {
          "tag": "sanatorium"
        },
        "businessRating":  {
          "score": 8.9,
          "ratings": 131,
          "reviews": 5
        }
      },
      {
        "type": "Share",
        "style": "large"
      }
    ]
  }
}
"""
