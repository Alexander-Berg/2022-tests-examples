package ru.yandex.realty.validators

import java.net.URL

import org.scalatest.{FlatSpec, Matchers}
import com.github.fge.jackson.JsonLoader
import com.github.fge.jsonschema.core.report.ProcessingReport
import com.github.fge.jsonschema.main.{JsonSchema, JsonSchemaFactory}
import com.github.fge.jsonschema.core.report.LogLevel
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.Json

import scala.collection.JavaConverters._

/**
  * Created by Anton Ivanov <antonio@yandex-team.ru> on 19.05.17
  */
@RunWith(classOf[JUnitRunner])
class ValidatorTest extends FlatSpec with Matchers {
  val factory = JsonSchemaFactory.byDefault()
  val classLoader: ClassLoader = classOf[JsonLoader].getClassLoader
  val uri: URL = classLoader.getResource("add_offer_data.schema.json")
  val schema: JsonSchema = factory.getJsonSchema(JsonLoader.fromURL(uri), "/definitions/SELL_APARTMENT") //factory.getJsonSchema(uri.toString)

  it should "correct check offer" in {
    schema
      .validate(
        JsonLoader
          .fromString(
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
        |            "rooms": [ 35, 35, 35, 35 ],
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
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos"
        |            ],
        |            "siteId": {
        |                "name": "ЖК «Орбита»",
        |                "id": "17723"
        |            },
        |            "dealStatus": "SALE"
        |        }
      """.stripMargin
          )
      )
      .isSuccess should be(true)
  }

  it should "correct check offer RENT COMMERCIAL" in {
    val localSchema: JsonSchema = factory.getJsonSchema(JsonLoader.fromURL(uri), "/definitions/RENT_COMMERCIAL") //factory.getJsonSchema(uri.toString)
    localSchema
      .validate(
        JsonLoader
          .fromString(
            """
              |{
              |            "type": "RENT",
              |            "category": "COMMERCIAl",
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
              |            "rooms": [ 35, 35, 35, 35 ],
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
              |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos"
              |            ],
              |            "siteId": {
              |                "name": "ЖК «Орбита»",
              |                "id": "17723"
              |            },
              |            "dealStatus": "SALE"
              |        }
      """.stripMargin
          )
      )
      .isSuccess should be(true)
  }

  it should "fail check offer SELL GARAGE with 1 photo only" in {
    val localSchema: JsonSchema = factory.getJsonSchema(JsonLoader.fromURL(uri), "/definitions/SELL_GARAGE") //factory.getJsonSchema(uri.toString)
    val r = localSchema
      .validate(
        JsonLoader
          .fromString(
            """
              |{
              |            "type": "SELL",
              |            "category": "GARAGE",
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
              |            "rooms": [ 35, 35, 35, 35 ],
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
              |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f"
              |            ],
              |            "siteId": {
              |                "name": "ЖК «Орбита»",
              |                "id": "17723"
              |            },
              |            "dealStatus": "SALE"
              |        }
      """.stripMargin
          )
      )
    r.isSuccess should be(false)
  }

  it should "correct check offer SELL GARAGE" in {
    val localSchema: JsonSchema = factory.getJsonSchema(JsonLoader.fromURL(uri), "/definitions/SELL_GARAGE") //factory.getJsonSchema(uri.toString)
    val r = localSchema
      .validate(
        JsonLoader
          .fromString(
            """
              |{
              |            "type": "SELL",
              |            "category": "GARAGE",
              |            "location": {
              |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
              |                "longitude": 30.38843346,
              |                "latitude": 60.01175308,
              |                "country": 225,
              |                "rgid": 417899
              |            },
              |            "garageOwnership": "PRIVATE",
              |            "cctv": true,
              |            "garageType": "GARAGE",
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
              |            "rooms": [ 35, 35, 35, 35 ],
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
              |            "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig",
              |                "avatars.mdst.yandex.net/get-realty/2941/add.1481646424772121cf6817f"
              |            ],
              |            "siteId": {
              |                "name": "ЖК «Орбита»",
              |                "id": "17723"
              |            },
              |            "dealStatus": "SALE"
              |        }
      """.stripMargin
          )
      )
    r.isSuccess should be(true)
  }

  "Validator" should "fail check offer SELL GARAGE with no photo" in {
    val localSchema: JsonSchema = factory.getJsonSchema(JsonLoader.fromURL(uri), "/definitions/SELL_GARAGE") //factory.getJsonSchema(uri.toString)
    val json =
      """
        |{
        |            "type": "SELL",
        |            "category": "GARAGE",
        |            "location": {
        |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
        |                "longitude": 30.38843346,
        |                "latitude": 60.01175308,
        |                "country": 225,
        |                "rgid": 417899
        |            },
        |            "buildingType": "MONOLIT",
        |            "apartments": 1,
        |            "garageOwnership": "PRIVATE",
        |            "cctv": true,
        |            "garageType": "GARAGE",
        |            "builtYear": 2010,
        |            "newFlat": false,
        |            "lift": true,
        |            "rubbishChute": true,
        |            "alarm": true,
        |            "passBy": true,
        |            "parkingType": "SECURE",
        |            "floors": [ 3 ],
        |            "floorsTotal": 200,
        |            "roomsTotalApartment": "4",
        |            "area": 200,
        |            "rooms": [ 35, 35, 35, 35 ],
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
        |            "siteId": {
        |                "name": "ЖК «Орбита»",
        |                "id": "17723"
        |            },
        |            "dealStatus": "SALE"
        |        }
               """.stripMargin
    val r = localSchema
      .validate(
        JsonLoader
          .fromString(json)
      )
    r.isSuccess should be(false)
  }

  it should "correct reposrt error about buildingType" in {
    schema
      .validate(
        JsonLoader
          .fromString(
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
        |            "buildingType": "FAKE",
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
        |            "rooms": [ 35, 35, 35, 35 ],
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
        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig",
        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos"
        |            ],
        |            "siteId": {
        |                "name": "ЖК «Орбита»",
        |                "id": "17723"
        |            },
        |            "garageOwnership": "PRIVATE",
        |            "cctv": true,
        |            "garageType": "GARAGE"
        |        }
      """.stripMargin
          )
      )
      .isSuccess should be(false)
  }

  it should "sell rooms success" in {
    val schemaSellRooms: JsonSchema = factory.getJsonSchema(JsonLoader.fromURL(uri), "/definitions/SELL_ROOMS")
    val res = schemaSellRooms
      .validate(
        JsonLoader
          .fromString(
            """
                        |{
                        |            "type": "SELL",
                        |            "dealStatus": "SALE",
                        |            "category": "ROOMS",
                        |            "location": {
                        |                "address": "Россия, Санкт-Петербург, Гжатская улица, 22к1",
                        |                "longitude": 30.38843346,
                        |                "latitude": 60.01175308,
                        |                "country": 225,
                        |                "rgid": 417899
                        |            },
                        |            "buildingType": "BLOCK",
                        |            "apartments": true,
                        |            "builtYear": 2010,
                        |            "lift": true,
                        |            "rubbishChute": true,
                        |            "alarm": true,
                        |            "roomsOffered": "2",
                        |            "passBy": true,
                        |            "parkingType": "SECURE",
                        |            "floors": [ 3 ],
                        |            "floorsTotal": 10,
                        |            "area": 200,
                        |            "rooms": [ 35, 35, 35, 35 ],
                        |            "kitchenSpace": 30,
                        |            "ceilingHeight": 2,
                        |            "bathroomUnit": "SEPARATED",
                        |            "windowView": [ "YARD", "STREET" ],
                        |            "floorCovering": "LAMINATED_FLOORING_BOARD",
                        |            "renovation": "EURO",
                        |            "phone": true,
                        |            "internet": true,
                        |            "roomFurniture": true,
                        |            "roomsTotal": "2",
                        |            "kitchenFurniture": true,
                        |            "buildInTech": true,
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
                        |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig",
                        |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos"
                        |            ],
                        |            "siteId": {
                        |                "name": "ЖК «Орбита»",
                        |                "id": "17723"
                        |            }
                        |        }
      """.stripMargin
          )
      )
    res.isSuccess should be(true)
  }

  it should "correct report error about floorsTotal and parkingType" in {
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
                 |            "floorsTotal": 200,
                 |            "roomsTotalApartment": "4",
                 |            "area": 200,
                 |            "rooms": [ 35, 35, 35, 35 ],
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
                 |                "https://avatars.mdst.yandex.net/get-realty/2935/add.156751502907523f88e2be0/orig",
                 |                "avatars.mdst.yandex.net/get-realty/2941/add.ffaf37666a1b9e08656ccfbf125a31fa.realty-api-vos"
                 |            ],
                 |            "siteId": {
                 |                "name": "ЖК «Орбита»",
                 |                "id": "17723"
                 |            },
                 |            "dealStatus": "SALE"
                 |        }
               """.stripMargin
    val result: ProcessingReport = schema.validateUnchecked(JsonLoader.fromString(json), true)
    result.isSuccess should be(false)

    val seq = result.asScala.toSeq.filter(_.getLogLevel == LogLevel.ERROR)

    seq.foreach(message => {
      System.out.println("Message " + Json.prettyPrint(Json.parse(message.asJson().toString)))
    })

    seq.size should be(2)
  }
}
