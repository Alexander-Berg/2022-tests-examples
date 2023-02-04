package ru.yandex.vertis.curator.recipes.map

import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import org.scalatest.WordSpec
import org.scalatest.Matchers
import scala.util.Success

class ValueSerializerSpec extends WordSpec with Matchers with ScalaCheckPropertyChecks {

  "LongValueSerializer" should {
    "preserve values over round-trip" in forAll { value: Long =>
      val result = LongValueSerializer.deserialize(LongValueSerializer.serialize(value))
      result should be(Success(value))
    }
  }
}

