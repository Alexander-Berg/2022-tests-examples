package ru.yandex.realty.model.serialization.json

import org.junit.runner.RunWith
import org.scalatest.{FlatSpec, Matchers}
import org.scalatestplus.junit.JUnitRunner
import play.api.libs.json.{JsString, JsSuccess}
import ru.yandex.realty.proto.offer.{BuildingState, ConstructionState}
import ru.yandex.vertis.protobuf.Options.jsonOmitPrefix

@RunWith(classOf[JUnitRunner])
class ProtoEnumJsonFormatTest extends FlatSpec with Matchers {

  behavior of classOf[ProtoEnumJsonFormat[_]].getCanonicalName

  it should "write and read an enum without json_omit_prefix" in {
    assert(BuildingState.getDescriptor.getOptions.getExtension(jsonOmitPrefix) == "")

    val format = new ProtoEnumJsonFormat[BuildingState]()
    val enumValue = BuildingState.BUILDING_STATE_BUILT
    val js = format.writes(enumValue)
    js shouldBe JsString("BUILDING_STATE_BUILT")
    format.reads(js) shouldBe JsSuccess(enumValue)
  }

  it should "write and read an enum with json_omit_prefix" in {
    assert(ConstructionState.getDescriptor.getOptions.getExtension(jsonOmitPrefix) == "CONSTRUCTION_STATE_")

    val format = new ProtoEnumJsonFormat[ConstructionState]()
    val enumValue = ConstructionState.CONSTRUCTION_STATE_HAND_OVER
    val js = format.writes(enumValue)
    js shouldBe JsString("HAND_OVER")
    format.reads(js) shouldBe JsSuccess(enumValue)
  }

}
