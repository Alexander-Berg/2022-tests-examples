package ru.yandex.vertis.billing.util

import java.util.UUID
import org.scalatest.Assertion
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpec
import ru.yandex.vertis.billing.model_core.EpochWithId
import ru.yandex.vertis.billing.util.KeyValueSerDe.EpochWithIdKeyValueSerDe

/**
  * Spec on [[EpochWithIdKeyValueSerDe]].
  * @author ruslansd
  */
class EpochWithIdKeyValueSerDeSpec extends AnyWordSpec with Matchers {

  private val serDe = EpochWithIdKeyValueSerDe

  "EpochWithIdKeyValueSerDe" should {
    "correctly parse only epoch" in {
      val epoch = DateTimeUtils.now().getMillis
      val epochWithId = EpochWithId(epoch, None)
      test(epochWithId)
    }

    "correctly parse epoch with id" in {
      val epoch = DateTimeUtils.now().getMillis
      val id = UUID.randomUUID().toString
      val epochWithId = EpochWithId(epoch, Some(id))
      test(epochWithId)
    }

    "correctly handle id with separator" in {
      val epoch = DateTimeUtils.now().getMillis
      val id = "______test_____test_"
      val epochWithId = EpochWithId(epoch, Some(id))
      test(epochWithId)
    }

    "fail on wrong serialized value" in {
      val wrongFormatValue = "aaabb"
      intercept[IllegalArgumentException] {
        serDe.from(wrongFormatValue)
      }
    }
  }

  private def test(epoch: EpochWithId): Assertion = {
    val serialized = serDe.to(epoch)
    serDe.from(serialized) shouldBe epoch
  }

}
