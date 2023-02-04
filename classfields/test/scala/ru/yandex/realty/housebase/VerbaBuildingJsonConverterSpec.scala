package ru.yandex.realty.housebase

import com.google.protobuf.StringValue
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsArray, JsNull, JsObject, JsValue, Json}
import ru.yandex.realty.SpecBase
import ru.yandex.realty.admin.proto.api.building.BuildingData
import ru.yandex.realty.clients.verba.{VerbaDictionaries, VerbaDictionary, VerbaTerm}

@RunWith(classOf[JUnitRunner])
class VerbaBuildingJsonConverterSpec extends SpecBase {

  private val verbaContext: VerbaDictionaries = prepareVerbaContext

  "VerbaBuildingJsonConverter" should {
    "add 756086 buildingEpoch to json correctly" in {
      val building = BuildingData
        .newBuilder()
        .setBuildingEpoch(StringValue.of("756086"))
        .build()

      val jsValue: JsValue = VerbaBuildingJsonConverter.toJson(building, verbaContext)

      val expected = Json.obj(
        "id" -> "756086".hashCode,
        "code" -> "756086",
        "name" -> "Дома 1991-2014"
      )

      (jsValue \ "buildingEpoch").get shouldBe expected
    }
    "add 756067 buildingEpoch to json correctly" in {
      val building = BuildingData
        .newBuilder()
        .setBuildingEpoch(StringValue.of("756067"))
        .build()

      val jsValue: JsValue = VerbaBuildingJsonConverter.toJson(building, verbaContext)

      val expected = Json.obj(
        "id" -> "756067".hashCode,
        "code" -> "756067",
        "name" -> "Хрущевская эпоха"
      )

      (jsValue \ "buildingEpoch").get shouldBe expected
    }
    "do not add unknown buildingEpoch" in {
      val building = BuildingData
        .newBuilder()
        .setBuildingEpoch(StringValue.of("756000"))
        .build()

      val jsValue: JsValue = VerbaBuildingJsonConverter.toJson(building, verbaContext)

      (jsValue \ "buildingEpoch").toOption shouldBe empty
    }
    "do not add buildingEpoch if building does not have one" in {
      val building = BuildingData
        .newBuilder()
        .build()

      val jsValue: JsValue = VerbaBuildingJsonConverter.toJson(building, verbaContext)

      (jsValue \ "buildingEpoch").toOption shouldBe empty
    }
    "add BREZHNEV buildingEpoch to json correctly" in {
      val building = BuildingData
        .newBuilder()
        .setBuildingEpoch(StringValue.of("BUILDING_EPOCH_BREZHNEV"))
        .build()

      val jsValue: JsValue = VerbaBuildingJsonConverter.toJson(building, verbaContext)

      val expected = Json.obj(
        "id" -> "756074".hashCode,
        "code" -> "756074",
        "name" -> "Брежневская эпоха"
      )

      (jsValue \ "buildingEpoch").get shouldBe expected
    }

    "convert buildingEpoch from json correctly" in {
      val buildingEpochCode = "756074"

      val term = VerbaTerm(
        1L,
        "code",
        "name",
        "path",
        1L,
        Json.obj(
          "code" -> "code",
          "address" -> "address",
          "buildingEpoch" ->
            JsArray(
              Seq(
                Json.obj(
                  "code" -> buildingEpochCode
                )
              )
            )
        )
      )

      val building: BuildingData = VerbaBuildingJsonConverter.fromJson(term, verbaContext)

      building.getBuildingEpoch.getValue shouldBe buildingEpochCode
    }
  }

  private def prepareVerbaContext: VerbaDictionaries = {
    val terms = Seq(
      Tuple3("756005", "Дореволюционная Россия", JsNull),
      Tuple3("756058", "Сталинская эпоха и Большевики", new JsObject(Map("code" -> Json.toJson("STALIN")))),
      Tuple3("756067", "Хрущевская эпоха", new JsObject(Map("code" -> Json.toJson("KHRUSHCHEV")))),
      Tuple3("756074", "Брежневская эпоха", new JsObject(Map("code" -> Json.toJson("BREZHNEV")))),
      Tuple3("756081", "Дома 1982-1991", JsNull),
      Tuple3("756086", "Дома 1991-2014", JsNull),
      Tuple3("756094", "Сданные новостройки 2015-2017", JsNull),
      Tuple3("756098", "Строящиеся новостройки", JsNull)
    ).map { case (code, name, attrs) => VerbaTerm(code.hashCode, code, name, "", 1, attrs) }

    val buildingEpochs = VerbaDictionary(2L, "era_building", "buildingEpochs", "", 1L, terms)

    VerbaDictionaries(
      None,
      None,
      None,
      Some(buildingEpochs),
      None
    )
  }

}
