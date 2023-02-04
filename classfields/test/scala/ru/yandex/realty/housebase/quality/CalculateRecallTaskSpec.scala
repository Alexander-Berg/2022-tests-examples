package ru.yandex.realty.housebase.quality

import com.google.protobuf.UInt32Value
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.storagemodel.StoredBuilding
import ru.yandex.realty.housebase.quality.CalculateRecallTask.getFilledFields
import ru.yandex.realty.proto.offer.BuildingType.{BUILDING_TYPE_BRICK => BRICK, BUILDING_TYPE_UNKNOWN => UNKNOWN}
import ru.yandex.realty.util.Mappings._

@RunWith(classOf[JUnitRunner])
class CalculateRecallTaskSpec extends FlatSpec with Matchers {

  behavior.of("getFilledFields")

  private def building(setter: StoredBuilding.Builder => Any = _ => {}): StoredBuilding =
    StoredBuilding
      .newBuilder()
      .setAddress("addr")
      .setBuildingId(1L)
      .applySideEffect(b => setter(b))
      .build()

  it should "detect wrappers with non-default values as filled" in {
    getFilledFields(building(_.setBuildYear(UInt32Value.of(1)))).getBuildYear shouldBe true
  }

  it should "detect wrappers with default values as filled" in {
    getFilledFields(building(_.setBuildYear(UInt32Value.of(0)))).getBuildYear shouldBe true
  }

  it should "detect missing wrappers as unfilled" in {
    getFilledFields(building()).getBuildYear shouldBe false
  }

  it should "detect enums with non-default values as filled" in {
    getFilledFields(building(_.setBuildingType(BRICK))).getBuildingType shouldBe true
  }

  it should "detect enums with default values as unfilled" in {
    getFilledFields(building(_.setBuildingType(UNKNOWN))).getBuildingType shouldBe false
  }

  it should "detect unset enums as unfilled" in {
    getFilledFields(building()).getBuildingType shouldBe false
  }

}
