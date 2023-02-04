package ru.yandex.realty.validators

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import play.api.libs.json._
import ru.yandex.realty.transformers.DataSource
import ru.yandex.realty.validators.i18n.Localizer

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 24.05.17
  */
@RunWith(classOf[JUnitRunner])
class JsLocalizedValidatorTest extends FlatSpec with Matchers with DataSource {
  val v = new JsLocalizedValidator("add_offer_data.schema.json")
  val u = new JsLocalizedValidator("vos_user.schema.json")

  private def sellApartmentOffer(dealStatus: Option[String], newFlat: Option[Boolean]): String =
    s"""
       |{
       |        "price": 5000000,
       |        "location": {
       |          "rgid": 417899,
       |          "country": 225,
       |          "address": "Россия, Санкт-Петербург, Парашютная улица, 25к1, подъезд 1",
       |          "subjectFederationId": 10174,
       |          "latitude": 60.01908493,
       |          "longitude": 30.2694416,
       |          "streetAddress": "Парашютная улица, 25к1",
       |          "localityName": "Санкт-Петербург",
       |          "region": "Санкт-Петербург",
       |          "houseNumber": "25к1",
       |          "street": "Парашютная улица",
       |          "point": {
       |            "longitude": 30.2694416,
       |            "latitude": 60.01908493
       |          }
       |        },
       |        "type": "SELL",
       |        "currency": "RUR",
       |        "category": "APARTMENT",
       |        "parkingType": "OPEN",
       |        "area": 76,
       |        "balcony": "BALCONY",
       |        "floors": [5],
       |        "floorsTotal": 10
       |        ${dealStatus.map(ds => s""", "dealStatus": "$ds"""").getOrElse("")}
       |        ${newFlat.map(nf => s""", "newFlat": "$nf"""").getOrElse("")}
       |}
       """.stripMargin

