package ru.yandex.realty.buildinginfo.converter

import org.junit.runner.RunWith
import org.scalatestplus.junit.JUnitRunner
import org.scalatest.prop.PropertyChecks
import org.scalatest.{FlatSpec, Matchers}
import ru.yandex.realty.buildinginfo.model.{BuildingError, BuildingErrorCode}
import ru.yandex.realty.model.gen.RealtyGenerators

/**
  * @author azakharov
  */
@RunWith(classOf[JUnitRunner])
class BuildingErrorProtoConverterSpec extends FlatSpec with Matchers with PropertyChecks with RealtyGenerators {

  "BuildingErrorProtoConverter" should "convert correctly to message and back to domain object" in {
    forAll(javaEnum(BuildingErrorCode.values().toSeq)) { errorCode =>
      forAll(readableString(1, 10)) { message =>
        val expected = BuildingError(errorCode, message)
        val proto = BuildingErrorProtoCoverter.toMessage(expected)
        val actual = BuildingErrorProtoCoverter.fromMessage(proto)
        actual shouldEqual (expected)
      }
    }
  }
}
