package ru.yandex.realty.buildinginfo.converter

import com.google.protobuf.UInt32Value
import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree
import ru.yandex.inside.yt.kosher.impl.ytree.builder.YTree.stringNode
import ru.yandex.inside.yt.kosher.ytree.{YTreeIntegerNode, YTreeNode}
import ru.yandex.realty.building.model.BuildingEpoch
import ru.yandex.realty.buildinginfo.converter.ConvertProtoFromYtNode.Caster
import ru.yandex.realty.buildinginfo.model.storagemodel.StoredBuilding
import ru.yandex.realty.proto.offer.BuildingType

@RunWith(classOf[JUnitRunner])
class ConvertProtoFromYtNodeSpec extends FlatSpec with Matchers {

  behavior.of(ConvertProtoFromYtNode.getClass.getName)

  import ConvertProtoFromYtNode.cast

  it should "discard primitive wrappers with default values" in {
    cast(
      YTree.integerNode(0),
      StoredBuilding.getDescriptor.findFieldByName("porch_count"),
      Map.empty
    ) shouldBe Some(UInt32Value.of(0))
    cast(
      YTree.integerNode(0),
      StoredBuilding.getDescriptor.findFieldByName("porch_count"),
      Map("porch_count" -> Caster(discardDefaults = true))
    ) shouldBe None
  }

  it should "discard enum default values" in {
    val caster = Some((n: YTreeNode) => Some(BuildingEpoch.forNumber(n.asInstanceOf[YTreeIntegerNode].getInt)))

    cast(
      YTree.integerNode(0),
      StoredBuilding.getDescriptor.findFieldByName("building_epoch"),
      Map("building_epoch" -> Caster(caster))
    ) shouldBe Some(BuildingEpoch.BUILDING_EPOCH_UNKNOWN.getValueDescriptor)

    cast(
      YTree.integerNode(0),
      StoredBuilding.getDescriptor.findFieldByName("building_epoch"),
      Map("building_epoch" -> Caster(caster, discardDefaults = true))
    ) shouldBe None
  }

  behavior.of("castEnumFromName")

  import ConvertProtoFromYtNode.castEnumFromName

  it should "convert names" in {
    castEnumFromName(BuildingType.getDescriptor)(stringNode("BUILDING_TYPE_BRICK")) shouldBe
      Some(BuildingType.BUILDING_TYPE_BRICK.getValueDescriptor)
  }

  it should "add and remove prefixes" in {
    def test(rm: String, add: String, value: String): Unit = {
      castEnumFromName(BuildingType.getDescriptor, rm, add)(stringNode(value)) shouldBe
        Some(BuildingType.BUILDING_TYPE_BRICK.getValueDescriptor)
    }

    test("x", "", "xBUILDING_TYPE_BRICK")
    test("", "BUILDING_TYPE_", "BRICK")
    test("x", "BUILDING_TYPE_", "xBRICK")
  }

}
