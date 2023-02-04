package ru.yandex.auto.vin.decoder.utils.enumerations

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters.ListHasAsScala

class VinUpdatePartSpec extends AnyFlatSpec with Matchers {

  "toString" should "return name of case" in {
    VinUpdatePart.AUTOCODE.toString() should be("AUTOCODE")
  }

  "withNameOpt" should "return value correctly" in {
    VinUpdatePart.withNameOpt("AUTOCODE") should be(Some(VinUpdatePart.AUTOCODE))
    VinUpdatePart.withNameOpt("BLABLA") should be(None)
  }

  "getContexts" should "generate proto model correctly" in {
    val model = VinUpdatePart.contextsMessage

    model.getContextsCount should be(VinUpdatePart.values.size)

    val referenceValue = VinUpdatePart.values.find(_ == VinUpdatePart.AUTOCODE)
    val testValue = model.getContextsList.asScala.toList.find(_.getId == VinUpdatePart.AUTOCODE.toString)

    testValue.map(_.getId) should be(referenceValue.map(_.toString))
    testValue.map(_.getRuName) should be(referenceValue.map(r => VinUpdatePart.valueToVinUpdatePartVal(r).ruName))
  }
}
