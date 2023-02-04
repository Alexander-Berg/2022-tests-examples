package ru.yandex.realty.buildinginfo.model.internal

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.storagemodel.StoredBuilding

import scala.collection.JavaConverters.asScalaBufferConverter

@RunWith(classOf[JUnitRunner])
class BuildingFillSpec extends FlatSpec with Matchers {

  behavior.of(classOf[BuildingFill].getName)

  it should "have fields that match StoredBuilding by names and ids" in {
    val sbDescriptor = StoredBuilding.getDescriptor
    for (fillField <- BuildingFill.getDescriptor.getFields.asScala) {
      val sbField = sbDescriptor.findFieldByNumber(fillField.getNumber)
      if (sbField == null) {
        fail(s"unexpected field id ${fillField.getNumber}")
      }
      sbField.getName shouldBe fillField.getName
    }
  }

}
