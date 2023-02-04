package ru.yandex.realty.housebase.quality

import com.google.protobuf.UInt32Value
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.internal.BuildingDiff
import ru.yandex.realty.buildinginfo.model.internal.BuildingDiff.Indicator
import ru.yandex.realty.buildinginfo.model.internal.BuildingDiff.Indicator.{DIFFERENT, SAME, UNKNOWN}
import ru.yandex.realty.buildinginfo.model.storagemodel.StoredBuilding
import ru.yandex.realty.housebase.quality.CalculatePrecisionTask.compare
import ru.yandex.realty.proto.offer.BuildingType.{BUILDING_TYPE_BLOCK => BLOCK, BUILDING_TYPE_BRICK => BRICK}
import ru.yandex.realty.util.Mappings._

@RunWith(classOf[JUnitRunner])
class CalculatePrecisionTaskSpec extends FlatSpec with Matchers {

  behavior.of("compare")

  private def suite(
    thing: String,
    set1: StoredBuilding.Builder => Any,
    set2: StoredBuilding.Builder => Any,
    setIndicator: (BuildingDiff.Builder, Indicator) => Any
  ): Unit = {

    def building(setter: StoredBuilding.Builder => Any = _ => {}): StoredBuilding =
      StoredBuilding
        .newBuilder()
        .setAddress("addr")
        .setBuildingId(1L)
        .applySideEffect(b => setter(b))
        .build()

    def diff(i: Indicator): BuildingDiff =
      BuildingDiff
        .newBuilder()
        .applySideEffect(b => setIndicator(b, i))
        .build()

    it should s"correctly detect equal ${thing}s" in {
      compare(building(set1), building(set1)) shouldBe diff(SAME)
    }

    it should s"correctly detect unequal ${thing}s" in {
      compare(building(set1), building(set2)) shouldBe diff(DIFFERENT)
    }

    it should s"not compare if only base $thing is missing" in {
      compare(building(), building(set2)) shouldBe diff(UNKNOWN)
    }

    it should s"not compare if only golden $thing is missing" in {
      compare(building(set1), building()) shouldBe diff(UNKNOWN)
    }

    it should s"not compare if both ${thing}s are missing" in {
      compare(building(), building()) shouldBe diff(UNKNOWN)
    }

  }

  suite("wrapper", _.setBuildYear(UInt32Value.of(1)), _.setBuildYear(UInt32Value.of(2)), _.setBuildYear(_))

  suite("enum", _.setBuildingType(BRICK), _.setBuildingType(BLOCK), _.setBuildingType(_))

}
