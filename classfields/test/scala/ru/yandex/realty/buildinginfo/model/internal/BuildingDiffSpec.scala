package ru.yandex.realty.buildinginfo.model.internal

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.storagemodel.StoredBuilding

import scala.collection.JavaConverters.asScalaBufferConverter

@RunWith(classOf[JUnitRunner])
class BuildingDiffSpec extends FlatSpec with Matchers {

  behavior.of(classOf[BuildingDiff].getName)

  it should "have fields that match StoredBuilding by names and ids" in {
    val sbDescriptor = StoredBuilding.getDescriptor
    for (diffField <- BuildingDiff.getDescriptor.getFields.asScala) {
      val sbField = sbDescriptor.findFieldByNumber(diffField.getNumber)
      if (sbField == null) {
        fail(s"unexpected field id ${diffField.getNumber}")
      }
      sbField.getName shouldBe diffField.getName
    }
  }

}
