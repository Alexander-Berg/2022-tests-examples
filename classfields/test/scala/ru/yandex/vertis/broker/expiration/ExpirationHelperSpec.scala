package ru.yandex.vertis.broker.expiration

import org.scalacheck.Gen
import org.scalatestplus.scalacheck.ScalaCheckPropertyChecks
import ru.yandex.vertis.broker.model.common.PartitionPeriods
import vertis.zio.test.ZioSpecBase

/** @author kusaeva
 */
class ExpirationHelperSpec extends ZioSpecBase with ScalaCheckPropertyChecks {
  private val daysGen = Gen.long.map(_.toInt).filter(_ > 0)

  "ExpirationHelper" should {
    "not apply expiration for partitioning > 1d" in {

      ExpirationHelper
        .getExpireInDays(true, PartitionPeriods.byYear, 30)
        .isEmpty shouldBe true

      ExpirationHelper
        .getExpireInDays(true, PartitionPeriods.byMonth, 30)
        .isEmpty shouldBe true

      ExpirationHelper
        .getExpireInDays(true, PartitionPeriods.byDay, 30)
        .isEmpty shouldBe false
    }

    "return passed expiration for production" in {
      forAll(daysGen) { days =>
        ExpirationHelper
          .getExpireInDays(true, PartitionPeriods.byDay, days)
          .get shouldBe days
      }
    }

    "return min expiration for testing" in {
      forAll(daysGen) { days =>
        ExpirationHelper
          .getExpireInDays(false, PartitionPeriods.byDay, days)
          .get shouldBe Math.min(ExpirationHelper.DefaultExpireInDaysForTesting, days)
      }
    }

    "return min expiration for testing when passed 0" in {

      ExpirationHelper
        .getExpireInDays(false, PartitionPeriods.byDay, 0)
        .get shouldBe ExpirationHelper.DefaultExpireInDaysForTesting
    }
  }
}