  "JsLocalizedValidator" should "correct build localized messages" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "apartments": 1,
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 3 ],
                 |            "floorsTotal": 100,
                 |            "roomsTotalApartment": "4",
                 |            "area": 200,
                 |            "rooms": [ 35 ],
                 |            "livingSpace": 140,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.head.parameter should be("/apartments")
    result.errors.head.localizedDescription should include("Некорректный тип данных")
  }

  it should "correct work with complex format attribute: required_rooms" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "ROOMS",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "apartments": true,
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 3 ],
                 |            "floorsTotal": 100,
                 |            "roomsTotalApartment": "4",
                 |            "area": 200,
                 |            "livingSpace": 140,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            }
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_ROOMS")
    result.valid should be(false)
  }

  it should "correct report about multiple errors" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "apartments": 1,
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 10 ],
                 |            "floorsTotal": 200,
                 |            "roomsTotalApartment": "4",
                 |            "area": 200,
                 |            "rooms": [ 35, 35 ],
                 |            "livingSpace": 201,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.size should be(3)
  }

  it should "correct return success validation" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "apartments": true,
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 3 ],
                 |            "floorsTotal": 10,
                 |            "roomsTotalApartment": "4",
                 |            "area": 200,
                 |            "rooms": [ 35, 35 ],
                 |            "livingSpace": 140,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    println(json)
    v.validate(json, "/definitions/SELL_APARTMENT").valid should be(true)
  }

  it should "correct block offer by floor_floorsTotal" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "apartments": true,
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 11 ],
                 |            "floorsTotal": 10,
                 |            "roomsTotalApartment": "4",
                 |            "area": 200,
                 |            "rooms": [ 35, 35 ],
                 |            "livingSpace": 140,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.size should be(1)
    result.errors.head.code should be("custom_floor_floorsTotal")
    result.errors.head.parameter should be("/floors")
    result.errors.head.details should be(Some(Details(Seq("/floorsTotal"))))
  }

  it should "correct check category_roomsTotalApartment_OPEN_PLAN" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "apartments": true,
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 1 ],
                 |            "floorsTotal": 10,
                 |            "roomsTotalApartment": "OPEN_PLAN",
                 |            "area": 200,
                 |            "rooms": [ 35, 35 ],
                 |            "livingSpace": 140,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.size should be(1)
    result.errors.head.code should be("custom_category_roomsTotalApartment_OPEN_PLAN")
  }

  it should "should correct check RENT ROOMS" in {
    val json =
      """
        |{
        |            "type": "RENT",
        |            "category": "ROOMS",
        |            "location": {
        |                "address": "Россия, Москва, Климентовский переулок, 2",
        |                "latitude": 55.74107361,
        |                "longitude": 37.63123703,
        |                "country": 225,
        |                "rgid": 193301
        |            },
        |            "builtYear": 2010,
        |            "lift": true,
        |            "rubbishChute": true,
        |            "alarm": false,
        |            "passBy": false,
        |            "parkingType": "SECURE",
        |            "floors": [7],
        |            "floorsTotal": 10,
        |            "roomsOffered": "1",
        |            "roomsTotal": "3",
        |            "area": 100,
        |            "rooms": [ 20 ],
        |            "kitchenSpace": 30,
        |            "ceilingHeight": 4,
        |            "bathroomUnit": "SEPARATED",
        |            "windowView": [ "YARD" ],
        |            "floorCovering": "LINOLEUM",
        |            "renovation": "EURO",
        |            "phone": false,
        |            "internet": false,
        |            "washingMachine": false,
        |            "roomFurniture": false,
        |            "dishwasher": true,
        |            "kitchenFurniture": true,
        |            "buildInTech": false,
        |            "aircondition": false,
        |            "refrigerator": true,
        |            "flatAlarm": false,
        |            "television": false,
        |            "withPets": true,
        |            "balcony": "BALCONY_LOGGIA",
        |            "withChildren": false,
        |            "price": 2000,
        |            "currency": "RUR",
        |            "period": "PER_DAY",
        |            "haggle": false,
        |            "rentPledge": true,
        |            "utilitiesIncluded": true,
        |            "prepayment": 60,
        |            "agentFee": 40,
        |            "description": "Все хорошо, покупайте ",
        |            "photo": [
        |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |            ],
        |            "siteId": {
        |                "name": "ЖК «Купеческая усадьба»",
        |                "id": "223181"
        |            }
        |        }
      """.stripMargin
    val result = v.validate(json, "/definitions/RENT_ROOMS")
    result.valid should be(true)
  }

  it should "should correct block for roomsOffered_roomsTotal_equal" in {
    val json =
      """
        |{
        |            "type": "RENT",
        |            "category": "ROOMS",
        |            "location": {
        |                "address": "Россия, Москва, Климентовский переулок, 2",
        |                "latitude": 55.74107361,
        |                "longitude": 37.63123703,
        |                "country": 225,
        |                "rgid": 193301
        |            },
        |            "builtYear": 2010,
        |            "lift": true,
        |            "rubbishChute": true,
        |            "alarm": false,
        |            "passBy": false,
        |            "parkingType": "SECURE",
        |            "floors": [7],
        |            "floorsTotal": 10,
        |            "roomsOffered": "2",
        |            "roomsTotal": "3",
        |            "area": 100,
        |            "rooms": [ 20, 15, 16, 17 ],
        |            "kitchenSpace": 30,
        |            "ceilingHeight": 4,
        |            "bathroomUnit": "SEPARATED",
        |            "windowView": [ "YARD" ],
        |            "floorCovering": "LINOLEUM",
        |            "renovation": "EURO",
        |            "phone": false,
        |            "internet": false,
        |            "washingMachine": false,
        |            "roomFurniture": false,
        |            "dishwasher": true,
        |            "kitchenFurniture": true,
        |            "buildInTech": false,
        |            "aircondition": false,
        |            "refrigerator": true,
        |            "flatAlarm": false,
        |            "television": false,
        |            "withPets": true,
        |            "balcony": "BALCONY_LOGGIA",
        |            "withChildren": false,
        |            "price": 2000,
        |            "currency": "RUR",
        |            "period": "PER_DAY",
        |            "haggle": false,
        |            "rentPledge": true,
        |            "utilitiesIncluded": true,
        |            "prepayment": 60,
        |            "agentFee": 40,
        |            "description": "Все хорошо, покупайте ",
        |            "photo": [ "https://avatars.mdst.yandex.net/get-realty/2991/add.1482003899394b294a9c178/orig" ],
        |            "siteId": {
        |                "name": "ЖК «Купеческая усадьба»",
        |                "id": "223181"
        |            }
        |        }
      """.stripMargin
    val result = v.validate(json, "/definitions/RENT_ROOMS")
    result.valid should be(false)
    result.errors.head.code should be("custom_rooms_roomsOffered_roomsTotal")
  }

  it should "correct check area_livingSpace_kitchenSpace" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 3 ],
                 |            "floorsTotal": 100,
                 |            "roomsTotalApartment": "4",
                 |            "area": 135,
                 |            "rooms": [ 35 ],
                 |            "livingSpace": 140,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            }
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.head.code should be("custom_area_livingSpace_kitchenSpace")
  }

  it should "correct check rooms_livingSpace" in {
    val json =
      """
                 |{
                 |            "type": "SELL",
                 |            "category": "APARTMENT",
                 |            "location": {
                 |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                 |                "longitude": 30.38843346,
                 |                "latitude": 60.01175308,
                 |                "country": 225,
                 |                "rgid": 417899
                 |            },
                 |            "buildingType": "MONOLIT",
                 |            "builtYear": 2010,
                 |            "newFlat": false,
                 |            "lift": true,
                 |            "rubbishChute": true,
                 |            "alarm": true,
                 |            "passBy": true,
                 |            "parkingType": "SECURE",
                 |            "floors": [ 3 ],
                 |            "floorsTotal": 10,
                 |            "roomsTotalApartment": "4",
                 |            "area": 100,
                 |            "rooms": [ 20, 20 ],
                 |            "livingSpace": 35,
                 |            "kitchenSpace": 30,
                 |            "ceilingHeight": 2,
                 |            "bathroomUnit": "SEPARATED",
                 |            "balcony": "BALCONY_LOGGIA",
                 |            "windowView": [ "YARD", "STREET" ],
                 |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                 |            "renovation": "EURO",
                 |            "phone": true,
                 |            "internet": true,
                 |            "roomFurniture": true,
                 |            "kitchenFurniture": true,
                 |            "buildInTech": true,
                 |            "aircondition": true,
                 |            "refrigerator": true,
                 |            "price": 10000000,
                 |            "currency": "RUR",
                 |            "haggle": true,
                 |            "mortgage": true,
                 |            "dealType": "DIRECT",
                 |            "apartment": "10",
                 |            "description": "Хорошая квартира, хороший ремонт, хорошие соседи, хороший дом",
                 |            "photo": [
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
                 |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.size should be(1)
    result.errors.head.code should be("custom_rooms_livingSpace")
  }

  it should "should correct block for rent_open_plan" in {
    val json =
      """
        |{
        |            "type": "RENT",
        |            "category": "APARTMENT",
        |            "location": {
        |                "address": "Россия, Москва, Климентовский переулок, 2",
        |                "latitude": 55.74107361,
        |                "longitude": 37.63123703,
        |                "country": 225,
        |                "rgid": 193301
        |            },
        |            "builtYear": 2010,
        |            "lift": true,
        |            "rubbishChute": true,
        |            "alarm": false,
        |            "passBy": false,
        |            "parkingType": "SECURE",
        |            "floors": [7],
        |            "floorsTotal": 10,
        |            "roomsTotalApartment": "OPEN_PLAN",
        |            "area": 100,
        |            "kitchenSpace": 30,
        |            "ceilingHeight": 4,
        |            "bathroomUnit": "SEPARATED",
        |            "windowView": [ "YARD" ],
        |            "floorCovering": "LINOLEUM",
        |            "renovation": "EURO",
        |            "phone": false,
        |            "internet": false,
        |            "washingMachine": false,
        |            "roomFurniture": false,
        |            "dishwasher": true,
        |            "kitchenFurniture": true,
        |            "buildInTech": false,
        |            "aircondition": false,
        |            "refrigerator": true,
        |            "flatAlarm": false,
        |            "television": false,
        |            "withPets": true,
        |            "balcony": "BALCONY_LOGGIA",
        |            "withChildren": false,
        |            "price": 2000,
        |            "currency": "RUR",
        |            "period": "PER_DAY",
        |            "haggle": false,
        |            "rentPledge": true,
        |            "utilitiesIncluded": true,
        |            "prepayment": 60,
        |            "agentFee": 40,
        |            "description": "Все хорошо, покупайте ",
        |            "photo": [
        |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |            ],
        |            "siteId": {
        |                "name": "ЖК «Купеческая усадьба»",
        |                "id": "223181"
        |            }
        |        }
      """.stripMargin
    val result = v.validate(json, "/definitions/RENT_APARTMENT")
    result.valid should be(false)
    result.errors.size should be(1)
    result.errors.head.code should be("custom_rent_open_plan")
  }

  it should "support parkingType OPEN for SELL APARTMENT" in {
    val json =
      s"""
         |{
         |        "price": 5000000,
         |        "location": {
         |          "rgid": 417899,
         |          "country": 225,
         |          "address": "Россия, Санкт-Петербург, Парашютная улица, 25к1, подъезд 1",
         |          "subjectFederationId": 10174,
         |          "latitude": 60.01908493,
         |          "longitude": 30.2694416,
         |          "streetAddress": "Парашютная улица, 25к1",
         |          "localityName": "Санкт-Петербург",
         |          "region": "Санкт-Петербург",
         |          "houseNumber": "25к1",
         |          "street": "Парашютная улица",
         |          "point": {
         |            "longitude": 30.2694416,
         |            "latitude": 60.01908493
         |          }
         |        },
         |        "type": "SELL",
         |        "currency": "RUR",
         |        "category": "APARTMENT",
         |        "parkingType": "OPEN",
         |        "area": 76,
         |        "balcony": "BALCONY",
         |        "floors": [5],
         |        "floorsTotal": 10,
         |        "newFlat": false,
         |        "dealStatus": "SALE",
         |        "roomsTotalApartment": "1",
         |        "photo": [
         |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
         |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
         |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
         |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
         |            ]
         |}
       """.stripMargin
    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(true)
  }

  it should "correct build localized messages for missing newFlat in SELL APARTMENT" in {
    val json = sellApartmentOffer(dealStatus = Some("REASSIGNMENTREASSIGNMENT"), newFlat = None)

    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid should be(false)
    result.errors.head.localizedDescription shouldBe "Обязательное поле"
  }

  it should "correct build localized messages for reassignment with false newFlat" in {
    val json = sellApartmentOffer(dealStatus = Some("REASSIGNMENT"), newFlat = Some(false))

    val result = v.validate(json, "/definitions/SELL_APARTMENT")
    result.valid shouldBe false
    result.errors.head.localizedDescription shouldBe Localizer.getKey("dealStatus_reassignment")
    result.errors.head.parameter shouldBe "/dealStatus"
  }

  it should "correct build localized messages for sale dealStatus with true newFlat" in {

    Seq(
      sellApartmentOffer(dealStatus = Some("SALE"), newFlat = Some(true)),
      sellApartmentOffer(dealStatus = Some("COUNTERSALE"), newFlat = Some(true))
    ).map(v.validate(_, "/definitions/SELL_APARTMENT")).foreach { result =>
      result.valid shouldBe false
      result.errors.head.parameter shouldBe "/dealStatus"
      result.errors.head.localizedDescription shouldBe Localizer.getKey("newFlat_dealStatusSale")
    }
  }

  it should "support parkingType CLOSED for RENT ROOMS" in {
    val json =
      s"""
         |{
         |        "agentFee": 100,
         |        "aircondition": true,
         |        "buildInTech": true,
         |        "currency": "RUR",
         |        "dishwasher": true,
         |        "flatAlarm": true,
         |        "haggle": true,
         |        "internet": true,
         |        "kitchenFurniture": true,
         |        "period": "PER_MONTH",
         |        "phone": true,
         |        "prepayment": 120,
         |        "price": 15000,
         |        "refrigerator": true,
         |        "rentPledge": true,
         |        "roomFurniture": true,
         |        "television": true,
         |        "utilitiesIncluded": true,
         |        "washingMachine": true,
         |        "withChildren": true,
         |        "withPets": true,
         |        "alarm": true,
         |        "area": 100.0,
         |        "balcony": "TWO_BALCONY",
         |        "bathroomUnit": "SEPARATED",
         |        "builtYear": 1860,
         |        "ceilingHeight": 4.0,
         |        "floorCovering": "PARQUET",
         |        "floors": [
         |            2
         |        ],
         |        "floorsTotal": 23,
         |        "kitchenSpace": 12.0,
         |        "lift": true,
         |        "parkingType": "CLOSED",
         |        "passBy": true,
         |        "renovation": "EURO",
         |        "rooms": [
         |            10.0,
         |            11.0
         |        ],
         |        "roomsOffered": "2",
         |        "roomsTotal": "3",
         |        "rubbishChute": true,
         |        "windowView": [
         |            "YARD",
         |            "STREET"
         |        ],
         |        "category": "ROOMS",
         |        "description": "текст",
         |        "location": {
         |            "address": "Россия, Ленинградская область, Всеволожский район, Муринское сельское поселение",
         |            "country": 225,
         |            "latitude": 60.06233,
         |            "longitude": 30.447166,
         |            "rgid": 407445,
         |            "shortAddress": "Муринское сельское поселение"
         |        },
         |        "type": "RENT",
         |            "photo": [
         |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
         |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
         |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
         |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
         |            ]
         |}
       """.stripMargin
    val result = v.validate(json, "/definitions/RENT_ROOMS")
    result.valid should be(true)
  }

  it should "should correct check all_offer_fields" in {
    val json =
      """
        |{
        |  "offer": {
        |            "type": "RENT",
        |            "category": "ROOMS",
        |            "location": {
        |                "address": "Россия, Москва, Климентовский переулок, 2",
        |                "latitude": 55.74107361,
        |                "longitude": 37.63123703,
        |                "country": 225,
        |                "rgid": 193301
        |            },
        |            "builtYear": 2010,
        |            "lift": true,
        |            "rubbishChute": true,
        |            "alarm": false,
        |            "passBy": false,
        |            "parkingType": "SECURE",
        |            "floors": [7],
        |            "floorsTotal": 10,
        |            "roomsOffered": "1",
        |            "roomsTotal": "3",
        |            "area": 100,
        |            "rooms": [ 20 ],
        |            "kitchenSpace": 30,
        |            "ceilingHeight": 4,
        |            "bathroomUnit": "SEPARATED",
        |            "windowView": [ "YARD" ],
        |            "floorCovering": "LINOLEUM",
        |            "renovation": "EURO",
        |            "phone": false,
        |            "internet": false,
        |            "washingMachine": false,
        |            "roomFurniture": false,
        |            "dishwasher": true,
        |            "kitchenFurniture": true,
        |            "buildInTech": false,
        |            "aircondition": false,
        |            "refrigerator": true,
        |            "flatAlarm": false,
        |            "television": false,
        |            "withPets": true,
        |            "balcony": "BALCONY_LOGGIA",
        |            "withChildren": false,
        |            "price": 2000,
        |            "currency": "RUR",
        |            "period": "PER_DAY",
        |            "haggle": false,
        |            "rentPledge": true,
        |            "utilitiesIncluded": true,
        |            "prepayment": 60,
        |            "agentFee": 40,
        |            "description": "Все хорошо, покупайте ",
        |            "photo": [
        |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |            ],
        |            "siteId": {
        |                "name": "ЖК «Купеческая усадьба»",
        |                "id": "223181"
        |            }
        |      }
        |  }
      """.stripMargin
    val result = v.validate(json, "/definitions/all_offer_fields")
    result.valid should be(true)
  }

  it should "correct build localized messages for houseType" in {
    val json =
      """
        |{"offer": {
        |        "price": 5000000,
        |        "location": {
        |          "rgid": 417899,
        |          "country": 225,
        |          "address": "Россия, Санкт-Петербург, Парашютная улица, 25к1, подъезд 1",
        |          "subjectFederationId": 10174,
        |          "latitude": 60.01908493,
        |          "longitude": 30.2694416,
        |          "streetAddress": "Парашютная улица, 25к1",
        |          "localityName": "Санкт-Петербург",
        |          "region": "Санкт-Петербург",
        |          "houseNumber": "25к1",
        |          "street": "Парашютная улица",
        |          "point": {
        |            "longitude": 30.2694416,
        |            "latitude": 60.01908493
        |          }
        |        },
        |        "type": "SELL",
        |        "lotType": "IGS",
        |        "currency": "RUR",
        |        "category": "HOUSE",
        |        "houseType": "BAD_HOUSE_TYPE",
        |        "houseArea": 200.0,
        |        "dealStatus": "SALE",
        |        "photo": [
        |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |            ]
        |      }
        |}
      """.stripMargin
    val result1 = v.validate(json, "/definitions/all_offer_fields")
    result1.valid should be(false)
    result1.errors.head.parameter should be("/offer/houseType")
    result1.errors.head.localizedDescription should include("Недопустимое значение")
    val parsed = Json.parse(json)
    val offerJson = (parsed \ "offer").get.toString()
    val result = v.validate(offerJson, "/definitions/SELL_HOUSE")
    result.valid should be(false)
    result.errors.head.parameter should be("/houseType")
    result.errors.head.localizedDescription should include("Недопустимое значение")
  }

  it should "correct return success validation for rent house" in {
    val json =
      """
        |{"offer": {
        |        "price": 5000000,
        |        "location": {
        |          "rgid": 417899,
        |          "country": 225,
        |          "address": "Россия, Санкт-Петербург, Парашютная улица, 25к1, подъезд 1",
        |          "subjectFederationId": 10174,
        |          "latitude": 60.01908493,
        |          "longitude": 30.2694416,
        |          "streetAddress": "Парашютная улица, 25к1",
        |          "localityName": "Санкт-Петербург",
        |          "region": "Санкт-Петербург",
        |          "houseNumber": "25к1",
        |          "street": "Парашютная улица",
        |          "point": {
        |            "longitude": 30.2694416,
        |            "latitude": 60.01908493
        |          }
        |        },
        |        "type": "RENT",
        |        "lotType": "IGS",
        |        "currency": "RUR",
        |        "category": "HOUSE",
        |        "photo": [
        |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |                "https://avatars.mdst.yandex.net/get-realty/2991/add.1482003899394b294a9c178/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |            ],
        |        "houseType": "PARTHOUSE",
        |        "houseArea": 200.0
        |      }
        |}
      """.stripMargin
    val result1 = v.validate(json, "/definitions/all_offer_fields")
    result1.valid should be(true)
    val parsed = Json.parse(json)
    val offerJson = (parsed \ "offer").get.toString()
    val result = v.validate(offerJson, "/definitions/RENT_HOUSE")
    result.valid should be(true)
  }

  it should "correct  return success validation for sell garage" in {
    val json = Json.obj("offer" -> FilterStateEditSellGarage2).toString()
    val result1 =
      v.validate(json, "/definitions/all_offer_fields")

    result1.valid should be(true)
    val parsed = Json.parse(json)
    val offerJson = (parsed \ "offer").get.toString()
    val result = v.validate(offerJson, "/definitions/SELL_GARAGE")
    println(result.errors)
    result.valid should be(true)
  }

  it should "correct build localized messages for videoReviewUrl" in {
    val json =
      """
        | {
        |        "price": 5000000,
        |        "location": {
        |          "rgid": 417899,
        |          "country": 225,
        |          "address": "Россия, Санкт-Петербург, Парашютная улица, 25к1, подъезд 1",
        |          "subjectFederationId": 10174,
        |          "latitude": 60.01908493,
        |          "longitude": 30.2694416,
        |          "streetAddress": "Парашютная улица, 25к1",
        |          "localityName": "Санкт-Петербург",
        |          "region": "Санкт-Петербург",
        |          "houseNumber": "25к1",
        |          "street": "Парашютная улица",
        |          "point": {
        |            "longitude": 30.2694416,
        |            "latitude": 60.01908493
        |          }
        |        },
        |        "photo": [
        |             "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |             "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
        |             "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |             "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |         ],
        |        "type": "SELL",
        |        "lotType": "IGS",
        |        "currency": "RUR",
        |        "category": "HOUSE",
        |        "houseType": "PARTHOUSE",
        |        "videoReviewUrl" : "bad url",
        |        "houseArea": 200.0
        |      }
        |
      """.stripMargin
    val result1 = v.validate(json, "/definitions/SELL_HOUSE")
    result1.valid should be(false)
    result1.errors.head.parameter should be("/videoReviewUrl")
    result1.errors.head.localizedDescription should include("Укажите корректную ссылку")
  }

  it should "correct return success with right videoReviewUrl field value" in {
    val json =
      """
        |{"offer": {
        |        "price": 5000000,
        |        "location": {
        |          "rgid": 417899,
        |          "country": 225,
        |          "address": "Россия, Санкт-Петербург, Парашютная улица, 25к1, подъезд 1",
        |          "subjectFederationId": 10174,
        |          "latitude": 60.01908493,
        |          "longitude": 30.2694416,
        |          "streetAddress": "Парашютная улица, 25к1",
        |          "localityName": "Санкт-Петербург",
        |          "region": "Санкт-Петербург",
        |          "houseNumber": "25к1",
        |          "street": "Парашютная улица",
        |          "point": {
        |            "longitude": 30.2694416,
        |            "latitude": 60.01908493
        |          }
        |        },
        |        "type": "SELL",
        |        "lotType": "IGS",
        |        "currency": "RUR",
        |        "category": "HOUSE",
        |        "houseType": "PARTHOUSE",
        |        "videoReviewUrl" : "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        |        "houseArea": 200.0,
        |        "photo": [
        |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f",
        |                "https://avatars.mdst.yandex.net/get-realty/2899/add.1567762286852d89bb806ef/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos",
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig"
        |            ]
        |      }
        |}
      """.stripMargin
    val result1 = v.validate(json, "/definitions/all_offer_fields")
    result1.valid should be(true)
  }

  it should "correct check simple user scheme" in {
    val json =
      """
        |{
        |  "user": {
        |    "phones": [{"id": "+79110047689", "select": true}],
        |    "telephones": ["+79110047689"]
        |  }
        |}
      """.stripMargin
    val result = u.validate(json, "")
    result.valid should be(true)

    val res = u.validate("""{"user": {"fake": "val", "email": 5}}""", "")
    res.valid should be(false)
    res.errors.size should be(4)
  }

  it should "correct catch select_phones error" in {
    val json =
      """
        |{
        |  "user": {
        |    "phones": [],
        |    "telephones": ["+79110047689"]
        |  }
        |}
      """.stripMargin
    val result = u.validate(json, "")
    result.valid should be(false)
    result.errors.size should be(2)
    result.errors.exists(_.code == "custom_phones_min_items") should be(true)
    result.errors.exists(_.code == "custom_select_phones") should be(true)
  }

  it should "correct catch select_vos_phones error" in {
    val json =
      """
        |{
        |  "user": {
        |    "phones": [{"id": "+79110047689", "select": true}],
        |    "telephones": []
        |  }
        |}
      """.stripMargin
    val result = u.validate(json, "")
    result.valid should be(false)
    result.errors.size should be(1)
    result.errors.head.code should be("custom_select_vos_phones")
  }

  it should "correct catch user_name error" in {
    val json =
      """
        |{
        |  "user": {
        |    "phones": [{"id": "+79110047689", "select": true}],
        |    "telephones": ["+79110047689"],
        |    "name": ""
        | }
        |}
      """.stripMargin
    val result = u.validate(json, "")
    result.valid should be(false)
    result.errors.size should be(1)
    result.errors.head.code should be("custom_user_name")
  }
}
