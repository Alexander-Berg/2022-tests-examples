package ru.yandex.realty.buildinginfo.converter

import com.google.protobuf.{BoolValue, StringValue, UInt32Value}
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.inside.yt.kosher.impl.ytree.builder.{YTree, YTreeBuilder}

@RunWith(classOf[JUnitRunner])
class ConvertImportBuildingFromYtNodeSpec extends FlatSpec with Matchers {

  behavior.of("ConvertImportBuildingFromYtNode")

  it should "convert porch_count" in {
    val expectedPorchCount = 123
    val ytNode = YTree
      .mapBuilder()
      .key("porch_count")
      .value(expectedPorchCount)
      .buildMap()
    val converted = ConvertImportBuildingFromYtNode(ytNode)
    converted.getPorchCount shouldBe UInt32Value.of(expectedPorchCount)
  }

  /*
  it should "convert building parts" in {
    val ytNode = YTree.mapBuilder()
      .key("parts").beginList()
        .beginMap()
          .key("is_residential").value(true)
          .key("height").value(123)
          .key("polygon").value("polygon ewkb hex")
        .endMap()
      .endList()
      .buildMap()
    val converted = ConvertImportBuildingFromYtNode(ytNode)
    converted.getPartsCount shouldBe 1
    converted.getParts(0).getIsResidential shouldBe BoolValue.of(true)
    converted.getParts(0).getPolygon shouldBe StringValue.of("polygon ewkb hex")
  }
   */

  /*
  it should "not fail when a string field is missing" in {
    val ytNode = YTree.mapBuilder()
      .key("parts").beginList()
        .beginMap()
          .key("is_residential").value(true)
        .endMap()
      .endList()
      .buildMap()
    val converted = ConvertImportBuildingFromYtNode(ytNode)
    converted.getPartsCount shouldBe 1
    converted.getParts(0).getIsResidential.getValue shouldBe true
    converted.getParts(0).hasPolygon shouldBe false
  }
   */

  it should "correctly handle nulls" in {
    val ytNode = YTree
      .mapBuilder()
      .key("porch_count")
      .entity()
      /*
      .key("parts").beginList()
        .beginMap()
          .key("is_residential").entity()
          .key("height").entity()
          .key("polygon").entity()
        .endMap()
      .endList()
       */
      .buildMap()
    val converted = ConvertImportBuildingFromYtNode(ytNode)
    converted.hasPorchCount shouldBe false
    /*
    converted.getPartsCount shouldBe 1
    converted.getParts(0).hasHeight shouldBe false
    converted.getParts(0).hasIsResidential shouldBe false
    converted.getParts(0).hasPolygon shouldBe false
   */
  }

}
